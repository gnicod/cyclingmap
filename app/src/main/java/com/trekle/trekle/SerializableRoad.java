package com.trekle.trekle;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ovski on 09/04/17.
 */

class SerializableRoad extends Road implements Serializable {

    private final ArrayList<GeoPoint> waypoints;

    public SerializableRoad(ArrayList<GeoPoint> waypoints) {
        this.waypoints = waypoints;
    }


}
