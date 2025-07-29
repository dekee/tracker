package com.example.taxtracker.repository

import com.example.taxtracker.model.StatusChange
import org.springframework.data.jpa.repository.JpaRepository

interface StatusChangeRepository : JpaRepository<StatusChange, String>