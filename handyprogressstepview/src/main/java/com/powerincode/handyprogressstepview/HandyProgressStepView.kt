package com.powerincode.handyprogressstepview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.lang.IllegalStateException
import java.lang.ref.WeakReference
import java.util.Collections.emptyList

class HandyProgressStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var adapter: Adapter<*, *>? = null
    var layoutManager: LayoutManager? = null
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

        val layoutManager = layoutManager
        val adapter = adapter

        if (layoutManager != null && adapter != null) {
            post { layoutManager.layoutChildren(adapter, this) }
        }
    }

    fun setState(state: State) {
        this.points = state.points
        this.indexActivePoint = state.indexActivePoint

        removeAllViews()

        val inflater = LayoutInflater.from(context)
        for (i in 0 until points.size) {
//            inflater.inflate(R.layout.view_progress_point, this, true)

            if (i < points.size - 1) {
//                inflater.inflate(R.layout.view_progress_interval, this, true)
            }
        }

        applyStyle()
//        recalculateItems()
    }
    
    private fun applyStyle() {
        for ((pointIndex, point) in points.withIndex()) {
            val pointViewIndex = pointIndex * 2
            val pointView = getChildAt(pointViewIndex)
            configurePoint(pointIndex, point, pointView)

            if (pointIndex < points.size - 1) {
                val intervalView = getChildAt(pointViewIndex + 1)
//                configureInterval(intervalView, pointIndex)
            }
        }
    }

    private fun configurePoint(position: Int, point: Point, pointView: View) {
        when {
//            position < indexActivePoint -> setPointStyle(point, pointView, style.completePointStyle)
//            position == indexActivePoint -> setPointStyle(point, pointView, style.activePointStyle)
//            else -> setPointStyle(point, pointView, style.inactivePointStyle)
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
//                    positionStyle.configurePoint(this, child, i, points.size)
                } else {
//                    positionStyle.configureInterval(this, child, i, points.size)
                }
            }

            isRecalculatingInProcess = childWidth != currentPointWidth
            requestLayout()
        }

        fun getView(position: Int): View {
            val adapter = adapter ?: throw IllegalStateException("$TAG: Adapter must be set, but it null")
            var holder: Adapter.Holder? = adapter.getViewHolder(position)

            if (position % 2 == 0) {
                val type = adapter.getStepType(position)
                holder = adapter.getViewHolder(position)

                if (holder == null) {
                    holder = adapter.createStepViewHolder(this, type)
                }

                adapter.bindStepViewHolder(holder, position / 2)
            } else {
                holder = adapter.getViewHolder(position)

                if (holder == null) {
                    holder = adapter.createIntervalViewHolder(this)
                }

                adapter.bindStepViewHolder(holder, (position - 1 ) / 2)
            }

            return holder.view
        }
    }

    abstract class Adapter<T: Any, V: Adapter.StepHolder<T>>(context: Context) {
        private val contextRef: WeakReference<Context> = WeakReference(context)

        protected val context: Context? get() = contextRef.get()
        protected var steps: List<T> = emptyList()
        set(value) {
            field = value

            stepViewHolders.clear()
            intervalViewHolders.clear()

        }
        protected val stepCount: Int = steps.size

        private val stepViewHolders: MutableList<StepHolder<T>> = emptyList()
        private val intervalViewHolders: MutableList<IntervalHolder> = emptyList()

        protected abstract fun onCreateStepHoler(parent: ViewGroup, stepType: StepType): V
        protected  abstract fun onBindStepHolder(stepHolder: V, position: Int)

        protected abstract fun onCreateIntervalHolder(parent: ViewGroup): IntervalHolder
        protected abstract fun onBindIntervalHolder(intervalHolder: IntervalHolder, intervalPosition: Int)

        fun getStepType(position: Int): StepType {
            return when (position) {
                0 -> StepType.FIRST
                steps.size - 1 -> StepType.LAST
                else -> StepType.MIDDLE
            }
        }

        fun getViewHolder(position: Int): Holder? {
            return if (position % 2 == 0) {
                getStepView(position)
            } else {
                getIntervalView(position)
            }
        }

        fun createStepViewHolder(handyProgressStepView: HandyProgressStepView, type: StepType): StepHolder<*> {
            return onCreateStepHoler(handyProgressStepView, type)
        }

        fun createIntervalViewHolder(handyProgressStepView: HandyProgressStepView): IntervalHolder {
            return onCreateIntervalHolder(handyProgressStepView)
        }

        @Suppress("UNCHECKED_CAST")
        fun bindStepViewHolder(stepHolder: Holder, position: Int) {
            return onBindStepHolder(stepHolder as V, position)
        }

        fun bindIntervalViewHolder(intervalHolder: IntervalHolder, position: Int) {
            return onBindIntervalHolder(intervalHolder, position)
        }

        private fun getStepView(globalPosition: Int): Holder? {
            return stepViewHolders.getOrNull(globalPosition / 2)
        }

        private fun getIntervalView(globalPosition: Int): Holder? {
            return intervalViewHolders.getOrNull((globalPosition - 1) / 2)
        }


        interface Holder {
            val view: View
        }

        abstract class StepHolder<T: Any>(override val view: View): Holder {
            protected lateinit var step: T

            open fun bind(step: T) {
                this.step = step
            }
        }

        abstract class IntervalHolder(override val view: View): Holder {
            open fun bind() { }
        }

        enum class StepType {
            FIRST, MIDDLE, LAST
        }
    }

    abstract class LayoutManager {
        fun onAttachView() {}
        fun onDetachView() {}

        abstract fun layoutChildren(adapter: Adapter<*, *>, handyProgressStepView: HandyProgressStepView)
    }

    data class Point(val title: String, val subtitle: String = "")
    data class State(val points: List<Point>, val indexActivePoint: Int = 0)

    companion object {
        const val TAG: String = "HandyProgressStepView"
    }
}