package com.example.camui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * App-process scope for photo work that must outlive the CameraScreen composition.
 *
 * This prevents navigation or a UI transition from cancelling an active multi-frame
 * render. It does not survive Android process death; durable WorkManager/foreground
 * service execution remains a separate upgrade.
 */
internal object LunaDreamProcessingScope : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = job + Dispatchers.IO
}
