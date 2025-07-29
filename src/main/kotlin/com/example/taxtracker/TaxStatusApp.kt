package com.example.taxtracker

import com.example.taxtracker.service.TaxStatusDbTracker
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class TaxStatusApp

fun main(args: Array<String>) {
    runApplication<TaxStatusApp>(*args) /*
    val context = runApplication<TaxStatusApp>(*args)
    val tracker = context.getBean(TaxStatusDbTracker::class.java)
    tracker.runDaily() // force the scheduled method to run immediately*/
}