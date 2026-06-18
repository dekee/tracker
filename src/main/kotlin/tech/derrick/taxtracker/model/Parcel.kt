package tech.derrick.taxtracker.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Transient

@Entity
data class Parcel(
    @Id val parcelId: String,
    val ownerName: String,
    var status: String,
    var balance: String,
    var parcelNumber: String,
    var address: String,
    var legalDescription: String,
    // All parcels are in St. Charles Parish, LA. City/ZIP and the Redfin
    // estimate aren't available from the tax site, so they're seeded statically
    // per parcel (see ParcelEnrichment).
    var city: String? = null,
    var zipCode: String? = null,
    var redfinEstimate: String? = null
) {
    // Redfin page link, derived from static data and set in the controller for the
    // view; not persisted (always available from ParcelEnrichment by parcelId).
    @Transient
    var redfinUrl: String? = null
}
