package com.acagribahar.muscleandmindapp.ui.theme

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.acagribahar.muscleandmindapp.data.local.SettingsManager
import com.acagribahar.muscleandmindapp.data.model.ThemePreference

// Material 3 için varsayılan renk şemaları
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F), // Örnek Koyu Arkaplan
    surface = Color(0xFF1C1B1F),    // Örnek Koyu Yüzey
    // Diğer renkleri buraya ekleyebilir veya Color.kt'den alabilirsiniz
    // background = Color(0xFF1C1B1F),
    // surface = Color(0xFF1C1B1F),
    // onPrimary = Color.Black,
    // onSecondary = Color.Black,
    // onTertiary = Color.Black,
    // onBackground = Color(0xFFE6E1E5),
    // onSurface = Color(0xFFE6E1E5),
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE), // Örnek Açık Arkaplan
    surface = Color(0xFFFFFBFE),    // Örnek Açık Yüzey
    // Diğer renkleri buraya ekleyebilir veya Color.kt'den alabilirsiniz
    // background = Color(0xFFFFFBFE),
    // surface = Color(0xFFFFFBFE),
    // onPrimary = Color.White,
    // onSecondary = Color.White,
    // onTertiary = Color.White,
    // onBackground = Color(0xFF1C1B1F),
    // onSurface = Color(0xFF1C1B1F),
)

@Composable
fun MindMuscleAppTheme( // Fonksiyon adımız bu!
    //darkTheme: Boolean = isSystemInDarkTheme(),
    // Dinamik renkler Android 12+ için kullanılabilir
    themePreference: ThemePreference = ThemePreference.SYSTEM, // Varsayılan değer
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // <<< useDarkTheme'i artık parametreden gelen themePreference'a göre belirleyelim >>>
    val useDarkTheme: Boolean = when(themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    Log.d("ThemeCheck", "Applying theme. useDarkTheme=$useDarkTheme, dynamicColor=$dynamicColor") // <<< Ek log

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Sistem çubuklarının (status bar vb.) görünümünü ayarla
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Örnek: Primary renk
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            // Navigation bar için de benzer ayar yapılabilir
            // window.navigationBarColor = colorScheme.surface.toArgb()
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Type.kt'den gelen tipografi
        content = content // Uygulama içeriği buraya gelecek
    )
}