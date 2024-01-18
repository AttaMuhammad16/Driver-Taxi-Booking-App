package com.pakdrivefordriver.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.pakdrivefordriver.R
import com.pakdrivefordriver.databinding.ActivityShowPaymetBinding

class ShowPaymentActivity : AppCompatActivity() {
    lateinit var binding:ActivityShowPaymetBinding
    var farBundle=""
    var timeBundle=""
    var distanceBundle=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@ShowPaymentActivity,R.layout.activity_show_paymet)

        farBundle=intent.getStringExtra("far")!!
        timeBundle=intent.getStringExtra("time")!!
        distanceBundle=intent.getStringExtra("distance")!!

        Log.i("TAG", "onCreate:$farBundle")
        Log.i("TAG", "onCreate:$timeBundle")
        Log.i("TAG", "onCreate:$distanceBundle")

    }
}