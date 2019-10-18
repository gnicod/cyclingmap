package fr.ovski.ovskimap.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;

import java.util.ArrayList;

/**
 * Created by ovski on 26/03/17.
 */

public class OverpassQueryTask extends AsyncTask<Void, Integer, FolderOverlay> {

    private MapView map;
    private BoundingBox bb;

    public OverpassQueryTask(MapView map, BoundingBox bb) {
        this.bb = bb;
        this.map = map;
    }

    @Override
    protected FolderOverlay doInBackground(Void... params) {

        OverpassAPIProvider overpassProvider = new OverpassAPIProvider();
        overpassProvider.setService("https://overpass-api.de/api/interpreter");
        Log.i("Overpassquery", this.bb.toString());
        String url = overpassProvider.urlForTagSearchKml("amenity=drinking_water", this.bb, 500, 30);

        ArrayList<POI> pois = overpassProvider.getPOIsFromUrl(url);
        for( POI poi : pois){
            Log.i("OPENRUNNER", poi.toString());
        }

        KmlDocument kmlDocument = new KmlDocument();
        boolean ok = overpassProvider.addInKmlFolder(kmlDocument.mKmlRoot, url);
        FolderOverlay kmlOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);
        return kmlOverlay;
    }

    @Override
    protected void onPostExecute(FolderOverlay folderOverlay) {
        super.onPostExecute(folderOverlay);
        map.getOverlays().add(folderOverlay);
    }
}
