package com.pakdrivefordriver.ui.activities

import android.app.Dialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.pakdrive.InternetChecker
import com.pakdrive.PermissionHandler
import com.pakdrive.Utils
import com.pakdrive.Utils.dismissProgressDialog
import com.pakdrive.Utils.myToast
import com.pakdrive.Utils.rippleEffect
import com.pakdrivefordriver.MyConstants.DRIVER
import com.pakdrivefordriver.MyConstants.DRIVER_TOKEN_NODE
import com.pakdrivefordriver.MyConstants.broadCastAction
import com.pakdrivefordriver.R
import com.pakdrivefordriver.databinding.ActivityMainBinding
import com.pakdrivefordriver.services.MyService
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_main)
        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        dialog=Utils.showProgressDialog(this,"Finding...")
        Utils.statusBarColor(this,R.color.tool_color)

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

    }

    override fun onMapReady(googleMap: GoogleMap) {

        lifecycleScope.launch {
            var internetChecker=async { InternetChecker().isInternetConnectedWithPackage(this@MainActivity) }
            if (internetChecker.await()){
                onGoogleMap=googleMap
//                onGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MainActivity,R.raw.map_style)) // setting map style.
                googleMap.apply {
                    uiSettings.isCompassEnabled = false;
                    uiSettings.isRotateGesturesEnabled = false;
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
                var internetChecker=async { InternetChecker().isInternetConnectedWithPackage(this@MainActivity) }
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
            launch {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    PermissionHandler.showEnableGpsDialog(this@MainActivity)
                    dismissProgressDialog(dialog)
                }else if (!InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    startActivity(intent)
                    myToast(this@MainActivity, "on your internet connection.", Toast.LENGTH_LONG)
                }else if (!Utils.isLocationPermissionGranted(this@MainActivity)){
                    Utils.requestLocationPermission(this@MainActivity)
                } else if (Utils.isLocationPermissionGranted(this@MainActivity) &&locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)&& InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                    Utils.generateFCMToken(DRIVER, DRIVER_TOKEN_NODE)
                    PermissionHandler.askNotificationPermission(this@MainActivity, requestPermissionLauncher)
                    if (::onGoogleMap.isInitialized&&::fusedLocationClient.isInitialized){
                    }
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
        startService(Intent(this@MainActivity,MyService::class.java)) // start the service.
    }

    override fun onDestroy() {
        super.onDestroy()
        startService(Intent(this@MainActivity,MyService::class.java)) // start the service.
    }

}
