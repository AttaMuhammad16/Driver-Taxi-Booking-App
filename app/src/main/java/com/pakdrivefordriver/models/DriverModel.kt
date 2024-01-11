package com.pakdrivefordriver.models

data class DriverModel(
    var uid:String?="",
    var docUrl: ArrayList<String> = arrayListOf(),
    var profileImageUrl: String = "",
    var userName: String = "",
    var email: String = "",
    var password: String = "",
    var phoneNumber: String = "",
    var address: String = "",
    var lat:Double?=0.0,
    var lang:Double?=0.0,
    var driverFCMToken:String="",
    var carDetails:String="",
    var totalRating:Int=0,
    var totalPersonRatings:Int=0,
    var availabe:Boolean=false,
    var verificationProcess:Boolean=false
)
