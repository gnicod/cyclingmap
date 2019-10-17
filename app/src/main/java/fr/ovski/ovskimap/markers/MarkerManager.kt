package fr.ovski.ovskimap.markers

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.EditText
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import fr.ovski.ovskimap.R


class MarkerManager() {
    private val TAG: String? = "MARKERMANAGER"
    private val db = FirebaseFirestore.getInstance()
    private var user: FirebaseUser? = null

    fun setUser(user: FirebaseUser) {
        this.user = user
    }

    fun saveMarker(point: GeoPoint, title: String, group: String, user: FirebaseUser) {
        val marker = hashMapOf(
                "lng" to point.latitude,
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
        showAddItemDialog(context) { name, group -> run {
            saveMarker(GeoPoint(point.longitude, point.latitude), name, group, this.user!!)
            results(name, group)
        }}
    }

    private fun showAddItemDialog(c: Context, results: ((name: String, group: String) -> Unit)) {
        val dialog = AlertDialog.Builder(c)
                .setTitle("Add a new marker")
                .setMessage("What do you want to do next?")
                .setView(R.layout.marker_popup)
                .setPositiveButton("Add") { dialog, _ ->
                    run {
                        val alertDialog = dialog as AlertDialog;
                        val nameWidget = alertDialog.findViewById<EditText>(R.id.popup_marker_name)
                        val groupWidget = alertDialog.findViewById<AutoCompleteTextView>(R.id.popup_marker_group)
                        Log.i(TAG, "text: " + nameWidget.text.toString())
                        Log.i(TAG, "text: " + groupWidget.text.toString())
                        Log.i(TAG, "positive button clicked")
                        results(nameWidget.text.toString(), groupWidget.text.toString())
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
        dialog.show()
    }

}