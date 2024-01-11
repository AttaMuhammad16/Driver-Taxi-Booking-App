package com.pakdrivefordriver.data.driver

import android.app.Activity
import android.graphics.Bitmap
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.pakdrive.MyResult
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.RequestModel
import com.pakdrivefordriver.models.DriverModel
import com.pakdrivefordriver.models.SendRequestModel
import kotlinx.coroutines.flow.Flow

interface DriverRepo {
    // storage
    suspend fun uploadImageToFirebaseStorage(uri: String): MyResult
    suspend fun uploadImageToFirebaseStorage(bitmap: Bitmap): MyResult
    suspend fun deleteImageToFirebaseStorage(url: String): MyResult
    // data base
    suspend fun uploadUserOnDatabase(driverModel: DriverModel): MyResult
    suspend fun isVerificationCompleted():Boolean

    // google map .
    fun startLocationUpdate(fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback, context: Activity, locationRequest: LocationRequest)
    fun stopLocationUpdate(fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback, context: Activity, locationRequest: LocationRequest)

    // data base
    suspend fun updateDriverLocationOnDataBase(location: Location?)
    suspend fun getRideRequestsForDrivers(): Flow<ArrayList<RequestModel>>
    suspend fun deletingRideRequests(key:String):MyResult
    suspend fun sendRideRequestToCustomer(sendRequestModel: SendRequestModel):MyResult
    suspend fun updateDriverDetails(far:String,timeTravelToCustomer:String,distanceTravelToCustomer:String)

    suspend fun calculateEstimatedTimeForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode = TravelMode.DRIVING): String?
    suspend fun calculateDistanceForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): Double?
    suspend fun readingCurrentDriver(): DriverModel

}