package com.rnsamsunghealthdataapi.model
 
 data class HeartRate(
    var min: Float,
    var max: Float,
    var avg: Float,
    var startTime: String,
    var endTime: String,
    var count: Int
)