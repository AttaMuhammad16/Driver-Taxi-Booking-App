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
    private var marker: Marker? = null
    private var isStartMarkerRemoved = false


    fun drawRoute(route: ArrayList<Route>, shortestRouteIndex: Int, context: Activity, onGoogleMap: GoogleMap, title:String, color: Int, startMarkerBol:Boolean=true) {

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
            updateOrAddMarker(polylineStartLatLng!!,onGoogleMap,title)
        }
        updateOrAddMarker(polylineEndLatLng!!,onGoogleMap,title)
    }

    fun clearMapObjects() {
        for (polyline in polylines) {
            polyline.remove()
        }
        polylines.clear()
    }

    fun removeMarker(){
        marker?.remove()
    }

    fun updateOrAddMarker(pickUpLatLang: LatLng, map: GoogleMap, title: String) {
        if (isStartMarkerRemoved) {
            return
        }
        if (marker == null) {
            val markerOptions = MarkerOptions()
            markerOptions.position(pickUpLatLang)
            markerOptions.title(title)
            marker = map.addMarker(markerOptions)
        } else {
            marker?.position = pickUpLatLang
            marker?.title = title
        }
        marker?.showInfoWindow()
    }

}