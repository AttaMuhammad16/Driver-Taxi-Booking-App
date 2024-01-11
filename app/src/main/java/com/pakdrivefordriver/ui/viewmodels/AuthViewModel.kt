package com.pakdrivefordriver.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pakdrive.MyResult
import com.pakdrivefordriver.data.auth.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepo: AuthRepo):ViewModel() {

    fun registerUser(email: String, password: String, callback: (MyResult) -> Unit){
        viewModelScope.launch(Dispatchers.IO) {
            authRepo.registerUser(email, password, callback)
        }
    }

    fun loginUser(email: String, password: String, callback: (MyResult) -> Unit){
        viewModelScope.launch(Dispatchers.IO) {
            authRepo.loginUser(email, password, callback)
        }
    }

}