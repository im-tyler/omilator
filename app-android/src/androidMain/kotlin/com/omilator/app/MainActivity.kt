package com.omilator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.omilator.data.library.AndroidLibraryScanner
import com.omilator.ui.OmilatorApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmilatorApp(libraryScanner = AndroidLibraryScanner())
        }
    }
}
