package com.acagribahar.muscleandmindapp.ui.screens.settings


import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.acagribahar.muscleandmindapp.biling.BillingClientWrapper
import com.acagribahar.muscleandmindapp.data.local.SettingsManager
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth

// Artık FirebaseAuth parametresi almıyor
class SettingsViewModelFactory(
    private val repository: TaskRepository,
    private val firebaseAuth: FirebaseAuth,
    private val application: Application,
    private val billingClientWrapper: BillingClientWrapper


) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")

            Log.d("ViewModelFactory", "Creating SettingsViewModel instance...")

            return SettingsViewModel(repository, firebaseAuth,SettingsManager(application),billingClientWrapper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}