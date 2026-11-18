package com.ennogrensemann.reminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ennogrensemann.reminder.data.FavoriteLocationEntity
import com.ennogrensemann.reminder.data.ReminderEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditReminderSheet(
    existingReminder: ReminderEntity? = null,
    preselectedFavorite: FavoriteLocationEntity? = null,
    favorites: List<FavoriteLocationEntity>,
    onDismiss: () -> Unit,
    onOpenFavoritesManager: () -> Unit,
    onDelete: ((ReminderEntity) -> Unit)? = null,
    onSave: (
        existingId: Long?,
        title: String,
        notes: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        favoriteId: Long?,
        isRecurring: Boolean,
        alsoSaveAsFavorite: Boolean,
        favoriteName: String,
        favoriteIconName: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialFav = preselectedFavorite ?: favorites.firstOrNull { it.id == existingReminder?.favoriteId }

    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var notes by remember { mutableStateOf(existingReminder?.notes ?: "") }
    var selectedFavoriteId by remember {
        mutableStateOf(existingReminder?.favoriteId ?: initialFav?.id)
    }
    var locationName by remember {
        mutableStateOf(
            existingReminder?.locationName
                ?: initialFav?.name
                ?: ""
        )
    }
    var latitude by remember {
        mutableDoubleStateOf(
            existingReminder?.latitude
                ?: initialFav?.latitude
                ?: 52.5200
        )
    }
    var longitude by remember {
        mutableDoubleStateOf(
            existingReminder?.longitude
                ?: initialFav?.longitude
                ?: 13.4050
        )
    }
    var hasValidLocation by remember {
        mutableStateOf(existingReminder != null || initialFav != null)
    }
    var radiusMeters by remember {
        mutableFloatStateOf(
            existingReminder?.radiusMeters
                ?: initialFav?.defaultRadiusMeters
                ?: 150f
        )
    }
    var isRecurring by remember { mutableStateOf(existingReminder?.isRecurring ?: false) }

    var pendingSaveAsFavorite by remember { mutableStateOf(false) }
    var pendingFavoriteName by remember { mutableStateOf("") }
    var pendingFavoriteIconName by remember { mutableStateOf("Place") }

    var showMapPicker by remember { mutableStateOf(false) }
    var showValidationHint by remember { mutableStateOf(false) }

    if (showMapPicker) {
        MapPickerDialog(
            initialLatitude = latitude,
            initialLongitude = longitude,
            initialRadiusMeters = radiusMeters,
            initialLocationName = locationName,
            allowSaveAsFavoriteOption = true,
            dialogTitle = "Standort auf Karte wählen",
            onDismiss = { showMapPicker = false },
            onConfirm = { result ->
                latitude = result.latitude
                longitude = result.longitude
                locationName = result.locationName
                radiusMeters = result.radiusMeters
                selectedFavoriteId = null
                hasValidLocation = true
                pendingSaveAsFavorite = result.saveAsFavorite
                pendingFavoriteName = result.favoriteName
                pendingFavoriteIconName = result.favoriteIconName
                showMapPicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingReminder == null) "Neue Standort-Erinnerung" else "Erinnerung bearbeiten",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (existingReminder != null && onDelete != null) {
                    IconButton(onClick = { onDelete(existingReminder) }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    showValidationHint = false
                },
                label = { Text("Woran möchtest du erinnert werden?") },
                placeholder = { Text("z.B. Milch & Kaffee kaufen, Paket abholen...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Details / Notiz (optional)") },
                placeholder = { Text("Zusätzliche Infos für die Benachrichtigung") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Direct Selection via Favorite Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. Schnellwahl über Favoriten-Buttons",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                AssistChip(
                    onClick = onOpenFavoritesManager,
                    label = { Text("Favoriten verwalten") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (favorites.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Noch keine Favoriten-Buttons hinterlegt.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Wähle unten einen Ort auf der Karte (und hake 'Als Favorit speichern' an) oder klicke auf 'Favoriten verwalten'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    favorites.forEach { fav ->
                        val isSelected = selectedFavoriteId == fav.id && hasValidLocation
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFavoriteId = fav.id
                                locationName = fav.name
                                latitude = fav.latitude
                                longitude = fav.longitude
                                radiusMeters = fav.defaultRadiusMeters
                                hasValidLocation = true
                                pendingSaveAsFavorite = false
                                showValidationHint = false
                            },
                            label = {
                                Text(
                                    text = "${fav.name} (${fav.defaultRadiusMeters.roundToInt()}m)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = FavoriteIcons.iconFor(fav.iconName),
                                    contentDescription = fav.name,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Or choose freely on the Map
            Text(
                text = "2. Oder Standort frei auf der Karte wählen",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    showMapPicker = true
                    showValidationHint = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (hasValidLocation && selectedFavoriteId == null) {
                        "Karten-Standort ändern ($locationName)"
                    } else {
                        "Standort auf Karte auswählen"
                    }
                )
            }

            if (hasValidLocation) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = locationName.ifBlank { "Gewählter Standort" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Koordinaten: %.4f, %.4f".format(latitude, longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Radius Slider (50m - 1000m, trigger on ENTER)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auslöse-Radius (beim Betreten)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
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

            Spacer(modifier = Modifier.height(8.dp))

            // Recurring switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Wiederkehrend bei jeder Ankunft",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Aus: Einmalig bis als erledigt abgehakt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
            }

            if (showValidationHint) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Bitte gib einen Erinnerungstext ein und wähle einen Standort (Favoriten-Button oder Karte).",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (title.isBlank() || !hasValidLocation) {
                        showValidationHint = true
                    } else {
                        onSave(
                            existingReminder?.id,
                            title,
                            notes,
                            locationName,
                            latitude,
                            longitude,
                            radiusMeters,
                            selectedFavoriteId,
                            isRecurring,
                            pendingSaveAsFavorite,
                            pendingFavoriteName,
                            pendingFavoriteIconName
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingReminder == null) "Erinnerung speichern" else "Änderungen speichern",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
