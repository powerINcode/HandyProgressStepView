package com.powerincode.handyprogressstepview

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import java.util.Collections.emptyList

class HandyProgressStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var style: Style = Style()
        set(value) {
            field = value
            applyStyle()
        }

    var positionStyle: PositionStyle = PositionStyle.Default
        set(value) {
            field = value

            isRecalculatingInProcess = true
            recalculateItems()
        }

    private var points: List<Point> = emptyList()
    private var isRecalculatingInProcess: Boolean = false
    private var indexActivePoint: Int = 0
        set(value) {
            if (points.isNotEmpty()) {
                if (value < 0 || value > points.size - 1) {
                    throw ArrayIndexOutOfBoundsException("Index: $value, Size: ${points.size}")
                }
            } else {
//                Timber.d("ProgressStatusView: Trying to set active index to empty set of points")
            }

            field = value
            invalidate()
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        post {
            recalculateItems()
        }
    }

    fun setState(state: State) {
        this.points = state.points
        this.indexActivePoint = state.indexActivePoint

        removeAllViews()

        val inflater = LayoutInflater.from(context)
        for (i in 0 until points.size) {
            inflater.inflate(R.layout.view_progress_point, this, true)

            if (i < points.size - 1) {
                inflater.inflate(R.layout.view_progress_interval, this, true)
            }
        }

        applyStyle()
        recalculateItems()
    }

    fun style(block: Style.() -> Unit) {
        style.block()
        applyStyle()
    }
    
    private fun applyStyle() {
        for ((pointIndex, point) in points.withIndex()) {
            val pointViewIndex = pointIndex * 2
            val pointView = getChildAt(pointViewIndex)
            configurePoint(pointIndex, point, pointView)

            if (pointIndex < points.size - 1) {
                val intervalView = getChildAt(pointViewIndex + 1)
                configureInterval(intervalView, pointIndex)
            }
        }
    }

    private fun configurePoint(position: Int, point: Point, pointView: View) {
        when {
            position < indexActivePoint -> setPointStyle(point, pointView, style.completePointStyle)
            position == indexActivePoint -> setPointStyle(point, pointView, style.activePointStyle)
            else -> setPointStyle(point, pointView, style.inactivePointStyle)
        }
    }

    private fun setPointStyle(point: Point, pointView: View, style: Style.Point) {
        val dotView: View = pointView.point
        val titleView: TextView = pointView.title
        val subtitleView: TextView = pointView.subtitle

        titleView.text = point.title
        subtitleView.text = point.subtitle

        dotView.setBackgroundResource(style.pointDrawable)

        titleView.setTextColorRes(style.titleColor)
        titleView.typeface = TypefaceUtil.getTypefaceByPath(context, style.titleTypeface)
        setPaintFlags(titleView, style.titleTypefaceFlags)

        subtitleView.setTextColorRes(style.subtitleColor)
        subtitleView.typeface = TypefaceUtil.getTypefaceByPath(context, style.subtitleTypeface)
        setPaintFlags(subtitleView, style.subtitleTypefaceFlags)
    }

    private fun configureInterval(intervalView: View, intervalIndex: Int) {
        intervalView.setBackgroundResource(if (intervalIndex < indexActivePoint) style.interval.activeColor else style.interval.inactiveColor)
    }

    private fun setPaintFlags(textView: TextView, flags: Int?) {
        textView.paintFlags = if (flags != null) {
            if ((textView.paintFlags and flags) != flags) {
                textView.paintFlags xor flags
            } else {
                textView.paintFlags
            }
        } else {
            0
        }
    }

    private fun recalculateItems() {
        if (width == 0 || childCount == 0) {
            return
        }

        val childWidth = width / points.size
        val currentPointWidth = getChildAt(0).width
        if (childWidth != currentPointWidth || isRecalculatingInProcess) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val layoutParams = child.layoutParams
                layoutParams.width = childWidth
                child.layoutParams = layoutParams

                if (i % 2 == 0) {
                    positionStyle.configurePoint(this, child, i, points.size)
                } else {
                    positionStyle.configureInterval(this, child, i, points.size)
                }
            }

            isRecalculatingInProcess = childWidth != currentPointWidth
            requestLayout()
        }
    }

    data class Style(
        val completePointStyle: Point = Point(
            R.drawable.progress_completed_indicator,
            R.color.cool_gray,
            R_Regular,
            Paint.STRIKE_THRU_TEXT_FLAG,
            R.color.cool_gray,
            R_Regular,
            Paint.STRIKE_THRU_TEXT_FLAG
        ),

        val activePointStyle: Point = Point(
            R.drawable.progress_active_indicator,
            R.color.color_black,
            R_Regular,
            null,
            R.color.cool_gray,
            R_Regular,
            null
        ),

        val inactivePointStyle: Point = Point(
            R.drawable.progress_inactive_indicator,
            R.color.cool_gray,
            R_Regular,
            null,
            R.color.cool_gray,
            R_Regular,
            null
        ),

        val interval: Interval = Interval(
            R.color.color_accent,
            R.color.background_gray
        )
    ) {
        class Point(
            @DrawableRes var pointDrawable: Int,
            @ColorRes var titleColor: Int,
            var titleTypeface: String,
            var titleTypefaceFlags: Int?,
            @ColorRes var subtitleColor: Int,
            var subtitleTypeface: String,
            var subtitleTypefaceFlags: Int?
        ) {
            fun style(block: Point.() -> Unit) {
                this.block()
            }
        }

        class Interval(
            var activeColor: Int,
            var inactiveColor: Int
        ) {
            fun style(block: Interval.() -> Unit) {
                this.block()
            }
        }
    }

    sealed class PositionStyle {
        abstract fun configurePoint(parent: ViewGroup, pointView: View, position: Int, points: Int)
        abstract fun configureInterval(
            parent: ViewGroup,
            intervalView: View,
            position: Int,
            points: Int
        )

        object Default : PositionStyle() {
            override fun configurePoint(
                parent: ViewGroup,
                pointView: View,
                position: Int,
                points: Int
            ) {
                if (position == 0) {
                    return
                }

                val offset = (position / 2) * pointView.width
                pointView.x = offset.toFloat()
            }

            override fun configureInterval(
                parent: ViewGroup,
                intervalView: View,
                position: Int,
                points: Int
            ) {
                val prevPoint = parent.getChildAt(position - 1)
                val dotView = prevPoint.point

                intervalView.x = (prevPoint.x + (prevPoint.width / 2)) + dotView.width / 2
                (intervalView.layoutParams as LayoutParams).width = prevPoint.width
            }
        }

        object ThreeStepFirstLastStick : PositionStyle() {
            override fun configurePoint(
                parent: ViewGroup,
                pointView: View,
                position: Int,
                points: Int
            ) {
                if (points > 3) {
                    throw IllegalStateException("ThreeStepFirstLastStick style designed only for 3 point. Received: $points")
                }

                val dotView: View = pointView.point
                val titleView: TextView = pointView.title
                val subtitleView: TextView = pointView.subtitle

                when (position) {
                    0 -> {
                        (dotView.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
                        titleView.gravity = Gravity.START
                        subtitleView.gravity = Gravity.START
                    }
                    parent.childCount - 1 -> {
                        pointView.x = parent.width - pointView.width.toFloat()

                        (dotView.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
                        titleView.gravity = Gravity.END
                        subtitleView.gravity = Gravity.END
                    }
                    else -> {
                        val offset = (position / 2) * pointView.width
                        pointView.x = offset.toFloat()
                    }
                }
            }

            override fun configureInterval(
                parent: ViewGroup,
                intervalView: View,
                position: Int,
                points: Int
            ) {
                if (points > 3) {
                    throw IllegalStateException("ThreeStepFirstLastStick style designed only for 3 point. Received: $points")
                }

                val prevPoint = parent.getChildAt(position - 1)
                val dotView = prevPoint.point

                when (position) {
                    1 -> {
                        intervalView.x = dotView.width.toFloat()
                        intervalView.layoutParams.width = ((prevPoint.width * 1.5) - dotView.width * 1.5).toInt()
                    }

                    parent.childCount - 2 -> {
                        intervalView.x = prevPoint.x + (prevPoint.width / 2) + dotView.width / 2
                        intervalView.layoutParams.width = ((prevPoint.width * 1.5) - dotView.width * 1.5).toInt()
                    }
                    else -> {

                        intervalView.x = (prevPoint.x + (prevPoint.width / 2)) + dotView.width / 2
                        intervalView.layoutParams.width = prevPoint.width - dotView.width
                    }
                }
            }

        }
    }

    data class Point(val title: String, val subtitle: String = "")
    data class State(val points: List<Point>, val indexActivePoint: Int = 0)
}