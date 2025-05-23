package com.pakdrive.models


data class CustomerModel(
    var uid:String?=null,
    var userName:String="",
    var email:String="",
    var password:String="",
    var phoneNumber:String="",
    var address:String="",
    var profileImage:String="",
    var customerFCMToken:String="",
    var startLatLang:String="",
    var endLatLang:String="",
    var pickUpPointName:String="",
    var destinationName:String=""
)