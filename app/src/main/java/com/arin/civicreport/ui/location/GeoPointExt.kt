package com.arin.civicreport.ui.location

import org.osmdroid.util.GeoPoint as OsmdroidGeoPoint

fun GeoPoint.toOsmdroid(): OsmdroidGeoPoint = OsmdroidGeoPoint(latitude * 1e6, longitude * 1e6)
