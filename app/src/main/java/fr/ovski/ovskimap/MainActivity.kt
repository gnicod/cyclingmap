package fr.ovski.ovskimap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.Surface
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton
import fr.ovski.ovskimap.layers.LayerManager
import fr.ovski.ovskimap.markers.MarkerManager
import fr.ovski.ovskimap.models.Route
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.IOrientationConsumer
import org.osmdroid.views.overlay.compass.IOrientationProvider
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener, MapEventsReceiver, IOrientationConsumer, LocationListener {
    private val mLocationListener = this
    // Routing
    internal var waypoints: ArrayList<GeoPoint>? = null
    internal var routingMarkers: ArrayList<Marker>? = null
    internal var trueNorth: Float? = 0f
    internal var gpsspeed: Float = 0.toFloat()
    internal var gpsbearing: Float = 0.toFloat()
    internal var lat = 0f
    internal var lon = 0f
    internal var alt = 0f
    internal var timeOfFix: Long = 0
    internal var screen_orientation = ""
    private var map: MapView? = null
    private var user: FirebaseUser? = null
    private var routingView: ScrollView? = null
    private var poiMarkers: RadiusMarkerClusterer? = null
    private var mLocationManager: LocationManager? = null
    private lateinit var markerManager: MarkerManager
    private val layerManager = LayerManager()
    var tapState = TAP_DEFAULT_MODE
    private var deviceOrientation = 0

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN && ::markerManager.isInitialized) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                this.user = FirebaseAuth.getInstance().currentUser
                this.markerManager.setUser(user!!)
                Log.println(Log.DEBUG, LOG_TAG, "user logged")
            } else {
                Log.println(Log.DEBUG, LOG_TAG, "user not logged")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!"Android-x86".equals(Build.BRAND, ignoreCase = true)) {
            //lock the device in current screen orientation
            val orientation: Int
            val rotation = (this.getSystemService(
                    Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            when (rotation) {
                Surface.ROTATION_0 -> {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    this.deviceOrientation = 0
                    screen_orientation = "ROTATION_0 SCREEN_ORIENTATION_PORTRAIT"
                }
                Surface.ROTATION_90 -> {
                    this.deviceOrientation = 90
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    screen_orientation = "ROTATION_90 SCREEN_ORIENTATION_LANDSCAPE"
                }
                Surface.ROTATION_180 -> {
                    this.deviceOrientation = 180
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    screen_orientation = "ROTATION_180 SCREEN_ORIENTATION_REVERSE_PORTRAIT"
                }
                else -> {
                    this.deviceOrientation = 270
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    screen_orientation = "ROTATION_270 SCREEN_ORIENTATION_REVERSE_LANDSCAPE"
                }
            }

            this.requestedOrientation = orientation
        }
    }

    /**
     * Read data passed to intent and extract the route passed in parameter
     * If a route is found, display the route and elevation profile
     */
    fun displayRouteFromIntent() {
        val bundle = intent.extras ?: return
        val route = bundle.getSerializable("route") as Route
        val kmlDocument = KmlDocument()
        kmlDocument.parseKMLFile(KMLUtils.convertStringToFile(route.kml))
        val entries = KMLUtils.getEntriesFromKmlDocument(kmlDocument)
        val elevationFragment = ElevationProfileFragment.newInstance(entries, "test")
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.elevation_fragment, elevationFragment)
        fragmentTransaction.commit()
        map!!.controller.animateTo(kmlDocument.mKmlRoot.boundingBox.center)
        val overlay = kmlDocument.mKmlRoot.buildOverlay(map,null,null,kmlDocument)
        map!!.overlays.add(overlay)
        map!!.invalidate()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        } else {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO), 1)

        }
        super.onCreate(savedInstanceState)
        Log.i(LOG_TAG, applicationInfo.dataDir)
        Log.i(LOG_TAG, "here")

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        FirebaseApp.initializeApp(this.applicationContext)
        val auth = FirebaseAuth.getInstance()

        poiMarkers = RadiusMarkerClusterer(this)
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        // Floating Action Menu
        val icon = ImageView(this) // Create an icon
        icon.setImageDrawable(resources.getDrawable(R.drawable.ic_menu_manage))
        val actionButton = FloatingActionButton.Builder(this)
                .setContentView(icon)
                .build()
        //actionButton.setBackgroundTintList(ColorStateList.valueOf(Color.MAGENTA));
        val itemBuilder = SubActionButton.Builder(this)
        // repeat many times:
        val itemIconLocation = ImageView(this)
        itemIconLocation.setImageDrawable(resources.getDrawable(R.drawable.ic_menu_mylocation))
        val buttonLocation = itemBuilder.setContentView(itemIconLocation).build()
        // layer
        val itemIconLayer = ImageView(this)
        itemIconLayer.setImageDrawable(resources.getDrawable(R.drawable.ic_menu_mapmode))
        val buttonLayer = itemBuilder.setContentView(itemIconLayer).build()
        // show col
        val itemIconShowCol = ImageView(this)
        itemIconShowCol.setImageDrawable(resources.getDrawable(R.drawable.ic_menu_gallery))
        val buttonShowCol = itemBuilder.setContentView(itemIconShowCol).build()

        val actionMenu = FloatingActionMenu.Builder(this)
                .addSubActionView(buttonLocation)
                .addSubActionView(buttonLayer)
                .addSubActionView(buttonShowCol)
                .attachTo(actionButton)
                .build()

        buttonLayer.setOnClickListener {
            createSourceSelectBox()
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
        buttonShowCol.setOnClickListener { createLayersSelectBox() }
        buttonLocation.setOnClickListener {
            try {
                val lastKnownLocation = mLocationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                map!!.controller.setCenter(GeoPoint(lastKnownLocation.latitude, lastKnownLocation.longitude))
                map!!.invalidate()
                mLocationManager!!.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null)
            } catch (e: SecurityException) {

            }
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        val ctx = applicationContext
        //important! set your user agent to prevent getting banned from the osm servers

        initExtraTilesSources()
        val tileSourceBase = TileSourceFactory.getTileSource(
                preferences.getString("tileSource", "OpenTopoMap")
        )

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




        routingView = findViewById(R.id.routing_view)

        val mapEventsOverlay = MapEventsOverlay(this)

        map = findViewById(R.id.map)
        map!!.setTileSource(tileSourceBase)
        map!!.setMultiTouchControls(true)
        map!!.isTilesScaledToDpi = true
        val mapController = map!!.controller
        mapController.setZoom(12)
        displayRouteFromIntent()


        val gpsMyLocationProvider = GpsMyLocationProvider(this@MainActivity.baseContext)
        gpsMyLocationProvider.locationUpdateMinDistance = 0f // [m]  // Set the minimum distance for location updates
        gpsMyLocationProvider.locationUpdateMinTime = 10   // [ms] // Set the minimum time interval for location updates
        val mMyLocationOverlay = MyLocationNewOverlay(gpsMyLocationProvider, map!!)
        mMyLocationOverlay.isDrawAccuracyEnabled = true
        val mCompassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), map)

        mCompassOverlay.enableCompass()
        mMyLocationOverlay.enableMyLocation()
        map!!.overlays.add(0, mapEventsOverlay)
        map!!.overlays.add(mCompassOverlay)
        map!!.overlays.add(mMyLocationOverlay)

        // cluster
        map!!.overlays.add(poiMarkers)
        val clusterIconD = resources.getDrawable(R.drawable.marker_cluster)
        val clusterIcon = (clusterIconD as BitmapDrawable).bitmap
        poiMarkers!!.setIcon(clusterIcon)

        // TODO refactor
        markerManager = MarkerManager(this)
        if (auth.currentUser != null) {
            this.user = auth.currentUser
            markerManager.setUser(this.user!!)
            // TODO make function userSetted
            markerManager.getAllMarkers(map!!)
            map!!.invalidate()
            Log.println(Log.DEBUG, LOG_TAG, this.user!!.displayName)
        } else {
            loginUI()
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
                .addOnSuccessListener(this) { location ->
                    if (location != null) {
                        val startPoint = GeoPoint(location.latitude, location.longitude)
                        mapController.setCenter(startPoint)
                    }
                }

    }

    private fun initExtraTilesSources() {
        // retrieve BING_KEY variable stored in manifest
        BingMapTileSource.retrieveBingKey(this)
        val bingAerial = BingMapTileSource(null)
        bingAerial.style = BingMapTileSource.IMAGERYSET_AERIAL
        TileSourceFactory.addTileSource(bingAerial)
        val bingAerialLabel = BingMapTileSource(null)
        bingAerialLabel.style = BingMapTileSource.IMAGERYSET_AERIALWITHLABELS
        TileSourceFactory.addTileSource(bingAerialLabel)
        val bing = BingMapTileSource(null)
        bing.style = BingMapTileSource.IMAGERYSET_ROAD
        TileSourceFactory.addTileSource(bing)
    }

    fun createLayersSelectBox() {
        val listItems = layerManager.layers.keys.toTypedArray()

        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Choose items")


        val checkedItems = BooleanArray(layerManager.layers.size)
        var index = 0
        for (key in layerManager.layers.keys) {
            checkedItems[index] = layerManager.layers[key]!!.state
            index++
        }
        builder.setMultiChoiceItems(listItems, checkedItems) { dialogInterface, i, b ->
            val clicked = listItems[i]
            val checkedLayer = layerManager.layers[clicked]
            if (checkedLayer === null) {
                return@setMultiChoiceItems
            }
            if (b) {
                val tileProvider = MapTileProviderBasic(this)
                val tileSource = layerManager.getTileSource(clicked)
                tileProvider.tileSource = tileSource
                val tilesOverlay = TilesOverlay(tileProvider, this.baseContext)
                tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
                checkedLayer.overlay = tilesOverlay
                layerManager.layers[clicked] = checkedLayer.setSate(true)
                map!!.overlays.add(tilesOverlay)
            } else {
                map!!.overlays.remove(checkedLayer.overlay)
                layerManager.layers[clicked] = checkedLayer.setSate(false)
            }
        }.setPositiveButton("Done") { dialog, which ->

        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun createSourceSelectBox() {
        val sources = arrayOf<CharSequence>("Mapnik", "CycleMap", "OpenTopoMap", "HikeBikeMap", "BingMapsAerial", "BingMapsAerialWithLabels", "BingMapsRoad")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Map source ?")
        builder.setItems(sources) { dialog, which ->
            editor = preferences.edit()
            editor.putString("tileSource", sources[which].toString())
            editor.apply()
            val tileSourceBase = TileSourceFactory.getTileSource(
                    preferences.getString("tileSource", "OpenTopoMap")
            )
            map!!.setTileSource(tileSourceBase)
            map!!.invalidate()

        }
        builder.show()
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
        when (tapState) {
            TAP_DEFAULT_MODE -> Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show()
            TAP_ROUTING_MODE -> {
            }
        }//this.addRoutingMarker(p);
        return false
    }

    override fun longPressHelper(p: GeoPoint): Boolean {

        val sources = ArrayList(Arrays.asList("Insert POI", "Go to"))
        if (tapState == TAP_ROUTING_MODE) {
            sources.add("Add waypoint")
        } else {
            sources.add("Start from")
        }
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Que faire ?")
        val fragment = supportFragmentManager.findFragmentById(R.id.routing_fragment) as RoutingFragment
        builder.setItems(sources.toTypedArray<CharSequence>()) { dialog, which ->
            when (which) {
                0 -> markerManager.createMarker(
                        this@MainActivity,
                        com.google.firebase.firestore.GeoPoint(p.latitude, p.longitude)
                ) { name, group ->
                    val startMarker = Marker(map!!)
                    startMarker.position = p
                    startMarker.title = name
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    map!!.overlays.add(startMarker)
                    map!!.invalidate()
                    Unit
                }
                1 -> {
                    tapState = TAP_ROUTING_MODE
                    try {
                        val lastKnownLocation = mLocationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        val start = GeoPoint(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        fragment.addRoutingMarker(start)
                        fragment.addRoutingMarker(p)
                    } catch (e: SecurityException) {
                        Toast.makeText(applicationContext, "TODO go to ", Toast.LENGTH_SHORT).show()
                    }

                }
                2 -> {
                    tapState = TAP_ROUTING_MODE
                    fragment.addRoutingMarker(p)
                }
            }
        }
        builder.show()
        return false
    }

    @SuppressLint("RestrictedApi")
    private fun loginUI() {
        // Choose authentication providers
        val context = this.applicationContext
        AuthUI.setApplicationContext(context)
        FirebaseApp.initializeApp(context)
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setTheme(R.style.AppTheme)
                        .setAvailableProviders(
                                arrayListOf(
                                        AuthUI.IdpConfig.EmailBuilder().build(),
                                        AuthUI.IdpConfig.PhoneBuilder().build(),
                                        AuthUI.IdpConfig.GoogleBuilder().build()
                                )
                        )
                        .build(),
                RC_SIGN_IN
        )
    }

    override fun onLocationChanged(location: Location) {
        if (map == null)
            return

        gpsbearing = location.bearing
        gpsspeed = location.speed
        lat = location.latitude.toFloat()
        lon = location.longitude.toFloat()
        alt = location.altitude.toFloat() //meters
        timeOfFix = location.time


        //use gps bearing instead of the compass

        var t = 360f - gpsbearing - this.deviceOrientation.toFloat()
        if (t < 0) {
            t += 360f
        }
        if (t > 360) {
            t -= 360f
        }
        //help smooth everything out
        t = t.toInt().toFloat()
        t = t / 5
        t = t.toInt().toFloat()
        t = t * 5

        if (gpsspeed >= 0.01) {
            map!!.mapOrientation = t
            //otherwise let the compass take over
        }

    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {

    }

    override fun onProviderEnabled(s: String) {

    }

    override fun onProviderDisabled(s: String) {

    }


    override fun onOrientationChanged(orientation: Float, source: IOrientationProvider) {
        /*
        Log.i(LOG_TAG, "orientation changed")
        //note, on devices without a compass this never fires...

        //only use the compass bit if we aren't moving, since gps is more accurate when we are moving
        if (gpsspeed < 0.01) {
            var gf: GeomagneticField? = GeomagneticField(lat, lon, alt, timeOfFix)
            trueNorth = orientation + gf!!.declination
            gf = null
            synchronized(trueNorth) {
                if (trueNorth > 360.0f) {
                    trueNorth = trueNorth!! - 360.0f
                }
                var actualHeading = 0f

                //this part adjusts the desired map rotation based on device orientation and compass heading
                var t = 360f - trueNorth!! - this.deviceOrientation.toFloat()
                if (t < 0) {
                    t += 360f
                }
                if (t > 360) {
                    t -= 360f
                }
                actualHeading = t
                //help smooth everything out
                t = t.toInt().toFloat()
                t = t / 5
                t = t.toInt().toFloat()
                t = t * 5
                map!!.mapOrientation = t
            }
        }
         */
    }

    companion object {

        private val RC_SIGN_IN = 6000
        val TAP_DEFAULT_MODE = 0
        val TAP_ROUTING_MODE = 1
        private val LOG_TAG = "LOG_TAG"
    }
}
