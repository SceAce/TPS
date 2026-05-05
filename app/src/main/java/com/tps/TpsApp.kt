package com.tps

/**
 * 文件说明：Android Application 入口，负责初始化全局依赖注入环境。
 */

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TpsApp : Application()
