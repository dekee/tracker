package tech.derrick.taxtracker.service



import tech.derrick.taxtracker.model.Parcel
import tech.derrick.taxtracker.model.StatusChange
import tech.derrick.taxtracker.repository.ParcelRepository
import tech.derrick.taxtracker.repository.StatusChangeRepository
import okhttp3.*
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Service
class TaxStatusDbTracker(
    private val parcelRepository: ParcelRepository,
    private val statusChangeRepository: StatusChangeRepository,
    private val pushoverNotifier: PushoverNotifier
) {
    private val log = LoggerFactory.getLogger(TaxStatusDbTracker::class.java)
    private val parcelsFilePath: String = System.getenv("PARCELS_FILE") ?: "parcels.txt"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private val headers = Headers.Builder()
        .add("Accept", "text/html, */*; q=0.01")
        .add("User-Agent", "Mozilla/5.0")
        .add("X-Requested-With", "XMLHttpRequest")
        .add("Referer", "https://snstaxpayments.com/stcharles?terms=yes")
        .build()

    private val placeholderStatus = "Call the office for redemption balance information"

    fun loadInitialParcelsIfNeeded() {
        val targetDate = "6/4/2025"

        if (parcelRepository.count() == 0L) {
            val file = File(parcelsFilePath)
            if (!file.exists()) {
                log.warn("🚫 parcels file not found at '{}'. Set PARCELS_FILE or mount parcels.txt into the container.", parcelsFilePath)
                return
            }

            file.forEachLine { line ->
                val (parcelId, ownerRaw) = line.split(":", limit = 2).map { it.trim() }
                val nameMatch = ownerRaw.split(",")[0].trim()

                val html = fetchDetails(parcelId, nameMatch) ?: return@forEachLine
                val doc = Jsoup.parse(html)

                val taxStatus = doc.selectFirst("div.row.taxsale")
                    ?.text()?.replace("Tax Sale Status:", "")?.trim() ?: "UNKNOWN"

                /*val balance = doc.select("label:contains(Balance)")
                    .first()?.nextSibling()?.toString()?.trim() ?: "N/A"*/
                val balanceLabel = doc.select("label:contains(Paid)").first()
                val balance = balanceLabel?.parent()?.ownText()?.replace("Paid:", "")?.trim() ?: "N/A"

                val legal = doc.select("h4:contains(Legal)")
                    .first()?.nextElementSibling()?.text()?.trim() ?: "N/A"

                val parcelSection = doc.select("h4:contains(Parcels)").firstOrNull()?.nextElementSibling()
                val firstRowTds = parcelSection?.select("tbody tr")?.firstOrNull()?.select("td")

                val parcelNumber = firstRowTds?.getOrNull(0)?.text()?.trim() ?: ""
                val address = firstRowTds?.getOrNull(1)?.text()?.trim() ?: ""

                val historyRows = doc.select("h4:contains(History)")
                    .firstOrNull()
                    ?.nextElementSibling()
                    ?.select("tbody tr") ?: emptyList()

                var paymentTotal = 0.0
                var interestTotal = 0.0

                for (row in historyRows) {
                    val cols = row.select("td")
                    if (cols.size >= 3) {
                        val date = cols[0].text().trim()
                        val descRaw = cols[1].text().trim()
                        val desc = descRaw.replace("\\s+".toRegex(), " ").trim().uppercase()
                        val amount = cols[2].text().trim().replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0

                        if (date == targetDate) {
                            when {
                                // Sum total payment as the absolute value of any payment rows
                                desc == "PAYMENT" || desc == "INTEREST PAYMENT" -> {
                                    paymentTotal += abs(amount)
                                    log.info("@@@@ paymentTotal (initial load) = {}", paymentTotal)
                                }

                                desc == "INTEREST CHARGE" -> {
                                    interestTotal += amount
                                    log.info("@@@@ interestTotal (initial load) = {}", interestTotal)
                                }
                            }
                        }
                    }
                }

                // Store total payment (and interest if you still want it) as the balance field
                val combinedBalance = paymentTotal
                    .takeIf { it != 0.0 }
                    ?.toString()
                    ?: "N/A"

                parcelRepository.save(
                    Parcel(
                        parcelId = parcelId,
                        ownerName = ownerRaw,
                        status = taxStatus,
                        balance = combinedBalance,
                        parcelNumber = parcelNumber,
                        address = address,
                        legalDescription = legal
                    )
                )
            }

            log.info("✅ Imported all parcels with full details from parcels.txt")
        }
    }


    @Scheduled(cron = "0 0 * * * *")
    fun runDaily() {
        val targetDate = "6/4/2025"

        log.info("▶️ runDaily starting (parcelsFilePath='{}')", parcelsFilePath)
        loadInitialParcelsIfNeeded()  // ✅ new line
        val parcels = parcelRepository.findAll()
        log.info("📦 runDaily processing {} parcels", parcels.size)

        for (parcel in parcels) {
            val oldStatus = parcel.status
            val html = fetchDetails(parcel.parcelId, parcel.ownerName.split(",")[0]) ?: continue
            val doc = Jsoup.parse(html)

            val newStatus = doc.selectFirst("div.row.taxsale")?.text()
                ?.replace("Tax Sale Status:", "")?.trim() ?: "UNKNOWN"
            //val balance = doc.select("label:contains(Balance)").first()?.nextSibling()?.toString()?.trim() ?: "N/A"
            val balanceLabel = doc.select("label:contains(Paid)").first()
            val balance = balanceLabel?.parent()?.ownText()?.replace("Paid:", "")?.trim() ?: "N/A"

            val legal = doc.select("h4:contains(Legal)")?.first()?.nextElementSibling()?.text()?.trim() ?: ""

            val parcelSection = doc.select("h4:contains(Parcels)").firstOrNull()?.nextElementSibling()
            val firstRowTds = parcelSection?.select("tbody tr")?.firstOrNull()?.select("td")

            val parcelNumber = firstRowTds?.getOrNull(0)?.text()?.trim() ?: ""
            val address = firstRowTds?.getOrNull(1)?.text()?.trim() ?: ""


            val changed = oldStatus != newStatus && newStatus != placeholderStatus
            val historyRows = doc.select("h4:contains(History)")
                .firstOrNull()
                ?.nextElementSibling()
                ?.select("tbody tr") ?: emptyList()

            var paymentTotal = 0.0
            var interestTotal = 0.0

            for (row in historyRows) {
                val cols = row.select("td")
                if (cols.size >= 3) {
                    val date = cols[0].text().trim()
                    val descRaw = cols[1].text().trim()
                    val desc = descRaw.replace("\\s+".toRegex(), " ").trim().uppercase()
                    val amount = cols[2].text().trim().replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0

                    if (date == targetDate) {
                        when {
                            // Sum total payment as the absolute value of any payment rows
                            desc == "PAYMENT" || desc == "INTEREST PAYMENT" -> {
                                paymentTotal += abs(amount)
                                log.info("@@@@ paymentTotal (runDaily) = {}", paymentTotal)
                            }

                            desc == "INTEREST CHARGE" -> {
                                interestTotal += amount
                                log.info("@@@@ interestTotal (runDaily) = {}", interestTotal)
                            }
                        }
                    }
                }
            }

            // Store total payment as the balance field for the target date
            val combinedBalance = paymentTotal
                .takeIf { it != 0.0 }
                ?.toString()
                ?: "N/A"
            parcel.status = newStatus
            parcel.balance = combinedBalance
            parcel.legalDescription = legal
            parcel.parcelNumber = parcelNumber
            parcel.address = address
            parcelRepository.save(parcel)

            if (changed) {
                statusChangeRepository.save(
                    StatusChange(
                        parcelId = parcel.parcelId,
                        previousStatus = oldStatus,
                        newStatus = newStatus,
                        ownerName = parcel.ownerName
                    )
                )
                pushoverNotifier.notifyStatusChange(
                    parcelId = parcel.parcelId,
                    ownerName = parcel.ownerName,
                    previousStatus = oldStatus,
                    newStatus = newStatus
                )
            }
        }
        log.info("✅ runDaily complete")
    }

    private fun fetchDetails(parcelId: String, nameMatch: String): String? {
        val searchUrl = "https://snstaxpayments.com/Search?" +
                "searchFor1=${URLEncoder.encode(parcelId, "UTF-8")}&" +
                "searchType=1&taxyear=2025&client=stcharles&" +
                "nameMatch=${URLEncoder.encode(nameMatch, "UTF-8")}"

        val searchRequest = Request.Builder().url(searchUrl).headers(headers).build()
        val searchResponse = client.newCall(searchRequest).execute()
        Thread.sleep(1500)
        val searchHtml = searchResponse.body?.string() ?: return null

        val doc = Jsoup.parse(searchHtml)
        val button = doc.selectFirst("button.detailsLink") ?: return null

        val dataId = button.attr("data-id")
        val dataGuid = button.attr("data-guid")
        Thread.sleep(1500)
        val postRequest = Request.Builder()
            .url("https://snstaxpayments.com/Details")
            .headers(headers)
            .post(FormBody.Builder()
                .add("client", "stcharles")
                .add("id", dataId)
                .add("guid", dataGuid)
                .build())
            .build()

        val postResponse = client.newCall(postRequest).execute()
        return postResponse.body?.string()
    }
}
