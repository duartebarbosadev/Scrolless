/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components.swipemenulayout

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Scroller
import com.scrolless.libraries.components.R
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max

@SuppressWarnings(
    "TooManyFunctions",
    "ComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "MagicNumber",
    "UnusedPrivateMember",
)
open class SwipeMenuLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {
    private val marginParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    var leftMenuView: SwipeMenuView = SwipeMenuView(context, attrs, defStyleAttr).apply {
        isClickable = true
        layoutParams = marginParams
    }
    var rightMenuView: SwipeMenuView = SwipeMenuView(context, attrs, defStyleAttr).apply {
        isClickable = true
        layoutParams = marginParams
    }
    private var scaledTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var scroller: Scroller = Scroller(context)

    private val matchParentChildren = ArrayList<View>(1)
    private var contentViewResID: Int = -1
    private var contentView: View? = null

    private var isSwiping: Boolean = false
    private var lastP: PointF = PointF()
    private var firstP: PointF = PointF()
    var menuOpenThreshold = 0.3f
    var rightMenuEnable = true
    var leftMenuEnable = true

    private var finalDistanceX: Float = 0.toFloat()
    private var menuState: MenuState = MenuState.CLOSE

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SwipeMenuLayout,
            defStyleAttr,
            0,
        )
        try {
            for (i in 0 until typedArray.indexCount) {
                when (typedArray.getIndex(i)) {
                    R.styleable.SwipeMenuLayout_leftMenuEnable ->
                        rightMenuEnable =
                            typedArray.getBoolean(R.styleable.SwipeMenuLayout_leftMenuEnable, true)

                    R.styleable.SwipeMenuLayout_rightMenuEnable ->
                        leftMenuEnable =
                            typedArray.getBoolean(R.styleable.SwipeMenuLayout_rightMenuEnable, true)

                    R.styleable.SwipeMenuLayout_menuOpenThreshold ->
                        menuOpenThreshold =
                            typedArray.getFloat(R.styleable.SwipeMenuLayout_menuOpenThreshold, 0.3f)

                    R.styleable.SwipeMenuLayout_contentView ->
                        contentViewResID =
                            typedArray.getResourceId(R.styleable.SwipeMenuLayout_contentView, -1)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            typedArray.recycle()
        }

        this.addView(leftMenuView)
        this.addView(rightMenuView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // childView
        isClickable = true
        var count = childCount
        // frameLayout
        val measureMatchParentChildren =
            MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                    MeasureSpec.getMode(
                        heightMeasureSpec,
                    ) != MeasureSpec.EXACTLY
        matchParentChildren.clear()
        var maxHeight = 0
        var maxWidth = 0
        var childState = 0
        // childViews
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val lp = child.layoutParams as MarginLayoutParams
                maxWidth =
                    maxWidth.coerceAtLeast(child.measuredWidth + lp.leftMargin + lp.rightMargin)
                maxHeight =
                    maxHeight.coerceAtLeast(child.measuredHeight + lp.topMargin + lp.bottomMargin)
                childState = View.combineMeasuredStates(childState, child.measuredState)
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT || lp.height == LayoutParams.MATCH_PARENT) {
                        matchParentChildren.add(child)
                    }
                }
            }
        }
        // Check against our minimum height and width
        maxHeight = maxHeight.coerceAtLeast(suggestedMinimumHeight)
        maxWidth = maxWidth.coerceAtLeast(suggestedMinimumWidth)
        setMeasuredDimension(
            View.resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            View.resolveSizeAndState(
                maxHeight,
                heightMeasureSpec,
                childState shl View.MEASURED_HEIGHT_STATE_SHIFT,
            ),
        )

        count = matchParentChildren.size
        if (count > 1) {
            for (i in 0 until count) {
                val child = matchParentChildren[i]
                val lp = child.layoutParams as MarginLayoutParams
                val childWidthMeasureSpec: Int = if (lp.width == LayoutParams.MATCH_PARENT) {
                    val width = max(
                        0,
                        measuredWidth -
                                lp.leftMargin - lp.rightMargin,
                    )
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
                } else {
                    getChildMeasureSpec(
                        widthMeasureSpec,
                        lp.leftMargin + lp.rightMargin,
                        lp.width,
                    )
                }
                val childHeightMeasureSpec: Int =
                    if (lp.height == FrameLayout.LayoutParams.MATCH_PARENT) {
                        val height = max(
                            0,
                            measuredHeight -
                                    lp.topMargin - lp.bottomMargin,
                        )
                        MeasureSpec.makeMeasureSpec(
                            height,
                            MeasureSpec.EXACTLY,
                        )
                    } else {
                        getChildMeasureSpec(
                            heightMeasureSpec,
                            lp.topMargin + lp.bottomMargin,
                            lp.height,
                        )
                    }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val left = 0 + paddingLeft
        val top = 0 + paddingTop

        if (contentViewResID != -1) {
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (contentView == null && child.id == contentViewResID) {
                    contentView = child
                    contentView?.isClickable = true
                }
            }
        } else {
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (contentView == null && child !is SwipeMenuView) {
                    contentView = child
                    contentView?.isClickable = true
                }
            }
        }
        // contentView
        var cRight: Int
        val contentView = contentView ?: return
        val contentViewLp = layoutParams as MarginLayoutParams
        with(contentView) {
            val cTop = top + contentViewLp.topMargin
            val cLeft = left + contentViewLp.leftMargin
            cRight = left + contentViewLp.leftMargin + measuredWidth
            val cBottom = cTop + measuredHeight
            layout(cLeft, cTop, cRight, cBottom)
        }
        with(leftMenuView) {
            val leftViewLp = layoutParams as MarginLayoutParams
            val lTop = top + leftViewLp.topMargin
            val lLeft =
                0 - measuredWidth + leftViewLp.leftMargin + leftViewLp.rightMargin
            val lRight = 0 - leftViewLp.rightMargin
            val lBottom = lTop + measuredHeight
            layout(lLeft, lTop, lRight, lBottom)
        }
        with(rightMenuView) {
            val rightViewLp = layoutParams as MarginLayoutParams
            val lTop = top + rightViewLp.topMargin
            val lLeft = contentView.right + contentViewLp.rightMargin + rightViewLp.leftMargin
            val lRight = lLeft + measuredWidth
            val lBottom = lTop + measuredHeight
            layout(lLeft, lTop, lRight, lBottom)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val contentView = contentView ?: return super.dispatchTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isSwiping = false
                lastP.set(ev.rawX, ev.rawY)
                firstP.set(ev.rawX, ev.rawY)
                if (lastInstance != null) {
                    if (lastInstance?.get() !== this) {
                        lastInstance?.get()?.close()
                    }
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val distanceX = lastP.x - ev.rawX
                val distanceY = lastP.y - ev.rawY
                if (abs(distanceY) > scaledTouchSlop && abs(distanceY) > abs(distanceX)) {
                    println()
                } else {
                    scrollBy(distanceX.toInt(), 0)
                    if (scrollX < 0) {
                        if (!leftMenuEnable || leftMenuView.childCount == 0) {
                            scrollTo(0, 0)
                        } else { // 左滑
                            if (scrollX < leftMenuView.left) {
                                scrollTo(leftMenuView.left, 0)
                            }
                        }
                    } else if (scrollX > 0) {
                        if (!rightMenuEnable || rightMenuView.childCount == 0) {
                            scrollTo(0, 0)
                        } else {
                            val contentViewLp = contentView.layoutParams as MarginLayoutParams
                            if (scrollX > rightMenuView.right - contentView.right - contentViewLp.rightMargin) {
                                scrollTo(
                                    rightMenuView.right - contentView.right - contentViewLp.rightMargin,
                                    0,
                                )
                            }
                        }
                    }
                    if (abs(distanceX) > scaledTouchSlop) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    lastP.set(ev.rawX, ev.rawY)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                /*val width = mLeftView?.width ?: 0
                if (ev.x > width) {
                    handlerSwipeMenu(CLOSE)
                }*/
                /*val width = mRightView?.width ?:0
                if (ev.x > width) {
                    handlerSwipeMenu(CLOSE)
                }*/
                finalDistanceX = firstP.x - ev.rawX
                if (abs(finalDistanceX) > scaledTouchSlop) {
                    isSwiping = true
                }
                val state = isShouldOpen(scrollX)
                handlerSwipeMenu(state, state == menuState)
            }

            else -> {
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun isClickOnContentViewLeft(x: Float): Boolean = x > leftMenuView.width

    private fun isClickOnContentViewRight(x: Float): Boolean {
        val contentView = contentView ?: return false
        return x < contentView.width - rightMenuView.width
    }

    private fun isClickOnContentView(x: Float): Boolean {
        val result = isShouldOpen(scrollX)
        if (result == MenuState.LEFT_OPEN) {
            return isClickOnContentViewLeft(x)
        } else if (result == MenuState.RIGHT_OPEN) {
            return isClickOnContentViewRight(x)
        }
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            }

            MotionEvent.ACTION_MOVE -> {
                if (abs(finalDistanceX) > scaledTouchSlop) {
                    // mScaledTouchSlop
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // contentView
                if (isSwiping) {
                    isSwiping = false
                    finalDistanceX = 0f
                    return true
                }
                val isClick = isClickOnContentView(event.x)
                if (isClick) {
                    handlerSwipeMenu(MenuState.CLOSE)
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    fun closeMenu() {
        handlerSwipeMenu(MenuState.CLOSE)
    }

    fun openLeftMenu() {
        handlerSwipeMenu(MenuState.LEFT_OPEN)
    }

    fun openRightMenu() {
        handlerSwipeMenu(MenuState.RIGHT_OPEN)
    }

    private val MenuState.isClose: Boolean
        get() = this == MenuState.CLOSE

    private val MenuState.isOpen: Boolean
        get() = !this.isClose

    private val MenuState.isLeftOpen: Boolean
        get() = this == MenuState.LEFT_OPEN

    private val MenuState.isRightOpen: Boolean
        get() = this == MenuState.RIGHT_OPEN

    val state: MenuState
        get() = menuState

    val isClosed: Boolean
        get() = menuState.isClose

    val isOpened: Boolean
        get() = menuState.isOpen

    val isLeftOpened: Boolean
        get() = menuState.isLeftOpen

    val isRightOpened: Boolean
        get() = menuState.isRightOpen

    /**
     *
     * @param target
     */
    private fun handlerSwipeMenu(target: MenuState, isQuiet: Boolean = false) {
        val contentView = contentView ?: return
        when (target) {
            MenuState.LEFT_OPEN -> {
                scroller.startScroll(scrollX, 0, leftMenuView.left - scrollX, 0)
                lastInstance = WeakReference(this)
                if (!isQuiet) {
                    onMenuLeftOpenedListener.forEach {
                        it?.invoke(this)
                    }
                }
            }

            MenuState.RIGHT_OPEN -> {
                val contentViewLp = contentView.layoutParams as MarginLayoutParams
                scroller.startScroll(
                    scrollX,
                    0,
                    rightMenuView.right - contentView.right - contentViewLp.rightMargin - scrollX,
                    0,
                )
                lastInstance = WeakReference(this)
                if (!isQuiet) {
                    onMenuRightOpenedListener.forEach {
                        it?.invoke(this)
                    }
                }
            }

            else -> {
                scroller.startScroll(scrollX, 0, -scrollX, 0)
                lastInstance = null
                if (!isQuiet) {
                    onMenuClosedListener.forEach {
                        it?.invoke(this)
                    }
                }
            }
        }
        menuState = target
        invalidate()
    }

    override fun computeScroll() {
        // Scroller
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            // invalidate()->onDraw()->computeScroll()
            invalidate()
        }
    }

    /**
     *
     * @param
     * @param scrollX
     * @return
     */
    private fun isShouldOpen(scrollX: Int): MenuState {
        if (scaledTouchSlop >= abs(finalDistanceX)) {
            return lastInstance?.get()?.state ?: MenuState.CLOSE
        }
        // Log.i(TAG, ">>>finalyDistanceX:$finalDistanceX")
        if (finalDistanceX < 0) {
            if (scrollX < 0) {
                if (abs(leftMenuView.width * menuOpenThreshold) < abs(scrollX)) {
                    return MenuState.LEFT_OPEN
                }
            }
            if (scrollX > 0) {
                return MenuState.CLOSE
            }
        } else if (finalDistanceX > 0) {
            if (scrollX > 0) {
                if (abs(rightMenuView.width * menuOpenThreshold) < abs(scrollX)) {
                    return MenuState.RIGHT_OPEN
                }
            }

            if (scrollX < 0) {
                return MenuState.CLOSE
            }
        }
        return MenuState.CLOSE
    }

    override fun onDetachedFromWindow() {
        if (this === lastInstance) {
            handlerSwipeMenu(MenuState.CLOSE)
        }
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this === lastInstance) {
            handlerSwipeMenu(menuState)
        }
    }

    private val onMenuClosedListener: MutableList<OnMenuClosedListener> = mutableListOf()
    fun addOnMenuClosedListener(listener: OnMenuClosedListener) {
        onMenuClosedListener.add(listener)
    }

    fun removeOnMenuClosedListener(listener: OnMenuClosedListener) {
        onMenuClosedListener.remove(listener)
    }

    fun clearOnMenuClosedListener() {
        onMenuClosedListener.clear()
    }

    var onMenuLeftOpenedListener: MutableList<OnMenuLeftOpenedListener> = mutableListOf()
    fun addOnMenuLeftOpenedListener(listener: OnMenuLeftOpenedListener) {
        onMenuLeftOpenedListener.add(listener)
    }

    fun removeOnMenuLeftOpenedListener(listener: OnMenuLeftOpenedListener) {
        onMenuLeftOpenedListener.remove(listener)
    }

    fun clearOnMenuLeftOpenedListener() {
        onMenuLeftOpenedListener.clear()
    }

    var onMenuRightOpenedListener: MutableList<OnMenuRightOpenedListener> = mutableListOf()
    fun addOnMenuRightOpenedListener(listener: OnMenuRightOpenedListener) {
        onMenuRightOpenedListener.add(listener)
    }

    fun removeOnMenuRightOpenedListener(listener: OnMenuRightOpenedListener) {
        onMenuRightOpenedListener.remove(listener)
    }

    fun clearOnMenuRightOpenedListener() {
        onMenuRightOpenedListener.clear()
    }

    private fun close() {
        handlerSwipeMenu(MenuState.CLOSE)
    }

    companion object {
        private const val TAG = "SwipeMenuLayout"
        var lastInstance: WeakReference<SwipeMenuLayout?>? = null
    }
}
