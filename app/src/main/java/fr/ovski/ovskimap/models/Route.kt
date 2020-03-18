package fr.ovski.ovskimap.models

import fr.ovski.ovskimap.KMLUtils
import org.osmdroid.bonuspack.kml.KmlDocument
import java.io.Serializable

class Route(var key:String, var name: String, var distance: Double, var ascent: Double) : Serializable{

    var kml: String = ""

    var kmlDocument: KmlDocument? = null
        get() {
            if (field == null && kml.isNotEmpty()) {
                val kmlDocument = KmlDocument()
                kmlDocument.parseKMLFile(KMLUtils.convertStringToFile(kml))
                return kmlDocument
            }
            return null
        }

    constructor(): this("", "", 0.0, 0.0)

}
