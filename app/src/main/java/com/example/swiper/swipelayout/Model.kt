package com.example.swiper.swipelayout

data class Model(
    val id:Int,
    val text:String,
    var isClickedOnce: Boolean = false,
    var isSelected: Boolean = false
)