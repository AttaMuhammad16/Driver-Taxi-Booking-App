package com.pakdrivefordriver.services.notification

import android.provider.Telephony.TextBasedSmsColumns.BODY
import android.util.Log
import com.pakdrivefordriver.MyConstants.CLICKACTION
import com.pakdrivefordriver.MyConstants.DRIVERUID
import com.pakdrivefordriver.MyConstants.TITLE
import com.pakdrivefordriver.MyConstants.approvedConst
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object SendNotification{
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

        val requestBody = RequestBody.create(mediaType, wholeObj.toString())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(requestBody)
            .addHeader("Authorization", "key=AAAAx5Jyo0U:APA91bEB1Z9IYIqrN7Tt6avCLOTcto6sLJurSg_JrFCEteF8LS4QKqrB_wMsuh1ZFDiUAlw2rnAS94QHonUtw9j_s5ayfsjFgCmv1xU4I7toSlzB82_mquaMT8M-Fdh20jnw2r0HANO3")
            .addHeader("Content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody ?: "{}")
                val success = responseJson.optInt("success", 0)
                if (success == 1) {
                    Log.i("TAG", "Notification sent successfully to $customerToken")
                } else {
                    Log.e("TAG", "Failed to send notification. Response: $responseBody")
                }
            }
        })
    }
    suspend fun sendRideCompletedNotification(title: String, des: String, customerToken:String, approved:String,driverUid:String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonNotif = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(DRIVERUID,driverUid)
            put(approvedConst, approved)
            put(CLICKACTION, "target_2")
        }

        val jsonData = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(DRIVERUID,driverUid)
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

        val requestBody = RequestBody.create(mediaType, wholeObj.toString())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(requestBody)
            .addHeader("Authorization", "key=AAAAx5Jyo0U:APA91bEB1Z9IYIqrN7Tt6avCLOTcto6sLJurSg_JrFCEteF8LS4QKqrB_wMsuh1ZFDiUAlw2rnAS94QHonUtw9j_s5ayfsjFgCmv1xU4I7toSlzB82_mquaMT8M-Fdh20jnw2r0HANO3")
            .addHeader("Content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody ?: "{}")
                val success = responseJson.optInt("success", 0)
                if (success == 1) {
                    Log.i("TAG", "Notification sent successfully to $customerToken")
                } else {
                    Log.e("TAG", "Failed to send notification. Response: $responseBody")
                }
            }
        })
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

        val requestBody = RequestBody.create(mediaType, wholeObj.toString())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(requestBody)
            .addHeader("Authorization", "key=AAAAx5Jyo0U:APA91bEB1Z9IYIqrN7Tt6avCLOTcto6sLJurSg_JrFCEteF8LS4QKqrB_wMsuh1ZFDiUAlw2rnAS94QHonUtw9j_s5ayfsjFgCmv1xU4I7toSlzB82_mquaMT8M-Fdh20jnw2r0HANO3")
            .addHeader("Content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody ?: "{}")
                val success = responseJson.optInt("success", 0)
                if (success == 1) {
                    Log.i("TAG", "Notification sent successfully to $customerToken")
                } else {
                    Log.e("TAG", "Failed to send notification. Response: $responseBody")
                }
            }
        })
    }

}
