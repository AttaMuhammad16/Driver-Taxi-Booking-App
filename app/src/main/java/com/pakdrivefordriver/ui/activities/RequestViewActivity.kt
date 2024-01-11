package com.pakdrivefordriver.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import com.pakdrive.Utils
import com.pakdrivefordriver.R
import com.pakdrivefordriver.databinding.ActivityRequestViewBinding

class RequestViewActivity : AppCompatActivity() {
    lateinit var binding:ActivityRequestViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_view)
        binding=DataBindingUtil.setContentView(this@RequestViewActivity,R.layout.activity_request_view)
        Utils.statusBarColor(this,R.color.tool_color)

        val uid=intent.getStringExtra(Utils.CUSTOMERUID)
        val title=intent.getStringExtra(Utils.TITLE)
        val comment=intent.getStringExtra(Utils.COMMENT)
        val time=intent.getStringExtra(Utils.TIME)
        val distance=intent.getStringExtra(Utils.DISTANCE)
        val priceRange=intent.getStringExtra(Utils.PRICERANGE)

        binding.titleTv.text="Ride Request"
        if (comment!=""||comment.isNotEmpty()){
            binding.commentTv.text=comment
        }else{
            binding.commentTv.visibility= View.GONE
        }

        binding.timeTaken.text=time
        binding.distanceTv.text="$distance KM"
        binding.priceEdt.setText("$priceRange")

        binding.cancelBtn.setOnClickListener {

        }

    }
}
