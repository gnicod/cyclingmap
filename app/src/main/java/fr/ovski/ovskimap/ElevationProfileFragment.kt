package fr.ovski.ovskimap

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_ENTRIES = "entries"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ElevationProfileFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ElevationProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ElevationProfileFragment : Fragment(), OnChartValueSelectedListener {
    // TODO: Rename and change types of parameters
    private var entries: ArrayList<Entry>? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var marker:Marker? = null

    override fun onNothingSelected() {

    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        Log.i("CHART", "onvalueselected")
        val map = activity!!.findViewById<MapView>(R.id.map)
        if (marker == null) {
            marker = Marker(map)
            marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(marker)
        }
        marker!!.position = (e!!.data as GeoPoint)
        map.controller.setCenter(marker!!.position)
        map.invalidate()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            entries = it.getSerializable(ARG_ENTRIES) as ArrayList<Entry>?
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_elevation_profile, container, false)
    }

    //La vista de layout ha sido creada y ya está disponible
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val elevationChart = view!!.findViewById(R.id.elevation_profile) as LineChart
        view.visibility = View.VISIBLE
        elevationChart.setOnChartValueSelectedListener(this)
        elevationChart.setDrawBorders(false)
        val dataSet = LineDataSet(entries, "Elevation")
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawCircles(false)
        dataSet.setDrawFilled(true)
        dataSet.lineWidth = 5F
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER;
        val lineData = LineData(dataSet)
        elevationChart.data = lineData
        elevationChart.invalidate() // refresh

    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            Log.i("TAG", "ligne 68 ")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ElevationProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(entries: ArrayList<Entry>, param2: String) =
                ElevationProfileFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(ARG_ENTRIES, entries)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
