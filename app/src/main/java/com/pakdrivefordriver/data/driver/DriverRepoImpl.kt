package com.pakdrivefordriver.data.driver

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import com.pakdrive.MyResult
import com.pakdrive.Utils
import com.pakdrive.Utils.DRIVER_LANG_NODE
import com.pakdrive.Utils.DRIVER_LAT_NODE
import com.pakdrive.Utils.REQUESTFROMDRIVER
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.RequestModel
import com.pakdrivefordriver.models.DriverModel
import com.pakdrivefordriver.models.SendRequestModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class DriverRepoImpl @Inject constructor(val auth:FirebaseAuth,val databaseReference: DatabaseReference,val storageReference: StorageReference):DriverRepo {

    override suspend fun uploadImageToFirebaseStorage(uri: String): MyResult {
        return try {
            val imageRef = storageReference.child("images/${System.currentTimeMillis()}.jpg")
            val uploadTask = imageRef.putFile(Uri.parse(uri))
            val result: UploadTask.TaskSnapshot = uploadTask.await()
            val downloadUrl = result.storage.downloadUrl.await()
            MyResult.Success(downloadUrl.toString())
        } catch (e: Exception) {
            MyResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun uploadImageToFirebaseStorage(bitmap: Bitmap): MyResult {
        return try {
            val byteArray = Utils.bitmapToByteArray(bitmap)
            val imageRef = storageReference.child("images/${System.currentTimeMillis()}.jpg")
            val uploadTask = imageRef.putBytes(byteArray)
            val result= uploadTask.await()
            val downloadUrl = result.storage.downloadUrl.await()
            MyResult.Success(downloadUrl.toString())
        } catch (e: Exception) {
            MyResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun deleteImageToFirebaseStorage(url: String): MyResult {
        return try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.getReferenceFromUrl(url)
            val deleteTask: Task<Void> = storageRef.delete()
            Tasks.await(deleteTask)
            MyResult.Success("Image deleted successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            MyResult.Error("Failed to delete image: ${e.message}")
        }
    }

    override suspend fun uploadUserOnDatabase(driverModel: DriverModel): MyResult {
        var uid = auth.currentUser?.uid ?: "${System.currentTimeMillis()}"
        driverModel.uid = uid
        return try {
            databaseReference.child(Utils.DRIVER).child(uid).setValue(driverModel).await()
            MyResult.Success("Successfully Registered")
        } catch (e: Exception) {
            MyResult.Error("Failed: ${e.message}")
        }
    }

    override suspend fun isVerificationCompleted(): Boolean {
        val uid = auth.currentUser?.uid ?: "${System.currentTimeMillis()}"
        val deferred = CompletableDeferred<Boolean?>()
        databaseReference.child(Utils.DRIVER).child(uid).child(Utils.VERIFICATION_NODE).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val verificationBol = snapshot.getValue(Boolean::class.java)
                    deferred.complete(verificationBol)
                }
                override fun onCancelled(error: DatabaseError) {
                    deferred.completeExceptionally(error.toException())
                }
            })
        return deferred.await() ?: false
    }



    override fun startLocationUpdate(fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback, context: Activity,locationRequest: LocationRequest) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())
    }

    override fun stopLocationUpdate(fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback, context: Activity,locationRequest:LocationRequest) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override suspend fun updateDriverLocationOnDataBase(location: Location?) {
        var uid=auth.currentUser?.uid?:"${System.currentTimeMillis()}"
        var map= hashMapOf<String,Any>()
        map[DRIVER_LAT_NODE]=location?.latitude?:0.0
        map[DRIVER_LANG_NODE]=location?.longitude?:0.0
        databaseReference.child(Utils.DRIVER).child(uid).updateChildren(map)
    }

    override suspend fun getRideRequestsForDrivers(): Flow<ArrayList<RequestModel>> = callbackFlow {
        val childEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = ArrayList<RequestModel>()
                snapshot.children.forEach { child ->
                    val driverModel = child.getValue(RequestModel::class.java)?: return@forEach
                    requests.add(driverModel)
                }
                trySend(requests).isSuccess
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        databaseReference.child(Utils.REQUESTSFORDRIVERS).addValueEventListener(childEventListener)
        awaitClose {
            databaseReference.removeEventListener(childEventListener)
        }
    }

    override suspend fun deletingRideRequests(key: String): MyResult {
        return suspendCoroutine { continuation ->
            try {
                databaseReference.child(Utils.REQUESTSFORDRIVERS).child(key).removeValue().addOnSuccessListener {
                        continuation.resume(MyResult.Success("Ride request cancelled successfully."))
                    }.addOnFailureListener { exception ->
                        continuation.resume(MyResult.Error(exception.message ?: "Unknown error occurred"))
                    }
            } catch (e: Exception) {
                continuation.resume(MyResult.Error(e.message ?: "An error occurred"))
            }
        }
    }

    override suspend fun sendRideRequestToCustomer(sendRequestModel: SendRequestModel): MyResult {
        var currentUser=auth.currentUser
        return suspendCoroutine{continuation ->
            if (currentUser!=null){
                sendRequestModel.driverUid=currentUser.uid
                databaseReference.child(REQUESTFROMDRIVER).child(currentUser.uid).setValue(sendRequestModel).addOnSuccessListener {
                    continuation.resume(MyResult.Success("Request send successfully."))
                }.addOnFailureListener {
                    continuation.resume(MyResult.Error("Something wrong."))
                }
            }
        }
    }

    override suspend fun updateDriverDetails(far: String, timeTravelToCustomer: String, distanceTravelToCustomer: String) {
        var currentUser=auth.currentUser
        if (currentUser!=null){
            var map=HashMap<String,Any>()
            map["far"]=far
            map["timeTravelToCustomer"]=timeTravelToCustomer
            map["distanceTravelToCustomer"]=distanceTravelToCustomer
            databaseReference.child(Utils.DRIVER).child(currentUser.uid).updateChildren(map)
        }
    }


    override suspend fun calculateEstimatedTimeForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): String? {
        val geoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

        return try {
            val directionsResult = DirectionsApi.newRequest(geoApiContext).mode(travelMode).origin(com.google.maps.model.LatLng(start.latitude, start.longitude)).destination(com.google.maps.model.LatLng(end.latitude, end.longitude)).await()
            val route = directionsResult.routes[0]
            val leg = route.legs[0]
            val duration = leg.duration
            duration.humanReadable
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override suspend fun calculateDistanceForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): Double? {
        val geoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

        return try {
            val directionsResult = DirectionsApi.newRequest(geoApiContext)
                .mode(travelMode)
                .origin(com.google.maps.model.LatLng(start.latitude, start.longitude))
                .destination(com.google.maps.model.LatLng(end.latitude, end.longitude))
                .await()

            val route = directionsResult.routes[0]
            val leg = route.legs[0]
            val distanceInMeters = leg.distance.inMeters
            distanceInMeters / 1000.0  // Convert meters to kilometers
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun readingCurrentDriver(): DriverModel {
        return suspendCoroutine { continuation ->
            databaseReference.child(Utils.DRIVER).child(auth.currentUser!!.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val driverModel = snapshot.getValue(DriverModel::class.java)
                        if (driverModel != null) {
                            continuation.resume(driverModel)
                        } else {
                            continuation.resumeWithException(
                                NoSuchElementException("DriverModel not found")
                            )
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }
                })
        }
    }




}