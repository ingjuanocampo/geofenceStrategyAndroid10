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

import android.content.Context
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit

/**
 * Returns the error string for a geofencing error code.
 */
fun errorMessage(context: Context, errorCode: Int): String {
    val resources = context.resources
    return when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
            R.string.geofence_not_available
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
            R.string.geofence_too_many_geofences
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
            R.string.geofence_too_many_pending_intents
        )
        else -> resources.getString(R.string.unknown_geofence_error)
    }
}

/**
 * Stores latitude and longitude information along with a hint to help user find the location.
 */
data class LandmarkDataObject(val id: String, val hint: Int, val name: Int, val latLong: LatLng)

internal object GeofencingConstants {

    /**
     * Used to set an expiration time for a geofence. After this amount of time, Location services
     * stops tracking the geofence. For this sample, geofences expire after one hour.
     */
    val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)

    val LANDMARK_DATA = arrayOf(
        LandmarkDataObject(
            "home",
            R.string.golden_gate_bridge_hint,
            R.string.golden_gate_bridge_location,
            LatLng(6.269567,-75.591526)),

        LandmarkDataObject(
            "merquepaisa",
            R.string.ferry_building_hint,
            R.string.ferry_building_location,
            LatLng(6.262945, -75.588908)),

        LandmarkDataObject(
            "onix",
            R.string.onix,
            R.string.onix,
            LatLng(6.270847, -75.590340)),

        LandmarkDataObject(
            "luisamigo",
            R.string.luis_amigo,
            R.string.luis_amigo,
            LatLng(6.259108, -75.584084)),


        LandmarkDataObject(
           "union_square",
            R.string.union_square_hint,
            R.string.union_square_location,
            LatLng(37.788348, -122.407112)),

                LandmarkDataObject(
                "automontana",
        R.string.union_square_hint,
        R.string.automontana,
        LatLng(6.227958, -75.570317)),


        LandmarkDataObject(
            "bodytech",
            R.string.body_tech,
            R.string.body_tech,
            LatLng(6.256802, -75.582449))
    )

    val NUM_LANDMARKS = LANDMARK_DATA.size
    const val GEOFENCE_RADIUS_IN_METERS = 100f
    const val EXTRA_GEOFENCE_INDEX = "GEOFENCE_INDEX"
    const val EXTRA_GEOFENCE_NOT_LOCATION_ACCESS = "EXTRA_GEOFENCE_NOT_LOCATION_ACCESS"

}
