package com.example.swiper

import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.swiper.swipelayout.DragStateChanged
import com.example.swiper.swipelayout.Model
import com.example.swiper.swipelayout.SwipeLayout

class SwipeAdapter(private var data: ArrayList<Model>) :
    RecyclerView.Adapter<SwipeAdapter.SwipeViewHolder>() {

    private var selectedItem: Model? = null

    // ViewHolder class
    class SwipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textItem: TextView = itemView.findViewById(R.id.textItem)
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val text: TextView = itemView.findViewById(R.id.text)
        val layout: ConstraintLayout = itemView.findViewById(R.id.remove_layout)
        val root: SwipeLayout = itemView.findViewById(R.id.root)
    }

    // Inflate item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_swipe, parent, false)
        return SwipeViewHolder(view)
    }

    // Return item count
    override fun getItemCount(): Int = data.size

    // Bind data and attach swipe listener
    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
        val item = data[position]
        holder.textItem.text = item.text
        holder.icon.setImageResource(if (item.isClickedOnce) R.drawable.ic_confirm else R.drawable.ic_trash)
        holder.text.text = if (item.isClickedOnce) "Confirm" else "Remove"
        holder.layout.setOnClickListener {
            if (!item.isClickedOnce) {
                item.isClickedOnce = true
                holder.icon.setImageResource(R.drawable.ic_confirm)
                holder.text.text = "Confirm"
            } else {
                remove(item)
            }

        }
        holder.root.setDragStateChangeListener(object : DragStateChanged {
            override fun onDragStateChanged(state: Int) {
                when (state) {
                    SwipeLayout.STATE_CLOSE -> {
                        item.isClickedOnce = false
                        holder.icon.setImageResource(R.drawable.ic_trash)
                        holder.text.text = "Remove"
                    }

                    SwipeLayout.STATE_DRAGGING -> {

                    }

                    SwipeLayout.STATE_OPEN -> {
                    }

                    SwipeLayout.STATE_OPENING -> {
                        selectedItem = item
                    }

                    SwipeLayout.STATE_CLOSING -> {

                    }
                }
            }
        })


//        if (holder.root.isClosed){
//            item.isClickedOnce = false
//            notifyItemChanged(position)
//        }
    }

    private fun remove(selectedItem: Model?) {


    }
}
