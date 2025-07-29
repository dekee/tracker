package com.example.taxtracker.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class StatusChange(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val parcelId: String,
    val previousStatus: String,
    val newStatus: String,
    val ownerName: String
)