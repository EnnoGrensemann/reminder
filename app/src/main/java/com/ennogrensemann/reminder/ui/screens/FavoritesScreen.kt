package com.ennogrensemann.reminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ennogrensemann.reminder.data.FavoriteLocationEntity
import com.ennogrensemann.reminder.ui.components.FavoriteIcons
import com.ennogrensemann.reminder.ui.components.MapPickerDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<FavoriteLocationEntity>,
    onBack: () -> Unit,
    onSaveFavorite: (
        existingId: Long?,
        name: String,
        iconName: String,
        latitude: Double,
        longitude: Double,
        defaultRadiusMeters: Float
    ) -> Unit,
    onDeleteFavorite: (FavoriteLocationEntity) -> Unit
) {
    var showAddOrEditMap by remember { mutableStateOf(false) }
    var editingFavorite by remember { mutableStateOf<FavoriteLocationEntity?>(null) }

    if (showAddOrEditMap) {
        val current = editingFavorite
        MapPickerDialog(
            initialLatitude = current?.latitude ?: 52.5200,
            initialLongitude = current?.longitude ?: 13.4050,
            initialRadiusMeters = current?.defaultRadiusMeters ?: 150f,
            initialLocationName = current?.name ?: "",
            allowSaveAsFavoriteOption = false,
            dialogTitle = if (current == null) "Neuen Favoriten-Standort anlegen" else "Favorit bearbeiten",
            onDismiss = {
                showAddOrEditMap = false
                editingFavorite = null
            },
            onConfirm = { result ->
                onSaveFavorite(
                    current?.id,
                    result.locationName,
                    result.favoriteIconName,
                    result.latitude,
                    result.longitude,
                    result.radiusMeters
                )
                showAddOrEditMap = false
                editingFavorite = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Favoriten-Standorte", fontWeight = FontWeight.Bold)
                        Text(
                            "Deine Schnellwahl-Buttons für Erinnerungen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingFavorite = null
                    showAddOrEditMap = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Neuer Favorit") }
            )
        }
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Noch keine Favoriten angelegt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lege häufig genutzte Orte wie 'Zuhause', 'Arbeit' oder 'Supermarkt' als Favoriten an, um sie beim Erstellen einer Erinnerung direkt per Button auszuwählen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites, key = { it.id }) { fav ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = FavoriteIcons.iconFor(fav.iconName),
                                        contentDescription = fav.name,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fav.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Standard-Radius: ${fav.defaultRadiusMeters.roundToInt()} m • %.4f, %.4f".format(
                                        fav.latitude,
                                        fav.longitude
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingFavorite = fav
                                    showAddOrEditMap = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Bearbeiten",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeleteFavorite(fav) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Löschen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
