package com.pakdrivefordriver.ui.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.google.maps.model.TravelMode
import com.pakdrive.InternetChecker
import com.pakdrive.MapUtils
import com.pakdrive.MapUtils.drawRoute
import com.pakdrive.PermissionHandler
import com.pakdrive.Utils
import com.pakdrive.Utils.dismissProgressDialog
import com.pakdrive.Utils.isLocationPermissionGranted
import com.pakdrive.Utils.myToast
import com.pakdrive.Utils.requestLocationPermission
import com.pakdrive.Utils.statusBarColor
import com.pakdrivefordriver.MyConstants.apiKey
import com.pakdrivefordriver.R
import com.pakdrivefordriver.databinding.ActivityLiveDriveBinding
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class LiveDriveActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, RoutingListener {
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

    private var start: LatLng? = null
    private var end: LatLng? = null

    val thresholdDistance = 9f // half meters
    val distanceToDestination = FloatArray(1)
    lateinit var  locationManager:LocationManager
    lateinit var dialog: Dialog
    var pickUpLatLang:String?=""
    var destinationLatLang:String?=""
    var startLocationMarkerVisibility:Boolean=false
    lateinit var sharedPreferences:SharedPreferences
    lateinit var editor:SharedPreferences.Editor

    @SuppressLint("CommitPrefEdits")
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
            val acceptModel=driverViewModel.readAccept()
            if (acceptModel!=null){
                val customerModel=driverViewModel.getCustomer(acceptModel.customerUid)
                pickUpLatLang=customerModel?.startLatLang
                destinationLatLang=customerModel?.endLatLang

                end=Utils.stringToLatLng(pickUpLatLang)

                if (end!=null){
                    val myFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
                    myFragment.getMapAsync(this@LiveDriveActivity)

                }else{
                    myToast(this@LiveDriveActivity,"pickup point and destination does not found.")
                    dismissProgressDialog(dialog)
                }
            }else{
                myToast(this@LiveDriveActivity,"you have not any customer.",Toast.LENGTH_LONG)
                dismissProgressDialog(dialog)
            }

            driverViewModel.time.observe(this@LiveDriveActivity) {
                binding.estimatedTime.text=it
            }
            driverViewModel.distance.observe(this@LiveDriveActivity){
                binding.distanceTv.text=it
            }
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
//                  isRotateGesturesEnabled=false
                    isCompassEnabled=false
                    isZoomControlsEnabled=true
                }
            }

        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            lifecycleScope.launch {
                if (System.currentTimeMillis() - stamp > 3000){
                    val reached=sharedPreferences.getInt("reached",0)

                    if (::onGoogleMap.isInitialized&&locationResult.lastLocation!=null){
                        driverViewModel.setUserLocationMarker(locationResult.lastLocation!!,onGoogleMap,this@LiveDriveActivity,R.drawable.map_nav)
                    }
                    if (end != null&&reached==0) {
                        start=LatLng(locationResult.lastLocation!!.latitude,locationResult.lastLocation!!.longitude)
                        Location.distanceBetween(start!!.latitude, start!!.longitude, end!!.latitude, end!!.longitude, distanceToDestination)
                        myToast(this@LiveDriveActivity,"${distanceToDestination[0]}")
                        if (distanceToDestination[0] <=thresholdDistance) {
                            Toast.makeText(this@LiveDriveActivity, "You have reached at the pickup point.", Toast.LENGTH_LONG).show()
                            start = LatLng(0.0, 0.0)
                            end=Utils.stringToLatLng(destinationLatLang)
                            editor.putInt("reached",1)
                            editor.apply()
                            // send notification to the user when drive reached at the pick up point.
                        }else{
                            driverViewModel.findingRoute(start?: LatLng(0.0,0.0), end?:LatLng(0.0,0.0), this@LiveDriveActivity, this@LiveDriveActivity, TravelMode.DRIVING)
                        }

                        driverViewModel.calculateEstimatedTimeForRoute(start?: LatLng(0.0,0.0),end?: LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)?:"0"
                        driverViewModel.calculateDistanceForRoute(start?: LatLng(0.0,0.0),end?:LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)?:0.0

                    }else if (start==null||start==LatLng(0.0, 0.0)&&reached==1){
                        start=LatLng(locationResult.lastLocation!!.latitude,locationResult.lastLocation!!.longitude)
                        end=Utils.stringToLatLng(destinationLatLang)

                        Location.distanceBetween(start!!.latitude, start!!.longitude, end!!.latitude, end!!.longitude, distanceToDestination)

                        if (distanceToDestination[0] <=thresholdDistance) {
                            Toast.makeText(this@LiveDriveActivity, "You have reached the at your destination.", Toast.LENGTH_LONG).show()
                            start = LatLng(0.0, 0.0)
                            editor.putInt("reached",2)
                            editor.apply()
                        }else{
                            driverViewModel.findingRoute(start?: LatLng(0.0,0.0), end?:LatLng(0.0,0.0), this@LiveDriveActivity, this@LiveDriveActivity, TravelMode.DRIVING)
                        }

                        driverViewModel.calculateEstimatedTimeForRoute(start?: LatLng(0.0,0.0),end?: LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)?:"0"
                        driverViewModel.calculateDistanceForRoute(start?: LatLng(0.0,0.0),end?:LatLng(0.0,0.0),apiKey,TravelMode.DRIVING)?:0.0
                    }
                    stamp = System.currentTimeMillis()

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
        if (start!=null && end!=null){
            drawRoute(route!!,shortestRouteIndex,this@LiveDriveActivity,onGoogleMap,"Pickup point","destination point",R.color.yellow,startLocationMarkerVisibility)
            dismissProgressDialog(dialog)
        }else{
            Toast.makeText(this@LiveDriveActivity, "Enter Starting and Ending Point.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRoutingCancelled() {
        driverViewModel.findingRoute(start,end,this@LiveDriveActivity,this, TravelMode.DRIVING)
    }

    override fun onRoutingFailure(p0: RouteException?) {
        driverViewModel.findingRoute(start,end,this@LiveDriveActivity,this, TravelMode.DRIVING)
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