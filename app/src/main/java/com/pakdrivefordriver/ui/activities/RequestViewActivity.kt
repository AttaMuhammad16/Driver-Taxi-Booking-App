package com.pakdrivefordriver.ui.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pakdrive.Utils
import com.pakdrive.models.RequestModel
import com.pakdrivefordriver.R
import com.pakdrivefordriver.adapters.RequestsAdapter
import com.pakdrivefordriver.databinding.ActivityRequestViewBinding
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RequestViewActivity : AppCompatActivity() {
    lateinit var binding:ActivityRequestViewBinding
    lateinit var adapter:RequestsAdapter
    var list:ArrayList<RequestModel> = arrayListOf()
    val driverViewModel:DriverViewModel by viewModels()
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_view)
        binding=DataBindingUtil.setContentView(this@RequestViewActivity,R.layout.activity_request_view)
        Utils.statusBarColor(this,R.color.tool_color)


        // notification data.
//        val uid=intent.getStringExtra(Utils.CUSTOMERUID)
//        val title=intent.getStringExtra(Utils.TITLE)
//        val comment=intent.getStringExtra(Utils.COMMENT)
//        val time=intent.getStringExtra(Utils.TIME)
//        val distance=intent.getStringExtra(Utils.DISTANCE)
//        val priceRange=intent.getStringExtra(Utils.PRICERANGE)


        lifecycleScope.launch {
           var dialog=Utils.showProgressDialog(this@RequestViewActivity,"Loading...")
           var driverModel=async {  driverViewModel.readingCurrentDriver() }.await()
            driverViewModel.getRideRequests().collect { requests ->
                list = requests
                adapter = RequestsAdapter(list,driverViewModel,this@RequestViewActivity,driverModel)
                binding.recyclerView.apply {
                    layoutManager = LinearLayoutManager(this@RequestViewActivity)
                    adapter = this@RequestViewActivity.adapter
                }
                adapter.notifyDataSetChanged()
                Utils.dismissProgressDialog(dialog)
                if (list.size==0){
                    binding.blankTv.visibility=View.VISIBLE
                }
            }
        }

    }
}
