package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.GeoSearchResult
import com.example.domain.model.SavedStation
import com.example.domain.model.WeatherConditionType
import com.example.domain.model.toDisplayTemp
import com.example.ui.components.BlockyEnterReveal
import com.example.ui.components.BlockyMetricIcon
import com.example.ui.components.BlockySmallIcon
import com.example.ui.components.BlockyWeatherIcon
import com.example.ui.components.PixelBlockContainer
import com.example.ui.theme.LocalGridcastTokens
import java.util.Locale

@Composable
fun StationSelectorSheet(
  activeStationId: String,
  savedStations: List<SavedStation>,
  searchQuery: String,
  searchResults: List<GeoSearchResult>,
  isSearching: Boolean,
  isLocating: Boolean,
  useFahrenheit: Boolean,
  statusBanner: String?,
  hasCoarseLocationPermission: () -> Boolean,
  onUseCoarseLocation: () -> Unit,
  onLocationPermissionDenied: () -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onSelectSavedStation: (String) -> Unit,
  onSelectGeoResult: (GeoSearchResult) -> Unit,
  onDeleteStation: (String) -> Unit,
  onClose: () -> Unit,
) {
  BackHandler(enabled = true) { onClose() }

  val tokens = LocalGridcastTokens.current
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      if (granted) {
        onUseCoarseLocation()
      } else {
        onLocationPermissionDenied()
      }
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(tokens.background.copy(alpha = 0.94f))
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 620.dp).fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Top Architectural Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          BlockySmallIcon(
            kind = BlockyMetricIcon.LOCATION,
            tint = tokens.accentPrimary,
            size = 20.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.location_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = tokens.inkPrimary,
          )
        }

        IconButton(
          onClick = onClose,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_station_sheet"),
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cd_close_sheet),
            tint = tokens.inkPrimary,
          )
        }
      }

      // Live City Search Input
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = Modifier.fillMaxWidth().testTag("city_search_input"),
        singleLine = true,
        shape = RoundedCornerShape(0.dp),
        placeholder = {
          Text(
            text = stringResource(R.string.location_search_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.inkMuted,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = tokens.accentPrimary,
          )
        },
        trailingIcon = {
          if (isSearching) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = tokens.accentPrimary,
            )
          } else if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChanged("") },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = tokens.inkMuted,
              )
            }
          }
        },
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.accentPrimary,
            unfocusedBorderColor = tokens.gridBorder,
            focusedContainerColor = tokens.surfaceTranslucent,
            unfocusedContainerColor = tokens.surfaceTranslucent,
            focusedTextColor = tokens.inkPrimary,
            unfocusedTextColor = tokens.inkPrimary,
          ),
      )

      // Quick Action: Use Device Coarse Location
      PixelBlockContainer(
        modifier = Modifier.fillMaxWidth().testTag("locate_me_button"),
        isHighlighted = isLocating,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        onClick = {
          if (hasCoarseLocationPermission()) {
            onUseCoarseLocation()
          } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
          }
        },
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          if (isLocating) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = tokens.accentPrimary,
            )
          } else {
            BlockySmallIcon(
              kind = BlockyMetricIcon.LOCATION,
              tint = tokens.accentPrimary,
              size = 16.dp,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.action_locate_me),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (statusBanner != null) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, tokens.accentPrimary)
              .background(tokens.accentSoftFill)
              .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text(
            text = statusBanner,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.inkPrimary,
          )
        }
      }

      // Scrollable Results & Cached Offline Stations
      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
      ) {
        if (searchResults.isNotEmpty()) {
          item {
            Text(
              text = stringResource(R.string.label_search_results),
              style = MaterialTheme.typography.labelSmall,
              color = tokens.accentPrimary,
              modifier = Modifier.padding(vertical = 4.dp),
            )
          }
          items(searchResults, key = { "geo_${it.id}" }) { place ->
            PixelBlockContainer(
              modifier = Modifier.fillMaxWidth().testTag("geo_result_${place.id}"),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              onClick = { onSelectGeoResult(place) },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = place.name.uppercase(Locale.US),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.inkPrimary,
                  )
                  val sub =
                    listOfNotNull(place.admin1?.takeIf { it.isNotBlank() }, place.country)
                      .joinToString(" · ")
                  Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.inkSecondary,
                  )
                }
                Text(
                  text = "+ ADD GRID",
                  style = MaterialTheme.typography.labelSmall,
                  color = tokens.accentPrimary,
                )
              }
            }
          }
        }

        item {
          Text(
            text = stringResource(R.string.label_cached_stations),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.inkMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
          )
        }

        itemsIndexed(savedStations, key = { _, it -> it.id }) { idx, station ->
          val isSelected = station.id == activeStationId
          BlockyEnterReveal(index = idx.coerceAtMost(8)) {
            PixelBlockContainer(
              modifier = Modifier.fillMaxWidth().testTag("station_item_${station.id}"),
              isHighlighted = isSelected,
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              onClick = { onSelectSavedStation(station.id) },
            ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                BlockyWeatherIcon(
                  condition = station.lastCondition ?: WeatherConditionType.CLEAR_DAY,
                  size = 26.dp,
                  animated = false,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = station.name.uppercase(Locale.US),
                      style = MaterialTheme.typography.titleMedium,
                      color = tokens.inkPrimary,
                    )
                    if (station.hasOfflineCache) {
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "CACHED",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentPrimary,
                        modifier =
                          Modifier.border(1.dp, tokens.accentPrimary.copy(alpha = 0.6f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                      )
                    }
                  }
                  val sub =
                    listOfNotNull(
                        station.admin1?.takeIf { it.isNotBlank() && it != station.name },
                        station.country,
                      )
                      .joinToString(" · ")
                  Text(
                    text = formatSyncSubtitle(sub, station.lastUpdatedMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                if (station.lastTempC != null) {
                  Text(
                    text = "${station.lastTempC.toDisplayTemp(useFahrenheit)}°",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) tokens.accentPrimary else tokens.inkPrimary,
                  )
                }
                if (!station.isPinned && savedStations.size > 1) {
                  Spacer(modifier = Modifier.width(4.dp))
                  IconButton(
                    onClick = { onDeleteStation(station.id) },
                    modifier =
                      Modifier.minimumInteractiveComponentSize()
                        .testTag("delete_station_${station.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = stringResource(R.string.cd_delete_city),
                      tint = tokens.inkMuted,
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
  }
}

private fun formatSyncSubtitle(region: String, updatedMillis: Long?): String {
  if (updatedMillis == null) return region
  val minsAgo = ((System.currentTimeMillis() - updatedMillis) / 60_000L).coerceAtLeast(0L)
  val ageLabel =
    when {
      minsAgo < 1L -> "JUST NOW"
      minsAgo < 60L -> "${minsAgo}M AGO"
      else -> "${minsAgo / 60L}H AGO"
    }
  return "$region · $ageLabel"
}
