package com.pakdrivefordriver.services.notification

import android.provider.Telephony.TextBasedSmsColumns.BODY
import android.util.Log
import com.pakdrivefordriver.MyConstants.CLICKACTION
import com.pakdrivefordriver.MyConstants.DRIVERUID
import com.pakdrivefordriver.MyConstants.TITLE
import com.pakdrivefordriver.MyConstants.approvedConst
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


object SendNotification{
    var key="key=AAAA7oDFO3c:APA91bGM6-AyNBT_j5fpZCJkTSu92ymRGkobdlbSpR7jB1AFPUSJoIn3ncinJVDi7h2bnvOe4rkBpC8T1aygPhFq2gM_ZnaCh8-PNtv3on00hq_p6BNxLDP13UisY-Oif47Z7TmT3j77"

    suspend fun sendCancellationNotification(title: String, des: String, customerToken:String, approved:String) {

        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonNotif = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val jsonData = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val androidConfig = JSONObject().apply {
            put("ttl", "3600s")  // Time-to-live set to 1 hour(expire time)
        }

        val wholeObj = JSONObject().apply {
            put("to", customerToken)
            put("notification", jsonNotif)
            put("data", jsonData)
            put("priority", "high")
            put("android", androidConfig)
            put("collapse_key", "update") // (updated notification) Example collapse key, change "update" to a suitable key for your app
        }

        sendViaHttps(mediaType, wholeObj, client)

    }
    suspend fun sendRideCompletedNotification(title: String, des: String, customerToken:String, approved:String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonNotif = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val jsonData = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val androidConfig = JSONObject().apply {
            put("ttl", "3600s")  // Time-to-live set to 1 hour(expire time)
        }

        val wholeObj = JSONObject().apply {
            put("to", customerToken)
            put("notification", jsonNotif)
            put("data", jsonData)
            put("priority", "high")
            put("android", androidConfig)
            put("collapse_key", "update") // (updated notification) Example collapse key, change "update" to a suitable key for your app
        }

        sendViaHttps(mediaType, wholeObj, client)

    }

    suspend fun sendPickUpNotification(title: String, des: String, customerToken:String, approved:String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonNotif = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val jsonData = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val androidConfig = JSONObject().apply {
            put("ttl", "3600s")  // Time-to-live set to 1 hour(expire time)
        }

        val wholeObj = JSONObject().apply {
            put("to", customerToken)
            put("notification", jsonNotif)
            put("data", jsonData)
            put("priority", "high")
            put("android", androidConfig)
            put("collapse_key", "update") // (updated notification) Example collapse key, change "update" to a suitable key for your app
        }

        sendViaHttps(mediaType, wholeObj, client)
    }


    fun sendViaHttps(mediaType: MediaType?, wholeObj:JSONObject, client: OkHttpClient){
        try {
            val requestBody = RequestBody.create(mediaType, wholeObj.toString())
            val request = Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .addHeader("Authorization", key)
                .addHeader("Content-type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("TAG", "onFailure:${e.message}")
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        val responseJson = JSONObject(responseBody ?: "{}")
                        val success = responseJson.optInt("success", 0)
                        if (success == 1) {
                            Log.i("TAG", "Notification sent successfully to ")
                        } else {
                            Log.e("TAG", "Failed to send notification. Response: $responseBody")
                        }
                    }catch (e:Exception){
                        Log.i("TAG", "onResponse:${e.message}")
                    }

                }
            })
        }catch (e:Exception){
            Log.i("TAG", "sendCancellationNotification: ${e.message}")
        }
    }

}
