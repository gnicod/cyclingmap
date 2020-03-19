package com.trekle.trekle.layers

import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.overlay.Overlay

class LayerOverlay(var state: Boolean, var overlay :Overlay?){
    fun setSate(state: Boolean): LayerOverlay {
        this.state = state;
        return this;
    }
}

class LayerManager {

    val layers = hashMapOf<String, LayerOverlay>("Hiking" to LayerOverlay(false, null), "Cycling" to LayerOverlay(false, null));

    fun getTileSource(name: String): XYTileSource {
        return XYTileSource(name, 3, 18, 256, ".png",
                arrayOf("https://tile.waymarkedtrails.org/" + name.toLowerCase() + "/"));
    }

}