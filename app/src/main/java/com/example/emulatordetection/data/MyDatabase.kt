package com.example.emulatordetection.data

import android.view.Display
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Device constructor(
    @PrimaryKey var model: String,
    var hardware: String,
    var display: String,
    var manufacturer: String
)