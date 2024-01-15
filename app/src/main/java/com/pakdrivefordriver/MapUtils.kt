package com.pakdrive


import android.app.Activity
import android.util.Log
import com.directions.route.Route
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object MapUtils {

    var polylines: ArrayList<Polyline> = ArrayList()
    var markers: ArrayList<Marker> = ArrayList()

    fun showPlaces(map:GoogleMap,places:ArrayList<LatLng>){
        places.forEach {
            map.addMarker(MarkerOptions().position(it))
        }
    }

    fun drawRoute(route: ArrayList<Route>, shortestRouteIndex: Int, context: Activity, onGoogleMap: GoogleMap, st: String, dt: String, color: Int, startMarkerBol:Boolean=true) {

        val newRoutePoints = route[shortestRouteIndex].points
        if (polylines.isNotEmpty() && polylines[0].points == newRoutePoints) {
            return
        }

        clearMapObjects()

        val polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = null
        var polylineEndLatLng: LatLng? = null

        for (i in route.indices) {
            if (i == shortestRouteIndex) {
                polyOptions.color(context.resources.getColor(color))
                polyOptions.width(5f)
                polyOptions.addAll(route[shortestRouteIndex].points)

                val polyline = onGoogleMap.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                val k = polyline.points.size
                polylineEndLatLng = polyline.points[k - 1]
                polylines.add(polyline)
            }
        }

        if (startMarkerBol){
            val startMarker = MarkerOptions()
            startMarker.position(polylineStartLatLng!!)
            startMarker.title(st)
            val startMarkerObject = onGoogleMap.addMarker(startMarker)
            markers.add(startMarkerObject!!)
        }

        val endMarker = MarkerOptions()
        endMarker.position(polylineEndLatLng!!)
        endMarker.title(dt)
        val endMarkerObject = onGoogleMap.addMarker(endMarker)
        markers.add(endMarkerObject!!)
    }

    fun clearMapObjects() {
        for (polyline in polylines) {
            polyline.remove()
        }
        polylines.clear()

        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
    }

    fun removePreviousMarkers(markersList: MutableList<Marker>) {
        if (markersList.isEmpty()){
            return
        }
        for (marker in markersList) {
            marker.remove()
        }
        markersList.clear()
    }

}