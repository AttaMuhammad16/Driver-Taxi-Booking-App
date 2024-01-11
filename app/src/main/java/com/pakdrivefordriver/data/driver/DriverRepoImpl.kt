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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.pakdrive.MyResult
import com.pakdrive.Utils
import com.pakdrive.Utils.DRIVER_LANG_NODE
import com.pakdrive.Utils.DRIVER_LAT_NODE
import com.pakdrive.models.CustomerModel
import com.pakdrivefordriver.models.DriverModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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

}