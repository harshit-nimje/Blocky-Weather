package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.delay

/**
 * Huge, bold standalone pixelated weather degrees rendered directly on the graph-paper grid
 * with NO container box behind them, paired with a left-side vertical thermal rail:
 * - Top-left blocky up-arrow + today's high temp hugging top-left of the temp number
 * - Vertical blocky line filling upward in yellow accurately based on current temp within [low..high]
 * - Bottom-left blocky down-arrow + today's low temp hugging bottom-left of the temp number
 * - Less-wide 3x3 degree symbol with empty center block and yellow blocks on each side
 */
@Composable
fun PixelMatrixTemperature(
  temperature: Int,
  currentTempC: Double,
  todayHigh: Int,
  todayHighC: Double,
  todayLow: Int,
  todayLowC: Double,
  unitSymbol: String,
  ecoMode: Boolean,
  useStandardHeroFont: Boolean = false,
  onHeroTapped: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  val tempText = "$temperature°"

  // Clean blocky column-sweep enter/update transition
  val sweepProgress = remember { Animatable(0f) }
  LaunchedEffect(temperature, unitSymbol) {
    sweepProgress.snapTo(0.15f)
    sweepProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
    )
  }

  // Calm discrete fidget tick (fidgets briefly every few seconds or immediately on tap)
  var fidgetStep by remember { mutableIntStateOf(0) }
  var tapBoostTicks by remember { mutableIntStateOf(0) }

  LaunchedEffect(ecoMode) {
    while (true) {
      delay(if (ecoMode) 900L else 480L)
      if (tapBoostTicks > 0) {
        tapBoostTicks--
      }
      fidgetStep++
    }
  }

  val interactionSource = remember { MutableInteractionSource() }

  // Accurate yellow fill fraction along today's [low..high] thermal range
  val effectiveLowC = minOf(todayLowC, currentTempC)
  val effectiveHighC = maxOf(todayHighC, currentTempC)
  val tempSpan = (effectiveHighC - effectiveLowC).coerceAtLeast(0.5)
  val rawFillFraction = ((currentTempC - effectiveLowC) / tempSpan).toFloat().coerceIn(0f, 1f)
  val yellowFillColor = if (tokens.isDark) Color(0xFFFACC15) else Color(0xFFF59E0B)

  val glyphs = remember(tempText) { tempText.map { glyphMatrixFor(it) } }
  val totalCols =
    remember(glyphs) { glyphs.sumOf { it[0].length } + (glyphs.size - 1).coerceAtLeast(0) }
  val totalRows = 7

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("hero_temperature")
        .semantics {
          contentDescription =
            "Current temperature $temperature degrees $unitSymbol, high $todayHigh degrees, low $todayLow degrees"
        }
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = {
            tapBoostTicks = 4
            fidgetStep++
            onHeroTapped()
          },
        ),
    contentAlignment = Alignment.Center,
  ) {
    val railWidth = 48.dp
    val railToDigitsGap = 8.dp
    val availableDigitsWidth = (maxWidth - railWidth - railToDigitsGap).coerceAtLeast(140.dp)
    val pixelPitchDp =
      (availableDigitsWidth / totalCols.toFloat()).coerceIn(13.dp, 26.dp)
    val matrixWidthDp = pixelPitchDp * totalCols
    val matrixHeightDp = pixelPitchDp * totalRows
    val heroRowHeightDp = matrixHeightDp + 10.dp

    val lengthScale =
      when {
        tempText.length >= 4 -> 0.78f
        tempText.length == 3 -> 0.92f
        else -> 1.0f
      }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val standardTempFontSp =
      (matrixHeightDp.value * 0.94f * lengthScale).coerceIn(88f, 148f) / fontScale

    Row(
      modifier =
        if (useStandardHeroFont) {
          Modifier.wrapContentWidth().height(heroRowHeightDp)
        } else {
          Modifier.width((railWidth + railToDigitsGap + matrixWidthDp).coerceAtMost(maxWidth))
            .height(heroRowHeightDp)
        },
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left Thermal Range Rail hugging top-left (Up Arrow + High) and bottom-left (Down Arrow + Low) of the temp number
      Column(
        modifier = Modifier.width(railWidth).fillMaxHeight(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top-left Today's Highest Temp + Up Arrow hugging top-left corner of temperature digits
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.testTag("hero_high_temp"),
        ) {
          Text(
            text = "$todayHigh°",
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = tokens.inkPrimary,
            maxLines = 1,
          )
          BlockyArrowIcon(isUp = true, color = yellowFillColor)
        }

        // Vertical Blocky Line directly beneath the Up Arrow filling upward to yellow based on current temp
        Canvas(
          modifier =
            Modifier.weight(1f)
              .width(10.dp)
              .padding(vertical = 4.dp)
        ) {
          val blockPitch = 9.dp.toPx()
          val count = kotlin.math.floor(size.height / blockPitch).toInt().coerceAtLeast(4)
          val seam = 1.5.dp.toPx()
          val blockSize = (blockPitch - seam * 2f).coerceAtLeast(4f)
          val totalLineH = count * blockPitch
          val startY = (size.height - totalLineH) * 0.5f
          val lineLeftX = (size.width - blockSize) * 0.5f

          val litFromBottom =
            when {
              rawFillFraction <= 0f -> 0
              rawFillFraction >= 1f -> count
              else -> kotlin.math.round(rawFillFraction * count).toInt().coerceIn(1, count - 1)
            }

          for (i in 0 until count) {
            // Index from bottom (0 = lowest temp, count - 1 = highest temp)
            val fromBottomIdx = (count - 1) - i
            val isYellow = fromBottomIdx < litFromBottom
            drawRect(
              color = if (isYellow) yellowFillColor else tokens.gridBorder,
              topLeft = Offset(lineLeftX, startY + i * blockPitch + seam),
              size = Size(blockSize, blockSize),
            )
          }
        }

        // Bottom-left Today's Lowest Temp + Down Arrow hugging bottom-left corner of temperature digits
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.testTag("hero_low_temp"),
        ) {
          Text(
            text = "$todayLow°",
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = tokens.inkSecondary,
            maxLines = 1,
          )
          BlockyArrowIcon(isUp = false, color = tokens.inkMuted)
        }
      }

      Spacer(modifier = Modifier.width(if (useStandardHeroFont) 10.dp else railToDigitsGap))

      // Huge Standalone Bold Temperature Number (Blocky Matrix vs Bold App Space Grotesk Font)
      if (useStandardHeroFont) {
        Box(
          modifier = Modifier.fillMaxHeight(),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = tempText,
            style =
              TextStyle(
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = standardTempFontSp.sp,
                lineHeight = (standardTempFontSp * 0.94f).sp,
                letterSpacing = (-3.5).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle =
                  LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                  ),
              ),
            color = tokens.inkPrimary,
            maxLines = 1,
            softWrap = false,
          )
        }
      } else {
        Box(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          contentAlignment = Alignment.CenterStart,
        ) {
          Canvas(modifier = Modifier.width(matrixWidthDp).height(matrixHeightDp)) {
            val pitchX = size.width / totalCols
            val pitchY = size.height / totalRows
            val seam = 1.35.dp.toPx()

            val isFidgetActive = tapBoostTicks > 0 || (fidgetStep % 11 == 2)
            val swatches = tokens.blockSwatches
          val sweep = sweepProgress.value

          var colCursor = 0
          var pixelCounter = 0

          glyphs.forEachIndexed { glyphIdx, matrix ->
            val gCols = matrix[0].length
            val isDegree = tempText[glyphIdx] == '°'

            for (r in 0 until totalRows) {
              val rowStr = matrix[r]
              for (c in 0 until gCols) {
                if (rowStr[c] == '#') {
                  pixelCounter++
                  val globalCol = colCursor + c
                  val colNormalized = globalCol.toFloat() / totalCols.coerceAtLeast(1)
                  if (colNormalized > sweep + 0.12f) continue

                  val isEdgeCell = (c == 0 || c == gCols - 1 || r == 0 || r == totalRows - 1)
                  val shouldFidget =
                    !isDegree &&
                      isFidgetActive &&
                      isEdgeCell &&
                      ((pixelCounter + fidgetStep) % 14 == 0)

                  val hopX =
                    if (shouldFidget) {
                      if (c == 0) -1 else if (c == gCols - 1) 1 else 0
                    } else {
                      0
                    }
                  val hopY =
                    if (shouldFidget && hopX == 0) {
                      if (r == 0) -1 else 1
                    } else {
                      0
                    }

                  val cellX = (globalCol + hopX) * pitchX + seam
                  val cellY = (r + hopY) * pitchY + seam
                  val blockW = (pitchX - seam * 2f).coerceAtLeast(4f)
                  val blockH = (pitchY - seam * 2f).coerceAtLeast(4f)

                  val fillColor =
                    when {
                      isDegree -> yellowFillColor
                      shouldFidget -> swatches[(pixelCounter + fidgetStep) % swatches.size]
                      else -> tokens.inkPrimary
                    }

                  drawRect(
                    color = fillColor,
                    topLeft = Offset(cellX, cellY),
                    size = Size(blockW, blockH),
                  )
                }
              }
            }
            colCursor += gCols + 1
          }
          }
        }
      }
    }
  }
}

