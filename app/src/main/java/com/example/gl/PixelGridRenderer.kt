package com.example.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.domain.model.WeatherConditionType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * OpenGL ES 2.0 graph-paper renderer where every visual element snaps to the architectural grid:
 * - Crisp 1px graph-paper gridlines on warm clean-room paper (light) or true #000000 AMOLED (dark).
 * - Sunny days: Big stepped yellow pixel sun in the upper-right sky with animated shining rays.
 * - Rainy/snowy weather: Intensity-scaled falling pixel streaks angled live by the phone's gyro tilt.
 * - Calm ambient anchor blocks + 60-FPS touch-excited hopping block clusters.
 */
class PixelGridRenderer(
  private val controller: PixelGridController,
  private val density: Float,
) : GLSurfaceView.Renderer {

  companion object {
    private const val MAX_BLOCKS = 640
    private const val TOUCH_POOL_SIZE = 128
    private const val RAIN_POOL_SIZE = 44
    private const val FLOATS_PER_VERTEX = 6
    private const val VERTICES_PER_QUAD = 4
    private const val INDICES_PER_QUAD = 6
    private const val POST_SYNC_WAVE_DURATION_SEC = 1.65f
    private const val POST_SYNC_WAVE_BAND_ROWS = 6.5f
  }

  private var widthPx: Float = 1080f
  private var heightPx: Float = 2400f
  private var cellSizePx: Float = (24f * density).coerceIn(38f, 88f)
  private var totalCols: Int = 15
  private var totalRows: Int = 32

  private var gridProgram: Int = 0
  private var blockProgram: Int = 0

  private var gAttrPos = 0
  private var gUniCellSize = 0
  private var gUniBgColor = 0
  private var gUniGridColor = 0

  private var bAttrPos = 0
  private var bAttrColor = 0
  private var bUniResolution = 0

  private val fullScreenQuadCoords =
    floatArrayOf(
      -1f, -1f,
      1f, -1f,
      -1f, 1f,
      1f, 1f,
    )
  private val fullScreenBuffer: FloatBuffer =
    ByteBuffer.allocateDirect(fullScreenQuadCoords.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .apply {
        put(fullScreenQuadCoords)
        position(0)
      }

  private val vertexArray = FloatArray(MAX_BLOCKS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX)
  private val vertexBuffer: FloatBuffer =
    ByteBuffer.allocateDirect(vertexArray.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()

  private val indexBuffer: ShortBuffer =
    ByteBuffer.allocateDirect(MAX_BLOCKS * INDICES_PER_QUAD * 2)
      .order(ByteOrder.nativeOrder())
      .asShortBuffer()
      .apply {
        for (i in 0 until MAX_BLOCKS) {
          val v = (i * 4).toShort()
          put(v)
          put((v + 1).toShort())
          put((v + 2).toShort())
          put((v + 2).toShort())
          put((v + 1).toShort())
          put((v + 3).toShort())
        }
        position(0)
      }

  // Deduplication buffer for solar core & radiating ray grid cells (packed (col shl 16) or row)
  private val sunVisitedCells = IntArray(220)
  private var sunVisitedCount = 0

  // Raindrop / snowflake particle pool with normalized positions [0..1]
  private val rainNormX =
    FloatArray(RAIN_POOL_SIZE) { i -> ((i * 73 + 19) % 100) / 100f }
  private val rainNormY =
    FloatArray(RAIN_POOL_SIZE) { i -> ((i * 41 + 7) % 100) / 100f }
  private val rainSpeedJitter =
    FloatArray(RAIN_POOL_SIZE) { i -> 0.82f + ((i * 17) % 11) * 0.036f }
  private var smoothedTiltSlope = 0f

  // High-FPS touch-excited grid blocks stepping smoothly across cells without ever overwriting active blocks
  private val tStartCol = FloatArray(TOUCH_POOL_SIZE) { -100f }
  private val tStartRow = FloatArray(TOUCH_POOL_SIZE) { -100f }
  private val tDirX = FloatArray(TOUCH_POOL_SIZE) { 0f }
  private val tDirY = FloatArray(TOUCH_POOL_SIZE) { 0f }
  private val tAgeSec = FloatArray(TOUCH_POOL_SIZE) { 99f }
  private val tDurationSec = FloatArray(TOUCH_POOL_SIZE) { 1.15f }
  private val tSwatch = IntArray(TOUCH_POOL_SIZE) { 0 }
  private var touchCursor = 0
  private val pendingBurstScratch = ArrayList<TouchGridBurst>(24)
  private var burstSeq = 0
  private var lastProcessedFidget = 0
  private var lastProcessedSyncCascade = 0
  private var lastProcessedPostSyncWave = 0
  private var postSyncWaveActive = false
  private var postSyncWaveAgeSec = 0f
  private var lastFrameNanos = 0L
  private var smoothedDt = 0.016f
  private var elapsedSec = 0f

  // Light Mode Pastel Swatches
  private val lightSwatches =
    arrayOf(
      floatArrayOf(0.761f, 0.859f, 0.753f), // Sage Green #C2DBC0
      floatArrayOf(0.965f, 0.886f, 0.608f), // Butter Yellow #F6E29B
      floatArrayOf(0.969f, 0.776f, 0.659f), // Peach Coral #F7C6A8
      floatArrayOf(0.776f, 0.741f, 0.969f), // Periwinkle Lavender #C6BDF7
      floatArrayOf(0.718f, 0.835f, 0.969f), // Sky Blue #B7D5F7
    )

  // True AMOLED High-Contrast Architectural Swatches
  private val darkSwatches =
    arrayOf(
      floatArrayOf(0.180f, 0.340f, 0.245f), // Forest Sage on AMOLED
      floatArrayOf(0.450f, 0.365f, 0.145f), // Warm Amber on AMOLED
      floatArrayOf(0.440f, 0.225f, 0.155f), // Terracotta Coral on AMOLED
      floatArrayOf(0.285f, 0.240f, 0.520f), // Electric Indigo on AMOLED
      floatArrayOf(0.145f, 0.310f, 0.475f), // Cobalt Sky on AMOLED
    )

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    GLES20.glDisable(GLES20.GL_DEPTH_TEST)

    gridProgram = compileProgram(GRID_VERTEX_SHADER, GRID_FRAGMENT_SHADER)
    gAttrPos = GLES20.glGetAttribLocation(gridProgram, "aPosition")
    gUniCellSize = GLES20.glGetUniformLocation(gridProgram, "uCellSize")
    gUniBgColor = GLES20.glGetUniformLocation(gridProgram, "uBgColor")
    gUniGridColor = GLES20.glGetUniformLocation(gridProgram, "uGridColor")

    blockProgram = compileProgram(BLOCK_VERTEX_SHADER, BLOCK_FRAGMENT_SHADER)
    bAttrPos = GLES20.glGetAttribLocation(blockProgram, "aPosition")
    bAttrColor = GLES20.glGetAttribLocation(blockProgram, "aColor")
    bUniResolution = GLES20.glGetUniformLocation(blockProgram, "uResolution")

    lastFrameNanos = System.nanoTime()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    widthPx = max(1, width).toFloat()
    heightPx = max(1, height).toFloat()
    GLES20.glViewport(0, 0, width, height)

    val targetCols = if (width > height) 28f else 15f
    cellSizePx = floor((widthPx / targetCols).coerceIn(38f, 88f)).coerceAtLeast(34f)
    totalCols = max(6, (widthPx / cellSizePx).toInt())
    totalRows = max(10, (heightPx / cellSizePx).toInt())
  }

  override fun onDrawFrame(gl: GL10?) {
    val nowNanos = System.nanoTime()
    val rawDt =
      if (lastFrameNanos == 0L) 0.016f
      else ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
    lastFrameNanos = nowNanos
    smoothedDt = if (elapsedSec == 0f) rawDt else smoothedDt * 0.72f + rawDt * 0.28f
    elapsedSec += smoothedDt

    val isDark = controller.isDarkMode()
    val condition = controller.getCondition()
    val isDay = controller.isDay()
    val precipIntensity = controller.getPrecipitationIntensity()

    // Smoothly low-pass filter phone gyro/gravity tilt angle at 60 FPS
    val windSlopeFallback =
      ((controller.windSpeedKmh.toFloat() - 10f) / 70f).coerceIn(0f, 0.25f)
    val sensorTilt = controller.rainTiltSlope
    val targetSlope =
      if (abs(sensorTilt) > 0.03f) sensorTilt else windSlopeFallback
    val blendFactor = (rawDt * 11f).coerceIn(0.05f, 1f)
    smoothedTiltSlope += (targetSlope - smoothedTiltSlope) * blendFactor

    // Occasional crisp architectural lightning pulse during thunderstorms
    var lightningBoost = 0f
    if (condition == WeatherConditionType.THUNDERSTORM) {
      val stormPhase = elapsedSec % 6.2f
      if (stormPhase in 0.10f..0.24f || stormPhase in 0.30f..0.38f) {
        lightningBoost = if (isDark) 0.07f else 0.03f
      }
    }

    // True AMOLED #000000 pure black in dark mode; warm clean-room paper #FAF9F5 in light mode
    val bgR = ((if (isDark) 0.0f else 0.980f) + lightningBoost).coerceIn(0f, 1f)
    val bgG = ((if (isDark) 0.0f else 0.976f) + lightningBoost).coerceIn(0f, 1f)
    val bgB = ((if (isDark) 0.0f else 0.961f) + lightningBoost * 1.4f).coerceIn(0f, 1f)

    // Crisp hairline gridlines (#1A1D24 on true AMOLED black)
    val gridR = if (isDark) 0.102f else 0.890f
    val gridG = if (isDark) 0.114f else 0.878f
    val gridB = if (isDark) 0.141f else 0.839f

    GLES20.glClearColor(bgR, bgG, bgB, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    // PASS 1: Crisp architectural graph-paper gridlines
    GLES20.glUseProgram(gridProgram)
    GLES20.glUniform1f(gUniCellSize, cellSizePx)
    GLES20.glUniform3f(gUniBgColor, bgR, bgG, bgB)
    GLES20.glUniform3f(gUniGridColor, gridR, gridG, gridB)

    fullScreenBuffer.position(0)
    GLES20.glEnableVertexAttribArray(gAttrPos)
    GLES20.glVertexAttribPointer(gAttrPos, 2, GLES20.GL_FLOAT, false, 0, fullScreenBuffer)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    GLES20.glDisableVertexAttribArray(gAttrPos)

    // Update touch-excited blocks using monotonic smoothed delta & precipitation drops
    updateTouchExcitedBlocks(smoothedDt)

    val swatches = if (isDark) darkSwatches else lightSwatches
    val weatherSwatch = weatherPrimarySwatchIndex(condition)
    var blockCount = 0
    var ptr = 0

    fun emitRawBlock(
      col: Float,
      row: Float,
      spanCols: Float,
      spanRows: Float,
      r: Float,
      g: Float,
      b: Float,
      alpha: Float,
      scale: Float = 1f,
      scaleX: Float = 1f,
      scaleY: Float = 1f,
      shearX: Float = 0f,
      jitterX: Float = 0f,
      jitterY: Float = 0f,
    ) {
      if (blockCount >= MAX_BLOCKS || alpha <= 0.01f || (scale <= 0.02f && scaleX <= 0.02f && scaleY <= 0.02f)) return
      if (
        col + spanCols <= -1.5f ||
          col >= totalCols + 1.5f ||
          row + spanRows <= -1.5f ||
          row >= totalRows + 1.5f
      ) {
        return
      }

      val inset = 1.0f
      val rawX0 = col * cellSizePx + inset
      val rawY0 = row * cellSizePx + inset
      val rawX1 = (col + spanCols) * cellSizePx - inset
      val rawY1 = (row + spanRows) * cellSizePx - inset

      val cx = (rawX0 + rawX1) * 0.5f + jitterX
      val cy = (rawY0 + rawY1) * 0.5f + jitterY
      val halfW = (rawX1 - rawX0) * 0.5f * scale * scaleX
      val halfH = (rawY1 - rawY0) * 0.5f * scale * scaleY

      val x0 = cx - halfW
      val x1 = cx + halfW
      val y0 = cy - halfH
      val y1 = cy + halfH

      vertexArray[ptr++] = x0; vertexArray[ptr++] = y0
      vertexArray[ptr++] = r; vertexArray[ptr++] = g; vertexArray[ptr++] = b; vertexArray[ptr++] = alpha

      vertexArray[ptr++] = x1; vertexArray[ptr++] = y0
      vertexArray[ptr++] = r; vertexArray[ptr++] = g; vertexArray[ptr++] = b; vertexArray[ptr++] = alpha

      vertexArray[ptr++] = x0; vertexArray[ptr++] = y1
      vertexArray[ptr++] = r; vertexArray[ptr++] = g; vertexArray[ptr++] = b; vertexArray[ptr++] = alpha

      vertexArray[ptr++] = x1; vertexArray[ptr++] = y1
      vertexArray[ptr++] = r; vertexArray[ptr++] = g; vertexArray[ptr++] = b; vertexArray[ptr++] = alpha

      blockCount++
    }

    fun emitSwatchBlock(
      col: Float,
      row: Float,
      spanCols: Float,
      spanRows: Float,
      swatchIdx: Int,
      alpha: Float = 0.92f,
      scale: Float = 1f,
      scaleX: Float = 1f,
      scaleY: Float = 1f,
      shearX: Float = 0f,
      jitterX: Float = 0f,
      jitterY: Float = 0f,
    ) {
      val idx = abs(swatchIdx) % swatches.size
      if (idx == 0 && controller.hasThemeAccent) {
        val tr = controller.themeAccentR
        val tg = controller.themeAccentG
        val tb = controller.themeAccentB
        val adjR = if (isDark) tr * 0.56f else 0.980f * 0.35f + tr * 0.65f
        val adjG = if (isDark) tg * 0.56f else 0.976f * 0.35f + tg * 0.65f
        val adjB = if (isDark) tb * 0.56f else 0.961f * 0.35f + tb * 0.65f
        emitRawBlock(
          col, row, spanCols, spanRows, adjR, adjG, adjB, alpha,
          scale, scaleX, scaleY, shearX, jitterX, jitterY,
        )
      } else {
        val c = swatches[idx]
        emitRawBlock(
          col, row, spanCols, spanRows, c[0], c[1], c[2], alpha,
          scale, scaleX, scaleY, shearX, jitterX, jitterY,
        )
      }
    }

    fun tryMarkSunCell(col: Int, row: Int): Boolean {
      if (col < 0 || col >= totalCols || row < 0 || row >= totalRows) return false
      val key = (col shl 16) or (row and 0xFFFF)
      for (i in 0 until sunVisitedCount) {
        if (sunVisitedCells[i] == key) return false
      }
      if (sunVisitedCount < sunVisitedCells.size) {
        sunVisitedCells[sunVisitedCount++] = key
      }
      return true
    }

    val isSunnyBackdrop =
      condition == WeatherConditionType.CLEAR_DAY ||
        (condition == WeatherConditionType.PARTLY_CLOUDY && isDay && precipIntensity <= 0.05f)
    val isPrecipitationBackdrop = precipIntensity > 0.05f

    // IMMERSIVE LAYER A: Big Yellow Pixel Sun + Shining Rays in Upper-Right Sky on Sunny Days
    if (isSunnyBackdrop) {
      sunVisitedCount = 0
      val isFullSun = condition == WeatherConditionType.CLEAR_DAY
      val intensityScale = if (isFullSun) 1.0f else 0.72f

      // Anchor big yellow pixel sun gracefully in upper-right sky with subtle gyro tilt parallax
      val sunCol =
        (totalCols - 2.6f - smoothedTiltSlope * 0.6f).coerceIn(3f, (totalCols - 2).toFloat())
      val sunRow = (totalRows - 4.6f).coerceIn(6f, (totalRows - 3).toFloat())

      // Core solar colors: luminous golden yellow in light mode, warm deep amber-gold on AMOLED black
      val coreR = if (isDark) 0.920f else 0.985f
      val coreG = if (isDark) 0.650f else 0.805f
      val coreB = if (isDark) 0.060f else 0.090f

      val outerR = if (isDark) 0.760f else 0.975f
      val outerG = if (isDark) 0.510f else 0.855f
      val outerB = if (isDark) 0.055f else 0.240f

      val pulse = 0.5f + 0.5f * sin(elapsedSec * 2.0f)

      // 1. Stepped Circular Solar Disc & Inner Corona
      val coreRadius = if (isFullSun) 2.55f else 2.15f
      val coronaRadius = if (isFullSun) 3.45f else 2.85f
      val centerColInt = sunCol.roundToInt()
      val centerRowInt = sunRow.roundToInt()

      for (dr in -4..4) {
        for (dc in -4..4) {
          val gc = centerColInt + dc
          val gr = centerRowInt + dr
          val dist = hypot(dc.toFloat(), dr.toFloat())
          if (dist <= coreRadius) {
            if (tryMarkSunCell(gc, gr)) {
              val isInner = dist <= 1.5f
              val baseAlpha =
                if (isDark) {
                  if (isInner) 0.62f else 0.44f
                } else {
                  if (isInner) 0.86f else 0.70f
                }
              emitRawBlock(
                col = gc.toFloat(),
                row = gr.toFloat(),
                spanCols = 1f,
                spanRows = 1f,
                r = if (isInner) coreR else outerR,
                g = if (isInner) coreG else outerG,
                b = if (isInner) coreB else outerB,
                alpha = baseAlpha * intensityScale,
              )
            }
          } else if (dist <= coronaRadius && ((gc + gr) % 2 == 0)) {
            if (tryMarkSunCell(gc, gr)) {
              val coronaAlpha =
                (if (isDark) 0.20f else 0.34f) + pulse * (if (isDark) 0.12f else 0.18f)
              emitRawBlock(
                col = gc.toFloat(),
                row = gr.toFloat(),
                spanCols = 1f,
                spanRows = 1f,
                r = outerR,
                g = outerG,
                b = outerB,
                alpha = coronaAlpha * intensityScale,
                scale = 0.88f + 0.12f * pulse,
              )
            }
          }
        }
      }

      // 2. Radiating Pixel Sunbeams Shimmering Outward Across the Graph Paper
      val spokeAnglesRad =
        floatArrayOf(
          3.14159f, // West (left across upper hero)
          3.53429f, // WSW
          3.92699f, // SW diagonal (primary hero sunbeam)
          4.31969f, // SSW
          4.71239f, // South (straight down right column)
          2.74889f, // WNW
          2.35619f, // NW diagonal
          1.57080f, // North (up toward top corner)
          5.49779f, // SE (toward right edge)
        )
      val spokeMaxSteps =
        if (isFullSun) intArrayOf(8, 9, 11, 9, 8, 6, 5, 4, 4)
        else intArrayOf(6, 7, 8, 7, 6, 5, 4, 3, 3)

      val angleTiltOffset = smoothedTiltSlope * 0.12f
      for (sIdx in spokeAnglesRad.indices) {
        val angle = spokeAnglesRad[sIdx] + angleTiltOffset
        val cosA = cos(angle)
        val sinA = sin(angle)
        val maxStep = spokeMaxSteps[sIdx]

        for (step in 4..maxStep) {
          val rc = (centerColInt + cosA * step).roundToInt()
          val rr = (centerRowInt + sinA * step).roundToInt()
          if (tryMarkSunCell(rc, rr)) {
            val radialFade = (1f - (step - 3.5f) / (maxStep + 1.5f)).coerceIn(0.18f, 0.92f)
            val travelingWave =
              0.5f + 0.5f * sin(elapsedSec * 2.9f - step * 0.72f + sIdx * 0.55f)
            val rayAlpha =
              radialFade *
                intensityScale *
                (if (isDark) 0.16f + 0.28f * travelingWave else 0.24f + 0.44f * travelingWave)
            val rayScale = 0.76f + 0.24f * travelingWave
            emitRawBlock(
              col = rc.toFloat(),
              row = rr.toFloat(),
              spanCols = 1f,
              spanRows = 1f,
              r = outerR,
              g = outerG,
              b = outerB,
              alpha = rayAlpha,
              scale = rayScale,
            )
          }
        }
      }

      // Soft stepped cloud accent peeking over lower-left edge when PARTLY_CLOUDY
      if (!isFullSun) {
        val cloudCol = (centerColInt - 4).coerceAtLeast(1).toFloat()
        val cloudRow = (centerRowInt - 2).coerceAtLeast(2).toFloat()
        val cR = if (isDark) 0.24f else 0.91f
        val cG = if (isDark) 0.27f else 0.92f
        val cB = if (isDark) 0.34f else 0.95f
        emitRawBlock(cloudCol, cloudRow, 4f, 1f, cR, cG, cB, if (isDark) 0.55f else 0.85f)
        emitRawBlock(cloudCol + 1f, cloudRow + 1f, 2f, 1f, cR, cG, cB, if (isDark) 0.48f else 0.78f)
      }
    }

    // IMMERSIVE LAYER B: Intensity-Scaled Blocky Rainfall / Snowfall Angled by Phone Gyro
    if (isPrecipitationBackdrop) {
      val isSnow = condition == WeatherConditionType.SNOW
      val isHeavyStorm =
        condition == WeatherConditionType.HEAVY_RAIN ||
          condition == WeatherConditionType.THUNDERSTORM

      val activeDrops =
        (8 + (precipIntensity * 34f).roundToInt()).coerceIn(8, RAIN_POOL_SIZE)
      val streakBlocks =
        when {
          isSnow -> 1
          precipIntensity >= 0.72f -> 4
          precipIntensity >= 0.40f -> 3
          else -> 2
        }
      val fallRateScreensPerSec =
        if (isSnow) {
          0.14f + precipIntensity * 0.10f
        } else {
          0.22f + precipIntensity * 0.34f
        }

      // Crisp pixel rain colors (cerulean + bright cyan tips in light; electric cyan-blue on AMOLED)
      val headR = if (isSnow) (if (isDark) 0.82f else 0.65f) else (if (isDark) 0.28f else 0.12f)
      val headG = if (isSnow) (if (isDark) 0.92f else 0.80f) else (if (isDark) 0.78f else 0.54f)
      val headB = if (isSnow) 0.99f else (if (isDark) 0.98f else 0.90f)

      val dropSpanW = if (isSnow) 0.74f else if (precipIntensity >= 0.65f) 0.74f else 0.62f
      val dropSpanH = if (isSnow) 0.74f else 0.86f
      val centerPadX = (1f - dropSpanW) * 0.5f

      for (i in 0 until activeDrops) {
        val prevY = rainNormY[i]
        val nextY = prevY - rawDt * fallRateScreensPerSec * rainSpeedJitter[i]
        if (nextY < -0.06f) {
          rainNormY[i] = 1.04f
          rainNormX[i] = ((i * 73 + (elapsedSec * 29f).toInt()) % 100) / 100f
        } else {
          rainNormY[i] = nextY
        }

        val normY = rainNormY[i]
        val headRow = normY * totalRows
        val fallDistFromTopRows = (1.0f - normY) * totalRows
        val baseCol = rainNormX[i] * totalCols
        val gyroSway =
          if (isSnow) {
            smoothedTiltSlope + 0.18f * sin(elapsedSec * 2.2f + i)
          } else {
            smoothedTiltSlope
          }
        val headCol = baseCol + gyroSway * fallDistFromTopRows

        for (seg in 0 until streakBlocks) {
          val segRow = headRow + seg * 0.88f
          val rawSegCol = headCol - gyroSway * (seg * 0.88f)
          val wrappedCol = ((rawSegCol % totalCols) + totalCols) % totalCols
          val snappedCellCol = floor(wrappedCol) + centerPadX
          val snappedCellRow = floor(segRow)

          val taper = (1f - seg * 0.24f).coerceIn(0.20f, 1f)
          val alpha =
            taper *
              (if (isDark) 0.56f + precipIntensity * 0.34f else 0.48f + precipIntensity * 0.38f)

          emitRawBlock(
            col = snappedCellCol,
            row = snappedCellRow,
            spanCols = dropSpanW,
            spanRows = dropSpanH,
            r = headR,
            g = headG,
            b = headB,
            alpha = alpha,
          )
        }

        // Splash pixel blocks on bottom grid row during heavy rain / thunderstorm
        if (isHeavyStorm && headRow in 0.0f..0.75f && i % 3 == 0) {
          val splashBaseCol = floor(((headCol % totalCols) + totalCols) % totalCols)
          val splashAlpha = (1f - headRow / 0.75f) * (if (isDark) 0.70f else 0.65f)
          emitRawBlock(
            col = (splashBaseCol - 0.65f).coerceAtLeast(0f),
            row = 0f,
            spanCols = 0.50f,
            spanRows = 0.50f,
            r = headR,
            g = headG,
            b = headB,
            alpha = splashAlpha,
          )
          emitRawBlock(
            col = (splashBaseCol + 1.15f).coerceAtMost((totalCols - 1).toFloat()),
            row = 0f,
            spanCols = 0.50f,
            spanRows = 0.50f,
            r = headR,
            g = headG,
            b = headB,
            alpha = splashAlpha,
          )
        }
      }
    }

    // Compute Weather-Reactive Physical Deformation Factors:
    val windKmh = controller.windSpeedKmh.toFloat()
    val directionSign = if (smoothedTiltSlope >= 0f) 1f else -1f
    val activeWindShear = (windKmh / 36f).coerceIn(0f, 0.82f) * directionSign

    val isRaining = isPrecipitationBackdrop && condition != WeatherConditionType.SNOW
    val baseSquishX = if (isRaining) 1f + 0.26f * sin(elapsedSec * 9.2f) else 1f
    val baseSquishY = if (isRaining) 1f - 0.22f * sin(elapsedSec * 9.2f) else 1f

    val glitchX = if (lightningBoost > 0f) (sin(elapsedSec * 64f) * 10f * lightningBoost) else 0f
    val glitchY = if (lightningBoost > 0f) (cos(elapsedSec * 52f) * 10f * lightningBoost) else 0f

    // LAYER C: Wind Gust Micro-Particle Streaks when Wind >= 22 km/h
    if (windKmh >= 22f) {
      val gustCount = (windKmh / 12f).roundToInt().coerceIn(3, 10)
      val gustR = if (isDark) 0.35f else 0.22f
      val gustG = if (isDark) 0.72f else 0.58f
      val gustB = if (isDark) 0.95f else 0.88f
      for (gIdx in 0 until gustCount) {
        val gustSpeed = (windKmh / 18f) * (0.8f + (gIdx % 3) * 0.25f)
        val rawCol = (elapsedSec * gustSpeed * directionSign + gIdx * 4.2f) % (totalCols + 4f)
        val wrappedCol = if (directionSign >= 0f) rawCol - 2f else (totalCols + 2f) - rawCol
        val row = ((gIdx * 7 + 3) % (totalRows - 4) + 2).toFloat()
        val streakAlpha = (0.35f + 0.45f * sin(elapsedSec * 3f + gIdx)).coerceIn(0.18f, 0.75f)
        emitRawBlock(
          col = wrappedCol,
          row = row,
          spanCols = 2.4f,
          spanRows = 0.22f,
          r = gustR,
          g = gustG,
          b = gustB,
          alpha = streakAlpha,
          shearX = activeWindShear * 0.85f,
        )
      }
    }

    // LAYER D: Drifting Fog / Mist Block Bands when FOG & MIST
    if (condition == WeatherConditionType.FOG) {
      val mistBands = 5
      val fogR = if (isDark) 0.38f else 0.88f
      val fogG = if (isDark) 0.42f else 0.90f
      val fogB = if (isDark) 0.48f else 0.94f
      for (mIdx in 0 until mistBands) {
        val mistRow = ((mIdx * 6 + 2) % (totalRows - 3)).toFloat()
        val driftCol = (sin(elapsedSec * 0.4f + mIdx) * 3f + (totalCols * 0.35f))
        val mistAlpha = 0.14f + 0.32f * sin(elapsedSec * 1.2f + mIdx * 1.1f)
        emitRawBlock(
          col = driftCol,
          row = mistRow,
          spanCols = 4.8f,
          spanRows = 0.90f,
          r = fogR,
          g = fogG,
          b = fogB,
          alpha = mistAlpha,
          scaleY = 0.78f,
        )
      }
    }

    // 1. Calm scattered architectural anchor blocks on gridlines with weather-condition deformation
    val glideA = pingPongStepGlide(elapsedSec * 0.36f)
    val glideB = pingPongStepGlide(elapsedSec * 0.27f + 1.4f)

    // Upper-left 2x1 accent block
    emitSwatchBlock(
      col = 1f,
      row = (totalRows - 2).toFloat(),
      spanCols = 2f,
      spanRows = 1f,
      swatchIdx = weatherSwatch,
      scaleX = baseSquishX,
      scaleY = baseSquishY,
      shearX = activeWindShear,
      jitterX = glitchX,
      jitterY = glitchY,
    )

    if (!isSunnyBackdrop && !isPrecipitationBackdrop) {
      // Upper-right 1x1 block
      emitSwatchBlock(
        col = (totalCols - 2).toFloat(),
        row = (totalRows - 3).toFloat() - glideA,
        spanCols = 1f,
        spanRows = 1f,
        swatchIdx = (weatherSwatch + 1) % 5,
        shearX = activeWindShear,
      )
      // Mid-right 2x2 architectural square block
      emitSwatchBlock(
        col = (totalCols - 3).toFloat() - glideB,
        row = floor(totalRows * 0.56f),
        spanCols = 2f,
        spanRows = 2f,
        swatchIdx = 1,
        shearX = activeWindShear,
      )
      // Mid-left solitary 1x1 block
      emitSwatchBlock(
        col = 1f,
        row = floor(totalRows * 0.48f) + glideA,
        spanCols = 1f,
        spanRows = 1f,
        swatchIdx = 2,
        shearX = activeWindShear,
      )
      // Lower-center solitary 1x1 block
      emitSwatchBlock(
        col = floor(totalCols * 0.44f),
        row = floor(totalRows * 0.31f),
        spanCols = 1f,
        spanRows = 1f,
        swatchIdx = 0,
        shearX = activeWindShear,
      )
      // Lower-right solitary 1x1 block
      emitSwatchBlock(
        col = (totalCols - 3).toFloat(),
        row = floor(totalRows * 0.19f),
        spanCols = 1f,
        spanRows = 1f,
        swatchIdx = 4,
        shearX = activeWindShear,
      )
      // Lower-left solitary 1x1 block
      emitSwatchBlock(
        col = 2f,
        row = floor(totalRows * 0.14f),
        spanCols = 1f,
        spanRows = 1f,
        swatchIdx = 3,
        shearX = activeWindShear,
      )

      // 2. Fluid 60-FPS randomly flickering ambient grid cells across the graph paper
      val flickerCells = 8
      for (f in 0 until flickerCells) {
        val cycleSec = 2.6f + (f % 4) * 0.55f
        val localPhase = (elapsedSec + f * 1.37f) / cycleSec
        val cycleIdx = floor(localPhase).toInt()
        val frac = localPhase - cycleIdx
        if (frac < 0.55f) {
          val u = frac / 0.55f
          val env =
            if (u < 0.28f) {
              val s = u / 0.28f
              s * s * (3f - 2f * s)
            } else if (u < 0.68f) {
              1f
            } else {
              val s = 1f - (u - 0.68f) / 0.32f
              s * s * (3f - 2f * s)
            }
          val hashCol = abs((cycleIdx * 37 + f * 19 + 5)) % max(4, totalCols - 2) + 1
          val hashRow = abs((cycleIdx * 53 + f * 29 + 7)) % max(6, totalRows - 3) + 1
          emitSwatchBlock(
            col = hashCol.toFloat(),
            row = hashRow.toFloat(),
            spanCols = 1f,
            spanRows = 1f,
            swatchIdx = (cycleIdx + f + weatherSwatch) % 5,
            alpha = env * (if (isDark) 0.72f else 0.62f),
            scale = 0.55f + 0.45f * env,
            shearX = activeWindShear * 0.5f,
          )
        }
      }
    } else {
      // Minimal subtle lower-corner accent squares when Sun or Rain is starring on screen
      emitSwatchBlock(
        col = (totalCols - 2).toFloat(),
        row = floor(totalRows * 0.22f),
        spanCols = 1f,
        spanRows = 1f,
        swatchIdx = weatherSwatch,
        alpha = 0.58f,
        scaleX = baseSquishX,
        scaleY = baseSquishY,
        shearX = activeWindShear,
        jitterX = glitchX,
        jitterY = glitchY,
      )
      emitSwatchBlock(
        col = 1f,
        row = floor(totalRows * 0.16f) + glideA,
        spanCols = 1f,
        spanRows = 1f,
        swatchIdx = (weatherSwatch + 2) % 5,
        alpha = 0.52f,
        scaleX = baseSquishX,
        scaleY = baseSquishY,
        shearX = activeWindShear,
        jitterX = glitchX,
        jitterY = glitchY,
      )
    }

    // 3. Touch-induced, touch-excited playful blocks stepping fluidly at 60 FPS across grid squares!
    for (idx in 0 until TOUCH_POOL_SIZE) {
      val age = tAgeSec[idx]
      val dur = tDurationSec[idx]
      if (age in 0f..dur) {
        val norm = (age / dur).coerceIn(0f, 1f)
        val totalSteps = 3f
        val stepRaw = norm * totalSteps
        val completedSteps = floor(stepRaw)
        val withinStep = stepRaw - completedSteps
        val easedStep = steepBlockEase(withinStep)
        val displacement = completedSteps + easedStep

        val curCol =
          (tStartCol[idx] + tDirX[idx] * displacement).coerceIn(0f, (totalCols - 1).toFloat())
        val curRow =
          (tStartRow[idx] + tDirY[idx] * displacement).coerceIn(0f, (totalRows - 1).toFloat())

        val popIn = (norm / 0.14f).coerceIn(0f, 1f)
        val fadeOut = ((1f - norm) / 0.32f).coerceIn(0f, 1f)
        val envelope = popIn * (fadeOut * fadeOut * (3f - 2f * fadeOut))

        emitSwatchBlock(
          col = curCol,
          row = curRow,
          spanCols = 1f,
          spanRows = 1f,
          swatchIdx = tSwatch[idx],
          alpha = envelope * 0.96f,
          scale = 0.65f + 0.35f * popIn,
          scaleX = baseSquishX,
          scaleY = baseSquishY,
          jitterX = glitchX,
          jitterY = glitchY,
        )
      }
    }

    // 4. Full-Phone-Height Collective Ripple Wave AFTER weather refresh completes:
    // Sweeps continuously from the very top row (totalRows) all the way through row 0 at the bottom edge.
    if (postSyncWaveActive) {
      val progress = (postSyncWaveAgeSec / POST_SYNC_WAVE_DURATION_SEC).coerceIn(0f, 1f)
      val rowsCount = totalRows.coerceAtLeast(10)
      val startFrontRow = rowsCount.toFloat() + 2.5f
      val endFrontRow = -POST_SYNC_WAVE_BAND_ROWS - 2.0f
      val baseFrontRow = startFrontRow + (endFrontRow - startFrontRow) * progress
      val colsDivisor = (totalCols - 1).coerceAtLeast(1).toFloat()

      for (c in 0 until totalCols) {
        val colNorm = c.toFloat() / colsDivisor
        val archOffset = sin(colNorm * Math.PI.toFloat()) * 2.2f
        val harmonicRipple = sin(colNorm * 9.42f + progress * 10f) * 0.72f
        val colFrontRow = baseFrontRow + archOffset + harmonicRipple

        val minRow = floor(colFrontRow).toInt().coerceAtLeast(0)
        val maxRow =
          kotlin.math.ceil(colFrontRow + POST_SYNC_WAVE_BAND_ROWS).toInt().coerceAtMost(rowsCount - 1)

        for (r in minRow..maxRow) {
          val distBehindFront = r.toFloat() - colFrontRow
          if (distBehindFront in 0f..POST_SYNC_WAVE_BAND_ROWS) {
            val u = distBehindFront / POST_SYNC_WAVE_BAND_ROWS
            val crest = sin(u * Math.PI.toFloat()).coerceIn(0f, 1f)
            val collectiveShimmer = 0.84f + 0.16f * cos(c * 1.15f + r * 0.85f - progress * 14f)
            val intensity = (crest * collectiveShimmer).coerceIn(0f, 1f)
            if (intensity > 0.14f) {
              val blockScale = (0.48f + 0.50f * crest).coerceIn(0.42f, 0.98f)
              val blockAlpha = intensity * (if (isDark) 0.78f else 0.72f)
              val swatchIdx = (c + r + lastProcessedPostSyncWave + lastProcessedSyncCascade) % swatches.size
              emitSwatchBlock(
                col = c.toFloat(),
                row = r.toFloat(),
                spanCols = 1f,
                spanRows = 1f,
                swatchIdx = swatchIdx,
                alpha = blockAlpha,
                scale = blockScale,
              )
            }
          }
        }
      }
    }

    if (blockCount > 0) {
      vertexBuffer.position(0)
      vertexBuffer.put(vertexArray, 0, ptr)

      GLES20.glUseProgram(blockProgram)
      GLES20.glUniform2f(bUniResolution, widthPx, heightPx)

      vertexBuffer.position(0)
      val strideBytes = FLOATS_PER_VERTEX * 4
      GLES20.glEnableVertexAttribArray(bAttrPos)
      GLES20.glVertexAttribPointer(bAttrPos, 2, GLES20.GL_FLOAT, false, strideBytes, vertexBuffer)

      vertexBuffer.position(2)
      GLES20.glEnableVertexAttribArray(bAttrColor)
      GLES20.glVertexAttribPointer(bAttrColor, 4, GLES20.GL_FLOAT, false, strideBytes, vertexBuffer)

      indexBuffer.position(0)
      GLES20.glDrawElements(
        GLES20.GL_TRIANGLES,
        blockCount * INDICES_PER_QUAD,
        GLES20.GL_UNSIGNED_SHORT,
        indexBuffer,
      )

      GLES20.glDisableVertexAttribArray(bAttrPos)
      GLES20.glDisableVertexAttribArray(bAttrColor)
    }
  }

  private fun steepBlockEase(u: Float): Float {
    val clamped = (u / 0.48f).coerceIn(0f, 1f)
    val inv = 1f - clamped
    return 1f - inv * inv * inv
  }

  private fun pingPongStepGlide(phase: Float): Float {
    val cyc = phase - floor(phase)
    return when {
      cyc < 0.38f -> 0f
      cyc < 0.52f -> {
        val u = (cyc - 0.38f) / 0.14f
        u * u * (3f - 2f * u)
      }
      cyc < 0.86f -> 1f
      else -> {
        val u = 1f - (cyc - 0.86f) / 0.14f
        u * u * (3f - 2f * u)
      }
    }
  }

  private fun weatherPrimarySwatchIndex(condition: WeatherConditionType): Int =
    when (condition) {
      WeatherConditionType.CLEAR_DAY -> 1 // Butter Yellow
      WeatherConditionType.CLEAR_NIGHT,
      WeatherConditionType.PARTLY_CLOUDY -> 3 // Periwinkle Lavender
      WeatherConditionType.OVERCAST,
      WeatherConditionType.FOG -> 0 // Sage Green
      WeatherConditionType.DRIZZLE,
      WeatherConditionType.RAIN,
      WeatherConditionType.HEAVY_RAIN,
      WeatherConditionType.SNOW -> 4 // Sky Blue
      WeatherConditionType.THUNDERSTORM -> 2 // Peach Coral
    }

  private fun allocateFreeTouchSlot(): Int {
    // First pass: find a completely finished slot so living blocks are NEVER interrupted mid-flight
    for (offset in 0 until TOUCH_POOL_SIZE) {
      val idx = (touchCursor + offset) % TOUCH_POOL_SIZE
      if (tAgeSec[idx] >= tDurationSec[idx]) {
        touchCursor = (idx + 1) % TOUCH_POOL_SIZE
        return idx
      }
    }
    // Fallback only if all 128 slots are simultaneously active: recycle the most-nearly-finished slot
    var oldestIdx = touchCursor
    var highestProgress = -1f
    for (i in 0 until TOUCH_POOL_SIZE) {
      val progress = tAgeSec[i] / tDurationSec[i].coerceAtLeast(0.1f)
      if (progress > highestProgress) {
        highestProgress = progress
        oldestIdx = i
      }
    }
    touchCursor = (oldestIdx + 1) % TOUCH_POOL_SIZE
    return oldestIdx
  }

  private fun updateTouchExcitedBlocks(dtSec: Float) {
    for (i in 0 until TOUCH_POOL_SIZE) {
      if (tAgeSec[i] <= tDurationSec[i]) {
        tAgeSec[i] += dtSec
      }
    }

    pendingBurstScratch.clear()
    controller.drainTouchBursts(pendingBurstScratch)
    if (pendingBurstScratch.isNotEmpty() && widthPx > 1f && heightPx > 1f) {
      val hopVectors =
        arrayOf(
          floatArrayOf(0f, 0f, 0f, 1f),
          floatArrayOf(1f, 0f, 1f, 0f),
          floatArrayOf(-1f, 0f, -1f, 0f),
          floatArrayOf(0f, 1f, 0f, 1f),
          floatArrayOf(0f, -1f, 0f, -1f),
          floatArrayOf(1f, 1f, 1f, 0f),
        )
      // Cap bursts per frame to at most 3 so even if a binder stall queued multiple events,
      // allocation remains constant-time and rock-solid
      val startBurst = (pendingBurstScratch.size - 3).coerceAtLeast(0)
      for (bIdx in startBurst until pendingBurstScratch.size) {
        val burst = pendingBurstScratch[bIdx]
        val glX = burst.x
        val glY = heightPx - burst.y
        if (glX in 0f..widthPx && glY in 0f..heightPx) {
          val baseCol = floor(glX / cellSizePx).coerceIn(0f, (totalCols - 1).toFloat())
          val baseRow = floor(glY / cellSizePx).coerceIn(0f, (totalRows - 1).toFloat())
          burstSeq++
          val count = burst.blockCount.coerceIn(1, 6)
          for (k in 0 until count) {
            val slot = allocateFreeTouchSlot()
            val v = hopVectors[(burstSeq + k) % hopVectors.size]
            tStartCol[slot] = (baseCol + v[0]).coerceIn(0f, (totalCols - 1).toFloat())
            tStartRow[slot] = (baseRow + v[1]).coerceIn(0f, (totalRows - 1).toFloat())
            tDirX[slot] = v[2]
            tDirY[slot] = v[3]
            tAgeSec[slot] = 0f
            tDurationSec[slot] = 0.88f + (k % 3) * 0.14f
            tSwatch[slot] = (slot + burstSeq) % 5
          }
        }
      }
    }

    val fidget = controller.getFidgetCount()
    if (fidget != lastProcessedFidget) {
      lastProcessedFidget = fidget
      val centerCol = floor(totalCols * 0.5f)
      val heroRow = floor(totalRows * 0.72f)
      val offsets =
        arrayOf(
          floatArrayOf(-4f, 1f, -1f, 0f),
          floatArrayOf(4f, 0f, 1f, 0f),
          floatArrayOf(-3f, -2f, 0f, -1f),
          floatArrayOf(3f, 2f, 0f, 1f),
          floatArrayOf(0f, 2f, 1f, 0f),
        )
      for (k in offsets.indices) {
        val slot = allocateFreeTouchSlot()
        val o = offsets[k]
        tStartCol[slot] = (centerCol + o[0]).coerceIn(0f, (totalCols - 1).toFloat())
        tStartRow[slot] = (heroRow + o[1]).coerceIn(0f, (totalRows - 1).toFloat())
        tDirX[slot] = o[2]
        tDirY[slot] = o[3]
        tAgeSec[slot] = 0f
        tDurationSec[slot] = 0.90f + (k % 2) * 0.15f
        tSwatch[slot] = (fidget + k) % 5
      }
    }

    val postWave = controller.getPostSyncWaveCount()
    val syncCascade = controller.getSyncCascadeCount()
    if (postWave != lastProcessedPostSyncWave || syncCascade != lastProcessedSyncCascade) {
      lastProcessedPostSyncWave = postWave
      lastProcessedSyncCascade = syncCascade
      postSyncWaveActive = true
      postSyncWaveAgeSec = 0f
    }
    if (postSyncWaveActive) {
      postSyncWaveAgeSec += dtSec
      if (postSyncWaveAgeSec >= POST_SYNC_WAVE_DURATION_SEC) {
        postSyncWaveActive = false
      }
    }
  }

  private fun compileProgram(vsSource: String, fsSource: String): Int {
    val vs = loadShader(GLES20.GL_VERTEX_SHADER, vsSource)
    val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fsSource)
    val program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vs)
    GLES20.glAttachShader(program, fs)
    GLES20.glLinkProgram(program)
    return program
  }

  private fun loadShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    return shader
  }
}

private const val GRID_VERTEX_SHADER = """
attribute vec2 aPosition;
void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
"""

private const val GRID_FRAGMENT_SHADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float uCellSize;
uniform vec3 uBgColor;
uniform vec3 uGridColor;

void main() {
    vec2 cellCoord = gl_FragCoord.xy / uCellSize;
    vec2 f = fract(cellCoord);
    vec2 distPx = min(f, 1.0 - f) * uCellSize;
    float minDist = min(distPx.x, distPx.y);
    float line = 1.0 - step(1.0, minDist);
    vec3 color = mix(uBgColor, uGridColor, line);
    gl_FragColor = vec4(color, 1.0);
}
"""

private const val BLOCK_VERTEX_SHADER = """
attribute vec2 aPosition;
attribute vec4 aColor;

uniform vec2 uResolution;

varying vec4 vColor;

void main() {
    vec2 ndc = (aPosition / uResolution) * 2.0 - 1.0;
    gl_Position = vec4(ndc, 0.0, 1.0);
    vColor = aColor;
}
"""

private const val BLOCK_FRAGMENT_SHADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

varying vec4 vColor;

void main() {
    gl_FragColor = vColor;
}
"""
