package com.trekle.trekle.markers

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Spinner
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.trekle.trekle.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class MarkerManager(context: Context) {
    private val TAG: String? = "MARKERMANAGER"
    private var db: FirebaseFirestore
    private var user: FirebaseUser? = null
    private var groups: ArrayList<String> = arrayListOf();

    init {
        FirebaseApp.initializeApp(context)
         db = FirebaseFirestore.getInstance()
    }

    fun setUser(user: FirebaseUser) {
        this.user = user
    }

    fun saveMarker(point: GeoPoint, title: String, group: String, user: FirebaseUser) {
        val marker = hashMapOf(
                "lng" to point.longitude,
                "lat" to point.latitude,
                "title" to title,
                "group" to group
        )
        Log.i(TAG, "add Marker")
        Log.i(TAG, user.displayName)
        this.db.collection("users").document(user.uid).collection("markers")
                .add(marker)
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

    }

    fun createMarker(context: Context, point: GeoPoint, results: ((name: String, group: String) -> Unit)) {
        if (this.user == null) {
            Log.e(TAG, "user is null")
            // TODO dunno what to do if user is not logged
            return
        }
        showAddItemDialog(context) { name, group, icon -> run {
            // TODO save icon
            saveMarker(GeoPoint(point.latitude, point.longitude), name, group, this.user!!)
            results(name, group)
        }}
    }

    private fun showAddItemDialog(c: Context, results: ((name: String, group: String, icon: String) -> Unit)) {
        val dialog = AlertDialog.Builder(c)
                .setTitle("Add a new marker")
                .setView(R.layout.marker_popup)
                .setPositiveButton("Add") { dialog, _ ->
                    run {
                        val alertDialog = dialog as AlertDialog
                        val nameWidget = alertDialog.findViewById<EditText>(R.id.popup_marker_name)
                        val groupWidget = alertDialog.findViewById<AutoCompleteTextView>(R.id.popup_marker_group)
                        val spinnerWidget = alertDialog.findViewById<Spinner>(R.id.icons_spinner)
                        val array = arrayOfNulls<String>(groups.size)
                        arrayOf(groups.size)
                        val adapter = ArrayAdapter<String>(c, android.R.layout.select_dialog_item, groups.toArray(array))
                        groupWidget.setAdapter(adapter);
                        Log.i(TAG, groups.toString());
                        Log.i(TAG, "text: " + nameWidget.text.toString())
                        Log.i(TAG, "text: " + groupWidget.text.toString())
                        Log.i(TAG, "positive button clicked")
                        results(nameWidget.text.toString(), groupWidget.text.toString(), spinnerWidget.selectedItem.toString())
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Cancel", null).create()
        dialog.show()
        val spinnerWidget = dialog.findViewById<Spinner>(R.id.icons_spinner)
        val spinnerAdapter = ArrayAdapter.createFromResource(
                c,
                R.array.markers_array,
                android.R.layout.simple_spinner_item)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        spinnerWidget.adapter = spinnerAdapter
    }

    fun getAllMarkers(map: MapView) {
        this.db.collection("users").document(this.user!!.uid).collection("markers")
                .get()
                .addOnSuccessListener {  result ->
                    for (document in result) {
                        val title = document.getString("title")
                        val group = document.getString("group")
                        val lat = document.getDouble("lat")
                        val lng = document.getDouble("lng")
                        val geopoint = org.osmdroid.util.GeoPoint(lat!!, lng!!)
                        if (group != null) {
                            Log.i(TAG, group)
                            groups.add(group)
                        }
                        addMarkerToMap(map, title!!, geopoint)
                    }}
                .addOnFailureListener { e -> Log.w(TAG, "Error reading document", e) }
    }

    fun addMarkerToMap(map :MapView, title: String, geoPoint: org.osmdroid.util.GeoPoint) {
        val marker = Marker(map)
        marker.position = geoPoint
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.setIcon(map.context.resources.getDrawable(R.drawable.ic_mountain))
        Log.i(TAG, "add marker to map")
        Log.i(TAG, marker.position.toString())
        map.overlays.add(marker)
        map.invalidate()
    }

}