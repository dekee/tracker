package tech.derrick.taxtracker.repository

import tech.derrick.taxtracker.model.Parcel
import org.springframework.data.jpa.repository.JpaRepository

interface ParcelRepository : JpaRepository<Parcel, String>