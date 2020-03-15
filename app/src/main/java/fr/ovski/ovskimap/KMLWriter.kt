package fr.ovski.ovskimap

import org.osmdroid.bonuspack.routing.Road
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class KMLWriter {

    companion object {

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

        fun getKMLFromRoad(road: Road): String {
            var xml = "<?xml version='1.0' encoding='UTF-8'?> <kml xmlns='http://www.opengis.net/kml/2.2' xmlns:gx='http://www.google.com/kml/ext/2.2'> <Document> <Placemark> <styleUrl>#1</styleUrl> <name>routing_overlay</name> <LineString> <coordinates>"
            road.mRouteHigh.forEach {
                xml = xml.plus("${it.longitude},${it.latitude},${it.altitude} ")
            }
            xml = xml.plus("</coordinates> </LineString> </Placemark> <Style id='1'> <LineStyle> <color>80FF0000</color> <width>15.0</width> </LineStyle> </Style> </Document> </kml>")
            return xml
        }

    }
}