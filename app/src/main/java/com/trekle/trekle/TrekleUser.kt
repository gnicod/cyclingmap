package com.trekle.trekle

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.Serializable


object StravaInfo : Serializable{
    var token: String? = null
    var userName = ""
}

object TrekleUser {

    var strava: StravaInfo? = null
        set(value) {
            isStravaLinked = true
            field = value
        }

    var isStravaLinked: Boolean = false
    /*
    fun isStravaLinked() :Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        val usersRef= db.collection("users")
        val docIdRef: DocumentReference = usersRef.document(user.uid)
        docIdRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document!!.exists()) {
                    if (document["strava"] != null) {
                        Log.d(TAG, "your field exist")
                    } else {
                        Log.d(TAG, "your field does not exist")
                        //Create the filed
                    }
                }
            }
        }
    }
     */

    fun save() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val usersRef= db.collection("users")
        val userDetails: HashMap<String, Any> = HashMap()
        if (StravaInfo.token != null) {
            userDetails["strava"] = StravaInfo
        }
        usersRef.document(userId).set(userDetails, SetOptions.merge())
    }
}