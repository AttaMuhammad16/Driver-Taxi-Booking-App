package com.pakdrivefordriver.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.pakdrive.DialogInterface
import com.pakdrive.InternetChecker
import com.pakdrive.MyResult
import com.pakdrive.Utils
import com.pakdrive.models.RequestModel
import com.pakdrivefordriver.MyConstants.apiKey
import com.pakdrivefordriver.R
import com.pakdrivefordriver.models.DriverModel
import com.pakdrivefordriver.models.OfferModel
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class RequestsAdapter(private val requestList: ArrayList<RequestModel>,val driverViewModel: DriverViewModel,var context:Activity,val driverModel:DriverModel,val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {
    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var titleTv: TextView = view.findViewById(R.id.titleTv)
        var commentTv: TextView = view.findViewById(R.id.commentTv)
        var timeTaken: TextView = view.findViewById(R.id.timeTaken)
        var distanceTv: TextView = view.findViewById(R.id.distanceTv)
        var priceEdt: TextView = view.findViewById(R.id.priceEdt)
        var cancelBtn:  Button = view.findViewById(R.id.cancelBtn)
        var sendBtn:  Button = view.findViewById(R.id.sendBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.request_item_view, parent, false)
        return RequestViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val data = requestList[position]
        holder.titleTv.text = "Request For Ride"
        holder.commentTv.text = data.comment
        holder.timeTaken.text = data.timeTaken
        holder.distanceTv.text = "${data.distance} KM"
        holder.priceEdt.text = data.far

        holder.cancelBtn.setOnClickListener {
            Utils.showAlertDialog(context,object: DialogInterface {
                override fun clickedBol(bol: Boolean) {
                    if (bol){
                        var dialog=Utils.showProgressDialog(context,"Cancelling...")
                        CoroutineScope(Dispatchers.Main).launch{

                            try {
                                val deleteRequest=async { driverViewModel.deletingRideRequest(data.customerUid) }.await()

                                if (deleteRequest is MyResult.Success) {
                                    val requestDeleteResult = driverViewModel.deleteOffer(data.customerUid)
                                    Utils.resultChecker(requestDeleteResult, context)
                                } else {
                                    Utils.resultChecker(deleteRequest, context)
                                }
                            }catch (e: Exception){
                                Utils.resultChecker(MyResult.Error(e.message ?: "Unknown error"), context)
                            }finally {
                                Utils.dismissProgressDialog(dialog)
                            }

                        }
                    }
                }
            },"Do you want to cancel this request?")
        }

        holder.sendBtn.setOnClickListener {
            var farPrice=holder.priceEdt.text.toString()
            if (farPrice.isEmpty()){
                Utils.myToast(context,"Enter your far price.")
            }else{
                Utils.showAlertDialog(context,object: DialogInterface {
                    override fun clickedBol(bol: Boolean) {
                        if (bol){
                            var dialog=Utils.showProgressDialog(context,"Sending...")
                            CoroutineScope(Dispatchers.Main).launch{
                                val internet=async { InternetChecker().isInternetConnectedWithPackage(context) }
                                if (internet.await()){

                                    val model=OfferModel(far = farPrice, driverUid = "null")

                                    val stringToLatLang=async { Utils.stringToLatLng(data.pickUpLatLang) }.await()
                                    val result=async { driverViewModel.sendOffer(model,data.customerUid) }.await() // offer sending.

                                    var time= driverViewModel.calculateEstimatedTimeForRoute(LatLng(driverModel.lat!!,driverModel.lang!!),stringToLatLang!!, apiKey, TravelMode.DRIVING)?:"none"
                                    var distance=driverViewModel.calculateDistanceForRoute(LatLng(driverModel.lat!!,driverModel.lang!!),stringToLatLang!!, apiKey, TravelMode.DRIVING)?:"0.0"

                                    driverViewModel.updateDriverDetails(farPrice,time,distance) // update some details in driver.

                                    if (result is MyResult.Success){
                                        Utils.dismissProgressDialog(dialog)
                                    }else{
                                        Utils.dismissProgressDialog(dialog)
                                    }
                                    Utils.resultChecker(result,context)
                                }else{
                                    Utils.dismissProgressDialog(dialog)
                                    Toast.makeText(context, "Please check your internet connection.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },"Do you want to send this request?")
            }
        }
    }
    override fun getItemCount() = requestList.size
}