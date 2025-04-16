package com.rnsamsunghealthdataapi

import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
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
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.android.sdk.health.data.data.AggregatedData
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.response.DataResponse
import com.rnsamsunghealthdataapi.model.HeartRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.time.Duration

class RnSamsungHealthDataApiModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

 private lateinit var mStore: HealthDataStore
 private val coroutineScope = CoroutineScope(Dispatchers.Main)

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
        response.putString("message", if (allGranted) "All permissions granted" else "Some permissions were denied")

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

  @ReactMethod
  fun readStepData(
    gap: Int,
    operator: String,
    groupBy: String,
    startDate: String?,
    endDate: String?,
    ascendingOrder: Boolean?,
    promise: Promise
  ) {
    if (!::mStore.isInitialized) {
      promise.reject("NOT_INITIALIZED", "Health data store is not initialized")
      return
    }

    val currentActivity = currentActivity ?: run {
      promise.reject("ACTIVITY_MISSING", "Activity is required for permission request")
      return
    }
    
    try {
      val localtimeFilter = when(operator) {
        "between" -> {
          if (startDate == null || endDate == null) {
            promise.reject("INVALID_DATES", "Both startDate and endDate are required for 'between'")
            return
          }

          val startDateTime = ZonedDateTime.parse(startDate).toLocalDateTime()
          val endDateTime = ZonedDateTime.parse(endDate).toLocalDateTime()
          LocalTimeFilter.of(startDateTime, endDateTime)
        }
        "after" -> {
          if (startDate == null) {
            promise.reject("INVALID_DATES", "StartDate is required for 'after'")
            return
          }

          val startDateTime = ZonedDateTime.parse(startDate).toLocalDateTime()
          LocalTimeFilter.since(startDateTime)
        }
        "before" -> {
           if (endDate == null) {
            promise.reject("INVALID_DATES", "EndDate is required for 'before'")
            return
          }

          val endDateTime = ZonedDateTime.parse(endDate).toLocalDateTime()
          LocalTimeFilter.to(endDateTime)
        }
        else -> {
          promise.reject("INVALID_OPERATOR", "Unsupported operator: $operator")
          return
        }
      }

      val localTimeGroup = when(groupBy) {
        "daily" -> LocalTimeGroup.of(LocalTimeGroupUnit.DAILY, gap)
        "monthly" -> LocalTimeGroup.of(LocalTimeGroupUnit.MONTHLY, gap)
        else -> LocalTimeGroup.of(LocalTimeGroupUnit.HOURLY, gap)
      }
      
      val aggregateRequest = if (ascendingOrder != null) {
        val ordering = if(ascendingOrder) { Ordering.ASC } else { Ordering.DESC }
        
        DataType.StepsType.TOTAL.requestBuilder
          .setLocalTimeFilterWithGroup(localtimeFilter, localTimeGroup)
          .setOrdering(ordering)
          .build()
      } else {
        DataType.StepsType.TOTAL.requestBuilder
          .setLocalTimeFilterWithGroup(localtimeFilter, localTimeGroup)
          .build()
      }
      
      coroutineScope.launch {
        try {
          val result = mStore.aggregateData(aggregateRequest)

          var totalSteps: Long = 0
          val hourlyData = Arguments.createArray()
        
          result.dataList.forEach { stepData ->
            val hourlySteps = stepData.value as Long
            totalSteps += hourlySteps

            val entry = Arguments.createMap()
            entry.putString("startTime", stepData.startTime.toString())
            entry.putString("endTime", stepData.endTime.toString())
            entry.putDouble("value", hourlySteps.toDouble())
            hourlyData.pushMap(entry)
          }

          val response = Arguments.createMap()
          response.putDouble("count", totalSteps.toDouble())
          response.putArray("data", hourlyData)
          promise.resolve(response)
        } catch (e: Exception) {
          promise.reject("READING_ERROR", "Error reading Samsung Health Step Data", e)
        }
      }
    } catch (e: Exception) {
      promise.reject("DATE_PARSING_ERROR", "Error parsing date strings", e)
    }
  }
  
  @ReactMethod
  fun readSleepData(
    operator: String,
    startDate: String?,
    endDate: String?,
    promise: Promise
  ) {
    if (!::mStore.isInitialized) {
      promise.reject("NOT_INITIALIZED", "Health data store is not initialized")
      return
    }

    val currentActivity = currentActivity ?: run {
      promise.reject("ACTIVITY_MISSING", "Activity is required for permission request")
      return
    }

    try {
      val localTimeFilter = when(operator) {
        "between" -> {
          if (startDate == null || endDate == null) {
            promise.reject("INVALID_DATES", "Both startDate and endDate are required for 'between'")
            return
          }

          val startDateTime = ZonedDateTime.parse(startDate).toLocalDate()
          val endDateTime = ZonedDateTime.parse(endDate).toLocalDate()
          LocalTimeFilter.of(startDateTime.atStartOfDay(), endDateTime.atStartOfDay())
        }
        "after" -> {
          if (startDate == null) {
            promise.reject("INVALID_DATES", "StartDate is required for 'after'")
            return
          }

          val startDateTime = ZonedDateTime.parse(startDate).toLocalDate()
          LocalTimeFilter.since(startDateTime.atStartOfDay())
        }
        "before" -> {
          if (endDate == null) {
            promise.reject("INVALID_DATES", "EndDate is required for 'before'")
            return
          }

          val endDateTime = ZonedDateTime.parse(endDate).toLocalDate()
          LocalTimeFilter.to(endDateTime.atStartOfDay())
        }
        else -> {
          promise.reject("INVALID_OPERATOR", "Unsupported operator: $operator")
          return
        }
      }

      val readRequest = DataTypes.SLEEP.readDataRequestBuilder
        .setLocalTimeFilter(localTimeFilter)
        .build()

       coroutineScope.launch {
        try {
          val result = mStore.readData(readRequest)

          var totalDurationInHours: Int = 0
          var totalDurationInMinutes: Int = 0
          val sleepResult = Arguments.createArray()
        
          result.dataList.forEach { sleepData ->
            val score: Int
            score = prepareSleepScore(sleepData) ?: 0

            val duration = sleepData.getValue(DataType.SleepType.DURATION) ?: Duration.ZERO
            val sleepSessionList = sleepData.getValue(DataType.SleepType.SESSIONS) ?: emptyList()
            
            totalDurationInHours += duration.toHours().toInt()
            totalDurationInMinutes += duration.toMinutes().toInt()

            val entry = Arguments.createMap()
            entry.putDouble("score", score.toDouble())
            entry.putDouble("sessionCount", sleepSessionList.size.toDouble())
            entry.putDouble("durationHours", duration.toHours().toDouble())
            entry.putDouble("durationMinutes", duration.minusHours(duration.toHours()).toMinutes().toDouble()) 
            entry.putString("startTime", sleepData.startTime.toString())
            entry.putString("endTime", sleepData.endTime.toString())
           
            sleepResult.pushMap(entry)
          }

          val response = Arguments.createMap()
          response.putDouble("totalDurationInHours", totalDurationInHours.toDouble())
          response.putDouble("totalDurationInMinutes", totalDurationInMinutes.toDouble())
          response.putArray("data", sleepResult)
          promise.resolve(response)
        } catch (e: Exception) {
          promise.reject("READING_ERROR", "Error reading Samsung Health Sleep Data", e)
        }
      }
    } catch (e: Exception) {
      promise.reject("DATE_PARSING_ERROR", "Error parsing date strings", e)
    }
  }

  @ReactMethod
  fun readHeartRateData(
    operator: String,
    startDate: String?,
    endDate: String?,
    ascendingOrder: Boolean?,
    promise: Promise
  ) {
     if (!::mStore.isInitialized) {
      promise.reject("NOT_INITIALIZED", "Health data store is not initialized")
      return
    }

    val currentActivity = currentActivity ?: run {
      promise.reject("ACTIVITY_MISSING", "Activity is required for permission request")
      return
    }
    
    try {
      val localTimeFilter = when(operator) {
        "between" -> {
          if (startDate == null || endDate == null) {
            promise.reject("INVALID_DATES", "Both startDate and endDate are required for 'between'")
            return
          }

          val startDateTime = ZonedDateTime.parse(startDate).toLocalDateTime()
          val endDateTime = ZonedDateTime.parse(endDate).toLocalDateTime()
          LocalTimeFilter.of(startDateTime, endDateTime)
        }
        "after" -> {
          if (startDate == null) {
            promise.reject("INVALID_DATES", "StartDate is required for 'after'")
            return
          }

          val startDateTime = ZonedDateTime.parse(startDate).toLocalDateTime()
          LocalTimeFilter.since(startDateTime)
        }
        "before" -> {
           if (endDate == null) {
            promise.reject("INVALID_DATES", "EndDate is required for 'before'")
            return
          }

          val endDateTime = ZonedDateTime.parse(endDate).toLocalDateTime()
          LocalTimeFilter.to(endDateTime)
        }
        else -> {
          promise.reject("INVALID_OPERATOR", "Unsupported operator: $operator")
          return
        }
      }
      
      val readRequest = if (ascendingOrder != null) {
        val ordering = if(ascendingOrder) { Ordering.ASC } else { Ordering.DESC }
        
        DataTypes.HEART_RATE.readDataRequestBuilder
          .setLocalTimeFilter(localTimeFilter)
          .setOrdering(ordering)
          .build()
      } else {
        DataTypes.HEART_RATE.readDataRequestBuilder
          .setLocalTimeFilter(localTimeFilter)
          .build()
      }
      
      coroutineScope.launch {
        try {
          val result = mStore.readData(readRequest)

          val hrOfFirstQuarter = HeartRate(1000f, 0f, 0f, "00:00", "06:00", 0)
          val hrOfSecondQuarter = HeartRate(1000f, 0f, 0f, "06:00", "12:00", 0)
          val hrOfThirdQuarter = HeartRate(1000f, 0f, 0f, "12:00", "18:00", 0)
          val hrOfFourthQuarter = HeartRate(1000f, 0f, 0f, "18:00", "24:00", 0)

          result.dataList.forEach { heartRateData ->
            val time = LocalDateTime.ofInstant(heartRateData.startTime, heartRateData.zoneOffset)
            when {
              time.isBetween(0, 5) -> processHeartRateData(heartRateData, hrOfFirstQuarter)
              time.isBetween(6, 11) -> processHeartRateData(heartRateData, hrOfSecondQuarter)
              time.isBetween(12, 17) -> processHeartRateData(heartRateData, hrOfThirdQuarter)
              time.isBetween(18, 23) -> processHeartRateData(heartRateData, hrOfFourthQuarter)
            }
          }

          val heartRateResult = Arguments.createArray()
          
          listOf(hrOfFirstQuarter, hrOfSecondQuarter, hrOfThirdQuarter, hrOfFourthQuarter).forEach { hrQuarter ->
            hrQuarter.apply { 
              if (hrQuarter.count != 0) { 
                hrQuarter.avg /= hrQuarter.count

                val entry = Arguments.createMap()
                entry.putDouble("min", hrQuarter.min.toDouble())
                entry.putDouble("max", hrQuarter.max.toDouble())
                entry.putDouble("avg", hrQuarter.avg.toDouble())
                entry.putString("startTime", hrQuarter.startTime)
                entry.putString("endTime", hrQuarter.endTime)
           
                heartRateResult.pushMap(entry)  
              }
            }
          }

          val response = Arguments.createMap()
          response.putArray("data", heartRateResult)
          promise.resolve(response)
        } catch (e: Exception) {
          promise.reject("READING_ERROR", "Error reading Samsung Health Heart Rate Data", e)
        }
      }
    } catch (e: Exception) {
      promise.reject("DATE_PARSING_ERROR", "Error parsing date strings", e)
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
      "SLEEP" -> DataTypes.SLEEP
      "HEART_RATE" -> DataTypes.HEART_RATE
      else -> null
    }
  }

  private fun getStringFromDataType(dataType: DataType): String {
    return when (dataType) {
      DataTypes.STEPS -> "STEPS"
      DataTypes.SLEEP -> "SLEEP"
      DataTypes.HEART_RATE -> "HEART_RATE"
      else -> dataType.toString()
    }
  }

  @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
  private fun prepareSleepScore(healthDataPoint: HealthDataPoint): Int? {
    var sleepScore: Int? = null
    sleepScore = healthDataPoint.getValue(DataType.SleepType.SLEEP_SCORE)
    return sleepScore
  }

  private fun processHeartRateData(heartRateData: HealthDataPoint, hrQuarter: HeartRate) {
    hrQuarter.apply {
      heartRateData.getValue(DataType.HeartRateType.HEART_RATE)?.let {
        avg += it
        count++
      }
      
      heartRateData.getValue(DataType.HeartRateType.MAX_HEART_RATE)?.let {
        max = maxOf(max, it)
      }
            
      heartRateData.getValue(DataType.HeartRateType.MIN_HEART_RATE)?.let {
        if (min != 0f) {
          min = minOf(min, it)
        }
      }
    }
  }

  private fun LocalDateTime.isBetween(fromHour: Int, toHour: Int) =
      this >= this.withHour(fromHour).withMinute(0).withSecond(0) &&
              this <= this.withHour(toHour).withMinute(59).withSecond(59)

  companion object {
    const val NAME = "RnSamsungHealthDataApi"
    const val APP_TAG = "RnSamsungHealthDataApi"
  }
}
