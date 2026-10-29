package com.example.voicenav.routing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.voicenav.core.model.GeoPoint
import com.example.voicenav.core.model.ManeuverInstruction
import com.example.voicenav.core.model.NavigationSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class RoutingResult {
    data class Success(val session: NavigationSession) : RoutingResult()
    data class Error(val message: String) : RoutingResult()
}

/**
 * Handles OpenRouteService Geocoding and Driving Directions APIs.
 * Securely persists the user's API key in SharedPreferences locally.
 */
class OpenRouteServiceRepository(context: Context) {
    private val tag = "ORSRepository"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_nav_preferences", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PREF_API_KEY = "ors_api_key"
        private const val GEOCODE_URL = "https://api.openrouteservice.org/geocode/search"
        private const val DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car/geojson"
    }

    fun getStoredApiKey(): String {
        return prefs.getString(PREF_API_KEY, "")?.trim() ?: ""
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(PREF_API_KEY, key.trim()).apply()
    }

    fun hasValidApiKey(): Boolean {
        return getStoredApiKey().isNotEmpty()
    }

    /**
     * Geocode a destination text query into a GeoPoint coordinate.
     */
    suspend fun geocodeDestination(
        destinationQuery: String,
        proximity: GeoPoint? = null
    ): Result<GeoPoint> = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("OpenRouteService API key not configured. Tap settings icon to enter your key.")
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(destinationQuery, "UTF-8")
            var url = "$GEOCODE_URL?api_key=$apiKey&text=$encodedQuery&boundary.country=IND&size=1"
            if (proximity != null) {
                url += "&focus.point.lat=${proximity.latitude}&focus.point.lon=${proximity.longitude}"
            }

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("Geocoding failed: $errorMsg"))
            }

            val json = JSONObject(bodyString)
            val features = json.optJSONArray("features")
            if (features == null || features.length() == 0) {
                return@withContext Result.failure(Exception("Destination '$destinationQuery' not found"))
            }

            val firstFeature = features.getJSONObject(0)
            val geometry = firstFeature.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")
            val lon = coords.getDouble(0)
            val lat = coords.getDouble(1)

            val properties = firstFeature.optJSONObject("properties")
            val label = properties?.optString("label") ?: destinationQuery

            Result.success(GeoPoint(latitude = lat, longitude = lon, label = label))
        } catch (e: Exception) {
            Log.e(tag, "Geocoding error", e)
            Result.failure(Exception("Geocoding network error: ${e.message}"))
        }
    }

    /**
     * Search multiple destination suggestions for user search queries.
     */
    suspend fun searchDestinations(
        query: String,
        proximity: GeoPoint? = null,
        limit: Int = 5
    ): Result<List<GeoPoint>> = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("API key not configured. Tap ⚙ to enter your OpenRouteService key.")
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            var url = "$GEOCODE_URL?api_key=$apiKey&text=$encodedQuery&boundary.country=IND&size=$limit"
            if (proximity != null) {
                url += "&focus.point.lat=${proximity.latitude}&focus.point.lon=${proximity.longitude}"
            }

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("Search error: $errorMsg"))
            }

            val json = JSONObject(bodyString)
            val features = json.optJSONArray("features")
            if (features == null || features.length() == 0) {
                return@withContext Result.success(emptyList())
            }

            val results = mutableListOf<GeoPoint>()
            for (i in 0 until features.length()) {
                val feat = features.getJSONObject(i)
                val geom = feat.getJSONObject("geometry")
                val coords = geom.getJSONArray("coordinates")
                val lon = coords.getDouble(0)
                val lat = coords.getDouble(1)
                val props = feat.optJSONObject("properties")
                val label = props?.optString("label") ?: props?.optString("name") ?: query
                results.add(GeoPoint(latitude = lat, longitude = lon, label = label))
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e(tag, "Search destinations error", e)
            Result.failure(Exception("Search error: ${e.message}"))
        }
    }

    /**
     * Fetch driving route between start and destination using OpenRouteService Directions API.
     */
    suspend fun getRoute(
        start: GeoPoint,
        destination: GeoPoint
    ): RoutingResult = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey()
        if (apiKey.isEmpty()) {
            return@withContext RoutingResult.Error(
                "OpenRouteService API key not configured. Tap settings icon to enter your key."
            )
        }

        try {
            val payload = JSONObject().apply {
                put("coordinates", org.json.JSONArray().apply {
                    put(org.json.JSONArray().apply {
                        put(start.longitude)
                        put(start.latitude)
                    })
                    put(org.json.JSONArray().apply {
                        put(destination.longitude)
                        put(destination.latitude)
                    })
                })
                put("instructions", true)
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(DIRECTIONS_URL)
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext RoutingResult.Error("Routing failed: $errorMsg")
            }

            val json = JSONObject(bodyString)
            val features = json.optJSONArray("features")
            if (features == null || features.length() == 0) {
                return@withContext RoutingResult.Error("No route found between locations")
            }

            val feature = features.getJSONObject(0)
            val properties = feature.getJSONObject("properties")
            val segments = properties.getJSONArray("segments")
            if (segments.length() == 0) {
                return@withContext RoutingResult.Error("No route segments available")
            }

            val segment = segments.getJSONObject(0)
            val distance = segment.optDouble("distance", 0.0).toInt()
            val duration = segment.optDouble("duration", 0.0).toInt()
            val stepsJson = segment.optJSONArray("steps")

            val maneuvers = mutableListOf<ManeuverInstruction>()
            if (stepsJson != null) {
                for (i in 0 until stepsJson.length()) {
                    val stepObj = stepsJson.getJSONObject(i)
                    val instruction = stepObj.optString("instruction", "")
                    val stepDist = stepObj.optDouble("distance", 0.0).toInt()
                    val streetName = stepObj.optString("name", "")
                    val turnType = stepObj.optInt("type", 0).toString()

                    if (instruction.isNotEmpty()) {
                        maneuvers.add(
                            ManeuverInstruction(
                                instruction = instruction,
                                distanceMeters = stepDist,
                                streetName = streetName,
                                turnType = turnType
                            )
                        )
                    }
                }
            }

            val routePoints = mutableListOf<GeoPoint>()
            val geometry = feature.optJSONObject("geometry")
            val coordsArray = geometry?.optJSONArray("coordinates")
            if (coordsArray != null) {
                for (i in 0 until coordsArray.length()) {
                    val pt = coordsArray.getJSONArray(i)
                    val lon = pt.getDouble(0)
                    val lat = pt.getDouble(1)
                    routePoints.add(GeoPoint(latitude = lat, longitude = lon))
                }
            }

            val session = NavigationSession(
                isActive = true,
                destinationName = destination.label.ifEmpty { "Destination" },
                destinationCoords = destination,
                maneuvers = maneuvers,
                currentStepIndex = 0,
                totalDistanceMeters = distance,
                totalDurationSeconds = duration,
                routePoints = routePoints
            )

            RoutingResult.Success(session)
        } catch (e: Exception) {
            Log.e(tag, "Routing network error", e)
            RoutingResult.Error("Routing network error: ${e.message}")
        }
    }
}
