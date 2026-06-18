package tech.derrick.taxtracker.service

import tech.derrick.taxtracker.repository.ParcelRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Backfills city, ZIP, and Redfin estimate onto parcels already in the database
 * using the static [ParcelEnrichment] table. Runs once at startup and is idempotent —
 * it only writes when a value is missing or has changed.
 */
@Component
class ParcelEnricher(
    private val parcelRepository: ParcelRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(ParcelEnricher::class.java)

    override fun run(args: ApplicationArguments?) {
        var updated = 0
        for (parcel in parcelRepository.findAll()) {
            val enrich = ParcelEnrichment.byParcelId[parcel.parcelId] ?: continue
            if (parcel.city == enrich.city &&
                parcel.zipCode == enrich.zipCode &&
                parcel.redfinEstimate == enrich.redfinEstimate
            ) {
                continue
            }
            parcel.city = enrich.city
            parcel.zipCode = enrich.zipCode
            parcel.redfinEstimate = enrich.redfinEstimate
            parcelRepository.save(parcel)
            updated++
        }
        if (updated > 0) {
            log.info("🏷️ Enriched {} parcels with city/ZIP/Redfin estimate", updated)
        }
    }
}
