package tech.derrick.taxtracker

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class TaxHistory(
    @Id val id: String,
    val parcelId: String,
    val status: String
)