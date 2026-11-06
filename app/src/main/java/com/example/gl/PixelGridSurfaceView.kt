package com.example.gl

import android.opengl.GLSurfaceView
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@Composable
fun PixelGridSurfaceView(
  controller: PixelGridController,
  ecoMode: Boolean,
  targetFrameRateHz: Float = 0f,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current.density
  val lifecycleOwner = LocalLifecycleOwner.current
  val renderer = remember(controller, density) { PixelGridRenderer(controller, density) }

  var surfaceViewRef: GLSurfaceView? = remember { null }

  // Only poll on-demand ticks when in battery-saver ecoMode (RENDERMODE_WHEN_DIRTY).
  // In continuous mode, GLSurfaceView renders directly on vsync with zero UI-thread lock contention.
  LaunchedEffect(controller, ecoMode) {
    if (ecoMode) {
      while (true) {
        delay(180L)
        controller.advanceDiscreteTick()
      }
    }
  }

  LaunchedEffect(ecoMode, targetFrameRateHz) {
    surfaceViewRef?.let { view ->
      view.renderMode =
        if (ecoMode) GLSurfaceView.RENDERMODE_WHEN_DIRTY else GLSurfaceView.RENDERMODE_CONTINUOUSLY
      applySurfaceFrameRateHint(view, targetFrameRateHz)
      view.requestRender()
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          surfaceViewRef?.onResume()
          surfaceViewRef?.let { applySurfaceFrameRateHint(it, targetFrameRateHz) }
          surfaceViewRef?.requestRender()
        }
        Lifecycle.Event.ON_PAUSE -> surfaceViewRef?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      controller.attachRenderCallback(null)
      lifecycleOwner.lifecycle.removeObserver(observer)
      surfaceViewRef?.onPause()
    }
  }

  AndroidView(
    factory = { context ->
      GLSurfaceView(context).apply {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        holder.addCallback(
          object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
              applySurfaceFrameRateHint(this@apply, targetFrameRateHz)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
              applySurfaceFrameRateHint(this@apply, targetFrameRateHz)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
          }
        )
        setRenderer(renderer)
        renderMode =
          if (ecoMode) GLSurfaceView.RENDERMODE_WHEN_DIRTY
          else GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceViewRef = this
        controller.attachRenderCallback { requestRender() }
        requestRender()
      }
    },
    modifier = modifier,
    update = { view ->
      surfaceViewRef = view
      view.renderMode =
        if (ecoMode) GLSurfaceView.RENDERMODE_WHEN_DIRTY else GLSurfaceView.RENDERMODE_CONTINUOUSLY
      applySurfaceFrameRateHint(view, targetFrameRateHz)
      controller.attachRenderCallback { view.requestRender() }
    },
  )
}

private fun applySurfaceFrameRateHint(view: GLSurfaceView, targetFrameRateHz: Float) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val surface = view.holder.surface
    if (surface != null && surface.isValid) {
      val compatibility =
        if (targetFrameRateHz > 0f) Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
        else Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
      runCatching { surface.setFrameRate(targetFrameRateHz.coerceAtLeast(0f), compatibility) }
    }
  }
}
