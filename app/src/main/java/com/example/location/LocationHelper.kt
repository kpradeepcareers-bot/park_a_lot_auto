package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val address: String = ""
)

typealias PlaceResult = LocationHelper.LocationDetails

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val addressStr = getAddressFromCoordinates(location.latitude, location.longitude)
                    continuation.resume(
                        LocationResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            address = addressStr
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    data class LocationDetails(
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val type: String = "Office"
    ) {
        val suggestedType: String
            get() = when {
                type.isNotBlank() && type != "Office" -> type
                name.contains("Office", true) || name.contains("Deloitte", true) || name.contains("Tower", true) || name.contains("Park", true) || name.contains("Campus", true) -> "Office"
                name.contains("Mall", true) -> "Mall"
                name.contains("Airport", true) -> "Airport"
                name.contains("Hospital", true) -> "Hospital"
                name.contains("Home", true) -> "Home"
                else -> "Office"
            }
    }

    fun getDetailsFromCoordinates(latitude: Double, longitude: Double): LocationDetails {
        // First check known landmarks for exact recognized place names
        val known = KNOWN_LANDMARKS.minByOrNull {
            val dLat = it.latitude - latitude
            val dLng = it.longitude - longitude
            dLat * dLat + dLng * dLng
        }
        if (known != null) {
            val dist = FloatArray(1)
            Location.distanceBetween(latitude, longitude, known.latitude, known.longitude, dist)
            if (dist[0] < 80f) {
                return LocationDetails(
                    name = known.name,
                    address = known.address,
                    latitude = latitude,
                    longitude = longitude
                )
            }
        }

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val feature = addr.featureName
                val subLocality = addr.subLocality
                val locality = addr.locality
                val street = addr.thoroughfare

                val placeName = when {
                    !feature.isNullOrBlank() && !feature.matches(Regex("^[0-9+]+$")) -> feature
                    !street.isNullOrBlank() -> street
                    !subLocality.isNullOrBlank() -> subLocality
                    !locality.isNullOrBlank() -> locality
                    else -> "Saved Location"
                }

                val fullAddress = addr.getAddressLine(0)
                    ?: listOfNotNull(street, subLocality, locality).joinToString(", ")

                LocationDetails(
                    name = placeName,
                    address = fullAddress.ifEmpty { "Lat: %.4f, Lng: %.4f".format(latitude, longitude) },
                    latitude = latitude,
                    longitude = longitude
                )
            } else {
                LocationDetails(
                    name = "Location (%.4f, %.4f)".format(latitude, longitude),
                    address = "Lat: %.4f, Lng: %.4f".format(latitude, longitude),
                    latitude = latitude,
                    longitude = longitude
                )
            }
        } catch (_: Exception) {
            LocationDetails(
                name = "Location (%.4f, %.4f)".format(latitude, longitude),
                address = "Lat: %.4f, Lng: %.4f".format(latitude, longitude),
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    fun searchPlaces(query: String): List<LocationDetails> {
        val q = query.trim().lowercase()
        val matchedKnown = KNOWN_LANDMARKS.filter {
            it.name.lowercase().contains(q) || it.address.lowercase().contains(q)
        }

        val geocoded = try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 5) ?: emptyList()
            addresses.map { addr ->
                val name = addr.featureName ?: addr.thoroughfare ?: addr.locality ?: query
                val fullAddress = addr.getAddressLine(0) ?: "$name, ${addr.locality ?: ""}"
                LocationDetails(
                    name = name,
                    address = fullAddress,
                    latitude = addr.latitude,
                    longitude = addr.longitude
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        return (matchedKnown + geocoded).distinctBy { "${it.name}_${it.latitude}_${it.longitude}" }
    }

    companion object {
        val KNOWN_LANDMARKS = listOf(
            LocationDetails("Deloitte Hyderabad", "Hitec City, Madhapur, Hyderabad, Telangana 500081", 17.4375, 78.3752, "Office"),
            LocationDetails("Mindspace Business Park", "Mindspace IT Park, Madhapur, Hyderabad 500081", 17.4399, 78.3812, "Office"),
            LocationDetails("Inorbit Mall Cyberabad", "APIIC Software Layout, Mindspace, Madhapur 500081", 17.4348, 78.3867, "Mall"),
            LocationDetails("Microsoft Campus", "ISB Road, Gachibowli, Hyderabad 500032", 17.4262, 78.3448, "Office"),
            LocationDetails("Amazon Development Centre", "Financial District, Nanakramguda, Hyderabad 500032", 17.4156, 78.3427, "Office"),
            LocationDetails("Google Signature Tower", "Financial District, Puppalaguda, Hyderabad 500075", 17.4128, 78.3562, "Office"),
            LocationDetails("Cyber Towers", "Hitec City Main Rd, Patrika Nagar, Madhapur 500081", 17.4504, 78.3808, "Office"),
            LocationDetails("Hyderabad Airport (RGIA)", "Shamshabad, Hyderabad, Telangana 500409", 17.2403, 78.4294, "Airport"),
            LocationDetails("Phoenix Marketcity", "Velachery Main Rd, Indira Gandhi Nagar, Chennai 600042", 12.9918, 80.2173, "Mall"),
            LocationDetails("Prestige Tech Park", "Marathahalli - Sarjapur Outer Ring Rd, Bengaluru 560103", 12.9352, 77.6946, "Office"),
            LocationDetails("Manyata Embassy Business Park", "Outer Ring Rd, Nagavara, Bengaluru 560045", 13.0475, 77.6200, "Office"),
            LocationDetails("DLF Cyber City", "DLF Phase 2, Sector 24, Gurugram, Haryana 122002", 28.4950, 77.0895, "Office")
        )
    }

    /**
     * Parses and resolves Google Maps URLs (short links, place links, search links, coordinates)
     * and extracts exact Latitude, Longitude, Place Name, and Address.
     */
    suspend fun resolveGoogleMapsUrlOrQuery(input: String): LocationDetails? {
        val raw = input.trim()
        if (raw.isBlank()) return null

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Check direct coordinates e.g. "17.4375, 78.3752" or "17.4375,78.3752"
                val coordRegex = Regex("""^(-?\d+(?:\.\d+)?)[,\s]+(-?\d+(?:\.\d+)?)$""")
                val coordMatch = coordRegex.find(raw)
                if (coordMatch != null) {
                    val lat = coordMatch.groupValues[1].toDoubleOrNull()
                    val lng = coordMatch.groupValues[2].toDoubleOrNull()
                    if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                        return@withContext getDetailsFromCoordinates(lat, lng)
                    }
                }

                // 2. Normalize potential Google Maps URLs
                var urlToResolve = raw
                if (raw.startsWith("aps/") || raw.startsWith("maps/")) {
                    urlToResolve = "https://maps.app.goo.gl/" + raw.substringAfter("/")
                } else if (raw.startsWith("maps.app.goo.gl/") || raw.startsWith("goo.gl/maps/")) {
                    urlToResolve = "https://$raw"
                }

                var resolvedUrl = urlToResolve
                var placeNameFromUrl: String? = null

                // 3. If it looks like a web URL or Google Maps link, follow redirects
                if (urlToResolve.startsWith("http://") || urlToResolve.startsWith("https://") || urlToResolve.contains("goo.gl") || urlToResolve.contains("google.com/maps")) {
                    try {
                        val connection = java.net.URL(urlToResolve).openConnection() as java.net.HttpURLConnection
                        connection.instanceFollowRedirects = false
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                        connection.connectTimeout = 8000
                        connection.readTimeout = 8000
                        connection.connect()

                        val responseCode = connection.responseCode
                        if (responseCode in 300..399) {
                            val locationHeader = connection.getHeaderField("Location")
                            if (!locationHeader.isNullOrBlank()) {
                                resolvedUrl = locationHeader
                                // If the redirect is another relative or short url, follow once more
                                if (resolvedUrl.startsWith("/")) {
                                    resolvedUrl = "https://www.google.com$resolvedUrl"
                                }
                            }
                        } else if (responseCode == 200) {
                            resolvedUrl = connection.url.toString()
                        }
                        connection.disconnect()
                    } catch (e: Exception) {
                        // Keep current URL or proceed
                    }
                }

                // 4. Extract Place Name from URL if present (e.g. /place/Cyber+Towers/)
                val placeNameRegex = Regex("""/place/([^/@?]+)""")
                val placeMatch = placeNameRegex.find(resolvedUrl)
                if (placeMatch != null) {
                    placeNameFromUrl = java.net.URLDecoder.decode(placeMatch.groupValues[1].replace("+", " "), "UTF-8")
                }

                // 5. Extract coordinates from patterns like:
                // @17.4375,78.3752 or ?q=17.4375,78.3752 or ll=17.4375,78.3752 or destination=17.4375,78.3752
                val atPattern = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")
                val qPattern = Regex("""[?&](?:q|ll|destination|center)=(-?\d+\.\d+)[,+](-?\d+\.\d+)""")
                val ftidPattern = Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)""")

                val atMatch = atPattern.find(resolvedUrl)
                val qMatch = qPattern.find(resolvedUrl)
                val ftidMatch = ftidPattern.find(resolvedUrl)

                val (latStr, lngStr) = when {
                    ftidMatch != null -> Pair(ftidMatch.groupValues[1], ftidMatch.groupValues[2])
                    atMatch != null -> Pair(atMatch.groupValues[1], atMatch.groupValues[2])
                    qMatch != null -> Pair(qMatch.groupValues[1], qMatch.groupValues[2])
                    else -> Pair(null, null)
                }

                if (latStr != null && lngStr != null) {
                    val lat = latStr.toDoubleOrNull()
                    val lng = lngStr.toDoubleOrNull()
                    if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                        val baseDetails = getDetailsFromCoordinates(lat, lng)
                        return@withContext if (!placeNameFromUrl.isNullOrBlank() && (baseDetails.name.startsWith("Location") || baseDetails.name.isBlank())) {
                            baseDetails.copy(name = placeNameFromUrl)
                        } else if (!placeNameFromUrl.isNullOrBlank()) {
                            baseDetails.copy(name = placeNameFromUrl)
                        } else {
                            baseDetails
                        }
                    }
                }

                // 6. If place name was found in URL but no coordinates directly in URL string, search place
                if (!placeNameFromUrl.isNullOrBlank()) {
                    val search = searchPlaces(placeNameFromUrl)
                    if (search.isNotEmpty()) {
                        return@withContext search.first()
                    }
                }

                // 7. Fallback: Search as query text
                val search = searchPlaces(raw)
                if (search.isNotEmpty()) {
                    return@withContext search.first()
                }

                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare ?: address.subLocality ?: ""
                val city = address.locality ?: address.subAdminArea ?: ""
                if (street.isNotEmpty() && city.isNotEmpty()) "$street, $city"
                else address.getAddressLine(0) ?: "$latitude, $longitude"
            } else {
                "Lat: %.4f, Lng: %.4f".format(latitude, longitude)
            }
        } catch (e: Exception) {
            "Lat: %.4f, Lng: %.4f".format(latitude, longitude)
        }
    }
}
