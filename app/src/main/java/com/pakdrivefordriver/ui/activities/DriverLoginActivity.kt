package com.pakdrivefordriver.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.pakdrive.InternetChecker
import com.pakdrive.MyResult
import com.pakdrive.Utils
import com.pakdrivefordriver.R
import com.pakdrivefordriver.databinding.ActivityDriverLoginBinding
import com.pakdrivefordriver.ui.viewmodels.AuthViewModel
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class DriverLoginActivity : AppCompatActivity() {
    lateinit var binding:ActivityDriverLoginBinding
    val authViewModel:AuthViewModel by viewModels()
    val driverViewModel:DriverViewModel by viewModels()
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@DriverLoginActivity,R.layout.activity_driver_login)
        Utils.statusBarColor(this@DriverLoginActivity)
        val user=auth.currentUser

        if (user!=null){
            startActivity(Intent(this@DriverLoginActivity,MainActivity::class.java))
            finish()
        }

        binding.signUpTv.setOnClickListener {
            startActivity(Intent(this@DriverLoginActivity,DriverSignUpActivity::class.java))
        }

        binding.loginBtn.setOnClickListener {

            val dialog = Utils.showProgressDialog(this,"Loading...")
            val email = binding.emailEdt.text.toString().trim()
            val password = binding.passwordEdt.text.toString().trim()

            if (email.isEmpty()) {
                Utils.invalidInputsMessage(this, binding.emailEdt, "Enter E-mail", dialog)

            } else if (!Utils.isValidEmail(email)) {
                Utils.invalidInputsMessage(this, binding.emailEdt, "Enter correct Email.", dialog)

            } else if (password.isEmpty()) {
                Utils.invalidInputsMessage(this, binding.passwordEdt, "Enter correct password.", dialog)

            } else if (password.length<=6){
                Utils.invalidInputsMessage(this, binding.passwordEdt, "Password length must be at least 6 characters.", dialog)

            } else {

                lifecycleScope.launch{
                    var isInternetAvailable=async { InternetChecker().isInternetConnectedWithPackage(this@DriverLoginActivity) }
                    if (isInternetAvailable.await()){
                        var verification=async { driverViewModel.isVerificationCompleted() }
                        if (verification.await()){
                            authViewModel.loginUser(email,password) { result ->
                                if (result is MyResult.Error) {
                                    Utils.myToast(this@DriverLoginActivity, result.error)
                                    Utils.dismissProgressDialog(dialog)
                                } else if (result is MyResult.Success) {
                                    Utils.myToast(this@DriverLoginActivity, result.success)
                                    Utils.dismissProgressDialog(dialog)
                                    startActivity(Intent(this@DriverLoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            }
                        }else{
                            Utils.myToast(this@DriverLoginActivity,"Verification in progress.",Toast.LENGTH_LONG)
                            Utils.dismissProgressDialog(dialog)
                        }
                    }else{
                        Utils.myToast(this@DriverLoginActivity, "check your internet connection.")
                        Utils.dismissProgressDialog(dialog)
                    }
                }

            }
        }

    }
}