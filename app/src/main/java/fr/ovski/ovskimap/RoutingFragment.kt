package fr.ovski.ovskimap

import android.app.Fragment
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.woxthebox.draglistview.DragListView
import fr.ovski.ovskimap.adapter.ItemRoutingAdapter
import fr.ovski.ovskimap.markers.NumMarker
import fr.ovski.ovskimap.tasks.GraphHopperTask
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.IOException
import java.io.StringWriter
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.collections.ArrayList


class RoutingFragment : Fragment() {

    private lateinit var listAdapter: ItemRoutingAdapter
    private var mItemArray: ArrayList<Pair<Long, String>> = arrayListOf<Pair<Long, String>>()
    private lateinit var map: MapView
    private lateinit var routingTask: AsyncTask<Any, Any, Road>
    private var waypoints: ArrayList<NumMarker> = arrayListOf<NumMarker>()
    private var routingMarkers: ArrayList<Marker> = arrayListOf<Marker>()
    private var LOG_TAG = "ROUTING_TAG"
    private val db = FirebaseFirestore.getInstance()
    private var user: FirebaseUser? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.routing_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        map = activity!!.findViewById<MapView>(R.id.map)
        this.user = FirebaseAuth.getInstance().getCurrentUser();
        // TODO: Use the ViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getView()?.findViewById<View>(R.id.btn_save_route)?.setOnClickListener { createSaveRouteBox() }
        getView()?.findViewById<View>(R.id.btn_cancel_route)?.setOnClickListener { cancelRouting() }

        mItemArray = ArrayList()
        val mDragListView = getView()?.findViewById<DragListView>(R.id.drag_list_view)

        mDragListView!!.setLayoutManager(LinearLayoutManager(context))
        mDragListView.setDragListListener(object: DragListView.DragListListener {
            override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {

            }

            override fun onItemDragStarted(position: Int) {
            }

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                Collections.swap(waypoints, fromPosition, toPosition)
                waypoints.forEachIndexed {index, marker -> marker.setNumber(index)}
                // TODO rerender marker layout
                launchRouting()
            }
        })

        listAdapter = ItemRoutingAdapter(mItemArray, R.layout.routing_list_item, R.id.image, false)
        mDragListView.setAdapter(listAdapter, true)
        mDragListView.setCanDragHorizontally(false)

    }

    private fun saveRoute(title: String) {
        val kmlDocument = KmlDocument()
        val writer = StringWriter()
        val routing = routingTask.get()
        for (o in map.overlays) {
            if (o is Polyline) {
                if (o.title === GraphHopperTask.OVERLAY_TITLE) {
                    kmlDocument.mKmlRoot.addOverlay(o, kmlDocument)
                }
            }
        }
        kmlDocument.saveAsGeoJSON(writer)
        val geojson = writer.toString()
        val roadGeoJson = hashMapOf(
                "title" to title,
                "geojson" to geojson
        )
        user?.uid?.let {
            this.db.collection("users").document(it).collection("routes")
                    .add(roadGeoJson)
                    .addOnSuccessListener { Log.d("TAG", "DocumentSnapshot successfully written!") }
                    .addOnFailureListener { e -> Log.w("TAG", "Error writing document", e) }
        }
    }

    private fun createSaveRouteBox() {
        val builder = AlertDialog.Builder(this.context!!)
        builder.setTitle(R.string.title_box_save_route)
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton(R.string.save_route) { dialog, which ->
            try {
                saveRoute(input.text.toString())
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun launchRouting() {
        val apiKey = getString(R.string.graphopper_key)
        for (o in map.overlays) {
            if (o is Polyline) {
                if (o.title === GraphHopperTask.OVERLAY_TITLE) {
                    o.isVisible = false
                    o.setEnabled(false)
                    map.invalidate()
                }
            }
        }
        if (waypoints.size >1) {
            val waypointsGeo = ArrayList(waypoints.map { marker -> marker.position}.toList())
            routingTask = GraphHopperTask(map, view, apiKey, waypointsGeo).execute()
        }
    }

    private fun cancelRouting() {
        this.view!!.visibility = View.GONE
        map.overlays.removeAll(routingMarkers)
        mItemArray.clear()
        map.invalidate()
        val act = activity as MainActivity
        act.tapState = MainActivity.TAP_DEFAULT_MODE
        view.visibility = View.GONE
    }

    fun addRoutingMarker(point: GeoPoint) {
        val startMarker = NumMarker(map, waypoints.size)
        startMarker.position = point
        waypoints.add(startMarker)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        routingMarkers.add(startMarker)
        mItemArray.add(Pair(routingMarkers.size.toLong(), "${point.latitude}, ${point.longitude}"))
        listAdapter.notifyDataSetChanged()
        map.overlays.add(startMarker)
        map.invalidate()
        view.visibility = View.VISIBLE
        launchRouting()
    }


}
