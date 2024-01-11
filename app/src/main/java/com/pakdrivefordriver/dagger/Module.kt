package com.pakdrivefordriver.dagger

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.pakdrivefordriver.data.auth.AuthRepoImpl
import com.pakdrivefordriver.data.auth.AuthRepo
import com.pakdrivefordriver.data.driver.DriverRepo
import com.pakdrivefordriver.data.driver.DriverRepoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Provides
    @Singleton
    fun provideAuthRepo(auth: FirebaseAuth,databaseReference: DatabaseReference): AuthRepo {
        return AuthRepoImpl(auth, databaseReference)
    }

    @Provides
    @Singleton
    fun provideDriverRepo(auth: FirebaseAuth,databaseReference: DatabaseReference,storageReference: StorageReference):DriverRepo{
        return DriverRepoImpl(auth, databaseReference, storageReference)
    }


}