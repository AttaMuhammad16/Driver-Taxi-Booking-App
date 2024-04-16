package com.pakdrivefordriver.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
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
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mindinventory.midrawer.MIDrawerView
import com.pakdrive.InternetChecker
import com.pakdrive.PermissionHandler
import com.pakdrivefordriver.MyConstants.DRIVER
import com.pakdrivefordriver.MyConstants.DRIVER_TOKEN_NODE
import com.pakdrivefordriver.MyConstants.broadCastAction
import com.pakdrivefordriver.R
import com.pakdrivefordriver.Utils
import com.pakdrivefordriver.Utils.dismissProgressDialog
import com.pakdrivefordriver.Utils.myToast
import com.pakdrivefordriver.Utils.shareAppLink
import com.pakdrivefordriver.Utils.statusBarColor
import com.pakdrivefordriver.databinding.ActivityMainBinding
import com.pakdrivefordriver.services.MyService
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.sql.Array
import java.util.Locale


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this@MainActivity, "permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    lateinit var  locationManager:LocationManager
    lateinit var dialog: Dialog
    lateinit var binding:ActivityMainBinding
    lateinit var onGoogleMap: GoogleMap
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var userLocationMarker: Marker
    val driverViewModel:DriverViewModel by viewModels()

    lateinit var addressName:String
    var totalRides=0
    var totalCancelledRides=0
    var totalCompletedRides=0
    var totalEarning=0

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_main)
        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        dialog = Utils.showProgressDialog(this, "Finding...")
        statusBarColor(this,R.color.tool_color)

        val myFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        myFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(this, getString(R.string.api));

        lifecycleScope.launch {
            if (InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                Utils.generateFCMToken(DRIVER, DRIVER_TOKEN_NODE)
            }
        }

        locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000L).setFastestInterval(2000L).setMaxWaitTime(2000L)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            PermissionHandler.askNotificationPermission(this, requestPermissionLauncher)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PermissionHandler.permissionRequestCode)
            dismissProgressDialog(dialog)
        }


        binding.drawer.setSliderType(MIDrawerView.MI_TYPE_DOOR_OUT)
        binding.menuImage.setOnClickListener {
            if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
                binding.drawer.closeDrawer(GravityCompat.START)
            } else {
                binding.drawer.openDrawer(GravityCompat.START)
            }
        }


        lifecycleScope.launch { // drawer item
            driverViewModel.getRideRequests().collect{
                binding.numberOfRequests.text=it.size.toString()
            }
        }

        binding.rideRequestLinear.setOnClickListener {// drawer item
            startActivity(Intent(this@MainActivity,RequestViewActivity::class.java))
        }

        binding.bookedRidesLinear.setOnClickListener {
            startActivity(Intent(this@MainActivity,LiveDriveActivity::class.java))
        }

        binding.rideHistoryLinear.setOnClickListener {
            startActivity(Intent(this@MainActivity,DriverRideHistoryActivity::class.java))
        }

        binding.shareLinear.setOnClickListener {
            shareAppLink(this@MainActivity, "Pak Drive (Driver) From Quantum App Works \nhttps://play.google.com/store/apps/details?id=com.pakdrivefordriver")
        }

        lifecycleScope.launch {
            val listOfHistory = driverViewModel.getDriverHistory()
            totalRides = listOfHistory?.size ?: 0
            totalCompletedRides = listOfHistory?.count { it.rideStatus } ?: 0
            totalCancelledRides = totalRides - totalCompletedRides
            totalEarning = listOfHistory?.sumBy { it.payment.toInt() } ?: 0
        }

        binding.ridesDetailLinear.setOnClickListener {

            val dialog = MaterialAlertDialogBuilder(this@MainActivity)
            val views = LayoutInflater.from(this@MainActivity).inflate(R.layout.rides_info_dialog,null,false)
            dialog.setView(views)

            val totalEarningTv=views.findViewById<TextView>(R.id.totalEarningTv)
            val totalCompletedRidesTv=views.findViewById<TextView>(R.id.totalCompletedRidesTv)
            val totalCancelledRidesTv=views.findViewById<TextView>(R.id.totalCancelledRidesTv)

            totalEarningTv.text=totalEarning.toString()
            totalCompletedRidesTv.text=totalCompletedRides.toString()
            totalCancelledRidesTv.text=totalCancelledRides.toString()

            dialog.show()
        }

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful && task.result != null) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(
                            task.result.latitude,
                            task.result.longitude,
                            1
                        )
                        if (addresses!!.isNotEmpty()) {
                            val address: Address = addresses[0]
                            addressName = address.getAddressLine(0)
                        } else {
//                            Toast.makeText(this, "Address not found.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Handle the case where location data is not available
//                        Toast.makeText(this, "Unable to retrieve location data.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }catch (e:Exception){
            Log.i("Tag", "onCreate:${e.message} ")
        }

        binding.sheetShow.setOnClickListener {
            val sheet=BottomSheetDialog(this@MainActivity)
            sheet.setContentView(R.layout.bottom_sheet_dialog)
            val location=sheet.findViewById<TextView>(R.id.location)
            location?.text="Current Location: $addressName"
            sheet.show()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {

        lifecycleScope.launch {
            val internetChecker =
                async { InternetChecker().isInternetConnectedWithPackage(this@MainActivity) }
            if (internetChecker.await()){
                onGoogleMap=googleMap
                onGoogleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        this@MainActivity,
                        R.raw.map_style
                    )
                ) // setting map style.
                googleMap.apply {
                    uiSettings.isCompassEnabled = false;
                    uiSettings.isRotateGesturesEnabled = true;
                    uiSettings.isMyLocationButtonEnabled = true;
                }
            }else{
                myToast(this@MainActivity,"On your Internet connection.")
            }
        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            lifecycleScope.launch {
                val internetChecker=async { InternetChecker().isInternetConnectedWithPackage(this@MainActivity) }
                if (::onGoogleMap.isInitialized&&internetChecker.await()&&locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    if (locationResult.lastLocation!=null){
                        driverViewModel.setUserLocationMarker(locationResult.lastLocation!!,onGoogleMap,this@MainActivity,R.drawable.car)
                        dismissProgressDialog(dialog)
                        driverViewModel.updateDriverLocationOnDataBase(locationResult?.lastLocation)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(broadCastAction) // stop the service.
        sendBroadcast(intent)

        lifecycleScope.launch{
             if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                 PermissionHandler.showEnableGpsDialog(this@MainActivity)
                 dismissProgressDialog(dialog)
             }else if (!InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                 val i = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                 startActivity(i)
                 myToast(this@MainActivity, "on your internet connection.", Toast.LENGTH_LONG)
             }else if (!Utils.isLocationPermissionGranted(this@MainActivity)){
                 Utils.requestLocationPermission(this@MainActivity)
             } else if (Utils.isLocationPermissionGranted(this@MainActivity) &&locationManager.isProviderEnabled(
                 LocationManager.GPS_PROVIDER)&& InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                 Utils.generateFCMToken(DRIVER, DRIVER_TOKEN_NODE)
                 PermissionHandler.askNotificationPermission(this@MainActivity, requestPermissionLauncher)
                 if (::onGoogleMap.isInitialized&&::fusedLocationClient.isInitialized){
                     driverViewModel.startLocationUpdate(fusedLocationClient,locationCallback,this@MainActivity,locationRequest)
                 }
                 try {
                     if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                         val locationResult = fusedLocationClient.lastLocation
                         locationResult.addOnCompleteListener(this@MainActivity) { task ->
                             if (task.isSuccessful && task.result != null) {
                                 val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                                 val addresses = geocoder.getFromLocation(task.result.latitude, task.result.longitude, 1)
                                 if (addresses!!.isNotEmpty()) {
                                     val address: Address = addresses[0]
                                     addressName = address.getAddressLine(0)
                                 } else {
//                            Toast.makeText(this, "Address not found.", Toast.LENGTH_SHORT).show()
                                 }
                             } else {
                                 // Handle the case where location data is not available
//                        Toast.makeText(this, "Unable to retrieve location data.", Toast.LENGTH_SHORT).show()
                             }
                         }
                     }
                 }catch (e:Exception){
                     Log.i("Tag", "onCreate:${e.message} ")
                 }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        driverViewModel.startLocationUpdate(fusedLocationClient,locationCallback,this@MainActivity,locationRequest)
    }

    override fun onStop() {
        super.onStop()
        driverViewModel.stopLocationUpdate(fusedLocationClient,locationCallback,this@MainActivity,locationRequest)
    }

    override fun onPause() {
        super.onPause()
        val intent = Intent(this, MyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

}
