package com.trekle.trekle

import org.osmdroid.bonuspack.routing.Road

interface AsyncResponse {
    fun processFinish(road: Road)
}