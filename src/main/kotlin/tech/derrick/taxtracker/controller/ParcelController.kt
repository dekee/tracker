package tech.derrick.taxtracker.controller

import tech.derrick.taxtracker.model.Parcel
import tech.derrick.taxtracker.repository.ParcelRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ParcelController(private val parcelRepository: ParcelRepository) {


    @GetMapping("/parcels")
    fun getAllParcels(): List<Parcel> = parcelRepository.findAll()
}