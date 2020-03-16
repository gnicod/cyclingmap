package fr.ovski.ovskimap.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.text.DecimalFormat;
import java.util.ArrayList;

import fr.ovski.ovskimap.AsyncResponse;
import fr.ovski.ovskimap.R;

public class GraphHopperTask extends AsyncTask<Object, Object, Road> {

    public final static String OVERLAY_TITLE = "routing_overlay";
    private ArrayList<GeoPoint> waypoints;
    private GraphHopperRoadManager roadManager;
    private MapView map;
    private View routingView;
    private TextView textDistance;
    private TextView textElevation;
    private Polyline roadOverlay;
    public AsyncResponse delegate = null;

    @Override
    protected Road doInBackground(Object... params) {
        return roadManager.getRoad(waypoints);
    }

    public GraphHopperTask(MapView map, View routingView, String apiKey, ArrayList<GeoPoint> waypoints) {
        this.map = map;
        this.routingView = routingView;
        textDistance = routingView.findViewById(R.id.routing_distance);
        textElevation = routingView.findViewById(R.id.routing_elevation);
        roadManager = new GraphHopperRoadManager(apiKey,false);
        roadManager.setElevation(true);
        roadManager.addRequestOption("vehicle=hike");
        this.waypoints = waypoints;
    }

    @Override
    protected void onPostExecute(Road road) {
        super.onPostExecute(road);
        double prevElevation = 0;
        double elevationTotal = 0;
        boolean isFirst = true;
        for (GeoPoint p : road.getRouteLow()) {
            double elevation = p.getAltitude();
            if(!isFirst && elevation>prevElevation){
                elevationTotal += elevation-prevElevation;
            }
            isFirst = false;
            prevElevation = elevation;
            Log.i("MAP",p.getAltitude()+"m");
            Log.i("MAP","total " + elevationTotal+"m");
        }
        routingView.setVisibility(View.VISIBLE);
        textElevation.setText(String.format("%s m", new DecimalFormat("##.#").format(elevationTotal)));
        textDistance.setText(String.format("%s km", new DecimalFormat("##.##").format(road.mLength)));
        routingView.invalidate();
        roadOverlay = RoadManager.buildRoadOverlay(road, 0x800000FF, 15.0f);
        roadOverlay.setTitle(OVERLAY_TITLE);
        map.getOverlays().add(roadOverlay);
        map.invalidate();
        delegate.processFinish(road);
    }
}
