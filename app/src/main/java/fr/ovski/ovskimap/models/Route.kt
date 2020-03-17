package fr.ovski.ovskimap.models

import java.io.Serializable

class Route(var name: String, var distance: Double, var ascent: Double) : Serializable{

    var kml: String = ""

    constructor(): this("", 0.0, 0.0)

}
