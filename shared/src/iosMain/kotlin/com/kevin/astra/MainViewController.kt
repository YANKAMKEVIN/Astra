package com.kevin.astra

import androidx.compose.ui.window.ComposeUIViewController
import com.kevin.astra.domain.gmail.registerIosGmailController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // Publish the Gmail controller once, before any composition / ViewModel is created.
    registerIosGmailController()
    return ComposeUIViewController { App() }
}
