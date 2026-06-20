package com.example.gestordetareas.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gestordetareas.ui.theme.GestorDeTareasTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

class MapActivity : ComponentActivity() {

    companion object {
        const val EXTRA_LATITUDE   = "latitude"
        const val EXTRA_LONGITUDE  = "longitude"
        const val EXTRA_VIEW_ONLY  = "view_only"
        const val EXTRA_TASK_TITLE = "task_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewOnly  = intent.getBooleanExtra(EXTRA_VIEW_ONLY, false)
        val initLat   = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val initLng   = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Ubicación"

        setContent {
            GestorDeTareasTheme {
                if (viewOnly) {
                    MapViewerScreen(
                        latitude  = initLat,
                        longitude = initLng,
                        taskTitle = taskTitle,
                        onClose   = { finish() }
                    )
                } else {
                    MapSelectorScreen(
                        onConfirm = { lat, lng ->
                            setResult(RESULT_OK, Intent().apply {
                                putExtra(EXTRA_LATITUDE,  lat)
                                putExtra(EXTRA_LONGITUDE, lng)
                            })
                            finish()
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

// ── Pantalla: seleccionar ubicación ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSelectorScreen(
    onConfirm: (Double, Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // MarkerState remembered: no se recrea en recomposiciones
    val markerState   = remember { MarkerState(position = LatLng(0.0, 0.0)) }
    var markerVisible by remember { mutableStateOf(false) }
    var mapLoaded     by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraState = rememberCameraPositionState {
        // Punto inicial neutro — se moverá a la ubicación real
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }

    // Obtiene la ubicación actual con getCurrentLocation (más confiable que lastLocation)
    // y mueve la cámara. Debe llamarse dentro de una coroutine.
    suspend fun fetchAndCenterLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        try {
            // Intento 1: getCurrentLocation — fuerza una lectura activa del GPS
            val loc = fusedClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .await()
            if (loc != null) {
                val latLng = LatLng(loc.latitude, loc.longitude)
                markerState.position = latLng
                markerVisible        = true
                cameraState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                return
            }
        } catch (_: Exception) { /* continuar al fallback */ }

        try {
            // Intento 2: lastLocation — usa la caché si getCurrentLocation falló
            val loc = fusedClient.lastLocation.await()
            if (loc != null) {
                val latLng = LatLng(loc.latitude, loc.longitude)
                markerState.position = latLng
                markerVisible        = true
                cameraState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
            }
        } catch (_: Exception) { /* sin ubicación disponible */ }
    }

    // Cuando el permiso cambia a true → centrar en ubicación actual
    LaunchedEffect(hasPermission) {
        if (hasPermission) fetchAndCenterLocation()
    }

    // Launcher de permiso: actualiza hasPermission y dispara el LaunchedEffect
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Al entrar: pedir permiso si no lo tiene
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar ubicación", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (markerVisible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Lat: %.5f   Lng: %.5f".format(
                                    markerState.position.latitude,
                                    markerState.position.longitude
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Toca el mapa para colocar el marcador",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                markerState.position.latitude,
                                markerState.position.longitude
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape   = MaterialTheme.shapes.medium,
                        enabled = markerVisible
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar ubicación", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(isMyLocationEnabled = hasPermission),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled     = true,
                    myLocationButtonEnabled = hasPermission
                ),
                onMapLoaded = { mapLoaded = true },
                onMapClick  = { latLng ->
                    markerState.position = latLng
                    markerVisible        = true
                }
            ) {
                if (markerVisible) {
                    Marker(
                        state = markerState,
                        title = "Ubicación seleccionada"
                    )
                }
            }

            if (!mapLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ── Pantalla: solo visualización ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapViewerScreen(
    latitude: Double,
    longitude: Double,
    taskTitle: String,
    onClose: () -> Unit
) {
    val position    = LatLng(latitude, longitude)
    val cameraState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 15f)
    }
    var mapLoaded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(taskTitle, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled     = true,
                    rotationGesturesEnabled = false,
                    myLocationButtonEnabled = false
                ),
                onMapLoaded = { mapLoaded = true }
            ) {
                Marker(
                    state = MarkerState(position = position),
                    title = taskTitle
                )
            }

            if (!mapLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text     = "Cargando mapa…",
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
