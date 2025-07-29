package com.example.taxtracker.controller

import com.example.taxtracker.model.Parcel
import com.example.taxtracker.repository.ParcelRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ParcelController(private val parcelRepository: ParcelRepository) {


    @GetMapping("/parcels")
    fun getAllParcels(): List<Parcel> = parcelRepository.findAll()
}