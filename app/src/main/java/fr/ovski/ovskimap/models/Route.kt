package fr.ovski.ovskimap.models

import java.io.Serializable

class Route(public var name: String) : Serializable{

    public var kml: String = ""

    constructor(): this("")

}
