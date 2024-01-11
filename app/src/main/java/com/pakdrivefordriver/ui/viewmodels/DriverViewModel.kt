package com.pakdrivefordriver.ui.viewmodels

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.pakdrive.MapUtils
import com.pakdrive.MyResult
import com.pakdrive.Utils.LATLANG_UPDATE_DELAY
import com.pakdrive.models.CustomerModel
import com.pakdrivefordriver.data.driver.DriverRepo
import com.pakdrivefordriver.models.DriverModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class DriverViewModel @Inject constructor(private val driverRepo: DriverRepo):ViewModel() {
    private var lastUpdateTime = 0L
    private var userLocationMarker: Marker? = null


    suspend fun uploadImageToStorage(bitmap: Bitmap): MyResult {
        return try {
            driverRepo.uploadImageToFirebaseStorage(bitmap)
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }

    suspend fun uploadImageToStorage(uri: String): MyResult {
        return try {
            driverRepo.uploadImageToFirebaseStorage(uri)
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }

    suspend fun deleteImageFromStorage(url:String): MyResult {
        return try {
            driverRepo.deleteImageToFirebaseStorage(url)
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }

    suspend fun uploadUserOnDatabase(driverModel: DriverModel): MyResult {
        return try {
            suspendCoroutine { continuation ->
                viewModelScope.launch(Dispatchers.IO) {
                    val result = driverRepo.uploadUserOnDatabase(driverModel)
                    continuation.resume(result)
                }
            }
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }

    suspend fun isVerificationCompleted():Boolean{
        return try {
            suspendCoroutine { continuation ->
                viewModelScope.launch(Dispatchers.IO) {
                    val result = driverRepo.isVerificationCompleted()
                    continuation.resume(result)
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    fun startLocationUpdate(fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback, context: Activity, locationRequest: LocationRequest){
        driverRepo.startLocationUpdate(fusedLocationClient, locationCallback, context, locationRequest)
    }

    fun stopLocationUpdate(fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback, context: Activity,locationRequest:LocationRequest){
        driverRepo.stopLocationUpdate(fusedLocationClient, locationCallback, context, locationRequest)
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun updateDriverLocationOnDataBase(location: Location?){
        GlobalScope.launch {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime >= LATLANG_UPDATE_DELAY) { // 5 seconds
                lastUpdateTime = currentTime
                driverRepo.updateDriverLocationOnDataBase(location)
            }
        }
    }


    fun getScaledCarIcon(zoomLevel: Float, context: Activity, drawable: Int): BitmapDescriptor {
        val scaleFactor = 1.0f + (zoomLevel - 18f) / 10.0f
        if (scaleFactor == 1.0f) {
            return BitmapDescriptorFactory.fromResource(drawable)
        }
        val originalIcon = BitmapFactory.decodeResource(context.resources, drawable)
        val scaledWidth = (originalIcon.width * scaleFactor).toInt()
        val scaledHeight = (originalIcon.height * scaleFactor).toInt()
        if (scaledWidth > 0 && scaledHeight > 0) {
            val scaledBitmap = Bitmap.createScaledBitmap(originalIcon, scaledWidth, scaledHeight, true)
            return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        }
        return BitmapDescriptorFactory.fromResource(drawable)
    }

    fun setUserLocationMarker(location: Location, mMap: GoogleMap, context: Activity, drawable: Int) {
        val latLng = LatLng(location.latitude, location.longitude)
        var lastZoomLevel = mMap.cameraPosition.zoom
        var lastScaledIcon: BitmapDescriptor? = null

        val updateMarkerIcon = {
            val currentZoomLevel = mMap.cameraPosition.zoom
            if (lastZoomLevel != currentZoomLevel || lastScaledIcon == null) {
                lastScaledIcon = getScaledCarIcon(currentZoomLevel, context, drawable)
                lastZoomLevel = currentZoomLevel
            }
            userLocationMarker?.setIcon(lastScaledIcon)
        }

        if (userLocationMarker == null) {
            val markerOptions = MarkerOptions().position(latLng).icon(getScaledCarIcon(mMap.cameraPosition.zoom, context, drawable)).rotation(location.bearing).anchor(0.5f, 0.5f)
            userLocationMarker = mMap.addMarker(markerOptions)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        } else {
            userLocationMarker?.position = latLng
            userLocationMarker?.rotation = location.bearing
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }

        mMap.setOnCameraIdleListener {
            updateMarkerIcon()
        }
    }



}