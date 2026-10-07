package com.aistudio.dayflow.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.presentation.navigation.MainScaffold
import com.aistudio.dayflow.app.ui.theme.DayFlowTheme

class MainActivity : ComponentActivity() {

    private var openedHabitId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DayFlowApplication
        val container = app.container
        openedHabitId = intent?.getLongExtra("EXTRA_HABIT_ID", -1L)?.takeIf { it > 0 }

        setContent {
            val settings by container.settingsRepository.getSettings().collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )

            val isDark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DayFlowTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScaffold(
                        container = container,
                        initialHabitId = openedHabitId
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getLongExtra("EXTRA_HABIT_ID", -1L).takeIf { it > 0 }?.let {
            openedHabitId = it
        }
    }
}
