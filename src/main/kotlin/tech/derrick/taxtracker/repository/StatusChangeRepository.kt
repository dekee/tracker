package tech.derrick.taxtracker.repository

import tech.derrick.taxtracker.model.StatusChange
import org.springframework.data.jpa.repository.JpaRepository

interface StatusChangeRepository : JpaRepository<StatusChange, String>