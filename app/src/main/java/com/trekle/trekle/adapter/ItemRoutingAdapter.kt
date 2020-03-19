package com.trekle.trekle.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.Pair
import com.trekle.trekle.R
import com.woxthebox.draglistview.DragItemAdapter
import java.util.*

class ItemRoutingAdapter internal constructor(list: ArrayList<Pair<Long, String>>, private val mLayoutId: Int, private val mGrabHandleId: Int, private val mDragOnLongPress: Boolean) : DragItemAdapter<Pair<Long, String>, ItemRoutingAdapter.ViewHolder>() {

    init {
        itemList = list
    }

    override fun onBindViewHolder(holder: ItemRoutingAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val text = mItemList[position].second
        holder.mText.text = text
        holder.itemView.tag = mItemList[position]
    }

    override fun getUniqueItemId(position: Int): Long {
        return mItemList[position].first!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemRoutingAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(mLayoutId, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : DragItemAdapter.ViewHolder(itemView, mGrabHandleId, mDragOnLongPress) {
        var mText: TextView

        init {
            mText = itemView.findViewById<View>(R.id.text) as TextView
        }

        override fun onItemClicked(view: View) {
            Toast.makeText(view.context, "Item clicked", Toast.LENGTH_SHORT).show()
        }

        override fun onItemLongClicked(view: View): Boolean {
            Toast.makeText(view.context, "Item long clicked", Toast.LENGTH_SHORT).show()
            return true
        }
    }
}
