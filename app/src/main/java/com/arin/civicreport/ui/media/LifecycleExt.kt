package com.arin.civicreport.ui.media

import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner

val androidx.lifecycle.LifecycleOwner.surfaceProvider: androidx.camera.view.SurfaceProvider?
    get() {
        var v: android.view.View? = this as? android.view.View
        while (v != null && v !is ComposeView) {
            v = v.parent as? android.view.View
        }
        return (v as? ComposeView)?.getChildAt(0) as? PreviewView
    }
