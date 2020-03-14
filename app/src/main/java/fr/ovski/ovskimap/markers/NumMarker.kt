package fr.ovski.ovskimap.markers

import android.graphics.Color
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class NumMarker(mapView: MapView?, index: Int) : Marker(mapView) {
    init {
        ENABLE_TEXT_LABELS_WHEN_NO_IMAGE = true
        textLabelBackgroundColor = Color.BLACK
        textLabelForegroundColor = Color.RED
        textLabelFontSize = 40
        mTitle = index.toString()
        this.setIcon(null)
    }

    fun setNumber(index: Int) {
        mTitle = index.toString()
        this.setIcon(null)
    }

}