@Composable
private fun BlockyArrowIcon(isUp: Boolean, color: Color) {
  Canvas(modifier = Modifier.width(10.dp).height(8.dp)) {
    val cols = 5
    val rows = 4
    val px = minOf(size.width / cols, size.height / rows)
    val pattern =
      if (isUp) {
        listOf(
          "..#..",
          ".###.",
          "#####",
          ".....",
        )
      } else {
        listOf(
          ".....",
          "#####",
          ".###.",
          "..#..",
        )
      }
    pattern.forEachIndexed { r, line ->
      line.forEachIndexed { c, ch ->
        if (ch == '#') {
          drawRect(
            color = color,
            topLeft = Offset(c * px, r * px),
            size = Size(px, px),
          )
        }
      }
    }
  }
}

private fun glyphMatrixFor(ch: Char): List<String> =
  when (ch) {
    '0' ->
      listOf(
        ".###.",
        "##.##",
        "##.##",
        "##.##",
        "##.##",
        "##.##",
        ".###.",
      )
    '1' ->
      listOf(
        "..##.",
        ".###.",
        "##.#.",
        "..##.",
        "..##.",
        "..##.",
        "#####",
      )
    '2' ->
      listOf(
        ".###.",
        "##.##",
        "...##",
        ".###.",
        "##...",
        "##...",
        "#####",
      )
    '3' ->
      listOf(
        "####.",
        "...##",
        "...##",
        ".###.",
        "...##",
        "##.##",
        ".###.",
      )
    '4' ->
      listOf(
        "...##",
        "..###",
        ".#.##",
        "##.##",
        "#####",
        "...##",
        "...##",
      )
    '5' ->
      listOf(
        "#####",
        "##...",
        "####.",
        "...##",
        "...##",
        "##.##",
        ".###.",
      )
    '6' ->
      listOf(
        ".###.",
        "##...",
        "####.",
        "##.##",
        "##.##",
        "##.##",
        ".###.",
      )
    '7' ->
      listOf(
        "#####",
        "...##",
        "..##.",
        "..##.",
        ".##..",
        ".##..",
        ".##..",
      )
    '8' ->
      listOf(
        ".###.",
        "##.##",
        "##.##",
        ".###.",
        "##.##",
        "##.##",
        ".###.",
      )
    '9' ->
      listOf(
        ".###.",
        "##.##",
        "##.##",
        ".####",
        "...##",
        "..##.",
        ".##..",
      )
    '-' ->
      listOf(
        ".....",
        ".....",
        ".....",
        "####.",
        ".....",
        ".....",
        ".....",
      )
    // Less-wide 3-column degree with empty center block and yellow blocks on each side
    '°' ->
      listOf(
        ".#.",
        "#.#",
        ".#.",
        "...",
        "...",
        "...",
        "...",
      )
    else ->
      listOf(
        "...",
        "...",
        "...",
        "...",
        "...",
        "...",
        "...",
      )
  }
