package com.pakdrive.models

data class RequestModel(
    val customerUid: String="",
    val far:String="",
    val pickUpLatLang:String="",
    val destinationLatLang:String="",
    val comment:String="",
    val timeTaken:String="",
    val distance:String="",
)