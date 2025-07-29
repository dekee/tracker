package com.example.taxtracker.repository

import com.example.taxtracker.model.Parcel
import org.springframework.data.jpa.repository.JpaRepository

interface ParcelRepository : JpaRepository<Parcel, String>