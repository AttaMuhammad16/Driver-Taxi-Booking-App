package com.pakdrivefordriver.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pakdrivefordriver.Utils
import com.pakdrivefordriver.R
import com.pakdrivefordriver.adapters.RideHistoryAdapter
import com.pakdrivefordriver.databinding.ActivityRideHistoryBinding
import com.pakdrivefordriver.models.RideHistoryModel
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DriverRideHistoryActivity : AppCompatActivity() {
    lateinit var binding:ActivityRideHistoryBinding
    var list:ArrayList<RideHistoryModel> = arrayListOf()
    val driverViewModel:DriverViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@DriverRideHistoryActivity,R.layout.activity_ride_history)
        Utils.statusBarColor(this@DriverRideHistoryActivity)

        var dialog= Utils.showProgressDialog(this@DriverRideHistoryActivity,"Loading...")
        lifecycleScope.launch{
            val data=driverViewModel.getDriverHistory()
            if (data!=null){
                list=data
            }
            if (list.isEmpty()){
                binding.dummyTv.visibility= View.VISIBLE
            }
            list.reverse()
            binding.recyclerView.layoutManager= LinearLayoutManager(this@DriverRideHistoryActivity)
            var adapter= RideHistoryAdapter(list,this@DriverRideHistoryActivity)
            binding.recyclerView.adapter=adapter
            dialog.dismiss()
        }

        binding.backArrowImg.setOnClickListener {
            finish()
        }
    }
}