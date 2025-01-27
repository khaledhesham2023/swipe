package com.example.swiper.swipelayout

interface Swipe {
    fun onClosed(view: SwipeLayout?)

    fun onOpened(view: SwipeLayout?)

    fun onSlide(view: SwipeLayout?, slideOffset: Float)
}