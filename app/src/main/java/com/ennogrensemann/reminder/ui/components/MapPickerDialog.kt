package com.ennogrensemann.reminder.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.ennogrensemann.reminder.util.GeocodingResult
import com.ennogrensemann.reminder.util.NominatimGeocoder
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.roundToInt

data class MapSelectionResult(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val radiusMeters: Float,
    val saveAsFavorite: Boolean,
    val favoriteName: String,
    val favoriteIconName: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MapPickerDialog(
    initialLatitude: Double = 52.5200,
    initialLongitude: Double = 13.4050,
    initialRadiusMeters: Float = 150f,
    initialLocationName: String = "",
    allowSaveAsFavoriteOption: Boolean = true,
    dialogTitle: String = "Standort auf Karte wählen",
    onDismiss: () -> Unit,
    onConfirm: (MapSelectionResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedLat by remember { mutableDoubleStateOf(initialLatitude) }
    var selectedLon by remember { mutableDoubleStateOf(initialLongitude) }
    var radiusMeters by remember { mutableFloatStateOf(initialRadiusMeters.coerceIn(50f, 1000f)) }
    var locationName by remember { mutableStateOf(initialLocationName) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodingResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var saveAsFavorite by remember { mutableStateOf(!allowSaveAsFavoriteOption) }
    var favoriteName by remember { mutableStateOf(initialLocationName) }
    var favoriteIconName by remember { mutableStateOf("Place") }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    @SuppressLint("MissingPermission")
    fun moveToCurrentUserLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                selectedLat = loc.latitude
                selectedLon = loc.longitude
                mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude), 16.5, 600L)
                scope.launch {
                    val revName = NominatimGeocoder.reverseGeocode(loc.latitude, loc.longitude)
                    if (!revName.isNullOrBlank()) {
                        locationName = revName
                        if (favoriteName.isBlank()) favoriteName = revName
                    }
                }
            }
        }
    }

    // Automatically jump to current GPS location if starting with default coordinates
    LaunchedEffect(Unit) {
        if (initialLocationName.isBlank()) {
            moveToCurrentUserLocation()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(dialogTitle) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                val resolvedName = locationName.ifBlank {
                                    "%.4f, %.4f".format(selectedLat, selectedLon)
                                }
                                onConfirm(
                                    MapSelectionResult(
                                        latitude = selectedLat,
                                        longitude = selectedLon,
                                        locationName = resolvedName,
                                        radiusMeters = radiusMeters,
                                        saveAsFavorite = saveAsFavorite,
                                        favoriteName = favoriteName.ifBlank { resolvedName },
                                        favoriteIconName = favoriteIconName
                                    )
                                )
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Übernehmen")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Address Search Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Adresse, Supermarkt oder Stadt suchen...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Suchen")
                        },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (searchQuery.isNotEmpty()) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            scope.launch {
                                                isSearching = true
                                                searchResults = NominatimGeocoder.searchAddress(searchQuery)
                                                isSearching = false
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Suche starten")
                                    }
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            searchResults = emptyList()
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Leeren")
                                    }
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                scope.launch {
                                    isSearching = true
                                    searchResults = NominatimGeocoder.searchAddress(searchQuery)
                                    isSearching = false
                                }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            LazyColumn(modifier = Modifier.height(160.dp)) {
                                items(searchResults) { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLat = item.latitude
                                                selectedLon = item.longitude
                                                locationName = item.shortName
                                                if (favoriteName.isBlank()) {
                                                    favoriteName = item.shortName
                                                }
                                                searchResults = emptyList()
                                                keyboardController?.hide()
                                                mapViewRef?.controller?.animateTo(
                                                    GeoPoint(item.latitude, item.longitude),
                                                    16.5,
                                                    600L
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = item.shortName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                // Interactive OSM Map with live radius circle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(16.0)
                                controller.setCenter(GeoPoint(selectedLat, selectedLon))

                                val eventsReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                        if (p != null) {
                                            selectedLat = p.latitude
                                            selectedLon = p.longitude
                                            scope.launch {
                                                val rev = NominatimGeocoder.reverseGeocode(p.latitude, p.longitude)
                                                if (!rev.isNullOrBlank()) {
                                                    locationName = rev
                                                }
                                            }
                                        }
                                        return true
                                    }

                                    override fun longPressHelper(p: GeoPoint?): Boolean {
                                        if (p != null) {
                                            selectedLat = p.latitude
                                            selectedLon = p.longitude
                                            scope.launch {
                                                val rev = NominatimGeocoder.reverseGeocode(p.latitude, p.longitude)
                                                if (!rev.isNullOrBlank()) {
                                                    locationName = rev
                                                }
                                            }
                                        }
                                        return true
                                    }
                                }
                                overlays.add(MapEventsOverlay(eventsReceiver))
                                mapViewRef = this
                            }
                        },
                        update = { mapView ->
                            val eventsOverlay = mapView.overlays.filterIsInstance<MapEventsOverlay>().firstOrNull()
                            mapView.overlays.clear()
                            if (eventsOverlay != null) {
                                mapView.overlays.add(eventsOverlay)
                            }

                            val centerPoint = GeoPoint(selectedLat, selectedLon)

                            // Radius circle polygon
                            val circlePoints = Polygon.pointsAsCircle(centerPoint, radiusMeters.toDouble())
                            val circleOverlay = Polygon(mapView).apply {
                                points = circlePoints
                                fillPaint.color = AndroidColor.argb(55, 26, 115, 232)
                                outlinePaint.color = AndroidColor.rgb(26, 115, 232)
                                outlinePaint.strokeWidth = 4f
                            }
                            mapView.overlays.add(circleOverlay)

                            // Center marker
                            val marker = Marker(mapView).apply {
                                position = centerPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = locationName.ifBlank { "Gewählter Standort" }
                            }
                            mapView.overlays.add(marker)

                            mapView.invalidate()
                        }
                    )

                    DisposableEffect(Unit) {
                        onDispose {
                            mapViewRef?.onDetach()
                        }
                    }

                    // Hint pill at top of map
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tippe auf die Karte, um den Punkt zu setzen",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // My location FAB
                    FloatingActionButton(
                        onClick = { moveToCurrentUserLocation() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Mein Standort")
                    }
                }

                // Bottom controls: Location Name, Radius Slider & Save as Favorite toggle
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Bezeichnung des Standorts") },
                            placeholder = { Text("z.B. Rewe, Büro, Bahnhof...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auslöse-Radius (beim Betreten)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${radiusMeters.roundToInt()} m",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = radiusMeters,
                            onValueChange = { radiusMeters = (it / 25f).roundToInt() * 25f },
                            valueRange = 50f..1000f,
                            steps = 37
                        )

                        if (allowSaveAsFavoriteOption) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { saveAsFavorite = !saveAsFavorite }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = saveAsFavorite,
                                    onCheckedChange = { saveAsFavorite = it }
                                )
                                Text(
                                    text = "Als Favoriten-Button für die Schnellwahl speichern",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (saveAsFavorite) {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (allowSaveAsFavoriteOption) {
                                OutlinedTextField(
                                    value = favoriteName,
                                    onValueChange = { favoriteName = it },
                                    label = { Text("Name des Favoriten-Buttons") },
                                    placeholder = { Text("z.B. Zuhause, Arbeit, Gym") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                text = "Icon für Favoriten-Button:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FavoriteIcons.options.forEach { option ->
                                    FilterChip(
                                        selected = favoriteIconName == option.key,
                                        onClick = { favoriteIconName = option.key },
                                        label = { Text(option.label) },
                                        leadingIcon = {
                                            Icon(
                                                option.icon,
                                                contentDescription = option.label,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
