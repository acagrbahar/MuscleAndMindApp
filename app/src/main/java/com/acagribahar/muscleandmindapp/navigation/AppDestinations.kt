package com.acagribahar.muscleandmindapp.navigation

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    // Ana Sayfa
    object Home : Screen("home", "Ana Sayfa", androidx.compose.material.icons.Icons.Filled.Home)
    // Egzersizler
    object Exercises : Screen("exercises", "Egzersizler", androidx.compose.material.icons.Icons.Filled.FitnessCenter) // İkonu değiştirilebilir
    // Zihin Görevleri
    object MindTasks : Screen("mind_tasks", "Zihin", androidx.compose.material.icons.Icons.Filled.Psychology) // İkonu değiştirilebilir
    // Gelişim
    object Progress : Screen("progress", "Gelişim", androidx.compose.material.icons.Icons.Filled.Timeline) // İkonu değiştirilebilir
    // Ayarlar
    object Settings : Screen("settings", "Ayarlar", androidx.compose.material.icons.Icons.Filled.Settings)

    // Bottom Navigation'da görünecek ekranları listelemek için yardımcı liste
    companion object {
        val bottomNavItems = listOf(Home, Exercises, MindTasks, Progress, Settings)
    }

    // AppDestinations.kt içine eklenebilir veya ayrı bir dosyada olabilir
    object AuthDestinations {
        const val ROOT_ROUTE = "auth_root" // Kimlik doğrulama akışının kök rotası
        const val LOGIN_ROUTE = "login"
        const val REGISTER_ROUTE = "register"
    }

    object MainDestinations {
        const val ROOT_ROUTE = "main_root" // Ana uygulama akışının kök rotası (Scaffold içerir)
    }

// ... (Screen sealed class'ı burada zaten var)

    object Graph { // Navigasyon graflarını gruplamak için
        const val ROOT = "root_graph"
        const val AUTHENTICATION = "auth_graph"
        const val MAIN = "main_graph" // Ana uygulama (alt barlı kısım)
    }

    object AuthScreen { // Kimlik doğrulama ekranları
        const val LOGIN = "login"
        const val REGISTER = "register"
    }

    // ... (Mevcut Screen, Graph, AuthScreen objeleri) ...

    object ExerciseDestinations {
        const val EXERCISE_DETAIL_ROUTE = "exercise_detail" // Ana rota
        const val ARG_EXERCISE_TITLE = "exerciseTitle" // Argüman adı
        // Tam yol: "exercise_detail/{exerciseTitle}"
        val routeWithArgs = "$EXERCISE_DETAIL_ROUTE/{$ARG_EXERCISE_TITLE}"
        // Argümanları tanımla (NavHost'ta kullanılacak)
        val arguments = listOf(
            androidx.navigation.navArgument(ARG_EXERCISE_TITLE) { type = androidx.navigation.NavType.StringType }
        )
    }

    object MindTaskDestinations {
        const val MIND_TASK_DETAIL_ROUTE = "mind_task_detail"
        const val ARG_TASK_TITLE = "taskTitle" // Argüman adı (Exercise ile aynı olabilir)
        // Tam yol: "mind_task_detail/{taskTitle}"
        val routeWithArgs = "$MIND_TASK_DETAIL_ROUTE/{$ARG_TASK_TITLE}"
        // Argümanları tanımla
        val arguments = listOf(
            androidx.navigation.navArgument(ARG_TASK_TITLE) { type = androidx.navigation.NavType.StringType }
        )
    }


}