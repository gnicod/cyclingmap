package com.trekle.trekle

import com.github.mikephil.charting.data.Entry
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.kml.KmlPlacemark
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


class KMLUtils {

    companion object {

        /**
         * Convert an xml string to a file object required by osmdroid
         */
        fun convertStringToFile(xml: String): File? {
            try {
                val temp = File.createTempFile("pattern", ".suffix")
                temp.deleteOnExit()
                val out = BufferedWriter(FileWriter(temp))
                out.write(xml)
                out.close()
                return temp
            } catch (e: IOException) {
                return null
            }
        }

        /**
         * Generate xml string from Road object passed in parameter
         */
        fun getKMLFromRoad(road: Road): String {
            var xml = "<?xml version='1.0' encoding='UTF-8'?> <kml xmlns='http://www.opengis.net/kml/2.2' xmlns:gx='http://www.google.com/kml/ext/2.2'> <Document> <Placemark> <styleUrl>#1</styleUrl> <name>routing_overlay</name> <LineString> <coordinates>"
            road.mRouteHigh.forEach {
                xml = xml.plus("${it.longitude},${it.latitude},${it.altitude} ")
            }
            xml = xml.plus("</coordinates> </LineString> </Placemark> <Style id='1'> <LineStyle> <color>80FF0000</color> <width>15.0</width> </LineStyle> </Style> </Document> </kml>")
            return xml
        }

        /**
         * Calculate distance between two points in latitude and longitude taking
         * into account height difference. If you are not interested in height
         * difference pass 0.0. Uses Haversine method as its base.
         *
         * @returns Distance in Meters
         */
        fun distanceBetweenGeoPoints(geo1: GeoPoint, geo2: GeoPoint): Float {

            val lat1 = geo1.latitude
            val lon1 = geo1.longitude
            val el1 = geo1.altitude
            val lat2 = geo2.latitude
            val lon2 = geo2.longitude
            val el2 = geo2.altitude

            val R = 6371 // Radius of the earth

            val latDistance = Math.toRadians(lat2 - lat1)
            val lonDistance = Math.toRadians(lon2 - lon1)
            val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2))
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            var distance = R.toDouble() * c * 1000.0 // convert to meters

            val height = el1 - el2

            distance = Math.pow(distance, 2.0) + Math.pow(height, 2.0)

            return Math.sqrt(distance).toFloat()
        }

        /**
         * Return a list of geopoints contained in the KMLDocument passed in parameters
         */
        fun getGeopoints(kmlDocument: KmlDocument): java.util.ArrayList<GeoPoint> {
            return (kmlDocument.mKmlRoot.mItems[0] as KmlPlacemark).mGeometry.mCoordinates
        }

        /**
         * From a KmlDocument object return an Entry of distance, altitude used to feed a LineChart
         */
        fun getEntriesFromKmlDocument(kmlDocument: KmlDocument): ArrayList<Entry> {
            var distance: Float = 0F
            val results = arrayListOf<Entry>()
            val coordinates = getGeopoints(kmlDocument)
            coordinates.forEachIndexed {
                index, geopoint ->
                run {
                    if (index > 0) {
                        distance = (distance + distanceBetweenGeoPoints(coordinates.get(index - 1), coordinates.get(index)))
                    }
                    results.add(Entry(distance, geopoint.altitude.toFloat(), geopoint))
                }
            }
            return results
        }
    }
}