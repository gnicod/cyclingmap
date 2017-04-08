package fr.ovski.ovskimap.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;

import java.util.ArrayList;

import fr.ovski.ovskimap.R;

/**
 * Created by ovski on 26/03/17.
 */

public class OverpassQueryTask extends AsyncTask<Void, Integer, ArrayList> {

    BoundingBox bb;

    public OverpassQueryTask(BoundingBox map) {
        this.bb = bb;

    }

    @Override
    protected ArrayList doInBackground(Void... params) {

        OverpassAPIProvider overpassProvider = new OverpassAPIProvider();
        String url = overpassProvider.urlForTagSearchKml("highway=speed_camera", bb, 500, 30);

        ArrayList<POI> pois = overpassProvider.getPOIsFromUrl(url);
        for( POI poi : pois){
            Log.i("OPENRUNNER", poi.toString());
        }

        /*boolean ok = overpassProvider.addInKmlFolder(kmlDocument.mKmlRoot, url);
        FolderOverlay kmlOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);
        map.getOverlays().add(kmlOverlay);
        */
        return null;
    }
}
