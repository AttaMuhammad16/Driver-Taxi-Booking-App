package com.pakdrivefordriver.models

data class AcceptModel(
    var customerUid: String = "",
    val driverUid: String = "",
    val start:Boolean=false
)
