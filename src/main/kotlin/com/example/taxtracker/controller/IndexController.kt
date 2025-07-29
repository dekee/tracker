package com.example.taxtracker.controller


import com.example.taxtracker.model.Parcel
import com.example.taxtracker.model.StatusChange
import com.example.taxtracker.repository.ParcelRepository
import com.example.taxtracker.repository.StatusChangeRepository
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
