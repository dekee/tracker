package tech.derrick.taxtracker.controller



import tech.derrick.taxtracker.repository.ParcelRepository
import tech.derrick.taxtracker.repository.StatusChangeRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController(
    private val parcelRepository: ParcelRepository,
    private val statusChangeRepository: StatusChangeRepository
) {

    @GetMapping("/")
    fun index(model: Model): String {
        val parcels = parcelRepository.findAll().sortedBy { it.status }
        val changes = statusChangeRepository.findAll()
        model.addAttribute("parcels", parcels)
        model.addAttribute("changes", changes)
        return "index"
    }
}
