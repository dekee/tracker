package tech.derrick.taxtracker.service

/** City, ZIP, Redfin Estimate, and (optional) Redfin page URL for a parcel. */
data class ParcelLocation(
    val city: String,
    val zipCode: String,
    val redfinEstimate: String,
    val redfinUrl: String? = null
)

/**
 * Static, per-parcel enrichment data. All parcels are in St. Charles Parish, LA.
 *
 * City/ZIP come from Redfin's address records (or USPS/parcel-area lookup for the
 * handful Redfin doesn't index). `redfinEstimate` is the property's Redfin Estimate;
 * it's "N/A" when Redfin publishes no estimate (vacant lots, commercial parcels, or
 * homes Redfin doesn't have a record for). `redfinUrl` links to the Redfin page and
 * is populated for the SOLD parcels (the dashboard links those rows); 714 East Easy
 * St has no individual Redfin listing, so it points at the Norco 70079 area page.
 * Keyed by parcelId.
 */
object ParcelEnrichment {
    val byParcelId: Map<String, ParcelLocation> = mapOf(
        // SOLD parcels — linked to Redfin on the dashboard
        "302200000946" to ParcelLocation("Destrehan", "70047", "$320,556", "https://www.redfin.com/LA/Destrehan/47-Belle-Helene-Dr-70047/home/85278874"), // 47 Belle Helene Dr
        "60720010031A" to ParcelLocation("Destrehan", "70047", "N/A", "https://www.redfin.com/LA/Destrehan/175-Annex-St-70047/home/85280422"),            // 175 Annex St
        "500300400002" to ParcelLocation("St. Rose", "70087", "$325,000", "https://www.redfin.com/LA/Saint-Rose/12-Horseshoe-Ln-70087/home/85284119"),   // 12 Horseshoe Ln
        "203500B00305" to ParcelLocation("Luling", "70070", "$224,204", "https://www.redfin.com/LA/Luling/232-Marcia-Dr-70070/home/85272605"),           // 232 Marcia Dr
        "45340170017D" to ParcelLocation("Paradis", "70080", "$56,695", "https://www.redfin.com/LA/Paradis/23-Bergeron-Ln-70080/home/195423294"),        // 23 Bergeron Ln (lot)
        "302103500012" to ParcelLocation("Norco", "70079", "N/A", "https://www.redfin.com/zipcode/70079"),                                                // 714 East Easy St (no Redfin listing)

        // Remaining parcels (not SOLD) — no Redfin link
        "302200001135" to ParcelLocation("Destrehan", "70047", "$346,718"), // 284 Dunleith Dr
        "607400100016" to ParcelLocation("Destrehan", "70047", "N/A"),       // 131 Westover Ln
        "50310000007B" to ParcelLocation("St. Rose", "70087", "$582,957"),   // 124 Oak Manor Ln
        "503100000037" to ParcelLocation("St. Rose", "70087", "$437,759"),   // 163 Oak Manor Ln
        "505200000049" to ParcelLocation("St. Rose", "70087", "$227,164"),   // 246 Riverview Dr
        "504000000026" to ParcelLocation("St. Rose", "70087", "N/A"),        // 716 Saint Rose Ave
        "504100400005" to ParcelLocation("St. Rose", "70087", "N/A"),        // 406 Oak St
        "551800000D-1" to ParcelLocation("St. Rose", "70087", "N/A"),        // 10362 Airline Hwy (driving range)
        "50160080001F" to ParcelLocation("Norco", "70079", "N/A"),           // 366 First St
        "603315100010" to ParcelLocation("Norco", "70079", "N/A")            // Louise St (lot)
    )
}
