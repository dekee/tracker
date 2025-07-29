package com.example.taxtracker.controller

import com.example.taxtracker.service.TaxStatusDbTracker
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TriggerController(val tracker: TaxStatusDbTracker) {

    @GetMapping("/run-now")
    fun runNow(): ResponseEntity<String> {
        tracker.runDaily()
        return ResponseEntity.ok("Run complete")
    }
}
