package fr.ovski.ovskimap.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.text.DecimalFormat;
import java.util.ArrayList;

import fr.ovski.ovskimap.R;

public class GraphHopperTask extends AsyncTask<Object, Object, Road> {

    public final static String OVERLAY_TITLE = "routing_overlay";
    private ArrayList<GeoPoint> waypoints;
    private GraphHopperRoadManager roadManager;
    private MapView map;
    private LinearLayout routingView;
    private TextView textDistance;
    private TextView textElevation;
    private Polyline roadOverlay;

    @Override
    protected Road doInBackground(Object... params) {
        Road road = roadManager.getRoad(waypoints);
        return road;
    }

    public GraphHopperTask(MapView map, LinearLayout routingView, String apiKey, ArrayList<GeoPoint> waypoints) {
        this.map = map;
        this.routingView = routingView;
        textDistance = (TextView) routingView.findViewById(R.id.routing_distance);
        textElevation = (TextView) routingView.findViewById(R.id.routing_elevation);
        roadManager = new GraphHopperRoadManager(apiKey,false);
        roadManager.setElevation(true);
        roadManager.addRequestOption("vehicle=hike");
        this.waypoints = waypoints;
    }

    @Override
    protected void onPostExecute(Road road) {
        //road.mDuration
        super.onPostExecute(road);
        double prevElevation = 0;
        double elevationTotal = 0;
        boolean isFirst = true;
        for (GeoPoint p : road.mRouteHigh) {
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
        textElevation.setText(new DecimalFormat("##.#").format(elevationTotal) + " m");
        textDistance.setText(new DecimalFormat("##.##").format(road.mLength) + " km");
        routingView.invalidate();

        /*
        FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(this);
        os.close();
        fos.close();
        */

        roadOverlay = RoadManager.buildRoadOverlay(road);
        roadOverlay.setTitle(this.OVERLAY_TITLE);
        map.getOverlays().add(roadOverlay);
        map.invalidate();
    }
}
