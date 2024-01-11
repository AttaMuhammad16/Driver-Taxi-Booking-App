package com.pakdrivefordriver.data.driver

import android.app.Activity
import android.graphics.Bitmap
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.pakdrive.MyResult
import com.pakdrive.models.CustomerModel
import com.pakdrivefordriver.models.DriverModel

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
    suspend fun updateDriverLocationOnDataBase(location: Location?)


}