package com.rnsamsunghealthdataapi

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class RnSamsungHealthDataApiModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

 private lateinit var mStore: HealthDataStore
 private val coroutineScope = CoroutineScope(Dispatchers.Main)
 private var permissionPromise: Promise? = null

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun initializeHealthStore(promise: Promise) {
    try {
      mStore = HealthDataService.getStore(reactApplicationContext)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject("INITIALIZATION_ERROR", "Cannot initialize Samsung Health", e)
    }
  }

  @ReactMethod
  fun checkHealthPermissionsGranted(permissions: ReadableArray, promise: Promise) {
    if (!::mStore.isInitialized) {
      promise.reject("NOT_INITIALIZED", "Health data store is not initialized")
      return
    }

    coroutineScope.launch {
      try {
        val permSet = createPermissionSet(permissions)

        if (permSet.isEmpty()) {
          promise.reject("INVALID_PERMISSIONS", "No valid permissions were specified")
          return@launch
        }

        val grantedPermissions = mStore.getGrantedPermissions(permSet)
        
        val response = Arguments.createMap()
        val deniedArray = Arguments.createArray()

        val allGranted = grantedPermissions.containsAll(permSet)
        response.putBoolean("allGranted", allGranted)

        if (!allGranted) {
          permSet.filterNot { grantedPermissions.contains(it) }
            .forEach { deniedArray.pushString(getStringFromDataType(it.dataType)) }
          response.putArray("deniedPermissions", deniedArray)
        }

        promise.resolve(response)
      } catch (e: Exception) {
        promise.reject("PERMISSION_ERROR", "Error checking Samsung Health permissions", e)
      }
    }
  }

  @ReactMethod
  fun requestHealthPermissions(permissions: ReadableArray, promise: Promise) {
    if (!::mStore.isInitialized) {
      promise.reject("NOT_INITIALIZED", "Health data store is not initialized")
      return
    }

    val currentActivity = currentActivity ?: run {
      promise.reject("ACTIVITY_MISSING", "Activity is required for permission request")
      return
    }

    coroutineScope.launch {
      try {
        val permSet = createPermissionSet(permissions)

        if (permSet.isEmpty()) {
          promise.reject("INVALID_PERMISSIONS", "No valid permissions were specified")
          return@launch
        }

        val result = mStore.requestPermissions(permSet, currentActivity)

        val response = Arguments.createMap()
        val allGranted = result.containsAll(permSet)

        response.putBoolean("allGranted", allGranted)
        response.putString("message", if (allGranted) "All permissions already granted" else "Some permissions were denied")

        if (!allGranted) {
            val deniedArray = Arguments.createArray()
            permSet.filterNot { result.contains(it) }
                .forEach { deniedArray.pushString(getStringFromDataType(it.dataType)) }
            response.putArray("deniedPermissions", deniedArray)
        }
        
        promise.resolve(response)
      } catch (e: Exception) {
        promise.reject("PERMISSION_ERROR", "Error checking Samsung Health permissions", e)
      }
    }
  }

  // Private utils method-------------------------------------------------------------
  private fun createPermissionSet(permissions: ReadableArray): Set<Permission> {
    val permSet = mutableSetOf<Permission>()
    
    for (i in 0 until permissions.size()) {
      val permString = permissions.getString(i)
      val dataType = getDataTypeFromString(permString)
      
      if (dataType != null) {
        permSet.add(Permission.of(dataType, AccessType.READ))
      } else {
        Log.w(APP_TAG, "Unknown permission type: $permString")
      }
    }
    
    return permSet
  }

  private fun getDataTypeFromString(permString: String?): DataType? {
    return when (permString?.uppercase()) {
      "STEPS" -> DataTypes.STEPS
      else -> null
    }
  }

  private fun getStringFromDataType(dataType: DataType): String {
    return when (dataType) {
      DataTypes.STEPS -> "STEPS"
      else -> dataType.toString()
    }
  }

  companion object {
    const val NAME = "RnSamsungHealthDataApi"
    const val APP_TAG = "RnSamsungHealthDataApi"
  }
}
