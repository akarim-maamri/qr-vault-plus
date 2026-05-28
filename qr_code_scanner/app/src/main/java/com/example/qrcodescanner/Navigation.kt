package com.example.qrcodescanner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.qrcodescanner.ui.main.MainScreen

@Composable
fun MainNavigation(initialTab: Int, authLink: String? = null) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            initialTab = initialTab,
            authLink = authLink,
            onItemClick = { navKey -> backStack.add(navKey) },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
