package com.autotalk.app

import android.app.Application

/** Application 入口，创建并持有全局 [AppContainer]。 */
class AutoTalkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.bootstrap()
    }
}
