package com.trekle.trekle

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.Serializable



object StravaInfo : Serializable{
    var refreshToken: String? = null
    var token: String? = null
    var userName : String?=null
}

object TrekleUser {

const val PREF_STRAVA_LINKED = "STRAVA_LINKED"

    private var sharedPreferences = TrekleApplication.appContext!!.getSharedPreferences("shared_preference", Context.MODE_PRIVATE)

    var strava: StravaInfo? = null
        set(value) {
            isStravaLinked = true
            field = value
        }


    var isStravaLinked: Boolean
        get() {
            return sharedPreferences.getBoolean(PREF_STRAVA_LINKED, false)
        }
        set(value) {
            with (sharedPreferences.edit()) {
                putBoolean(PREF_STRAVA_LINKED, value)
                commit()
            }

        }
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

     suspend fun getUser(): StravaInfo? {
         val user = FirebaseAuth.getInstance().currentUser ?: return null
         val userId = user.uid
        val snapshot = try {
            FirebaseFirestore.getInstance().document("users/$userId").get().await() // Cancellable await
        } catch (e: FirebaseFirestoreException) {
            Log.e("TREKLE", e.toString())
            // Handle exception
            return null
        }
         StravaInfo.token = snapshot.getString("strava.token")
         StravaInfo.userName = snapshot.getString("strava.userName")
         Log.i("TREKLE", "token =" + StravaInfo.token)

         strava = StravaInfo
         return StravaInfo
     }

    /*
    suspend fun getStravaInfo(id: String) : StravaInfo? {
        val snapshot = try {
            FirebaseFirestore.getInstance().document("user/$id").get().await()
        } catch(e: Exception) {
            null
        }
        return snapshot?.toObject(User::class.java)
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
