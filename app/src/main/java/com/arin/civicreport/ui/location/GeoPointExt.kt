package com.arin.civicreport.ui.location

import org.osmdroid.util.GeoPoint as OsmdroidGeoPoint

fun GeoPoint.toOsmdroid(): OsmdroidGeoPoint = OsmdroidGeoPoint(latitude, longitude)

fun OsmdroidGeoPoint.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
