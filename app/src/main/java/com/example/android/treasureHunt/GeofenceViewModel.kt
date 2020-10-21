/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.treasureHunt

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.work.*
import java.util.concurrent.TimeUnit

/*
 * This class contains the state of the game.  The two important pieces of state are the index
 * of the geofence, which is the geofence that the game thinks is active, and the state of the
 * hint being shown.  If the hint matches the geofence, then the Activity won't update the geofence
 * as it cycles through various activity states.
 *
 * These states are stored in SavedState, which matches the Android lifecycle.  Destroying the
 * associated Activity with the back action will delete all state and reset the game, while
 * the Home action will cause the state to be saved, even if the game is terminated by Android in
 * the background.
 */

private const val TAG = "GeofenceViewModel"

class GeofenceViewModel(state: SavedStateHandle) : ViewModel() {

    private lateinit var locationWorkRequest: PeriodicWorkRequest
    private lateinit var geofenceRequest: PeriodicWorkRequest
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q



    private val _geofenceIndex = state.getLiveData(GEOFENCE_INDEX_KEY, -1)
    private val _hintIndex = state.getLiveData(HINT_INDEX_KEY, 0)
    val geofenceIndex: LiveData<Int>
        get() = _geofenceIndex

    val geofenceHintResourceId = Transformations.map(geofenceIndex) {
        val index = geofenceIndex?.value ?: -1
        when {
            index < 0 -> R.string.not_started_hint
            index < GeofencingConstants.NUM_LANDMARKS -> GeofencingConstants.LANDMARK_DATA[geofenceIndex.value!!].hint
            else -> R.string.geofence_over
        }
    }

    val geofenceImageResourceId = Transformations.map(geofenceIndex) {
        val index = geofenceIndex.value ?: -1
        when {
            index < GeofencingConstants.NUM_LANDMARKS -> R.drawable.android_map
            else -> R.drawable.android_treasure
        }
    }

    fun updateHint(currentIndex: Int) {
        _hintIndex.value = currentIndex+1
    }

    fun geofenceActivated() {
        _geofenceIndex.value = _hintIndex.value
    }

    fun geofenceIsActive() =_geofenceIndex.value == _hintIndex.value

    fun nextGeofenceIndex() = _hintIndex.value ?: 0

    fun init(activity: Context) {
        GeofenceManager.init(activity)
       /* geofenceRequest =
            PeriodicWorkRequestBuilder<GeofenceManager>(10, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()*/
        /*locationWorkRequest =
            PeriodicWorkRequestBuilder<MyLocationManager>(10, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()*/

    }

    fun checkDeviceLocationSettingsAndStartGeofence(
        context: Context
    , workManager: WorkManager
    ) {
        GeofenceManager.doWork(context)
        //workManager.enqueue(locationWorkRequest)

        //GeofenceManager.checkDeviceLocationSettingsAndStartGeofence(resolve, context as Activity, onError)
    }

    fun checkPermissionsAndStartGeofencing(context: Context, workManager: WorkManager) {
        if (geofenceIsActive()) return
        if (foregroundAndBackgroundLocationPermissionApproved(context)) {
            checkDeviceLocationSettingsAndStartGeofence(context, workManager)
        } else {
            requestForegroundAndBackgroundLocationPermissions(context)
        }
    }

    /*
*  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
*/
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions(context: Context) {
        if (foregroundAndBackgroundLocationPermissionApproved(context))
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                permissionsArray,
                resultCode
            )
        }
    }

    /*
*  Determines whether the app has the appropriate permissions across Android 10+ and all other
*  Android versions.
*/
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(context: Context): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }


    fun processPermissionRequest(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onDenied: () -> Unit,
        onGranted: () -> Unit
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            onDenied()
        } else {
            onGranted()
        }
    }


}

private const val HINT_INDEX_KEY = "hintIndex"
private const val GEOFENCE_INDEX_KEY = "geofenceIndex"
