package tech.derrick.taxtracker.controller

import tech.derrick.taxtracker.service.TaxStatusDbTracker
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TriggerController(val tracker: TaxStatusDbTracker) {
    private val log = LoggerFactory.getLogger(TriggerController::class.java)

    @GetMapping("/run-now")
    fun runNow(): ResponseEntity<String> {
        log.info("▶️ /api/run-now triggered")
        tracker.runDaily()
        log.info("✅ /api/run-now finished")
        return ResponseEntity.ok("Run complete")
    }
}
