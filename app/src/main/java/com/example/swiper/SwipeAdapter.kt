package com.example.swiper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class SwipeAdapter : RecyclerView.Adapter<SwipeAdapter.SwipeViewHolder>() {

    private var swipedPosition: Int? = null // Track the currently swiped position

    // ViewHolder class
    inner class SwipeViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView)

    // Inflate item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        return SwipeViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_swipe, parent, false)
        )
    }

    // Return item count
    override fun getItemCount(): Int = 20

    // Bind data and attach swipe listener
    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
    }
}
