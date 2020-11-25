package com.example.android.treasureHunt

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.android.treasureHunt.service.LocationForegroundService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*


private const val TAG = "GeofenceManager"

object GeofenceManager {

    private var locationService: LocationForegroundService? = null
    private lateinit var geofencePendingIntent: PendingIntent
    private lateinit var geofencingClient: GeofencingClient

    fun init(context: Context) {
        geofencingClient = LocationServices.getGeofencingClient(context)
        geofencePendingIntent = getGeofencePendingIntent(context)
    }


    fun attachStrategy(context: Context) {
        checkDeviceLocationSettingsAndStartGeofence(context = context,
            onError = {
                val notificationManager = ContextCompat.getSystemService(
                    context,
                    NotificationManager::class.java
                ) as NotificationManager

                notificationManager.sendErrorMessage(
                    context
                )
            })
    }


    /*
    *  Uses the Location Client to check the current state of location settings, and gives the user
    *  the opportunity to turn on location services within our app.
    */
    @SuppressLint("MissingPermission")
    fun checkDeviceLocationSettingsAndStartGeofence(
        resolve: Boolean = true,
        context: Context,
        onError: () -> Unit
    ) {
        removeGeofences(context)

        if (locationService == null) {
            context.bindService(Intent(context, LocationForegroundService::class.java),
                object: ServiceConnection {
                    override fun onServiceDisconnected(name: ComponentName?) {
                        this@GeofenceManager.locationService = null
                    }

                    override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                        serviceBinder as LocationForegroundService.LocalBinder
                        this@GeofenceManager.locationService = serviceBinder.service
                        locationService?.startLocationUpdates()
                    }
                }, Context.BIND_AUTO_CREATE)
        }

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(context)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    if (context is Activity) {
                        exception.startResolutionForResult(
                            context,
                            REQUEST_TURN_DEVICE_LOCATION_ON
                        )
                    }

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                onError()

            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                context.let { it1 -> addGeofenceForClue(it1) }
            }
        }
    }

    /*
    * Adds a Geofence for the current clue if needed, and removes any existing Geofence. This
    * method should be called after the user has granted the location permission.  If there are
    * no more geofences, we remove the geofence and let the viewmodel know that the ending hint
    * is now "active."
    */
    @SuppressLint("MissingPermission")
    private fun addGeofenceForClue(context: Context) {

        val currentGeofenceDataList = GeofencingConstants.LANDMARK_DATA

        currentGeofenceDataList.forEach { currentGeofenceData ->
            // Build the Geofence Object
            val geofence = Geofence.Builder()
                // Set the request ID, string to identify the geofence.
                .setRequestId(currentGeofenceData.id)
                // Set the circular region of this geofence.
                .setCircularRegion(
                    currentGeofenceData.latLong.latitude,
                    currentGeofenceData.latLong.longitude,
                    GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
                )
                // Set the expiration duration of the geofence. This geofence gets
                // automatically removed after this period of time.
                .setExpirationDuration(Geofence.NEVER_EXPIRE)

                .setLoiteringDelay(1000)
                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
            .build()

            // Build the geofence request
            val geofencingRequest = GeofencingRequest.Builder()
                // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
                // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
                // is already inside that geofence.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

                // Add the geofences to be monitored by geofencing service.
                .addGeofence(geofence)
                .build()

            // First, remove any existing geofences that use our pending intent
            geofencingClient.removeGeofences(geofencePendingIntent)?.run {
                // Regardless of success/failure of the removal, add the new geofence
                addOnCompleteListener {
                    // Add the new geofence request with the new geofence
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                        addOnSuccessListener {
                            // Geofences added.
                            Toast.makeText(
                                context, R.string.geofences_added,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            Log.e("Add Geofence", geofence.requestId)
                            // Tell the viewmodel that we've reached the end of the game and
                            // activated the last "geofence" --- by removing the Geofence.
                        }
                        addOnFailureListener {
                            // Failed to add geofences.
                            Toast.makeText(
                                context, R.string.geofences_not_added,
                                Toast.LENGTH_SHORT
                            ).show()
                            if ((it.message != null)) {
                                Log.w(TAG, it.message)
                            }
                        }
                    }
                }
            }
        }

    }


    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    private fun removeGeofences(context: Context) {
        /* if (!foregroundAndBackgroundLocationPermissionApproved(context)) {
             return
         }*/
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                Log.d(TAG, context.getString(R.string.geofences_removed))
                Toast.makeText(context, R.string.geofences_removed, Toast.LENGTH_SHORT)
                    .show()
            }
            addOnFailureListener {
                // Failed to remove geofences
                Log.d(TAG, context.getString(R.string.geofences_not_removed))
            }
        }
    }


    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = HuntMainActivity.ACTION_GEOFENCE_EVENT
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


}

const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
const val LOCATION_PERMISSION_INDEX = 0
const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1