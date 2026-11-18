package com.ennogrensemann.reminder.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeocodingResult(
    val displayName: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double
)

object NominatimGeocoder {

    private const val USER_AGENT = "GeoTasksReminderApp/1.0 (https://github.com/EnnoGrensemann/reminder)"

    suspend fun searchAddress(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&addressdetails=1")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "de,en;q=0.8")
                connectTimeout = 7000
                readTimeout = 7000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext emptyList()
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(responseText)
            val results = mutableListOf<GeocodingResult>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val displayName = obj.optString("display_name", "")
                val lat = obj.optString("lat", "").toDoubleOrNull() ?: continue
                val lon = obj.optString("lon", "").toDoubleOrNull() ?: continue
                val shortName = extractShortName(obj, displayName)

                results.add(
                    GeocodingResult(
                        displayName = displayName,
                        shortName = shortName,
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&addressdetails=1")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "de,en;q=0.8")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(responseText)
            val displayName = obj.optString("display_name", "")
            extractShortName(obj, displayName).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractShortName(obj: JSONObject, fallbackDisplayName: String): String {
        val name = obj.optString("name", "")
        val address = obj.optJSONObject("address")
        if (address != null) {
            val road = address.optString("road", "")
            val houseNumber = address.optString("house_number", "")
            val city = address.optString("city", "")
                .ifBlank { address.optString("town", "") }
                .ifBlank { address.optString("village", "") }
                .ifBlank { address.optString("suburb", "") }

            val streetPart = listOf(road, houseNumber).filter { it.isNotBlank() }.joinToString(" ")
            val parts = listOf(name, streetPart, city).filter { it.isNotBlank() }.distinct()
            if (parts.isNotEmpty()) {
                return parts.take(2).joinToString(", ")
            }
        }
        return fallbackDisplayName.split(",").take(2).joinToString(",").trim()
    }
}
