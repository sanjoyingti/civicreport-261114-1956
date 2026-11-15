package com.arin.civicreport.ui.media

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView

/** Returns the preview surface provider in the type the camera library now expects. */
fun PreviewView.cameraSurfaceProvider(): Preview.SurfaceProvider = this.surfaceProvider
