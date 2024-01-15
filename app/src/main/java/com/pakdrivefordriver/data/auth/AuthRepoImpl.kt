package com.pakdrivefordriver.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.pakdrive.MyResult
import com.pakdrivefordriver.MyConstants.DRIVER
import com.pakdrivefordriver.MyConstants.EMAIL_NODE
import com.pakdrivefordriver.MyConstants.VERIFICATION_NODE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepoImpl @Inject constructor(val auth:FirebaseAuth,val databaseReference: DatabaseReference):AuthRepo {

    override suspend fun registerUser(email: String, password: String, callback: (MyResult) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            callback(MyResult.Success("Successfully Registered."))
        }.addOnCanceledListener {
            callback(MyResult.Error("Something Wrong or check internet connection."))
        }.addOnFailureListener {
            if (it is FirebaseAuthUserCollisionException) {
                callback(MyResult.Error("User exits choose another email.")) // FirebaseAuthUserCollisionException
            } else {
                callback(MyResult.Error("Choose strong password.")) // FirebaseAuthWeakPasswordException
            }
        }
    }

    override suspend fun loginUser(email: String, password: String, callback: (MyResult) -> Unit) {
        if (checkEmailExits(email)){
            CoroutineScope(Dispatchers.IO).launch {
                signIn(email, password, callback)
            }
        }else{
            callback(MyResult.Error("Email does not exits create your account."))
        }
    }




    override suspend fun checkVerification(callback: (MyResult) -> Unit) {
        var user=auth.currentUser
        if (user!=null){
            var databaseRef=databaseReference.ref.child(DRIVER).child(user.uid)
            databaseRef.addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isProcessCompleted=snapshot.child(VERIFICATION_NODE).getValue(Boolean::class.java)
                    if (isProcessCompleted==true){
                        callback(MyResult.Success("verification completed"))
                    }else{
                        callback(MyResult.Error("verification in progress"))
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    callback(MyResult.Error("something wrong or check internet connection."))
                }
            })
        }
    }


    private suspend fun checkEmailExits(email: String): Boolean = suspendCoroutine { continuation ->
        val query = databaseReference.child(DRIVER).orderByChild(EMAIL_NODE).equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                continuation.resume(dataSnapshot.exists())
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.i("TAG", "onCancelled: ${databaseError.message}")
                continuation.resume(false)
            }
        })
    }



    private suspend fun signIn(email: String, password: String, callback: (MyResult) -> Unit){
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            callback(MyResult.Success("Successfully Login."))
        }.addOnFailureListener { exception ->

            when (exception) {
                is FirebaseAuthInvalidUserException -> {
                    callback(MyResult.Error("This user does not exit."))
                }

                is FirebaseAuthInvalidCredentialsException -> {
                    callback(MyResult.Error("Wrong email or password."))
                }

                else -> {
                    callback(MyResult.Error("Something wrong or check email or password."))
                }

            }
        }.addOnCanceledListener {
            callback(MyResult.Error("Something wrong or check internet."))
        }
    }


}
