package com.pakdrivefordriver.services.broadcastreciver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pakdrivefordriver.services.MyService

class StopServiceReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, MyService::class.java)
        context?.stopService(serviceIntent)
    }
}
