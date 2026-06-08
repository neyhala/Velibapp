package com.example.velibapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.velibapp.repository.VelibRepository
import com.example.velibapp.ui.theme.VelibAppTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.velibapp.model.StationDetail
import androidx.compose.foundation.clickable
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.platform.LocalContext
import com.example.velibapp.database.DatabaseProvider
import com.example.velibapp.model.FavoriteStation
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
class MainActivity : ComponentActivity() {

    private val repository = VelibRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VelibAppTheme {
                MapScreen()
            }
        }
    }
}

@Composable
fun StationScreen(repository: VelibRepository) {

    var stations by remember {
        mutableStateOf<List<StationDetail>>(emptyList())
    }

    var selectedStation by remember {
        mutableStateOf<StationDetail?>(null)
    }

    var message by remember {
        mutableStateOf("Chargement des stations...")
    }

    LaunchedEffect(Unit) {
        try {
            stations = repository.getStations()
            message = ""
        } catch (e: Exception) {
            message = "Erreur : ${e.message}"
        }
    }

    if (selectedStation != null) {
        StationDetail(
            station = selectedStation!!,
            onBack = { selectedStation = null }
        )
    } else if (message.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(stations) { station ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedStation = station
                        }
                        .padding(16.dp)
                ) {
                    Text(text = station.name)
                    Text(text = "🚲 ${station.bikesAvailable} vélos disponibles")
                    Text(text = "🅿️ ${station.docksAvailable} places disponibles")
                }
            }
        }
    }
}
@Composable
fun MapScreen() {

    var stations by remember { mutableStateOf<List<StationDetail>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    var openedStationId by remember { mutableStateOf<String?>(null) }
    var showFavorites by remember { mutableStateOf(false) }
    var selectedFavorite by remember { mutableStateOf<StationDetail?>(null) }

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }

    var showNearbyOnly by remember { mutableStateOf(false) }
    var radiusMeters by remember { mutableStateOf(1000) }
    var selectedRadiusLabel by remember { mutableStateOf("1 km") }

    var customLat by remember { mutableStateOf<Double?>(null) }
    var customLon by remember { mutableStateOf<Double?>(null) }
    var isPlacingCursor by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = DatabaseProvider.getDatabase(context)
    var stationFilter by remember { mutableStateOf("ALL") }
    var searchText by remember { mutableStateOf("") }

    var selectedSearchStation by remember {
        mutableStateOf<StationDetail?>(null)
    }
    var mapViewState by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        stations = VelibRepository().getStations()

        val favorites = database.favoriteDao().getAllFavorites()
        favoriteIds = favorites.map { it.stationId }.toSet()

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLat = location.latitude
                    userLon = location.longitude
                }
            }
        }
    }

    org.osmdroid.config.Configuration.getInstance().userAgentValue =
        context.packageName

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewState = this

                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(48.8566, 2.3522))

                    overlays.add(
                        MapEventsOverlay(object : MapEventsReceiver {

                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                if (isPlacingCursor) {
                                    customLat = p.latitude
                                    customLon = p.longitude
                                    showNearbyOnly = true
                                    isPlacingCursor = false
                                    invalidate()
                                    return true
                                }
                                return false
                            }

                            override fun longPressHelper(p: GeoPoint): Boolean {
                                return false
                            }
                        })
                    )
                }
            },
            update = { mapView ->

                mapView.overlays.clear()

                mapView.overlays.add(
                    MapEventsOverlay(object : MapEventsReceiver {

                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            if (isPlacingCursor) {
                                customLat = p.latitude
                                customLon = p.longitude
                                showNearbyOnly = true
                                isPlacingCursor = false
                                mapView.invalidate()
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean {
                            return false
                        }
                    })
                )

                val stationsInRadius =
                    if (showNearbyOnly && customLat != null && customLon != null) {
                        stations.filter { station ->
                            val results = FloatArray(1)

                            android.location.Location.distanceBetween(
                                customLat!!,
                                customLon!!,
                                station.lat,
                                station.lon,
                                results
                            )

                            results[0] <= radiusMeters
                        }
                    } else {
                        stations
                    }

                val displayedStations =
                    when (stationFilter) {
                        "BIKES" -> stationsInRadius.filter { it.bikesAvailable > 0 }
                        "DOCKS" -> stationsInRadius.filter { it.docksAvailable > 0 }
                        else -> stationsInRadius
                    }.filter {
                        it.name.contains(
                            searchText,
                            ignoreCase = true
                        )
                    }

                displayedStations.forEach { station ->

                    val marker = Marker(mapView)
                    val isFavorite = favoriteIds.contains(station.stationId)

                    marker.position = GeoPoint(station.lat, station.lon)

                    if (isFavorite) {
                        marker.icon =
                            context.getDrawable(R.drawable.baseline_location_on_64)
                    }

                    marker.title =
                        if (isFavorite) "⭐ ${station.name}" else "🚲 ${station.name}"

                    marker.snippet =
                        "🚲 Vélos : ${station.bikesAvailable}\n" +
                                "🅿️ Places : ${station.docksAvailable}\n" +
                                "📦 Capacité : ${station.capacity}\n" +
                                "📍 ${station.lat}, ${station.lon}\n\n" +
                                if (isFavorite) {
                                    "⭐ Favori — cliquez pour retirer"
                                } else {
                                    "☆ Cliquez pour ajouter aux favoris"
                                }

                    marker.setOnMarkerClickListener { clickedMarker, _ ->

                        if (openedStationId == station.stationId) {

                            val newIsFavorite =
                                !favoriteIds.contains(station.stationId)

                            favoriteIds =
                                if (newIsFavorite) {
                                    favoriteIds + station.stationId
                                } else {
                                    favoriteIds - station.stationId
                                }

                            scope.launch {
                                if (newIsFavorite) {
                                    database.favoriteDao().insertFavorite(
                                        FavoriteStation(
                                            stationId = station.stationId,
                                            name = station.name,
                                            lat = station.lat,
                                            lon = station.lon,
                                            capacity = station.capacity,
                                            bikesAvailable = station.bikesAvailable,
                                            docksAvailable = station.docksAvailable
                                        )
                                    )
                                } else {
                                    database.favoriteDao().deleteById(station.stationId)
                                }
                            }

                            clickedMarker.title =
                                if (newIsFavorite) "⭐ ${station.name}" else "🚲 ${station.name}"

                            clickedMarker.snippet =
                                "🚲 Vélos : ${station.bikesAvailable}\n" +
                                        "🅿️ Places : ${station.docksAvailable}\n" +
                                        "📦 Capacité : ${station.capacity}\n" +
                                        "📍 ${station.lat}, ${station.lon}\n\n" +
                                        if (newIsFavorite) {
                                            "⭐ Favori — cliquez pour retirer"
                                        } else {
                                            "☆ Cliquez pour ajouter aux favoris"
                                        }

                            if (newIsFavorite) {
                                clickedMarker.icon =
                                    context.getDrawable(R.drawable.baseline_location_on_64)
                            }

                            openedStationId = station.stationId

                            clickedMarker.closeInfoWindow()
                            clickedMarker.showInfoWindow()
                            mapView.invalidate()

                        } else {
                            openedStationId = station.stationId
                            clickedMarker.showInfoWindow()
                        }

                        true
                    }

                    mapView.overlays.add(marker)
                }

                if (userLat != null && userLon != null) {
                    val userMarker = Marker(mapView)

                    userMarker.position = GeoPoint(userLat!!, userLon!!)
                    userMarker.title = "📍 Vous êtes ici"
                    userMarker.snippet = "Position actuelle"

                    mapView.overlays.add(userMarker)
                }

                if (customLat != null && customLon != null) {
                    val customMarker = Marker(mapView)

                    customMarker.position = GeoPoint(customLat!!, customLon!!)
                    customMarker.title = "📌 Point choisi"
                    customMarker.snippet = "Centre du rayon : $selectedRadiusLabel"

                    customMarker.icon = context.getDrawable(R.drawable.baseline_location_on_64b)

                    customMarker.setAnchor(
                        Marker.ANCHOR_CENTER,
                        Marker.ANCHOR_BOTTOM
                    )

                    mapView.overlays.add(customMarker)
                }

                selectedFavorite?.let { station ->
                    selectedSearchStation?.let { station ->

                        mapView.controller.setZoom(17.0)

                        mapView.controller.setCenter(
                            GeoPoint(
                                station.lat,
                                station.lon
                            )
                        )

                        selectedSearchStation = null
                    }
                    mapView.controller.setZoom(17.0)
                    mapView.controller.setCenter(
                        GeoPoint(station.lat, station.lon)
                    )
                    selectedFavorite = null
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                showFavorites = !showFavorites
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(if (showFavorites) "▲ Favoris" else "▼ Favoris")
        }

        Button(
            onClick = {
                isPlacingCursor = true
                showNearbyOnly = false
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                if (isPlacingCursor)
                    "Cliquez sur la carte"
                else
                    "📌 Placer un point"
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 280.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                },
                label = {
                    Text("Rechercher une station")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (searchText.isNotBlank()) {

                val searchResults =
                    stations
                        .filter {
                            it.name.contains(
                                searchText,
                                ignoreCase = true
                            )
                        }
                        .take(5)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp)
                    ) {
                        items(searchResults) { station ->

                            TextButton(
                                onClick = {
                                    searchText = station.name
                                    selectedSearchStation = station
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📍 ${station.name}")
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rayon de recherche")

                    Row {
                        Button(
                            onClick = {
                                mapViewState?.controller?.zoomOut()
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-")
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                mapViewState?.controller?.zoomIn()
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RadiusButton("500 m", selectedRadiusLabel) {
                        radiusMeters = 500
                        selectedRadiusLabel = "500 m"
                        if (customLat != null && customLon != null) {
                            showNearbyOnly = true
                        }
                    }

                    RadiusButton("1 km", selectedRadiusLabel) {
                        radiusMeters = 1000
                        selectedRadiusLabel = "1 km"
                        if (customLat != null && customLon != null) {
                            showNearbyOnly = true
                        }
                    }

                    RadiusButton("2,5 km", selectedRadiusLabel) {
                        radiusMeters = 2500
                        selectedRadiusLabel = "2,5 km"
                        if (customLat != null && customLon != null) {
                            showNearbyOnly = true
                        }
                    }

                    RadiusButton("5 km", selectedRadiusLabel) {
                        radiusMeters = 5000
                        selectedRadiusLabel = "5 km"
                        if (customLat != null && customLon != null) {
                            showNearbyOnly = true
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Filtrer les stations")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilterButton("🌍 Toutes", stationFilter == "ALL") {
                        stationFilter = "ALL"
                    }

                    FilterButton("🚲 Vélos", stationFilter == "BIKES") {
                        stationFilter = "BIKES"
                    }

                    FilterButton("🅿️ Places", stationFilter == "DOCKS") {
                        stationFilter = "DOCKS"
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        showNearbyOnly = false
                        customLat = null
                        customLon = null
                        isPlacingCursor = false

                        searchText = ""
                        selectedSearchStation = null
                        stationFilter = "ALL"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🌍 Tout afficher")
                }
            }
        }

            if (showFavorites) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⭐ Mes favoris")

                            TextButton(onClick = { showFavorites = false }) {
                                Text("Retour")
                            }
                        }

                        val favoriteStations = stations.filter {
                            favoriteIds.contains(it.stationId)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        ) {
                            items(favoriteStations) { station ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = station.name)
                                        Text(text = "🚲 ${station.bikesAvailable} | 🅿️ ${station.docksAvailable}")
                                    }

                                    TextButton(
                                        onClick = {
                                            selectedFavorite = station
                                            showFavorites = false
                                        }
                                    ) {
                                        Text("Voir")
                                    }

                                    TextButton(
                                        onClick = {
                                            favoriteIds = favoriteIds - station.stationId

                                            scope.launch {
                                                database.favoriteDao().deleteById(station.stationId)
                                            }
                                        }
                                    ) {
                                        Text("Suppr.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


@Composable
fun RadiusButton(
    label: String,
    selectedLabel: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(1.dp)
            .height(38.dp)
    ) {
        Text(
            text = if (label == selectedLabel) "✓ $label" else label,
            fontSize = 11.sp
        )
    }
}

@Composable
fun StationDetail(
    station: StationDetail,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "← Retour",
            modifier = Modifier
                .clickable { onBack() }
                .padding(bottom = 24.dp)
        )

        Text(text = station.name)

        Text(text = "Vélos disponibles : ${station.bikesAvailable}")

        Text(text = "Places disponibles : ${station.docksAvailable}")

        Text(text = "Capacité : ${station.capacity}")

        Text(text = "Latitude : ${station.lat}")

        Text(text = "Longitude : ${station.lon}")
    }
}

@Composable
fun FilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(1.dp)
            .height(38.dp)
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            fontSize = 11.sp
        )
    }
}