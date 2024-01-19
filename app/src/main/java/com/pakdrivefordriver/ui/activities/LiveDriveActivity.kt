package com.pakdrivefordriver.ui.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.RoutingListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.model.TravelMode
import com.pakdrive.InternetChecker
import com.pakdrive.MapUtils
import com.pakdrive.MapUtils.clearMapObjects
import com.pakdrive.MapUtils.drawRoute
import com.pakdrive.MapUtils.removeMarker
import com.pakdrive.PermissionHandler
import com.pakdrive.Utils
import com.pakdrive.Utils.dismissProgressDialog
import com.pakdrive.Utils.isLocationPermissionGranted
import com.pakdrive.Utils.myToast
import com.pakdrive.Utils.requestLocationPermission
import com.pakdrive.Utils.resultChecker
import com.pakdrive.Utils.statusBarColor
import com.pakdrivefordriver.MyConstants.apiKey
import com.pakdrivefordriver.R
import com.pakdrivefordriver.databinding.ActivityLiveDriveBinding
import com.pakdrivefordriver.services.notification.SendNotification.sendCancellationNotification
import com.pakdrivefordriver.services.notification.SendNotification.sendPickUpNotification
import com.pakdrivefordriver.services.notification.SendNotification.sendRideCompletedNotification
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LiveDriveActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, RoutingListener {
    @Inject
    lateinit var auth:FirebaseAuth
    val driverViewModel: DriverViewModel by viewModels()
    var stamp = System.currentTimeMillis()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this@LiveDriveActivity, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@LiveDriveActivity, "permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var binding:ActivityLiveDriveBinding
    private lateinit var onGoogleMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var list:ArrayList<LatLng>

    private var currentLatLang:LatLng?=null
    private var pickUpLatLang:LatLng?=null
    private var destinationLatLang:LatLng?=null

    val thresholdDistance = 10f // in meters
    val distanceToDestination = FloatArray(1)
    lateinit var  locationManager:LocationManager
    lateinit var dialog: Dialog
    var pickUpString:String?=""
    var destinationString:String?=""
    var startLocationMarkerVisibility:Boolean=false
    lateinit var sharedPreferences:SharedPreferences
    lateinit var editor:SharedPreferences.Editor
    var title=""
    var customerFCM:String?=""

    @SuppressLint("CommitPrefEdits", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_drive)
        statusBarColor(this@LiveDriveActivity)
        list= ArrayList()
        dialog=Utils.showProgressDialog(this@LiveDriveActivity,"Loading")
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@LiveDriveActivity)

        Places.initialize(this@LiveDriveActivity, getString(R.string.api));
        placesClient = Places.createClient(this@LiveDriveActivity);

        locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000L).setFastestInterval(2000L).setMaxWaitTime(2000L)

        if (!isLocationPermissionGranted(this@LiveDriveActivity)) {
            requestLocationPermission(this@LiveDriveActivity)
        }

        lifecycleScope.launch {
            val acceptModel=async { driverViewModel.readAccept() }.await()

            if (acceptModel!=null){
               driverViewModel.getCustomer(acceptModel.customerUid).collect{

                pickUpString=it?.startLatLang
                destinationString=it?.endLatLang
                customerFCM= it?.customerFCMToken // customer fcm

                destinationLatLang=Utils.stringToLatLng(destinationString)
                pickUpLatLang=Utils.stringToLatLng(pickUpString)

                if (destinationLatLang!=null){
                    val myFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
                    myFragment.getMapAsync(this@LiveDriveActivity)

                    binding.customerNameTv.text=it?.userName?:"Unknown"
                    binding.pickUpPointNameTv.text="Pickup: ${it?.pickUpPointName?:" Unknown "}"
                    binding.destinationNameTv.text="Destination: ${it?.destinationName?:" Unknown "}"

                    binding.dialImg.setOnClickListener {view->
                        if (ContextCompat.checkSelfPermission(this@LiveDriveActivity, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@LiveDriveActivity, arrayOf(android.Manifest.permission.CALL_PHONE), 122)
                        }else{
                            val intent = Intent(Intent.ACTION_CALL);
                            intent.data = Uri.parse("tel:${it?.phoneNumber}")
                            startActivity(intent)
                        }
                    }

                }else{
                    myToast(this@LiveDriveActivity,"pickup point and destination does not found.")
                    binding.constraintLayout.visibility= View.GONE
                    binding.cardView.visibility= View.GONE
                    binding.mapFragment.visibility= View.GONE
                    binding.startBtn.visibility= View.GONE
                    binding.blankTv.visibility= View.VISIBLE
                    dismissProgressDialog(dialog)
                  }
               }
            }else{
                myToast(this@LiveDriveActivity,"you have not any customer for now.",Toast.LENGTH_LONG)
                binding.constraintLayout.visibility= View.GONE
                binding.cardView.visibility= View.GONE
                binding.mapFragment.visibility= View.GONE
                binding.startBtn.visibility= View.GONE
                binding.blankTv.visibility= View.VISIBLE
                dismissProgressDialog(dialog)
            }

        }

        binding.cancelRideBtn.setOnClickListener {
            Utils.showAlertDialog(this@LiveDriveActivity,object:com.pakdrive.DialogInterface{
                override fun clickedBol(bol: Boolean) {
                    if (bol&&customerFCM!=null&&auth.currentUser!=null){
                        var dialog=Utils.showProgressDialog(this@LiveDriveActivity,"Cancelling...")
                        lifecycleScope.launch {
                            driverViewModel.updateAvailableNode(false)
                            var result=async { driverViewModel.deleteAcceptModel(auth.currentUser!!.uid) }.await()
                            resultChecker(result,this@LiveDriveActivity)
                            sendCancellationNotification("Pak Drive","Ride cancellation Notification.Your ride has been cancelled by the driver.",customerFCM!!,"false")
                            dismissProgressDialog(dialog)
                            finish()
                        }
                    }
                }
            },"Do you want to cancel the ride?")
        }


        binding.startBtn.setOnClickListener {
            Utils.showAlertDialog(this@LiveDriveActivity,object:com.pakdrive.DialogInterface{
                override fun clickedBol(bol: Boolean) {
                    if (bol){
                        lifecycleScope.launch {
                            Toast.makeText(this@LiveDriveActivity, "You have reached at the pickup point.", Toast.LENGTH_LONG).show()
                            sendPickUpNotification("Pak Drive ","Driver reached at the pickup point.",customerFCM!!,"reached")
                            editor.putBoolean("bol",true)
                            editor.apply()
                        }
                    }
                }
            },"Do you want to start the ride?")
        }

        binding.completedRideBtn.setOnClickListener {
            Utils.showAlertDialog(this@LiveDriveActivity,object:com.pakdrive.DialogInterface{
                override fun clickedBol(bol: Boolean) {
                    if (bol){
                        val dialog=Utils.showProgressDialog(this@LiveDriveActivity,"Loading...")
                        lifecycleScope.launch {

                            val driverModel=driverViewModel.readingCurrentDriver()
                            driverViewModel.updateAvailableNode(false)
                            async { driverViewModel.deleteAcceptModel(auth.currentUser!!.uid) }.await()

                            sendRideCompletedNotification("Pak Drive ride completed","Your journey is now complete. Welcome to your destination – we hope you enjoyed your ride!",customerFCM!!,"true")
                            Toast.makeText(this@LiveDriveActivity, "You have reached at the destination", Toast.LENGTH_LONG).show()
                            clearMapObjects()
                            removeMarker()
                            editor.remove("bol")
                            editor.apply()
                            dismissProgressDialog(dialog)
                            val far=driverModel.far
                            if (far.isNotEmpty()&&pickUpLatLang!=null&&destinationLatLang!=null){

                                val distance=driverViewModel.calculateDistanceForRoute(pickUpLatLang!!,destinationLatLang!!,apiKey,TravelMode.DRIVING)
                                val alert=AlertDialog.Builder(this@LiveDriveActivity).setView(R.layout.ride_completed_dialog).show()
                                val distanceTv=alert.findViewById<TextView>(R.id.distanceTv)
                                val paymentTv=alert.findViewById<TextView>(R.id.paymentTv)
                                val doneBtn=alert.findViewById<Button>(R.id.doneBtn)
                                distanceTv.text="Distance Traveled: $distance KM"
                                paymentTv.text="Payment: $far Rs"
                                doneBtn.setOnClickListener{
                                    alert.dismiss()
                                }

                            }else{
                                myToast(this@LiveDriveActivity,"Far is null")
                            }
                        }
                    }
                }
            },"Ride Completed?")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        onGoogleMap=googleMap
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

        lifecycleScope.launch {
            val internetChecker= InternetChecker().isInternetConnectedWithPackage(this@LiveDriveActivity)
            val gpsLocation=locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!internetChecker){
                myToast(this@LiveDriveActivity,"Please on your internet.",Toast.LENGTH_LONG)
                dismissProgressDialog(dialog)
            }else if (!gpsLocation){
                myToast(this@LiveDriveActivity,"Please on your location.",Toast.LENGTH_LONG)
                PermissionHandler.showEnableGpsDialog(this@LiveDriveActivity)
                dismissProgressDialog(dialog)
            } else{
                onGoogleMap.isTrafficEnabled=true
                onGoogleMap.uiSettings.apply {
                    isMapToolbarEnabled=false
                    isMyLocationButtonEnabled = true
                    isCompassEnabled=false
                    isZoomControlsEnabled=true
                    isTiltGesturesEnabled=false
                }
            }
        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            lifecycleScope.launch {
                val internetChecker= InternetChecker().isInternetConnectedWithPackage(this@LiveDriveActivity)

                if (internetChecker&&pickUpLatLang!=null){
                    if (System.currentTimeMillis() - stamp > 3000){
                        val data=driverViewModel.readAccept()
                        if (data?.driverUid!=null){
                            val bol=sharedPreferences.getBoolean("bol",false)

                            if (::onGoogleMap.isInitialized&&locationResult.lastLocation!=null){
                                driverViewModel.setUserLocationMarker(locationResult.lastLocation!!,onGoogleMap,this@LiveDriveActivity,R.drawable.map_nav)
                            }

                            if (pickUpLatLang !=null&&!bol&&locationResult.lastLocation!=null) {
                                currentLatLang=LatLng(locationResult.lastLocation!!.latitude,locationResult.lastLocation!!.longitude)
                                Location.distanceBetween(currentLatLang!!.latitude, currentLatLang!!.longitude, pickUpLatLang!!.latitude, pickUpLatLang!!.longitude, distanceToDestination)
                                title="Pickup point"
                                if (distanceToDestination[0] <=thresholdDistance) {
                                    Toast.makeText(this@LiveDriveActivity, "You have reached at the pickup point.", Toast.LENGTH_LONG).show()
                                    sendPickUpNotification("Pak Drive ","Driver reached at the pickup point.",customerFCM!!,"reached")
                                    editor.putBoolean("bol",true)
                                    editor.apply()
                                }else{
                                    driverViewModel.findingRoute(currentLatLang?: LatLng(0.0,0.0), pickUpLatLang?:LatLng(0.0,0.0), this@LiveDriveActivity, this@LiveDriveActivity, TravelMode.DRIVING)
                                }

                                val time=driverViewModel.calculateEstimatedTimeForRoute(currentLatLang?: LatLng(0.0,0.0),pickUpLatLang?: LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)?:"0"
                                val distance=driverViewModel.calculateDistanceForRoute(currentLatLang?: LatLng(0.0,0.0),pickUpLatLang?:LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)

                                binding.estimatedTime.text=time
                                binding.distanceTv.text=distance

                            }else if (destinationLatLang!=null && bol){
                                title="Destination point"
                                currentLatLang=LatLng(locationResult.lastLocation!!.latitude,locationResult.lastLocation!!.longitude)
                                Location.distanceBetween(currentLatLang!!.latitude, currentLatLang!!.longitude, destinationLatLang!!.latitude, destinationLatLang!!.longitude, distanceToDestination)

                                if (distanceToDestination[0] <=thresholdDistance) {

                                    driverViewModel.updateAvailableNode(false)
                                    async { driverViewModel.deleteAcceptModel(auth.currentUser!!.uid) }.await()

                                    sendRideCompletedNotification("Pak Drive ride completed","Your journey is now completed. Welcome to your destination – we hope you enjoyed your ride!",customerFCM!!,"true")
                                    Toast.makeText(this@LiveDriveActivity, "You have reached at the destination", Toast.LENGTH_LONG).show()
                                    clearMapObjects()
                                    removeMarker()

                                    editor.remove("bol")
                                    editor.apply()

                                    val driverModel=driverViewModel.readingCurrentDriver()
                                    val far=driverModel.far
                                    if (far.isNotEmpty()){
                                        val distance=driverViewModel.calculateDistanceForRoute(pickUpLatLang!!,destinationLatLang!!,apiKey,TravelMode.DRIVING)
                                        val alert=AlertDialog.Builder(this@LiveDriveActivity).setView(R.layout.ride_completed_dialog).show()
                                        val distanceTv=alert.findViewById<TextView>(R.id.distanceTv)
                                        val paymentTv=alert.findViewById<TextView>(R.id.paymentTv)
                                        val doneBtn=alert.findViewById<Button>(R.id.doneBtn)
                                        distanceTv.text=distance
                                        paymentTv.text=far.toString()
                                        doneBtn.setOnClickListener{
                                            alert.dismiss()
                                        }
                                    }else{
                                        myToast(this@LiveDriveActivity,"Far is null")
                                    }

                                }else{
                                    driverViewModel.findingRoute(currentLatLang?: LatLng(0.0,0.0), destinationLatLang?:LatLng(0.0,0.0), this@LiveDriveActivity, this@LiveDriveActivity, TravelMode.DRIVING)
                                }
                                val time=driverViewModel.calculateEstimatedTimeForRoute(currentLatLang?: LatLng(0.0,0.0),destinationLatLang?: LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)?:"0"
                                val distance=driverViewModel.calculateDistanceForRoute(currentLatLang?: LatLng(0.0,0.0),destinationLatLang?:LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)

                                binding.estimatedTime.text=time
                                binding.distanceTv.text=distance
                            }
                            stamp = System.currentTimeMillis()
                        }else{
                            binding.constraintLayout.visibility= View.GONE
                            binding.cardView.visibility= View.GONE
                            binding.mapFragment.visibility= View.GONE
                            binding.startBtn.visibility= View.GONE
                            binding.blankTv.visibility= View.VISIBLE
                            stamp = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this@LiveDriveActivity, "connection Failed", Toast.LENGTH_SHORT).show()
    }

    override fun onRoutingStart() {
        Log.i("onRoutingStart", "onRoutingStart: routing start")
    }

    override fun onRoutingSuccess(route: ArrayList<Route>?, shortestRouteIndex: Int) {
        if (destinationLatLang!=null && currentLatLang!=null){
            drawRoute(route!!,shortestRouteIndex,this@LiveDriveActivity,onGoogleMap,title,R.color.yellow,startLocationMarkerVisibility)
            dismissProgressDialog(dialog)
        }else{
            Toast.makeText(this@LiveDriveActivity, "Enter Starting and Ending Point.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRoutingCancelled() {
        driverViewModel.findingRoute(pickUpLatLang,destinationLatLang,this@LiveDriveActivity,this, TravelMode.DRIVING)
    }

    override fun onRoutingFailure(p0: RouteException?) {
        driverViewModel.findingRoute(pickUpLatLang,destinationLatLang,this@LiveDriveActivity,this, TravelMode.DRIVING)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch{
            launch {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    PermissionHandler.showEnableGpsDialog(this@LiveDriveActivity)
                    myToast(this@LiveDriveActivity,"Please on your location.")
                }else if (!InternetChecker().isInternetConnectedWithPackage(this@LiveDriveActivity)){
                    myToast(this@LiveDriveActivity, "Please on your internet connection.", Toast.LENGTH_LONG)
                }else if (!isLocationPermissionGranted(this@LiveDriveActivity)){
                    requestLocationPermission(this@LiveDriveActivity)
                } else if (isLocationPermissionGranted(this@LiveDriveActivity)&&locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)&&InternetChecker().isInternetConnectedWithPackage(this@LiveDriveActivity)){
                    PermissionHandler.askNotificationPermission(this@LiveDriveActivity, requestPermissionLauncher)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        driverViewModel.startLocationUpdate(fusedLocationClient,locationCallback,this@LiveDriveActivity,locationRequest)
    }

}