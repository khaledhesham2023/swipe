package com.example.swiper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchUIUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.internal.ViewUtils.getBackgroundColor
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val swipeAdapter = SwipeAdapter()
        val recycler = findViewById<RecyclerView>(R.id.recyclerList)
        recycler.adapter = swipeAdapter
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(0, ItemTouchHelper.START)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

            override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return super.getMoveThreshold(viewHolder)
            }

            override fun onChildDraw(
                canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val viewItem = viewHolder.itemView
                    paintDrawCommandToStart(canvas, viewItem, R.layout.remove_layout, dX)
                }
                super.onChildDraw(
                    canvas,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        })
        itemTouchHelper.attachToRecyclerView(recycler)
    }
}

/**
 * For encapsulation of resources
 */
private class DrawCommand internal constructor(
    internal val layoutView: View?,
    internal var backgroundColor: Int
)

private fun createDrawCommand(viewItem: View, dX: Float, layoutResId: Int): DrawCommand {
    val context = viewItem.context
    val layoutInflater = LayoutInflater.from(context)
    val layoutView = layoutInflater.inflate(layoutResId, null, false)
    val text = layoutView.findViewById<TextView>(R.id.text)
    if (willActionBeTriggered(dX,viewItem.width)){
        text.text = "Confirm"
    } else {
        text.text = "Remove"
    }

    // Set the background color based on swipe distance
    val backgroundColor = getSwipeBackgroundColor(R.color.red, dX, viewItem)
    return DrawCommand(layoutView, backgroundColor)
}

private fun getSwipeBackgroundColor(
    color: Int,
    dX: Float,
    viewItem: View
): Int {
    return when (willActionBeTriggered(dX, viewItem.width)) {
        true -> ContextCompat.getColor(viewItem.context, color)
        false -> ContextCompat.getColor(viewItem.context, color)
    }
}

private fun willActionBeTriggered(dX: Float, viewWidth: Int): Boolean {
    return abs(dX) >= viewWidth / 2
}

private fun drawBackground(canvas: Canvas, viewItem: View, dX: Float, color: Int) {
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    backgroundPaint.color = color
    val backgroundRectangle = getBackgroundRectangle(viewItem, dX)
    canvas.drawRect(backgroundRectangle, backgroundPaint)
}

private fun getBackgroundRectangle(viewItem: View, dX: Float): RectF {
    return RectF(
        viewItem.right.toFloat() + dX, viewItem.top.toFloat(),
        viewItem.right.toFloat(), viewItem.bottom.toFloat()
    )
}

private fun calculateTopMargin(viewItem: View, layoutHeight: Int): Int {
    return (viewItem.height - layoutHeight) / 10
}

private fun getLayoutBounds(viewItem: View, layoutWidth: Int, layoutHeight: Int, dX: Float): Rect {
    val leftBound = viewItem.right + dX.toInt()
    val rightBound = leftBound + layoutWidth
    val topMargin = calculateTopMargin(viewItem, layoutHeight)
    val topBound = viewItem.top + topMargin
    val bottomBound = topBound + layoutHeight

    return Rect(leftBound, topBound, rightBound, bottomBound)
}

private fun drawLayout(canvas: Canvas, viewItem: View, dX: Float, layoutView: View) {
    layoutView.measure(
        View.MeasureSpec.makeMeasureSpec(viewItem.width, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(viewItem.height, View.MeasureSpec.AT_MOST)
    )
    layoutView.layout(layoutView.measuredWidth, layoutView.measuredHeight, layoutView.measuredWidth, layoutView.measuredHeight)

    val bounds = getLayoutBounds(viewItem, layoutView.measuredWidth, layoutView.measuredHeight, dX)

    // Translate canvas for layout drawing
    canvas.save()
    canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
    layoutView.draw(canvas)
    canvas.restore()
}

private fun paintDrawCommand(drawCommand: DrawCommand, canvas: Canvas, dX: Float, viewItem: View) {
    drawBackground(canvas, viewItem, dX, drawCommand.backgroundColor)
    drawCommand.layoutView?.let { drawLayout(canvas, viewItem, dX, it) }
}

fun paintDrawCommandToStart(
    canvas: Canvas,
    viewItem: View,
    @LayoutRes layoutResId: Int,
    dX: Float
) {
    val drawCommand = createDrawCommand(viewItem, dX, layoutResId)
    paintDrawCommand(drawCommand, canvas, dX, viewItem)
}


private class SwipeResource internal constructor(
    internal val layoutView: View?,
    internal var backgroundColor: Int,
    internal var removeText: View?,
    internal var confirmText: View?
)

private fun createSwipe(viewItem: View, dX: Float, layoutResId: Int): DrawCommand {
    val context = viewItem.context
    val layoutInflater = LayoutInflater.from(context)
    val layoutView =
        layoutInflater.inflate(layoutResId, null, false)
    val text = layoutView.findViewById<TextView>(R.id.text)
    text.text = "Confirm"
    // Set the background color based on swipe distance
    val backgroundColor = getSwipeBackgroundColor(R.color.red, dX, viewItem)
    return DrawCommand(layoutView, backgroundColor)
}