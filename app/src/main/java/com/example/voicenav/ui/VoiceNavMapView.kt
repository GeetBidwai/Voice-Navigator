package com.example.voicenav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.DarkBackground
import com.example.voicenav.core.model.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polyline as OsmPolyline

/**
 * Real interactive OpenStreetMap view integrated seamlessly into Voice Nav.
 * Uses official OpenStreetMap standard tiles (TileSourceFactory.MAPNIK) under OSM Tile Usage Policy
 * with a high-contrast dark color filter that matches Voice Nav's #12161B aesthetic.
 * This completely eliminates third-party "API KEY REQUIRED" tile watermarks while maintaining
 * legal, documented, and free open tile delivery.
 */
@Composable
fun VoiceNavMapView(
    currentLocation: GeoPoint?,
    routePoints: List<GeoPoint>,
    destinationPoint: GeoPoint?,
    destinationName: String,
    recenterTrigger: Int,
    modifier: Modifier = Modifier,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current

    // Initialize osmdroid configuration with required User-Agent
    remember {
        try {
            val prefs = context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
            Configuration.getInstance().load(context, prefs)
            Configuration.getInstance().userAgentValue = context.packageName
        } catch (_: Exception) {}
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setBuiltInZoomControls(false)

            // Standard OpenStreetMap tiles (100% free, no API key required, no watermarks)
            setTileSource(TileSourceFactory.MAPNIK)

            // High-contrast motorcycle night mode color matrix filter:
            // Converts light tiles into a deep #12161B dark-slate theme,
            // highlights streets in high contrast, and keeps labels crisp and legible.
            val darkFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    set(
                        floatArrayOf(
                            -0.78f,  0.00f,  0.00f, 0.0f, 185f,
                             0.00f, -0.78f,  0.00f, 0.0f, 190f,
                             0.00f,  0.00f, -0.72f, 0.0f, 205f,
                             0.00f,  0.00f,  0.00f, 1.0f,   0f
                        )
                    )
                }
            )
            overlayManager.tilesOverlay.setColorFilter(darkFilter)

            // Default center: India / Pune (18.5204, 73.8567) until GPS locks
            controller.setZoom(14.0)
            controller.setCenter(OsmGeoPoint(18.5204, 73.8567))
        }
    }

    // Lifecycle handling for MapView
    DisposableEffect(mapView) {
        mapView.onResume()
        onMapReady(mapView)
        onDispose {
            mapView.onPause()
        }
    }

    // Animate to current location on initial GPS fix or when recenter button is tapped
    LaunchedEffect(currentLocation, recenterTrigger) {
        if (currentLocation != null) {
            val target = OsmGeoPoint(currentLocation.latitude, currentLocation.longitude)
            if (routePoints.isEmpty()) {
                mapView.controller.animateTo(target, 16.0, 800L)
            } else if (recenterTrigger > 0) {
                mapView.controller.animateTo(target, 16.5, 600L)
            }
        }
    }

    // Update map overlays (User GPS Marker, Destination Marker, Route Polyline)
    LaunchedEffect(currentLocation, routePoints, destinationPoint) {
        mapView.overlays.clear()

        // 1. Draw route polyline if navigation route is active
        if (routePoints.isNotEmpty()) {
            val osmPoints = routePoints.map { OsmGeoPoint(it.latitude, it.longitude) }
            val polyline = OsmPolyline(mapView).apply {
                outlinePaint.color = Color.parseColor("#F2A900") // Voice Nav amber accent
                outlinePaint.strokeWidth = 14f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                setPoints(osmPoints)
            }
            mapView.overlays.add(polyline)

            // Zoom bounding box to show the whole calculated route
            try {
                var minLat = 90.0
                var maxLat = -90.0
                var minLon = 180.0
                var maxLon = -180.0
                for (pt in routePoints) {
                    if (pt.latitude < minLat) minLat = pt.latitude
                    if (pt.latitude > maxLat) maxLat = pt.latitude
                    if (pt.longitude < minLon) minLon = pt.longitude
                    if (pt.longitude > maxLon) maxLon = pt.longitude
                }
                currentLocation?.let {
                    if (it.latitude < minLat) minLat = it.latitude
                    if (it.latitude > maxLat) maxLat = it.latitude
                    if (it.longitude < minLon) minLon = it.longitude
                    if (it.longitude > maxLon) maxLon = it.longitude
                }

                val bbox = BoundingBox(maxLat + 0.005, maxLon + 0.005, minLat - 0.005, minLon - 0.005)
                mapView.zoomToBoundingBox(bbox, true, 80)
            } catch (_: Exception) {}
        }

        // 2. Draw destination marker
        if (destinationPoint != null) {
            val destMarker = OsmMarker(mapView).apply {
                position = OsmGeoPoint(destinationPoint.latitude, destinationPoint.longitude)
                title = destinationName.ifEmpty { "Destination" }
                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(destMarker)
        }

        // 3. Draw live user GPS location marker
        if (currentLocation != null) {
            val userMarker = OsmMarker(mapView).apply {
                position = OsmGeoPoint(currentLocation.latitude, currentLocation.longitude)
                title = "Your Location"
                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
            }
            mapView.overlays.add(userMarker)
        }

        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
