package fr.ovski.ovskimap.markers

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore


class MarkerManager(db: FirebaseFirestore) {
    private val TAG: String? = "MARKERMANAGER"
    private var db: FirebaseFirestore = db;

    fun addMarker() {
        val city = hashMapOf(
                "name" to "Los Angeles",
                "state" to "CA",
                "country" to "USA"
        )

        this.db.collection("cities").document("LA")
                .set(city)
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

    }

}