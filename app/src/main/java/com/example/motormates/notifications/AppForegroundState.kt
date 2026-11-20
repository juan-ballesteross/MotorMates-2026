package com.example.motormates.notifications

import android.app.Activity
import android.app.Application
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ¿Está la app en primer plano? Se usa para decidir si un resultado de AI
 * merece notificación: si el usuario está mirando la pantalla, no hace falta.
 *
 * Cuenta las Activities iniciadas (hoy solo hay una, MainActivity) en vez de
 * agregar la dependencia de ProcessLifecycleOwner solo para esto.
 */
@Singleton
class AppForegroundState @Inject constructor() : Application.ActivityLifecycleCallbacks {

    private val _isInForeground = MutableStateFlow(false)
    val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()

    private var startedActivities = 0

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        _isInForeground.value = true
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        _isInForeground.value = startedActivities > 0
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
