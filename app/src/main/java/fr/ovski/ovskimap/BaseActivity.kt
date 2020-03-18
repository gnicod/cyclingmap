package fr.ovski.ovskimap

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import fr.ovski.ovskimap.models.Route
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTile
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.cachemanager.CacheManager.CacheManagerCallback as CacheManagerCallback1
import kotlin.Int as Int

open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected lateinit var preferences: SharedPreferences
    protected lateinit var editor: SharedPreferences.Editor
    var currentRoute: Route? = null

    companion object {
        val MODE_NORMAL = 0
        val MODE_ROUTING = 1
        val MODE_DISPLAY_ROUTE = 2
    }

    var mapMode = MODE_NORMAL
        set(value) {
            invalidateOptionsMenu()
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Floating Action Menu
        val icon = ImageView(this) // Create an icon
        icon.setImageDrawable(resources.getDrawable(R.drawable.ic_menu_mapmode))
        // repeat many times:
        val itemIcon = ImageView(this)
        itemIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_menu_mylocation))

        val ctx = applicationContext
        preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        Configuration.getInstance().load(ctx, preferences)
        editor = preferences.edit()
        setContentView(R.layout.activity_base)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        if (mapMode != MODE_DISPLAY_ROUTE) {
            val item = menu.findItem(R.id.action_cache_route)
            item.isVisible = false
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        val rootView: View = window.decorView.rootView.findViewById(R.id.main_app_view)

        if (id == R.id.nav_gallery) {
            val intent = Intent(this, RoutesListActivity::class.java)
            startActivity(intent)
        } else if (id == R.id.nav_share) {
            Snackbar.make(rootView, "Will be implemented soon, or not...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        } else if (id == R.id.nav_send) {
            Snackbar.make(rootView, "Will be implemented soon, or not...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val rootView: View = window.decorView.rootView.findViewById(R.id.main_app_view)


        if (id == R.id.action_toggle_elevation) {
            val elevationFragment = rootView.findViewById<View>(R.id.elevation_fragment_layout)
            if (elevationFragment.visibility == View.GONE) {
                elevationFragment.visibility = View.VISIBLE
            } else {
                elevationFragment.visibility = View.GONE
            }

        }
        if (id == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        if (id == R.id.action_cache_route) {
            if (currentRoute == null) true
            val map = findViewById<MapView>(R.id.map)
            val cm = CacheManager(map)
            val geopoints = currentRoute!!.kmlDocument?.let { KMLUtils.getGeopoints(it) }
            cm.downloadAreaAsync(this, geopoints, 15, 17)
            Log.i("OVSKI CACHE", "cache capacity " + (cm.cacheCapacity()/(1024*1024)).toString())
            Log.i("OVSKI CACHE", "cache usage " + (cm.currentCacheUsage()/ (1024 * 1024)).toString())
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    fun askLocationPermissions() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }


}
