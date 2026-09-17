package com.example.heartdiseaseapp.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = Teal,
  secondary = Sky,
  tertiary = Coral,
  background = Ink,
  surface = DeepOcean,
  onPrimary = Ink,
  onBackground = Cloud,
  onSurface = Cloud,
  onSurfaceVariant = MutedCloud,
  outline = GlassBorder,
  error = Coral,
)

private val LightColorScheme = lightColorScheme(
  primary = DeepOcean,
  secondary = Teal,
  tertiary = Coral,
  background = Ink,
  surface = DeepOcean,
  onPrimary = Cloud,
  onBackground = Cloud,
  onSurface = Cloud,
  onSurfaceVariant = MutedCloud,
  outline = GlassBorder,
  error = Coral,
)

@Composable
fun HeartDiseaseAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
