package fr.ovski.ovskimap.markers

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint


class MarkerManager(user: FirebaseUser) {
    private val TAG: String? = "MARKERMANAGER"
    private val user:FirebaseUser = user
    private val db = FirebaseFirestore.getInstance()

    fun addMarker(point: GeoPoint, title: String, group: String) {
        val marker = hashMapOf(
                "lng" to point.latitude,
                "lat" to point.latitude,
                "title" to title,
                "group" to group
        )

        this.db.collection("markers").document(user.uid)
                .set(marker)
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

    }

}