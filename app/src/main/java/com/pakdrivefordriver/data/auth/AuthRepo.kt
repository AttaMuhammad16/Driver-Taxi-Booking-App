package com.pakdrivefordriver.data.auth

import com.pakdrive.MyResult


interface AuthRepo {
    suspend fun registerUser(email:String,password:String,callback:(MyResult)->Unit)
    suspend fun loginUser(email:String, password:String, callback:(MyResult)->Unit)
    suspend fun checkVerification(callback: (MyResult) -> Unit)

}