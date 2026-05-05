package com.tps

/**
 * 文件说明：Android 应用入口 Activity，负责挂载 Compose 根界面。
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.tps.ui.TpsNavHost
import com.tps.ui.theme.TpsTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TpsTheme {
                TpsNavHost()
            }
        }
    }
}
