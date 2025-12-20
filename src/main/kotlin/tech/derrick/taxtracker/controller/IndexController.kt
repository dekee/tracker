package tech.derrick.taxtracker.controller



import tech.derrick.taxtracker.repository.ParcelRepository
import tech.derrick.taxtracker.repository.StatusChangeRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.text.DecimalFormat

@Controller
class IndexController(
    private val parcelRepository: ParcelRepository,
    private val statusChangeRepository: StatusChangeRepository
) {

    @GetMapping("/")
    fun index(model: Model): String {
        val parcels = parcelRepository.findAll()
            .sortedWith(
                compareBy<tech.derrick.taxtracker.model.Parcel> {
                    if (it.status == "SOLD. Call the office for redemption balance information") 0 else 1
                }.thenBy { it.status }
            )
        val changes = statusChangeRepository.findAll()

        // Total of balances for parcels whose status indicates they are SOLD
        val soldTotal = parcels
            .filter { it.status.contains("SOLD", ignoreCase = true) }
            .mapNotNull { it.balance.toDoubleOrNull() }
            .sum()

        // Format numbers as currency strings (e.g. $9,051.44)
        val decimalFormat = DecimalFormat("#,##0.00")
        val soldTotalDisplay = if (soldTotal != 0.0) {
            "$" + decimalFormat.format(soldTotal)
        } else {
            "$0.00"
        }

        val viewParcels = parcels.map { p ->
            val formattedBalance = p.balance.toDoubleOrNull()
                ?.let { "$" + decimalFormat.format(it) }
                ?: p.balance
            p.copy(balance = formattedBalance)
        }

        model.addAttribute("parcels", viewParcels)
        model.addAttribute("changes", changes)
        model.addAttribute("soldTotalDisplay", soldTotalDisplay)
        return "index"
    }
}
