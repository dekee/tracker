package tech.derrick.taxtracker.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class Parcel(
    @Id val parcelId: String,
    val ownerName: String,
    var status: String,
    var balance: String,
    var parcelNumber: String,
    var address: String,
    var legalDescription: String
)
