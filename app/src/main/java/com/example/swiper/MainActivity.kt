package com.example.swiper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
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


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                Log.i("TAGGG", "onMove")
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                Toast.makeText(this@MainActivity, "Action Triggered", Toast.LENGTH_SHORT).show()
            }

            override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                Log.i("TAGGG", "getMoveThreshold")
                return super.getMoveThreshold(viewHolder)
            }

            override fun onChildDraw(
                canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
//                Log.i("TAGGG","onChildDraw")
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val viewItem = viewHolder.itemView
                    if (dX > -253){
                        paintDrawCommandToStart(canvas, viewItem, R.layout.remove_layout, dX)

                    } else {

                    }
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

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(0, ItemTouchHelper.END)
            }

            override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
                Log.i("TAGGG", "convertToAbsoluteDirection")
                return super.convertToAbsoluteDirection(flags, layoutDirection)
            }

            override fun canDropOver(
                recyclerView: RecyclerView,
                current: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                Log.i("TAGGG", "canDropOver")
                return super.canDropOver(recyclerView, current, target)
            }

            override fun isLongPressDragEnabled(): Boolean {
                Log.i("TAGGG", "isLongPressDragEnabled")
                return super.isLongPressDragEnabled()
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                Log.i("TAGGG", "isItemViewSwipeEnabled")
                return super.isItemViewSwipeEnabled()
            }

            override fun getBoundingBoxMargin(): Int {
                Log.i("TAGGG", "getBoundingBoxMargin")
                return super.getBoundingBoxMargin()
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                Log.i("TAGGG", "getSwipeThreshold")
                return super.getSwipeThreshold(viewHolder)
            }

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                Log.i("TAGGG", "getSwipeEscapeVelocity")
                return super.getSwipeEscapeVelocity(defaultValue)
            }

            override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                Log.i("TAGGG", "getSwipeVelocityThreshold")
                return super.getSwipeVelocityThreshold(defaultValue)
            }

            override fun chooseDropTarget(
                selected: RecyclerView.ViewHolder,
                dropTargets: MutableList<RecyclerView.ViewHolder>,
                curX: Int,
                curY: Int
            ): RecyclerView.ViewHolder {
                Log.i("TAGGG", "chooseDropTarget")
                return super.chooseDropTarget(selected, dropTargets, curX, curY)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                Log.i("TAGGG", "onSelectedChanged")
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun onMoved(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                fromPos: Int,
                target: RecyclerView.ViewHolder,
                toPos: Int,
                x: Int,
                y: Int
            ) {
                Log.i("TAGGG", "onMoved")
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
            }

            override fun onChildDrawOver(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder?,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                super.onChildDrawOver(
                    c,
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
    val icon = layoutView.findViewById<ImageView>(R.id.icon)
    if (willActionBeTriggered(dX, viewItem.width)) {
        text.text = "Confirm"
        icon.setImageResource(R.drawable.ic_confirm)
    } else {

        text.text = "Remove"
        icon.setImageResource(R.drawable.ic_trash)
    }

    // Set the background color based on swipe distance
    val backgroundColor = getSwipeBackgroundColor(R.color.red, viewItem)
    return DrawCommand(layoutView, backgroundColor)
}

private fun getSwipeBackgroundColor(
    color: Int,
    viewItem: View
): Int {
    return ContextCompat.getColor(viewItem.context, color)
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
//    Log.d("TAGGG", "left ${dX} top ${viewItem.top.toFloat()} right ${viewItem.right.toFloat()} bottom ${viewItem.bottom.toFloat()}")
    return RectF(
        viewItem.left.toFloat(), viewItem.top.toFloat(),
        viewItem.left.toFloat()+ dX, viewItem.bottom.toFloat()
    )
}

private fun calculateTopMargin(viewItem: View, layoutHeight: Int): Int {
    return (viewItem.height - layoutHeight) / 10
}

private fun getLayoutBounds(viewItem: View, layoutWidth: Int, layoutHeight: Int, dX: Float): Rect {
    val rightBound = viewItem.left + dX.toInt()
    val leftBound = rightBound - layoutWidth
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
    layoutView.layout(
        layoutView.measuredWidth,
        layoutView.measuredHeight,
        layoutView.measuredWidth,
        layoutView.measuredHeight
    )

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


class ItemTouchListener(view: View) : OnTouchListener {
    private val mSlop: Int
    private val mView: View
    private var mDownX = 0f
    private var mDownY = 0f
    private var mSwiping = false
    private var mSwipingSlop = 0
    private var mVelocityTracker: VelocityTracker? = null
    var mTranslationX = 0f

    init {
        val vc = ViewConfiguration.get(view.context)
        mSlop = vc.scaledTouchSlop
        mView = view
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        // offset because the view is translated during swipe
        motionEvent.offsetLocation(mTranslationX, 0f)
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = motionEvent.rawX
                mDownY = motionEvent.rawY
                return true
            }

            MotionEvent.ACTION_UP -> {
                // if needed, implement part of limit swipe logic also here
                mVelocityTracker?.addMovement(motionEvent)
                mVelocityTracker?.computeCurrentVelocity(1000)
                mVelocityTracker?.recycle()
                mVelocityTracker = null
                mTranslationX = 0f
                mDownX = 0f
                mDownY = 0f
                mSwiping = false
            }

            MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker?.recycle()
                mVelocityTracker = null
                mTranslationX = 0f
                mDownX = 0f
                mDownY = 0f
                mSwiping = false
            }

            MotionEvent.ACTION_MOVE -> {
                mVelocityTracker?.addMovement(motionEvent)
                val deltaX = motionEvent.rawX - mDownX
                val deltaY = motionEvent.rawY - mDownY
                if (abs(deltaX.toDouble()) > mSlop && abs(deltaY.toDouble()) < abs(deltaX.toDouble()) / 2) {
                    mSwiping = true
                    mSwipingSlop = (if (deltaX > 0) mSlop else -mSlop)
                    // cancel view's touch
                    val cancelEvent = MotionEvent.obtain(motionEvent)
                    cancelEvent.action = MotionEvent.ACTION_CANCEL or
                            (motionEvent.actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    cancelEvent.recycle()
                }
                if (mSwiping) {
                    // limit deltaX here: this will keep the swipe up to desired point

                    mTranslationX = deltaX
                    mView.translationX = deltaX - mSwipingSlop
                    return true
                }
            }
        }
        return false
    }
}
