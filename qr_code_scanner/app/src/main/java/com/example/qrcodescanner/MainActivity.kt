package com.example.qrcodescanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.qrcodescanner.theme.QRCodeScannerTheme

class MainActivity : FragmentActivity() {
  // Use Compose state for responsive runtime updates from onNewIntent
  private val initialTabState = mutableStateOf(3)
  private val authLinkState = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)

    enableEdgeToEdge()
    setContent {
      QRCodeScannerTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
          MainNavigation(initialTab = initialTabState.value, authLink = authLinkState.value) 
        } 
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    intent?.let {
      if (it.hasExtra("selected_tab")) {
        initialTabState.value = it.getIntExtra("selected_tab", 0)
      }
      if (it.data != null) {
        authLinkState.value = it.data.toString()
      }
    }
  }
}
