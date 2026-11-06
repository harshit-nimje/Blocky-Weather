package com.example.gl

import com.example.domain.model.WeatherConditionType
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.hypot

data class TouchGridBurst(
  val x: Float,
  val y: Float,
  val blockCount: Int,
  val isPress: Boolean,
)

class PixelGridController {
  companion object {
    private const val MAX_QUEUED_BURSTS = 24
    private const val MIN_DRAG_DISTANCE_PX = 52f
    private const val MIN_DRAG_INTERVAL_NANOS = 65_000_000L
  }

  private val conditionRef = AtomicReference(WeatherConditionType.PARTLY_CLOUDY)
  private val darkModeRef = AtomicBoolean(false)
  private val ecoModeRef = AtomicBoolean(false)
  private val isDayRef = AtomicBoolean(true)

  @Volatile var precipitationMm: Double = 0.0
    private set

  @Volatile var precipProbPercent: Int = 0
    private set

  @Volatile var windSpeedKmh: Double = 0.0
    private set

  @Volatile var rainTiltSlope: Float = 0f
    private set

  @Volatile var touchX: Float = -1000f
    private set

  @Volatile var touchY: Float = -1000f
    private set

  @Volatile var touchActive: Boolean = false
    private set

  @Volatile var hasThemeAccent: Boolean = false
    private set

  @Volatile var themeAccentR: Float = 0.42f
    private set

  @Volatile var themeAccentG: Float = 0.34f
    private set

  @Volatile var themeAccentB: Float = 0.84f
    private set

  private val burstLock = Any()
  private val pendingBursts = ArrayDeque<TouchGridBurst>(MAX_QUEUED_BURSTS)
  private var lastSpawnX: Float = -1000f
  private var lastSpawnY: Float = -1000f
  private var lastSpawnNanos: Long = 0L

  private val tickCounter = AtomicInteger(0)
  private val touchImpulseCounter = AtomicInteger(0)
  private val fidgetCounter = AtomicInteger(0)

  @Volatile private var onRenderRequested: (() -> Unit)? = null

  fun attachRenderCallback(callback: (() -> Unit)?) {
    onRenderRequested = callback
  }

  fun requestFrame() {
    onRenderRequested?.invoke()
  }

  fun advanceDiscreteTick() {
    tickCounter.incrementAndGet()
    if (ecoModeRef.get()) {
      onRenderRequested?.invoke()
    }
  }

  fun updateThemeAccent(enabled: Boolean, r: Float, g: Float, b: Float) {
    hasThemeAccent = enabled
    themeAccentR = r.coerceIn(0f, 1f)
    themeAccentG = g.coerceIn(0f, 1f)
    themeAccentB = b.coerceIn(0f, 1f)
    if (ecoModeRef.get()) {
      onRenderRequested?.invoke()
    }
  }

  fun updateWeatherState(
    condition: WeatherConditionType,
    isDarkMode: Boolean,
    ecoMode: Boolean,
    precipitationMm: Double = 0.0,
    precipProbPercent: Int = 0,
    windSpeedKmh: Double = 0.0,
    isDay: Boolean = true,
  ) {
    conditionRef.set(condition)
    darkModeRef.set(isDarkMode)
    ecoModeRef.set(ecoMode)
    this.precipitationMm = precipitationMm
    this.precipProbPercent = precipProbPercent
    this.windSpeedKmh = windSpeedKmh
    isDayRef.set(isDay)
    onRenderRequested?.invoke()
  }

  /**
   * Updates live phone tilt slope for rainfall angle (-1.15f left tilt .. +1.15f right tilt).
   */
  fun setMotionTilt(slope: Float) {
    rainTiltSlope = slope.coerceIn(-1.15f, 1.15f)
  }

