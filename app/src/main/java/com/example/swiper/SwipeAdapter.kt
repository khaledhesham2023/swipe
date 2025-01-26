package com.example.swiper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

class SwipeAdapter: Adapter<SwipeAdapter.SwipeViewHolder>() {

    inner class SwipeViewHolder(val itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        return SwipeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_swipe,parent,false))
    }

    override fun getItemCount(): Int = 20

    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
    }
}