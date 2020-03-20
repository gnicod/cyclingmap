package com.trekle.trekle

import android.R.id.message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.Serializable

object StravaInfo : Serializable{
    var token: String = ""
    var userName = ""
}

object TrekleUser {

    var strava = StravaInfo

    fun save() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val usersRef= db.collection("users")
        val userDetails: HashMap<String, Any> = HashMap()
        userDetails["strava"] = StravaInfo
        usersRef.document(userId).set(userDetails, SetOptions.merge())
    }
}