  /**
   * Computes normalized rainfall/snow intensity in [0f..1f] from active condition & precipitation rate.
   */
  fun getPrecipitationIntensity(): Float {
    val cond = conditionRef.get()
    val mm = precipitationMm.coerceAtLeast(0.0)
    return when (cond) {
      WeatherConditionType.HEAVY_RAIN,
      WeatherConditionType.THUNDERSTORM ->
        (0.76f + (mm / 18.0).toFloat() * 0.24f).coerceIn(0.76f, 1f)
      WeatherConditionType.RAIN ->
        (0.45f + (mm / 8.0).toFloat() * 0.30f).coerceIn(0.45f, 0.75f)
      WeatherConditionType.DRIZZLE ->
        (0.22f + (mm / 3.0).toFloat() * 0.20f).coerceIn(0.22f, 0.42f)
      WeatherConditionType.SNOW ->
        (0.28f + (mm / 5.0).toFloat() * 0.45f).coerceIn(0.28f, 0.75f)
      else ->
        if (mm >= 0.15) {
          (0.22f + (mm / 8.0).toFloat() * 0.65f).coerceIn(0.22f, 0.88f)
        } else {
          0f
        }
    }
  }

  private fun enqueueBurst(x: Float, y: Float, blockCount: Int, isPress: Boolean) {
    synchronized(burstLock) {
      if (pendingBursts.size >= MAX_QUEUED_BURSTS) {
        pendingBursts.pollFirst()
      }
      pendingBursts.offerLast(TouchGridBurst(x = x, y = y, blockCount = blockCount, isPress = isPress))
    }
    touchImpulseCounter.incrementAndGet()
    if (ecoModeRef.get()) {
      onRenderRequested?.invoke()
    }
  }

  fun drainTouchBursts(out: MutableList<TouchGridBurst>) {
    synchronized(burstLock) {
      while (pendingBursts.isNotEmpty()) {
        out.add(pendingBursts.removeFirst())
      }
    }
  }

  fun onTouchDown(x: Float, y: Float) {
    touchX = x
    touchY = y
    touchActive = true
    lastSpawnX = x
    lastSpawnY = y
    lastSpawnNanos = System.nanoTime()
    enqueueBurst(x = x, y = y, blockCount = 5, isPress = true)
  }

  fun onTouchMove(x: Float, y: Float) {
    touchX = x
    touchY = y
    touchActive = true

    val nowNanos = System.nanoTime()
    val distPx = hypot(x - lastSpawnX, y - lastSpawnY)
    val elapsedNanos = nowNanos - lastSpawnNanos

    // Debounce high-frequency 120-240Hz pointer move events so dragging spawns clean grid-spaced trails
    // without flooding the block pool or resetting active animations mid-flight.
    if (distPx >= MIN_DRAG_DISTANCE_PX || (distPx >= 26f && elapsedNanos >= MIN_DRAG_INTERVAL_NANOS)) {
      lastSpawnX = x
      lastSpawnY = y
      lastSpawnNanos = nowNanos
      enqueueBurst(x = x, y = y, blockCount = 3, isPress = false)
    }
  }

  fun onTouchUp() {
    val distPx = hypot(touchX - lastSpawnX, touchY - lastSpawnY)
    if (touchActive && touchX >= 0f && touchY >= 0f && distPx >= 22f) {
      enqueueBurst(x = touchX, y = touchY, blockCount = 2, isPress = false)
    }
    touchActive = false
  }

  fun triggerPlayfulFidget() {
    fidgetCounter.incrementAndGet()
    if (ecoModeRef.get()) {
      onRenderRequested?.invoke()
    }
  }

  private val syncCascadeCounter = AtomicInteger(0)

  fun triggerSyncSuccessCascade() {
    syncCascadeCounter.incrementAndGet()
    if (ecoModeRef.get()) {
      onRenderRequested?.invoke()
    }
  }

  fun getCondition(): WeatherConditionType = conditionRef.get()

  fun isDarkMode(): Boolean = darkModeRef.get()

  fun isEcoMode(): Boolean = ecoModeRef.get()

  fun isDay(): Boolean = isDayRef.get()

  fun getTick(): Int = tickCounter.get()

  fun getTouchImpulseCount(): Int = touchImpulseCounter.get()

  fun getFidgetCount(): Int = fidgetCounter.get()

  fun getSyncCascadeCount(): Int = syncCascadeCounter.get()
}
