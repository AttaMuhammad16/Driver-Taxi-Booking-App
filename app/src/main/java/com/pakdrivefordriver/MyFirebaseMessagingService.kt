package com.pakdrivefordriver

import android.app.PendingIntent
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pakdrive.service.notification.NotificationManager.Companion.showNotification
import com.pakdrivefordriver.MyConstants.CUSTOMERUID
import com.pakdrivefordriver.MyConstants.DRIVER
import com.pakdrivefordriver.MyConstants.DRIVER_TOKEN_NODE
import com.pakdrivefordriver.MyConstants.TITLE
import com.pakdrivefordriver.MyConstants.approvedConst
import com.pakdrivefordriver.ui.activities.LiveDriveActivity
import com.pakdrivefordriver.ui.activities.MainActivity
import com.pakdrivefordriver.ui.activities.RequestViewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyFirebaseMessagingService:FirebaseMessagingService() {
    val auth: FirebaseAuth by lazy{
        FirebaseAuth.getInstance()
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val approve=data[approvedConst]

        if (approve=="false"){
            remoteMessage.notification?.let {
                showCancelNotification(it.title?:"Pak Drive",it.body?:"Ride Request")
            }
        }else if (approve=="true"){
            remoteMessage.notification.let {
                showApproveNotification(it?.title?:"Pak Drive",it?.body?:"Ride Accepted.")
            }
        } else{
            remoteMessage.notification.let {
                val customerUid=data[CUSTOMERUID]
                showNotification(it?.title?:"Pak Drive",it?.body?:"Ride cancellation Request",customerUid!!)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            if (InternetChecker().isInternetConnectedWithPackage(this@MyFirebaseMessagingService)){
                Utils.updateFCMToken(DRIVER, DRIVER_TOKEN_NODE,token)
            }else{
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MyFirebaseMessagingService, "check your Internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showNotification(title: String, messageBody: String,uid:String) {
        val intent = Intent(this, RequestViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        intent.putExtra(TITLE,title)
        intent.putExtra(CUSTOMERUID,uid)

        val pendingIntent = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        showNotification(2,"Pak Drive",pendingIntent , this,title , messageBody)

    }

    private fun showCancelNotification(title: String,messageBody: String){
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(TITLE,title)
        val pendingIntent = PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        showNotification(3,"Pak Drive ride cancel",pendingIntent , this,title , messageBody)
    }

    private fun showApproveNotification(title: String,messageBody: String){
        val intent = Intent(this, LiveDriveActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(TITLE,title)
        val pendingIntent = PendingIntent.getActivity(this, 4, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        showNotification(4,"Pak Drive ride accepted",pendingIntent , this,title , messageBody)
    }

}