package com.pakdrivefordriver

import android.app.PendingIntent
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pakdrive.InternetChecker
import com.pakdrive.Utils
import com.pakdrive.service.notification.NotificationManager.Companion.showNotification
import com.pakdrivefordriver.ui.activities.RequestViewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MyFirebaseMessagingService:FirebaseMessagingService() {
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data

        remoteMessage.notification.let {
            var customerUid=data[Utils.CUSTOMERUID]
            var comment=data[Utils.COMMENT]
            var time=data[Utils.TIME]
            var distance=data[Utils.DISTANCE]
            var priceRange=data[Utils.PRICERANGE]
            showNotification(it?.title!!,it?.body!!,customerUid!!,comment!!,time!!,distance!!,priceRange!!)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            if (InternetChecker().isInternetConnectedWithPackage(this@MyFirebaseMessagingService)){
                Utils.updateFCMToken(Utils.DRIVER, Utils.DRIVER_TOKEN_NODE,token)
            }else{
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MyFirebaseMessagingService, "check your Internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showNotification(title: String, messageBody: String,uid:String,comment:String,time:String,distance:String,priceRange:String) {
        val intent = Intent(this, RequestViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        intent.putExtra(Utils.TITLE,title)
        intent.putExtra(Utils.CUSTOMERUID,uid)

        val pendingIntent = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        showNotification(2,"Pak Drive",pendingIntent , this,title , messageBody)

    }

}