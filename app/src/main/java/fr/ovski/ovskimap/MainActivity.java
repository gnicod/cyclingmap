package fr.ovski.ovskimap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import fr.ovski.ovskimap.layers.LayerManager;
import fr.ovski.ovskimap.layers.LayerOverlay;
import fr.ovski.ovskimap.markers.MarkerManager;
import kotlin.Unit;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, MapEventsReceiver, IOrientationConsumer ,LocationListener{

    private static final int RC_SIGN_IN = 6000;
    public static final int TAP_DEFAULT_MODE = 0;
    public static final int TAP_ROUTING_MODE = 1;
    private static final String LOG_TAG = "LOG_TAG";
    private final LocationListener mLocationListener = this;
    // Routing
    ArrayList<GeoPoint> waypoints;
    ArrayList<Marker> routingMarkers;
    Float trueNorth = 0f;
    float gpsspeed;
    float gpsbearing;
    float lat = 0;
    float lon = 0;
    float alt = 0;
    long timeOfFix = 0;
    String screen_orientation = "";
    private MapView map;
    private FirebaseUser user;
    private ScrollView routingView;
    private RadiusMarkerClusterer poiMarkers;
    private LocationManager mLocationManager;
    private MarkerManager markerManager = new MarkerManager();
    private LayerManager layerManager = new LayerManager();
    public int tapState = TAP_DEFAULT_MODE;
    private int deviceOrientation = 0;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                this.user = FirebaseAuth.getInstance().getCurrentUser();
                this.markerManager.setUser(user);
                Log.println(Log.DEBUG, LOG_TAG, "user logged");
            } else {
                Log.println(Log.DEBUG, LOG_TAG, "user not logged");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!"Android-x86".equalsIgnoreCase(Build.BRAND)) {
            //lock the device in current screen orientation
            int orientation;
            int rotation = ((WindowManager) this.getSystemService(
                    Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    this.deviceOrientation = 0;
                    screen_orientation = "ROTATION_0 SCREEN_ORIENTATION_PORTRAIT";
                    break;
                case Surface.ROTATION_90:
                    this.deviceOrientation = 90;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    screen_orientation = "ROTATION_90 SCREEN_ORIENTATION_LANDSCAPE";
                    break;
                case Surface.ROTATION_180:
                    this.deviceOrientation = 180;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    screen_orientation = "ROTATION_180 SCREEN_ORIENTATION_REVERSE_PORTRAIT";
                    break;
                default:
                    this.deviceOrientation = 270;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    screen_orientation = "ROTATION_270 SCREEN_ORIENTATION_REVERSE_LANDSCAPE";
                    break;
            }

            this.setRequestedOrientation(orientation);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission. RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
        } else {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);

        }
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG,getApplicationInfo().dataDir);
        Log.i(LOG_TAG,"here");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        poiMarkers = new RadiusMarkerClusterer(this);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        // Floating Action Menu
        ImageView icon = new ImageView(this); // Create an icon
        icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_manage));
        FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(icon)
                .build();
        //actionButton.setBackgroundTintList(ColorStateList.valueOf(Color.MAGENTA));
        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        // repeat many times:
        ImageView itemIconLocation = new ImageView(this);
        itemIconLocation.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_mylocation));
        SubActionButton buttonLocation = itemBuilder.setContentView(itemIconLocation).build();
        // layer
        ImageView itemIconLayer = new ImageView(this);
        itemIconLayer.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_mapmode));
        SubActionButton buttonLayer = itemBuilder.setContentView(itemIconLayer).build();
        // show col
        ImageView itemIconShowCol = new ImageView(this);
        itemIconShowCol.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_gallery));
        SubActionButton buttonShowCol = itemBuilder.setContentView(itemIconShowCol).build();

        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(buttonLocation)
                .addSubActionView(buttonLayer)
                .addSubActionView(buttonShowCol)
                .attachTo(actionButton)
                .build();

        buttonLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createSourceSelectBox();
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
        buttonShowCol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createLayersSelectBox();
            }
        });
        buttonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    map.getController().setCenter(new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                    map.invalidate();
                    mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
                } catch (SecurityException e) {

                }
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers

        initExtraTilesSources();
        ITileSource tileSourceBase = TileSourceFactory.getTileSource(
                preferences.getString("tileSource", "OpenTopoMap")
        );

        /*
        IGN GEOPORTAIL LAYER

        String key = "lv4ajb751s4scbbr4f5p2mo0";
        return "http://wxs.ign.fr/" + key
                + "/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&"
                + "LAYER=" + layer + "&STYLE=normal&TILEMATRIXSET=PM&"
                + "TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=image%2Fjpeg";
        final OnlineTileSourceBase IGN_GEOPORTAIL = new XYTileSource("Geoportail",
                0, 18, 256, ".png",
                new String[] { "http://overlay.openstreetmap.nl/basemap/" });
        TileSourceFactory.addTileSource();
        */




        routingView = findViewById(R.id.routing_view);

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);

        map = findViewById(R.id.map);
        map.setTileSource(tileSourceBase);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        IMapController mapController = map.getController();
        mapController.setZoom(12);


        GpsMyLocationProvider gpsMyLocationProvider = new GpsMyLocationProvider(MainActivity.this.getBaseContext());
        gpsMyLocationProvider.setLocationUpdateMinDistance(0); // [m]  // Set the minimum distance for location updates
        gpsMyLocationProvider.setLocationUpdateMinTime(10);   // [ms] // Set the minimum time interval for location updates
        MyLocationNewOverlay mMyLocationOverlay = new MyLocationNewOverlay(gpsMyLocationProvider, map);
        mMyLocationOverlay.setDrawAccuracyEnabled(true);
        CompassOverlay mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);

        mCompassOverlay.enableCompass();
        mMyLocationOverlay.enableMyLocation();
        map.getOverlays().add(0, mapEventsOverlay);
        map.getOverlays().add(mCompassOverlay);
        map.getOverlays().add(mMyLocationOverlay);

        // cluster
        map.getOverlays().add(poiMarkers);
        Drawable clusterIconD = getResources().getDrawable(R.drawable.marker_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        poiMarkers.setIcon(clusterIcon);

        if (auth.getCurrentUser() != null) {
            this.user = auth.getCurrentUser();
            markerManager.setUser(this.user);
            // TODO make function userSetted
            markerManager.getAllMarkers(map);
            map.invalidate();
            Log.println(Log.DEBUG, LOG_TAG, this.user.getDisplayName());
        } else {
            loginUI();
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapController.setCenter(startPoint);
                        }
                    }
                });

    }

    private void initExtraTilesSources() {
        // retrieve BING_KEY variable stored in manifest
        BingMapTileSource.retrieveBingKey(this);
        BingMapTileSource bingAerial = new BingMapTileSource(null);
        bingAerial.setStyle(BingMapTileSource.IMAGERYSET_AERIAL);
        TileSourceFactory.addTileSource(bingAerial);
        BingMapTileSource bingAerialLabel = new BingMapTileSource(null);
        bingAerialLabel.setStyle(BingMapTileSource.IMAGERYSET_AERIALWITHLABELS);
        TileSourceFactory.addTileSource(bingAerialLabel);
        BingMapTileSource bing = new BingMapTileSource(null);
        bing.setStyle(BingMapTileSource.IMAGERYSET_ROAD);
        TileSourceFactory.addTileSource(bing);
    }

    public void createLayersSelectBox() {
        String[] listItems = layerManager.getLayers().keySet().toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose items");


        boolean[] checkedItems = new boolean[layerManager.getLayers().size()];
        int index = 0;
        for (String key : layerManager.getLayers().keySet()) {
            checkedItems[index] = layerManager.getLayers().get(key).getState();
            index ++;
        }
        builder.setMultiChoiceItems(listItems, checkedItems, (dialogInterface, i, b) -> {
            String clicked = listItems[i];
            LayerOverlay checkedLayer = layerManager.getLayers().get(clicked);
            if (b) {
                final MapTileProviderBasic tileProvider = new MapTileProviderBasic(this);
                final ITileSource tileSource = layerManager.getTileSource(clicked);
                tileProvider.setTileSource(tileSource);
                final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());
                tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
                checkedLayer.setOverlay(tilesOverlay);
                layerManager.getLayers().put(clicked, checkedLayer.setSate(true));
                map.getOverlays().add(tilesOverlay);
            } else {
                map.getOverlays().remove(checkedLayer.getOverlay());
                layerManager.getLayers().put(clicked, checkedLayer.setSate(false));
            }
        }).setPositiveButton("Done", (dialog, which) -> {

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createSourceSelectBox() {
        final CharSequence[] sources = new CharSequence[]{"Mapnik", "CycleMap", "OpenTopoMap", "HikeBikeMap", "BingMapsAerial", "BingMapsAerialWithLabels", "BingMapsRoad"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Map source ?");
        builder.setItems(sources, (dialog, which) -> {
            editor = preferences.edit();
            editor.putString("tileSource", sources[which].toString());
            editor.apply();
            ITileSource tileSourceBase = TileSourceFactory.getTileSource(
                preferences.getString("tileSource", "OpenTopoMap")
            );
            map.setTileSource(tileSourceBase);
            map.invalidate();

        });
        builder.show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        switch (tapState) {
            case TAP_DEFAULT_MODE:
                Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show();
                break;
            case TAP_ROUTING_MODE:
                //this.addRoutingMarker(p);
                break;
        }
        return false;
    }

    @Override
    public boolean longPressHelper(final GeoPoint p) {

        ArrayList<String> sources = new ArrayList<>(Arrays.asList("Insert POI", "Go to"));
        if (tapState == TAP_ROUTING_MODE) {
            sources.add("Add waypoint");
        } else {
            sources.add("Start from");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Que faire ?");
        RoutingFragment fragment = (RoutingFragment) getFragmentManager().findFragmentById(R.id.routing_fragment);
        builder.setItems(sources.toArray(new CharSequence[sources.size()]), (dialog, which) -> {
            switch (which) {
                case 0:
                    markerManager.createMarker(
                            MainActivity.this,
                            new com.google.firebase.firestore.GeoPoint(p.getLatitude(), p.getLongitude()),
                            (name, group)-> {
                                Marker startMarker = new Marker(map);
                                startMarker.setPosition(p);
                                startMarker.setTitle(name);
                                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                map.getOverlays().add(startMarker);
                                map.invalidate();
                                return Unit.INSTANCE;
                            });
                    break;
                case 1:
                    tapState = TAP_ROUTING_MODE;
                    try {
                        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        GeoPoint start = (new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                        fragment.addRoutingMarker(start);
                        fragment.addRoutingMarker(p);
                    } catch (SecurityException e) {
                        Toast.makeText(getApplicationContext(), "TODO go to ", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    tapState = TAP_ROUTING_MODE;
                    fragment.addRoutingMarker(p);
                    break;
            }
        });
        builder.show();
        return false;
    }

    private void loginUI() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(), RC_SIGN_IN);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (map == null)
            return;

        gpsbearing = location.getBearing();
        gpsspeed = location.getSpeed();
        lat = (float) location.getLatitude();
        lon = (float) location.getLongitude();
        alt = (float) location.getAltitude(); //meters
        timeOfFix = location.getTime();


        //use gps bearing instead of the compass

        float t = (360 - gpsbearing - this.deviceOrientation);
        if (t < 0) {
            t += 360;
        }
        if (t > 360) {
            t -= 360;
        }
        //help smooth everything out
        t = (int) t;
        t = t / 5;
        t = (int) t;
        t = t * 5;

        if (gpsspeed >= 0.01) {
            map.setMapOrientation(t);
            //otherwise let the compass take over
        }

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    @Override
    public void onOrientationChanged(float orientation, IOrientationProvider source) {
        Log.i(LOG_TAG, "orientation changed");
        //note, on devices without a compass this never fires...

        //only use the compass bit if we aren't moving, since gps is more accurate when we are moving
        if (gpsspeed < 0.01) {
            GeomagneticField gf = new GeomagneticField(lat, lon, alt, timeOfFix);
            trueNorth = orientation + gf.getDeclination();
            gf = null;
            synchronized (trueNorth) {
                if (trueNorth > 360.0f) {
                    trueNorth = trueNorth - 360.0f;
                }
                float actualHeading = 0f;

                //this part adjusts the desired map rotation based on device orientation and compass heading
                float t = (360 - trueNorth - this.deviceOrientation);
                if (t < 0) {
                    t += 360;
                }
                if (t > 360) {
                    t -= 360;
                }
                actualHeading = t;
                //help smooth everything out
                t = (int) t;
                t = t / 5;
                t = (int) t;
                t = t * 5;
                map.setMapOrientation(t);
            }
        }
    }
}
