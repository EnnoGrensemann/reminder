package com.ennogrensemann.reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ennogrensemann.reminder.data.FavoriteLocationEntity
import com.ennogrensemann.reminder.data.ReminderEntity
import com.ennogrensemann.reminder.ui.ReminderViewModel
import com.ennogrensemann.reminder.ui.components.AddEditReminderSheet
import com.ennogrensemann.reminder.ui.screens.FavoritesScreen
import com.ennogrensemann.reminder.ui.screens.HomeScreen
import com.ennogrensemann.reminder.ui.theme.GeoTasksTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoTasksTheme {
                GeoTasksApp()
            }
        }
    }
}

@Composable
fun GeoTasksApp(viewModel: ReminderViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeReminders by viewModel.activeReminders.collectAsState()
    val completedReminders by viewModel.completedReminders.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var showFavoritesScreen by remember { mutableStateOf(false) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var preselectedFavorite by remember { mutableStateOf<FavoriteLocationEntity?>(null) }
    var showBackgroundLocationRationale by remember { mutableStateOf(false) }

    fun checkFineLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkBackgroundLocationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var hasLocationPermissions by remember {
        mutableStateOf(checkFineLocationGranted() && checkBackgroundLocationGranted())
    }
    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationGranted())
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hasLocationPermissions = checkFineLocationGranted() && checkBackgroundLocationGranted()
        if (hasLocationPermissions) {
            viewModel.syncAllGeofences()
        }
    }

    val foregroundPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasNotificationPermission = checkNotificationGranted()
        val fineGranted = checkFineLocationGranted()
        val bgGranted = checkBackgroundLocationGranted()
        hasLocationPermissions = fineGranted && bgGranted

        if (fineGranted && !bgGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBackgroundLocationRationale = true
        } else if (hasLocationPermissions) {
            viewModel.syncAllGeofences()
        }
    }

    // Refresh permission status when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermissions = checkFineLocationGranted() && checkBackgroundLocationGranted()
                hasNotificationPermission = checkNotificationGranted()
                if (hasLocationPermissions) {
                    viewModel.syncAllGeofences()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (!checkFineLocationGranted() || !checkNotificationGranted()) {
            foregroundPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else if (!checkBackgroundLocationGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBackgroundLocationRationale = true
        }
    }

    if (showBackgroundLocationRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationRationale = false },
            title = { Text("Hintergrund-Standort ('Immer zulassen')") },
            text = {
                Text(
                    "Damit GeoTasks auch dann ein Popup-Banner anzeigt, wenn dein Handy in der Tasche ist oder die App geschlossen wurde, wähle im nächsten Schritt bitte 'Immer zulassen' für den Standort."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundLocationRationale = false
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                ) {
                    Text("Jetzt 'Immer zulassen' wählen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackgroundLocationRationale = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("App-Einstellungen")
                }
            }
        )
    }

    if (showFavoritesScreen) {
        FavoritesScreen(
            favorites = favorites,
            onBack = { showFavoritesScreen = false },
            onSaveFavorite = { id, name, icon, lat, lon, radius ->
                viewModel.saveFavorite(
                    existingId = id,
                    name = name,
                    iconName = icon,
                    latitude = lat,
                    longitude = lon,
                    defaultRadiusMeters = radius
                )
            },
            onDeleteFavorite = { fav ->
                viewModel.deleteFavorite(fav)
            }
        )
    } else {
        HomeScreen(
            activeReminders = activeReminders,
            completedReminders = completedReminders,
            favorites = favorites,
            hasLocationPermissions = hasLocationPermissions,
            hasNotificationPermission = hasNotificationPermission,
            onRequestPermissions = { requestRequiredPermissions() },
            onAddReminderClick = { fav ->
                editingReminder = null
                preselectedFavorite = fav
                showAddEditSheet = true
            },
            onEditReminderClick = { reminder ->
                editingReminder = reminder
                preselectedFavorite = null
                showAddEditSheet = true
            },
            onToggleComplete = { reminder ->
                viewModel.toggleReminderCompletion(reminder)
            },
            onRearmReminder = { reminder ->
                viewModel.rearmTriggeredReminder(reminder)
            },
            onTestNotification = { reminder ->
                viewModel.triggerTestPopup(reminder)
            },
            onClearCompleted = {
                viewModel.clearCompletedReminders()
            },
            onOpenFavoritesManager = {
                showFavoritesScreen = true
            }
        )

        if (showAddEditSheet) {
            AddEditReminderSheet(
                existingReminder = editingReminder,
                preselectedFavorite = preselectedFavorite,
                favorites = favorites,
                onDismiss = {
                    showAddEditSheet = false
                    editingReminder = null
                    preselectedFavorite = null
                },
                onOpenFavoritesManager = {
                    showAddEditSheet = false
                    showFavoritesScreen = true
                },
                onDelete = { reminderToDelete ->
                    viewModel.deleteReminder(reminderToDelete)
                    showAddEditSheet = false
                    editingReminder = null
                },
                onSave = { existingId, title, notes, locName, lat, lon, radius, favId, isRecurring, alsoSaveFav, favName, favIcon ->
                    viewModel.saveReminder(
                        existingId = existingId,
                        title = title,
                        notes = notes,
                        locationName = locName,
                        latitude = lat,
                        longitude = lon,
                        radiusMeters = radius,
                        favoriteId = favId,
                        isRecurring = isRecurring,
                        alsoSaveAsFavorite = alsoSaveFav,
                        favoriteName = favName,
                        favoriteIconName = favIcon
                    )
                    showAddEditSheet = false
                    editingReminder = null
                    preselectedFavorite = null
                }
            )
        }
    }
}
