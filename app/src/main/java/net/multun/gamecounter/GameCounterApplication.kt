package net.multun.gamecounter

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltAndroidApp
class GameCounterApplication : Application() {
    // workaround for a deprecation warning: https://github.com/google/dagger/issues/3601
    @Inject
    @ApplicationContext
    lateinit var context: Context
}
