package com.pakdrivefordriver.ui.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.pakdrivefordriver.InternetChecker
import com.pakdrive.MyResult
import com.pakdrivefordriver.Utils
import com.pakdrivefordriver.Utils.convertUriToBitmap
import com.pakdrivefordriver.Utils.dismissProgressDialog
import com.pakdrivefordriver.Utils.myToast
import com.pakdrivefordriver.Utils.pickImage
import com.pakdrivefordriver.Utils.resultChecker
import com.pakdrivefordriver.Utils.showProgressDialog
import com.pakdrivefordriver.R
import com.pakdrivefordriver.databinding.ActivityDriverSignUpBinding
import com.pakdrivefordriver.models.DriverModel
import com.pakdrivefordriver.ui.viewmodels.AuthViewModel
import com.pakdrivefordriver.ui.viewmodels.DriverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DriverSignUpActivity : AppCompatActivity() {
    lateinit var binding:ActivityDriverSignUpBinding
    private val PROFILE_IMAGE_REQUEST_CODE=0
    private val FRONT_ID_CARD_REQUEST_CODE=1
    private val BACK_ID_CARD_REQUEST_CODE=2
    private val DRIVING_LICENSE_REQUEST_CODE=3

    private lateinit var frontIdBitmap: Bitmap
    private lateinit var backIdBitmap: Bitmap
    private lateinit var drivingLicencesBitmap: Bitmap
    private lateinit var profileImageBitmap: Bitmap
    private var bitmapList:ArrayList<Bitmap> = ArrayList()
    val authViewModel: AuthViewModel by viewModels()
    val driverViewModel:DriverViewModel by viewModels()
    val listOfUrls:ArrayList<String> = arrayListOf()
    var profileDownloadUrl=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@DriverSignUpActivity,R.layout.activity_driver_sign_up)
        Utils.statusBarColor(this)
        binding.marqueeTV.isSelected = true
        binding.loginTv.setOnClickListener {
            startActivity(Intent(this, DriverLoginActivity::class.java))
            finish()
        }

        binding.frontIdCardCard.setOnClickListener{
            pickImage(FRONT_ID_CARD_REQUEST_CODE,this)
        }
        binding.idCardBackCard.setOnClickListener {
            pickImage(BACK_ID_CARD_REQUEST_CODE,this)
        }
        binding.drivingLicenseCard.setOnClickListener {
            pickImage(DRIVING_LICENSE_REQUEST_CODE,this)
        }
        binding.selectImg.setOnClickListener {
            pickImage(PROFILE_IMAGE_REQUEST_CODE,this)
        }

        binding.signUpBtn.setOnClickListener {

            val dialog=showProgressDialog(this@DriverSignUpActivity,"SigningUp...")
            val userName=binding.nameEdt.text.toString().trim()
            val email=binding.emailEdt.text.toString().trim()
            val password=binding.passwordEdt.text.toString().trim()
            val phoneNumber=binding.phoneNumberEdt.text.toString().trim()
            val address=binding.addressEdt.text.toString().trim()
            val carDetails=binding.carDetails.text.toString().trim()

            if(!::profileImageBitmap.isInitialized){
                myToast(this,"Select profile image", Toast.LENGTH_LONG)
                dismissProgressDialog(dialog)

            }else if (userName.isEmpty()){
                binding.nameEdt.error = "Enter Name"
                myToast(this,"Enter Name")
                dismissProgressDialog(dialog)

            }else if (email.isEmpty()){
                binding.emailEdt.error = "Enter E-mail"
                myToast(this,"Enter E-mail")
                dismissProgressDialog(dialog)

            }else if (password.isEmpty()){
                binding.passwordEdt.error = "Enter Password"
                myToast(this,"Enter Password")
                dismissProgressDialog(dialog)

            }else if (phoneNumber.isEmpty()){
                binding.phoneNumberEdt.error = "Enter PhoneNumber"
                myToast(this,"Enter Phone Number")
                dismissProgressDialog(dialog)

            }else if (!Utils.isValidPakistaniPhoneNumber(phoneNumber)){
                binding.phoneNumberEdt.error = "Please Enter correct phone number."
                myToast(this,"Enter correct Phone Number")
                dismissProgressDialog(dialog)

            } else if (address.isEmpty()){
                binding.addressEdt.error = "Enter Address"
                myToast(this,"Enter Address")
                dismissProgressDialog(dialog)

            }else if (address.length<5){
                binding.addressEdt.error = "Enter correct address."
                myToast(this,"address length must be at least 5")
                dismissProgressDialog(dialog)

            } else if (!Utils.isValidEmail(email)){
                binding.emailEdt.error = "Enter correct E-mail"
                myToast(this,"Enter correct E-mail.", Toast.LENGTH_LONG)
                dismissProgressDialog(dialog)

            }else if (password.length<=6){
                binding.passwordEdt.error = "Password length must be at least 6."
                myToast(this,"Password length must be at least 6.")
                dismissProgressDialog(dialog)

            } else if (bitmapList.isEmpty()){
                myToast(this@DriverSignUpActivity,"Select id card and driving license.")
                dismissProgressDialog(dialog)

            }else if (!::frontIdBitmap.isInitialized){
                myToast(this@DriverSignUpActivity,"Select Front id card.")
                dismissProgressDialog(dialog)

            }else if (!::backIdBitmap.isInitialized){
                myToast(this@DriverSignUpActivity,"Select Back Front id card.")
                dismissProgressDialog(dialog)

            }else if (!::drivingLicencesBitmap.isInitialized){
                myToast(this@DriverSignUpActivity,"Select Driving license.")
                dismissProgressDialog(dialog)

            }else if(carDetails.isEmpty()){
                binding.carDetails.error = "Enter your vehicle details like (Riksha,097352)"
                myToast(this@DriverSignUpActivity,"Enter your vehicle details like (name=Riksha,number=097352)", Toast.LENGTH_LONG)
                dismissProgressDialog(dialog)

            } else{

                lifecycleScope.launch {
                    var isInternetAvailable=async { InternetChecker().isInternetConnectedWithPackage(this@DriverSignUpActivity) }

                    if (isInternetAvailable.await()){
                        authViewModel.registerUser(email,password){authResult->
                            if (authResult is MyResult.Error){
                                resultChecker(authResult,this@DriverSignUpActivity)
                                dismissProgressDialog(dialog)
                            }else if (authResult is MyResult.Success) run {
                                lifecycleScope.launch {

                                    bitmapList.forEach {
                                        var job = async {
                                            var result =   driverViewModel.uploadImageToStorage(it)
                                            when(result){
                                                is MyResult.Success -> {
                                                    listOfUrls.add(result.success)
                                                }
                                                else -> {
                                                    resultChecker(result,this@DriverSignUpActivity)
                                                }
                                            }
                                        }
                                        job.await()
                                    }

                                    val job=async {
                                        var result=driverViewModel.uploadImageToStorage(profileImageBitmap)
                                        when(result){
                                            is MyResult.Success->{
                                                profileDownloadUrl=result.success
                                            }else->{
                                            resultChecker(result,this@DriverSignUpActivity)
                                        }
                                        }
                                    }

                                    job.await()
                                    val model=DriverModel(null,listOfUrls,profileDownloadUrl,userName, email, password, phoneNumber, address,0.0,0.0,"",carDetails,0f,0, availabe = false, verificationProcess = false, far = "",timeTravelToCustomer="",distanceTravelToCustomer="", bearing = 0.0f, lock = false)
                                    val uploadResult=driverViewModel.uploadUserOnDatabase(model)
                                    if (uploadResult is MyResult.Success){
                                        resultChecker(uploadResult,this@DriverSignUpActivity)
                                        finish()
                                        dismissProgressDialog(dialog)
                                    }else if (uploadResult is MyResult.Error){
                                        resultChecker(uploadResult,this@DriverSignUpActivity)
                                    }
                                }

                            } else{
                                resultChecker(authResult,this@DriverSignUpActivity)
                                dismissProgressDialog(dialog)
                            }
                        }
                    }else{
                        myToast(this@DriverSignUpActivity,"Please connect with the internet.")
                    }
                }

            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode== Activity.RESULT_OK){
            var uri=data?.data
            var job= CoroutineScope(Dispatchers.IO).async {
                convertUriToBitmap(uri,this@DriverSignUpActivity)
            }

            when (requestCode) {

                FRONT_ID_CARD_REQUEST_CODE -> {
                    binding.frontIdCardTv.visibility= View.GONE
                    lifecycleScope.launch {
                        frontIdBitmap=job.await()!!
                        bitmapList.add(frontIdBitmap)
                        Glide.with(this@DriverSignUpActivity).load(frontIdBitmap).into(binding.frontImg)
                    }
                }

                BACK_ID_CARD_REQUEST_CODE -> {
                    binding.idCardBackTv.visibility= View.GONE
                    lifecycleScope.launch {
                        backIdBitmap=job.await()!!
                        bitmapList.add(backIdBitmap)
                        Glide.with(this@DriverSignUpActivity).load(backIdBitmap).into(binding.backImg)
                    }
                }

                DRIVING_LICENSE_REQUEST_CODE -> {
                    binding.drivingLicenseTv.visibility= View.GONE
                    lifecycleScope.launch {
                        drivingLicencesBitmap=job.await()!!
                        bitmapList.add(drivingLicencesBitmap)
                        Glide.with(this@DriverSignUpActivity).load(drivingLicencesBitmap).into(binding.drivingLicenceImg)
                    }
                }

                PROFILE_IMAGE_REQUEST_CODE -> {
                    lifecycleScope.launch {
                        profileImageBitmap=job.await()!!
                        Glide.with(this@DriverSignUpActivity).load(profileImageBitmap).into(binding.circleImageView)
                    }
                }

            }
        }
    }


}