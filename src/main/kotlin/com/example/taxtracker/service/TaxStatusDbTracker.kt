package com.example.taxtracker.service



import com.example.taxtracker.model.Parcel
import com.example.taxtracker.model.StatusChange
import com.example.taxtracker.repository.ParcelRepository
import com.example.taxtracker.repository.StatusChangeRepository
import okhttp3.*
import org.jsoup.Jsoup
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Service
class TaxStatusDbTracker(
    private val parcelRepository: ParcelRepository,
    private val statusChangeRepository: StatusChangeRepository
) {
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
            val file = File("parcels.txt")
            if (!file.exists()) {
                println("🚫 parcels.txt not found.")
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
                        val desc = cols[1].text().trim().uppercase()
                        val amount = cols[2].text().trim().replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0

                        if (date == targetDate) {
                            when {
                                desc.contains("PAYMENT") -> {paymentTotal += amount; println("@@@@ !!!!$paymentTotal"); }
                                desc.contains("INTEREST CHARGE") -> {interestTotal += amount; println("@@@@ !!!!$interestTotal");}
                            }
                        }
                    }
                }

                val combinedBalance = (paymentTotal + interestTotal).takeIf { it > 0 }?.toString() ?: "N/A"

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

            println("✅ Imported all parcels with full details from parcels.txt")
        }
    }


    @Scheduled(cron = "0 0 * * * *")
    fun runDaily() {
        val targetDate = "6/4/2025"

        loadInitialParcelsIfNeeded()  // ✅ new line
        val parcels = parcelRepository.findAll()

        for (parcel in parcels) {
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


            val changed = parcel.status != newStatus && newStatus != placeholderStatus
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
                    val desc = cols[1].text().trim().uppercase()
                    val amount = cols[2].text().trim().replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0

                    if (date == targetDate) {
                        when {
                            desc.contains("PAYMENT") -> paymentTotal += amount
                            desc.contains("INTEREST CHARGE") -> interestTotal += amount
                        }
                    }
                }
            }

            val combinedBalance = (paymentTotal + interestTotal).takeIf { it > 0 }?.toString() ?: "N/A"
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
                        previousStatus = parcel.status,
                        newStatus = newStatus,
                        ownerName = parcel.ownerName
                    )
                )
            }
        }
    }

    private fun fetchDetails(parcelId: String, nameMatch: String): String? {
        val searchUrl = "https://snstaxpayments.com/Search?" +
                "searchFor1=${URLEncoder.encode(parcelId, "UTF-8")}&" +
                "searchType=1&taxyear=2024&client=stcharles&" +
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
