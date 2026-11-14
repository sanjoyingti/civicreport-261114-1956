package com.arin.civicreport.ui.media

import androidx.camera.view.PreviewView
import androidx.camera.view.SurfaceProvider

fun PreviewView.getCameraSurfaceProvider(): SurfaceProvider {
    return this.surfaceProvider
}
