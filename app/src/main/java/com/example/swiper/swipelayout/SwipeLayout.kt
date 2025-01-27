package com.example.swiper.swipelayout

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.example.swiper.R
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SwipeLayout : ViewGroup {
    private var mMainView: View? = null

    private var mSecondaryView: View? = null

    private val mRectMainClose = Rect()

    private val mRectMainOpen = Rect()

    private val mRectSecClose = Rect()

    private val mRectSecOpen = Rect()

    private var mMinDistRequestDisallowParent = 0

    private var mIsOpenBeforeInit = false

    @Volatile
    private var mAborted = false

    @Volatile
    private var mIsScrolling = false

    @Volatile
    var isDragLocked: Boolean = false
        private set

    var minFlingVelocity: Int = DEFAULT_MIN_FLING_VELOCITY
    private var mState = STATE_CLOSE
    private var mMode = MODE_NORMAL

    private var mLastMainLeft = 0
    private var mLastMainTop = 0

    var dragEdge: Int = DRAG_EDGE_LEFT

    private var mDragDist = 0f
    private var mPrevX = -1f
    private var mPrevY = -1f

    private var mDragHelper: ViewDragHelper? = null
    private var mGestureDetector: GestureDetectorCompat? = null

    private var mDragStateChanged: DragStateChanged? = null
    private var mSwipe: Swipe? = null

    private var mOnLayoutCount = 0

    constructor(context: Context?) : super(context) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector?.onTouchEvent(event)
        mDragHelper?.processTouchEvent(event)
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isDragLocked) {
            return super.onInterceptTouchEvent(ev)
        }

        mDragHelper?.processTouchEvent(ev)
        mGestureDetector?.onTouchEvent(ev)
        accumulateDragDist(ev)

        val couldBecomeClick = couldBecomeClick(ev)
        val settling = mDragHelper?.viewDragState == ViewDragHelper.STATE_SETTLING
        val idleAfterScrolled = mDragHelper?.viewDragState == ViewDragHelper.STATE_IDLE
                && mIsScrolling

        mPrevX = ev.getX()
        mPrevY = ev.getY()

        return !couldBecomeClick && (settling || idleAfterScrolled)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (childCount >= 2) {
            mSecondaryView = getChildAt(0)
            mMainView = getChildAt(1)
        } else if (childCount == 1) {
            mMainView = getChildAt(0)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mAborted = false

        for (index in 0 until childCount) {
            val child = getChildAt(index)

            var left: Int
            var right: Int
            var top: Int
            var bottom: Int
            bottom = 0
            top = bottom
            right = top
            left = right

            val minLeft = paddingLeft
            val maxRight = max((r - paddingRight - l).toDouble(), 0.0).toInt()
            val minTop = paddingTop
            val maxBottom =
                max((b - paddingBottom - t).toDouble(), 0.0).toInt()

            var measuredChildHeight = child.measuredHeight
            var measuredChildWidth = child.measuredWidth

            val childParams = child.layoutParams
            var matchParentHeight = false
            var matchParentWidth = false

            if (childParams != null) {
                matchParentHeight = (childParams.height == LayoutParams.FILL_PARENT)
                matchParentWidth = (childParams.width == LayoutParams.FILL_PARENT)
            }

            if (matchParentHeight) {
                measuredChildHeight = maxBottom - minTop
                childParams!!.height = measuredChildHeight
            }

            if (matchParentWidth) {
                measuredChildWidth = maxRight - minLeft
                childParams!!.width = measuredChildWidth
            }

            when (dragEdge) {
                DRAG_EDGE_RIGHT -> {
                    left = max(
                        (r - measuredChildWidth - paddingRight - l).toDouble(),
                        minLeft.toDouble()
                    ).toInt()
                    top = min(paddingTop.toDouble(), maxBottom.toDouble()).toInt()
                    right =
                        max((r - paddingRight - l).toDouble(), minLeft.toDouble()).toInt()
                    bottom = min(
                        (measuredChildHeight + paddingTop).toDouble(),
                        maxBottom.toDouble()
                    ).toInt()
                }

                DRAG_EDGE_TOP, DRAG_EDGE_LEFT -> {
                    left = min(paddingLeft.toDouble(), maxRight.toDouble()).toInt()
                    top = min(paddingTop.toDouble(), maxBottom.toDouble()).toInt()
                    right =
                        min(
                            (measuredChildWidth + paddingLeft).toDouble(),
                            maxRight.toDouble()
                        ).toInt()
                    bottom = min(
                        (measuredChildHeight + paddingTop).toDouble(),
                        maxBottom.toDouble()
                    ).toInt()
                }

                DRAG_EDGE_BOTTOM -> {
                    left = min(paddingLeft.toDouble(), maxRight.toDouble()).toInt()
                    top = max(
                        (b - measuredChildHeight - paddingBottom - t).toDouble(),
                        minTop.toDouble()
                    ).toInt()
                    right =
                        min(
                            (measuredChildWidth + paddingLeft).toDouble(),
                            maxRight.toDouble()
                        ).toInt()
                    bottom =
                        max((b - paddingBottom - t).toDouble(), minTop.toDouble()).toInt()
                }
            }

            child.layout(left, top, right, bottom)
        }

        if (mMode == MODE_SAME_LEVEL) {
            when (dragEdge) {
                DRAG_EDGE_LEFT -> mSecondaryView!!.offsetLeftAndRight(-mSecondaryView!!.width)
                DRAG_EDGE_RIGHT -> mSecondaryView!!.offsetLeftAndRight(
                    mSecondaryView!!.width
                )

                DRAG_EDGE_TOP -> mSecondaryView!!.offsetTopAndBottom(-mSecondaryView!!.height)
                DRAG_EDGE_BOTTOM -> mSecondaryView!!.offsetTopAndBottom(
                    mSecondaryView!!.height
                )
            }
        }

        initRects()

        if (mIsOpenBeforeInit) {
            open(false)
        } else {
            close(false)
        }

        mLastMainLeft = mMainView!!.left
        mLastMainTop = mMainView!!.top

        mOnLayoutCount++
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (childCount < 2) {
            throw RuntimeException("Layout must have two children")
        }

        val params = layoutParams

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var desiredWidth = 0
        var desiredHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            desiredWidth =
                max(child.measuredWidth.toDouble(), desiredWidth.toDouble()).toInt()
            desiredHeight =
                max(child.measuredHeight.toDouble(), desiredHeight.toDouble()).toInt()
        }

        widthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredWidth, widthMode)
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredHeight, heightMode)

        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childParams = child.layoutParams

            if (childParams != null) {
                if (childParams.height == LayoutParams.MATCH_PARENT) {
                    child.minimumHeight = measuredHeight
                }

                if (childParams.width == LayoutParams.MATCH_PARENT) {
                    child.minimumWidth = measuredWidth
                }
            }

            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            desiredWidth =
                max(child.measuredWidth.toDouble(), desiredWidth.toDouble()).toInt()
            desiredHeight =
                max(child.measuredHeight.toDouble(), desiredHeight.toDouble()).toInt()
        }

        desiredWidth += paddingLeft + paddingRight
        desiredHeight += paddingTop + paddingBottom

        if (widthMode == MeasureSpec.EXACTLY) {
            desiredWidth = measuredWidth
        } else {
            if (params.width == LayoutParams.MATCH_PARENT) {
                desiredWidth = measuredWidth
            }

            if (widthMode == MeasureSpec.AT_MOST) {
                desiredWidth = min(desiredWidth.toDouble(), measuredWidth.toDouble()).toInt()
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            desiredHeight = measuredHeight
        } else {
            if (params.height == LayoutParams.MATCH_PARENT) {
                desiredHeight = measuredHeight
            }

            if (heightMode == MeasureSpec.AT_MOST) {
                desiredHeight =
                    min(desiredHeight.toDouble(), measuredHeight.toDouble()).toInt()
            }
        }

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun computeScroll() {
        if (mDragHelper?.continueSettling(true) == true) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun open(animation: Boolean) {
        mIsOpenBeforeInit = true
        mAborted = false

        if (animation) {
            mState = STATE_OPENING
            mMainView?.let { mDragHelper?.smoothSlideViewTo(it, mRectMainOpen.left, mRectMainOpen.top) }

            if (mDragStateChanged != null) {
                mDragStateChanged!!.onDragStateChanged(mState)
            }
        } else {
            mState = STATE_OPEN
            mDragHelper?.abort()

            mMainView!!.layout(
                mRectMainOpen.left,
                mRectMainOpen.top,
                mRectMainOpen.right,
                mRectMainOpen.bottom
            )

            mSecondaryView!!.layout(
                mRectSecOpen.left,
                mRectSecOpen.top,
                mRectSecOpen.right,
                mRectSecOpen.bottom
            )
        }

        ViewCompat.postInvalidateOnAnimation(this@SwipeLayout)
    }

    fun close(animation: Boolean) {
        mIsOpenBeforeInit = false
        mAborted = false

        if (animation) {
            mState = STATE_CLOSING
            mMainView?.let { mDragHelper?.smoothSlideViewTo(it, mRectMainClose.left, mRectMainClose.top) }

            if (mDragStateChanged != null) {
                mDragStateChanged!!.onDragStateChanged(mState)
            }
        } else {
            mState = STATE_CLOSE
            mDragHelper?.abort()

            mMainView!!.layout(
                mRectMainClose.left,
                mRectMainClose.top,
                mRectMainClose.right,
                mRectMainClose.bottom
            )

            mSecondaryView!!.layout(
                mRectSecClose.left,
                mRectSecClose.top,
                mRectSecClose.right,
                mRectSecClose.bottom
            )
        }

        ViewCompat.postInvalidateOnAnimation(this@SwipeLayout)
    }

    fun setSwipeListener(listener: Swipe?) {
        mSwipe = listener
    }

    fun setLockDrag(lock: Boolean) {
        isDragLocked = lock
    }

    val isOpened: Boolean
        get() = (mState == STATE_OPEN)

    val isClosed: Boolean
        get() = (mState == STATE_CLOSE)

    fun setDragStateChangeListener(listener: DragStateChanged?) {
        mDragStateChanged = listener
    }

    fun abort() {
        mAborted = true
        mDragHelper?.abort()
    }

    fun shouldRequestLayout(): Boolean {
        return mOnLayoutCount < 2
    }

    private val mainOpenLeft: Int
        get() = when (dragEdge) {
            DRAG_EDGE_LEFT -> mRectMainClose.left + mSecondaryView!!.width

            DRAG_EDGE_RIGHT -> mRectMainClose.left - mSecondaryView!!.width

            DRAG_EDGE_TOP -> mRectMainClose.left

            DRAG_EDGE_BOTTOM -> mRectMainClose.left

            else -> 0
        }

    private val mainOpenTop: Int
        get() {
            return when (dragEdge) {
                DRAG_EDGE_LEFT -> mRectMainClose.top

                DRAG_EDGE_RIGHT -> mRectMainClose.top

                DRAG_EDGE_TOP -> mRectMainClose.top + mSecondaryView!!.height

                DRAG_EDGE_BOTTOM -> mRectMainClose.top - mSecondaryView!!.height

                else -> 0
            }
        }

    private val secOpenLeft: Int
        get() {
            if (mMode == MODE_NORMAL || dragEdge == DRAG_EDGE_BOTTOM || dragEdge == DRAG_EDGE_TOP) {
                return mRectSecClose.left
            }

            return if (dragEdge == DRAG_EDGE_LEFT) {
                mRectSecClose.left + mSecondaryView!!.width
            } else {
                mRectSecClose.left - mSecondaryView!!.width
            }
        }

    private val secOpenTop: Int
        get() {
            if (mMode == MODE_NORMAL || dragEdge == DRAG_EDGE_LEFT || dragEdge == DRAG_EDGE_RIGHT) {
                return mRectSecClose.top
            }

            return if (dragEdge == DRAG_EDGE_TOP) {
                mRectSecClose.top + mSecondaryView!!.height
            } else {
                mRectSecClose.top - mSecondaryView!!.height
            }
        }

    private fun initRects() {
        mRectMainClose[mMainView!!.left, mMainView!!.top, mMainView!!.right] = mMainView!!.bottom

        mRectSecClose[mSecondaryView!!.left, mSecondaryView!!.top, mSecondaryView!!.right] =
            mSecondaryView!!.bottom

        mRectMainOpen[mainOpenLeft, mainOpenTop, mainOpenLeft + mMainView!!.width] =
            mainOpenTop + mMainView!!.height

        mRectSecOpen[secOpenLeft, secOpenTop, secOpenLeft + mSecondaryView!!.width] =
            secOpenTop + mSecondaryView!!.height
    }

    private fun couldBecomeClick(ev: MotionEvent): Boolean {
        return isInMainView(ev) && !shouldInitiateADrag()
    }

    private fun isInMainView(ev: MotionEvent): Boolean {
        val x: Float = ev.getX()
        val y: Float = ev.getY()

        val withinVertical = mMainView!!.top <= y && y <= mMainView!!.bottom
        val withinHorizontal = mMainView!!.left <= x && x <= mMainView!!.right

        return withinVertical && withinHorizontal
    }

    private fun shouldInitiateADrag(): Boolean {
        val minDistToInitiateDrag: Float = mDragHelper?.touchSlop?.toFloat() ?: 0f
        return mDragDist >= minDistToInitiateDrag
    }

    private fun accumulateDragDist(ev: MotionEvent) {
        val action: Int = ev.getAction()
        if (action == MotionEvent.ACTION_DOWN) {
            mDragDist = 0f
            return
        }

        val dragHorizontally = dragEdge == DRAG_EDGE_LEFT ||
                dragEdge == DRAG_EDGE_RIGHT

        val dragged: Float
        if (dragHorizontally) {
            dragged = abs((ev.getX() - mPrevX).toDouble()).toFloat()
        } else {
            dragged = abs((ev.getY() - mPrevY).toDouble()).toFloat()
        }

        mDragDist += dragged
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        if (attrs != null && context != null) {
            val a: TypedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.SwipeLayout,
                0, 0
            )

            dragEdge = a.getInteger(R.styleable.SwipeLayout_dragEdge, DRAG_EDGE_LEFT)
            minFlingVelocity =
                a.getInteger(R.styleable.SwipeLayout_flingVelocity, DEFAULT_MIN_FLING_VELOCITY)
            mMode = a.getInteger(R.styleable.SwipeLayout_mode, MODE_NORMAL)

            mMinDistRequestDisallowParent = a.getDimensionPixelSize(
                R.styleable.SwipeLayout_minDistRequestDisallowParent,
                dpToPx(DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT)
            )
        }

        mDragHelper = ViewDragHelper.create(this, 1.0f, mDragHelperCallback)
        mDragHelper?.setEdgeTrackingEnabled(ViewDragHelper.EDGE_ALL)

        mGestureDetector = context?.let { GestureDetectorCompat(it, mGestureListener) }
    }

    private val mGestureListener: GestureDetector.OnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            var hasDisallowed: Boolean = false

            override fun onDown(e: MotionEvent): Boolean {
                mIsScrolling = false
                hasDisallowed = false
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                mIsScrolling = true
                return false
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                mIsScrolling = true

                if (parent != null) {
                    val shouldDisallow: Boolean

                    if (!hasDisallowed) {
                        shouldDisallow = distToClosestEdge >= mMinDistRequestDisallowParent
                        if (shouldDisallow) {
                            hasDisallowed = true
                        }
                    } else {
                        shouldDisallow = true
                    }

                    parent.requestDisallowInterceptTouchEvent(shouldDisallow)
                }

                return false
            }
        }

    private val distToClosestEdge: Int
        get() {
            when (dragEdge) {
                DRAG_EDGE_LEFT -> {
                    val pivotRight = mRectMainClose.left + mSecondaryView!!.width

                    return min(
                        (mMainView!!.left - mRectMainClose.left).toDouble(),
                        (pivotRight - mMainView!!.left).toDouble()
                    ).toInt()
                }

                DRAG_EDGE_RIGHT -> {
                    val pivotLeft = mRectMainClose.right - mSecondaryView!!.width

                    return min(
                        (mMainView!!.right - pivotLeft).toDouble(),
                        (mRectMainClose.right - mMainView!!.right).toDouble()
                    ).toInt()
                }

                DRAG_EDGE_TOP -> {
                    val pivotBottom = mRectMainClose.top + mSecondaryView!!.height

                    return min(
                        (mMainView!!.bottom - pivotBottom).toDouble(),
                        (pivotBottom - mMainView!!.top).toDouble()
                    ).toInt()
                }

                DRAG_EDGE_BOTTOM -> {
                    val pivotTop = mRectMainClose.bottom - mSecondaryView!!.height

                    return min(
                        (mRectMainClose.bottom - mMainView!!.bottom).toDouble(),
                        (mMainView!!.bottom - pivotTop).toDouble()
                    ).toInt()
                }
            }

            return 0
        }

    private val halfwayPivotHorizontal: Int
        get() {
            return if (dragEdge == DRAG_EDGE_LEFT) {
                mRectMainClose.left + mSecondaryView!!.width / 2
            } else {
                mRectMainClose.right - mSecondaryView!!.width / 2
            }
        }

    private val halfwayPivotVertical: Int
        get() {
            return if (dragEdge == DRAG_EDGE_TOP) {
                mRectMainClose.top + mSecondaryView!!.height / 2
            } else {
                mRectMainClose.bottom - mSecondaryView!!.height / 2
            }
        }

    private val mDragHelperCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            mAborted = false

            if (isDragLocked) return false

            mMainView?.let { mDragHelper?.captureChildView(it, pointerId) }
            return false
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return when (dragEdge) {
                DRAG_EDGE_TOP -> max(
                    min(
                        top.toDouble(),
                        (mRectMainClose.top + mSecondaryView!!.height).toDouble()
                    ),
                    mRectMainClose.top.toDouble()
                ).toInt()

                DRAG_EDGE_BOTTOM -> max(
                    min(top.toDouble(), mRectMainClose.top.toDouble()),
                    (mRectMainClose.top - mSecondaryView!!.height).toDouble()
                ).toInt()

                else -> child.top
            }
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return when (dragEdge) {
                DRAG_EDGE_RIGHT -> max(
                    min(left.toDouble(), mRectMainClose.left.toDouble()),
                    (mRectMainClose.left - mSecondaryView!!.width).toDouble()
                ).toInt()

                DRAG_EDGE_LEFT -> max(
                    min(
                        left.toDouble(),
                        (mRectMainClose.left + mSecondaryView!!.width).toDouble()
                    ),
                    mRectMainClose.left.toDouble()
                ).toInt()

                else -> child.left
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val velRightExceeded: Boolean = pxToDp(xvel.toInt()) >= minFlingVelocity
            val velLeftExceeded: Boolean = pxToDp(xvel.toInt()) <= -minFlingVelocity
            val velUpExceeded: Boolean = pxToDp(yvel.toInt()) <= -minFlingVelocity
            val velDownExceeded: Boolean = pxToDp(yvel.toInt()) >= minFlingVelocity

            val pivotHorizontal: Int = halfwayPivotHorizontal
            val pivotVertical: Int = halfwayPivotVertical

            when (dragEdge) {
                DRAG_EDGE_RIGHT -> if (velRightExceeded) {
                    close(true)
                } else if (velLeftExceeded) {
                    open(true)
                } else {
                    if (mMainView!!.right < pivotHorizontal) {
                        open(true)
                    } else {
                        close(true)
                    }
                }

                DRAG_EDGE_LEFT -> if (velRightExceeded) {
                    open(true)
                } else if (velLeftExceeded) {
                    close(true)
                } else {
                    if (mMainView!!.left < pivotHorizontal) {
                        close(true)
                    } else {
                        open(true)
                    }
                }

                DRAG_EDGE_TOP -> if (velUpExceeded) {
                    close(true)
                } else if (velDownExceeded) {
                    open(true)
                } else {
                    if (mMainView!!.top < pivotVertical) {
                        close(true)
                    } else {
                        open(true)
                    }
                }

                DRAG_EDGE_BOTTOM -> if (velUpExceeded) {
                    open(true)
                } else if (velDownExceeded) {
                    close(true)
                } else {
                    if (mMainView!!.bottom < pivotVertical) {
                        open(true)
                    } else {
                        close(true)
                    }
                }
            }
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            super.onEdgeDragStarted(edgeFlags, pointerId)

            if (isDragLocked) {
                return
            }

            val edgeStartLeft = (dragEdge == DRAG_EDGE_RIGHT)
                    && edgeFlags == ViewDragHelper.EDGE_LEFT

            val edgeStartRight = (dragEdge == DRAG_EDGE_LEFT)
                    && edgeFlags == ViewDragHelper.EDGE_RIGHT

            val edgeStartTop = (dragEdge == DRAG_EDGE_BOTTOM)
                    && edgeFlags == ViewDragHelper.EDGE_TOP

            val edgeStartBottom = (dragEdge == DRAG_EDGE_TOP)
                    && edgeFlags == ViewDragHelper.EDGE_BOTTOM

            if (edgeStartLeft || edgeStartRight || edgeStartTop || edgeStartBottom) {
                mMainView?.let { mDragHelper?.captureChildView(it, pointerId) }
            }
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            if (mMode == MODE_SAME_LEVEL) {
                if (dragEdge == DRAG_EDGE_LEFT || dragEdge == DRAG_EDGE_RIGHT) {
                    mSecondaryView!!.offsetLeftAndRight(dx)
                } else {
                    mSecondaryView!!.offsetTopAndBottom(dy)
                }
            }

            val isMoved = (mMainView!!.left != mLastMainLeft) || (mMainView!!.top != mLastMainTop)
            if (mSwipe != null && isMoved) {
                if (mMainView!!.left == mRectMainClose.left && mMainView!!.top == mRectMainClose.top) {
                    mSwipe?.onClosed(this@SwipeLayout)
                } else if (mMainView!!.left == mRectMainOpen.left && mMainView!!.top == mRectMainOpen.top) {
                    mSwipe?.onOpened(this@SwipeLayout)
                } else {
                    mSwipe?.onSlide(this@SwipeLayout, slideOffset)
                }
            }

            mLastMainLeft = mMainView!!.left
            mLastMainTop = mMainView!!.top
            ViewCompat.postInvalidateOnAnimation(this@SwipeLayout)
        }

        private val slideOffset: Float
            get() {
                return when (dragEdge) {
                    DRAG_EDGE_LEFT -> (mMainView!!.left - mRectMainClose.left).toFloat() / mSecondaryView!!.width

                    DRAG_EDGE_RIGHT -> (mRectMainClose.left - mMainView!!.left).toFloat() / mSecondaryView!!.width

                    DRAG_EDGE_TOP -> (mMainView!!.top - mRectMainClose.top).toFloat() / mSecondaryView!!.height

                    DRAG_EDGE_BOTTOM -> (mRectMainClose.top - mMainView!!.top).toFloat() / mSecondaryView!!.height

                    else -> 0F
                }
            }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            val prevState = mState

            when (state) {
                ViewDragHelper.STATE_DRAGGING -> mState = STATE_DRAGGING
                ViewDragHelper.STATE_IDLE -> mState =
                    if (dragEdge == DRAG_EDGE_LEFT || dragEdge == DRAG_EDGE_RIGHT) {
                        if (mMainView!!.left == mRectMainClose.left) {
                            STATE_CLOSE
                        } else {
                            STATE_OPEN
                        }
                    } else {
                        if (mMainView!!.top == mRectMainClose.top) {
                            STATE_CLOSE
                        } else {
                            STATE_OPEN
                        }
                    }
            }

            if (mDragStateChanged != null && !mAborted && prevState != mState) {
                mDragStateChanged!!.onDragStateChanged(mState)
            }
        }
    }

    private fun pxToDp(px: Int): Int {
        val resources = context.resources
        val metrics: DisplayMetrics = resources.displayMetrics
        return (px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

    private fun dpToPx(dp: Int): Int {
        val resources = context.resources
        val metrics: DisplayMetrics = resources.displayMetrics
        return (dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

    companion object {
        const val STATE_CLOSE: Int = 0
        const val STATE_CLOSING: Int = 1
        const val STATE_OPEN: Int = 2
        const val STATE_OPENING: Int = 3
        const val STATE_DRAGGING: Int = 4

        private const val DEFAULT_MIN_FLING_VELOCITY = 300
        private const val DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT = 1

        const val DRAG_EDGE_LEFT: Int = 0x1
        const val DRAG_EDGE_RIGHT: Int = 0x1 shl 1
        const val DRAG_EDGE_TOP: Int = 0x1 shl 2
        const val DRAG_EDGE_BOTTOM: Int = 0x1 shl 3

        const val MODE_NORMAL: Int = 0

        const val MODE_SAME_LEVEL: Int = 1

        fun getStateString(state: Int): String {
            return when (state) {
                STATE_CLOSE -> "state_close"

                STATE_CLOSING -> "state_closing"

                STATE_OPEN -> "state_open"

                STATE_OPENING -> "state_opening"

                STATE_DRAGGING -> "state_dragging"

                else -> "undefined"
            }
        }
    }
}
