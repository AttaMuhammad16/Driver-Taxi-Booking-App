package com.pakdrivefordriver.data.driver

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import com.directions.route.RoutingListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.pakdrive.MyResult
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.RequestModel
import com.pakdrivefordriver.models.AcceptModel
import com.pakdrivefordriver.models.DriverModel
import com.pakdrivefordriver.models.OfferModel
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
    suspend fun getRideRequests(): Flow<ArrayList<RequestModel>>
    suspend fun deletingRideRequests(customerUid:String):MyResult
    suspend fun sendOffer(sendRequestModel: OfferModel, customerUid: String):MyResult
    suspend fun updateDriverDetails(far:String,timeTravelToCustomer:String,distanceTravelToCustomer:String)

    suspend fun calculateEstimatedTimeForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode = TravelMode.DRIVING): String?
    suspend fun calculateDistanceForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): Double?
    suspend fun readingCurrentDriver(): DriverModel

    suspend fun deleteOffer(customerUid: String):MyResult

    fun findRoutes(Start: LatLng?, End: LatLng?, context: Activity, routingListener: RoutingListener, travelMode: TravelMode)

    suspend fun readAccept():AcceptModel?
    fun getCustomer(uid:String): Flow<CustomerModel?>
    suspend fun updateAvailable(available:Boolean)

    suspend fun deleteAcceptModel(driverUid: String):MyResult

    suspend fun updateRideCompletedNode()


}
