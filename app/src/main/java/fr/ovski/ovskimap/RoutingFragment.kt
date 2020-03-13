package fr.ovski.ovskimap

import android.app.Fragment
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.recyclerview.widget.LinearLayoutManager
import com.woxthebox.draglistview.DragListView
import fr.ovski.ovskimap.tasks.GraphHopperTask
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.concurrent.ExecutionException


class RoutingFragment : Fragment() {

    private lateinit var listAdapter: ItemRoutingAdapter
    private var mItemArray: ArrayList<Pair<Long, String>> = arrayListOf<Pair<Long, String>>()
    private lateinit var map: MapView
    private lateinit var routingTask: AsyncTask<Any, Any, Road>
    private var waypoints: ArrayList<GeoPoint> = arrayListOf<GeoPoint>()
    private var routingMarkers: ArrayList<Marker> = arrayListOf<Marker>()
    private var LOG_TAG = "ROUTING_TAG"

    companion object {
        fun newInstance() = RoutingFragment()
    }

    private lateinit var viewModel: RoutingViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.routing_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        map = activity!!.findViewById<MapView>(R.id.map)
        // TODO: Use the ViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getView()?.findViewById<View>(R.id.btn_save_route)?.setOnClickListener(View.OnClickListener { createSaveRouteBox() })
        getView()?.findViewById<View>(R.id.btn_cancel_route)?.setOnClickListener(View.OnClickListener { cancelRouting() })

        mItemArray = java.util.ArrayList<Pair<Long, String>>()
        val mDragListView = getView()?.findViewById<DragListView>(R.id.drag_list_view)

        mDragListView!!.setLayoutManager(LinearLayoutManager(context))
        listAdapter = ItemRoutingAdapter(mItemArray, R.layout.routing_list_item, R.id.image, false)
        mDragListView.setAdapter(listAdapter, true)
        mDragListView.setCanDragHorizontally(false)

    }

    private fun createSaveRouteBox() {
        Toast.makeText(this.context, "save route", Toast.LENGTH_SHORT).show()
        val builder = AlertDialog.Builder(this!!.context!!)
        builder.setTitle(R.string.title_box_save_route)
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton(R.string.save_route) { dialog, which ->
            val road: Road
            try {
                road = routingTask!!.get()
                var fos: FileOutputStream? = null
                fos = context?.openFileOutput("testroute", Context.MODE_PRIVATE)
                val os = ObjectOutputStream(fos)
                os.writeObject(SerializableRoad(road.mRouteHigh))
                os.close()
                fos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun cancelRouting() {
        Toast.makeText(this.context, "cancel routing", Toast.LENGTH_SHORT).show()
        this.view!!.visibility = View.GONE
        map.overlays.removeAll(routingMarkers)
        map.invalidate()
        for (o in map.overlays) {
            if (o is Polyline) {
                if ((o as Polyline).title === GraphHopperTask.OVERLAY_TITLE) {
                    Log.i(LOG_TAG, "remove")
                    (o as Polyline).isVisible = false
                    o.setEnabled(false)
                    map.invalidate()
                }
            }
        }
        val act = activity as MainActivity
        act.tapState = MainActivity.TAP_DEFAULT_MODE
    }

    public fun addRoutingMarker(point: GeoPoint) {
        Toast.makeText(this.context, "add new point ", Toast.LENGTH_SHORT).show()
        waypoints.add(point)
        val apiKey = this.getString(R.string.graphopper_key)
        if (waypoints.size >1) {
            routingTask = GraphHopperTask(map, this.view, apiKey, waypoints).execute()
        }
        val startMarker = Marker(map)
        startMarker.position = point
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        routingMarkers.add(startMarker)
        mItemArray.add(Pair(routingMarkers.size.toLong(), "${point.latitude}, ${point.longitude}"))
        listAdapter.notifyDataSetChanged()
        map.overlays.add(startMarker)
        map.invalidate()
    }


}
