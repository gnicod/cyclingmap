package fr.ovski.ovskimap

import org.osmdroid.bonuspack.routing.Road

interface AsyncResponse {
    fun processFinish(road: Road)
}