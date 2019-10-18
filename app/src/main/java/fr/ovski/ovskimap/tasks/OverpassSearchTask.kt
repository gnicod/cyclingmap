package fr.ovski.ovskimap.tasks

import android.os.AsyncTask
import android.util.Log
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.location.OverpassAPIProvider
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import java.util.*

/**
 * Created by ovski on 26/03/17.
 */

class OverpassQueryTaska(private val map: MapView, private val bb: BoundingBox, private val callback: Runnable) : AsyncTask<Void, Int, ArrayList<*>>() {

    override fun doInBackground(vararg params: Void): ArrayList<*>? {

        val overpassProvider = OverpassAPIProvider()
        overpassProvider.setService("https://overpass-api.de/api/interpreter")
        Log.i("Overpassquery", this.bb.toString())
        val url = overpassProvider.urlForTagSearchKml("amenity=drinking_water", this.bb, 500, 30)

        val pois = overpassProvider.getPOIsFromUrl(url)
        for (poi in pois) {
            Log.i("OPENRUNNER", poi.toString())
        }

        val kmlDocument = KmlDocument()
        val ok = overpassProvider.addInKmlFolder(kmlDocument.mKmlRoot, url)
        val kmlOverlay = kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument) as FolderOverlay
        //callback.run(kmlOverlay);

        return null
    }

}

