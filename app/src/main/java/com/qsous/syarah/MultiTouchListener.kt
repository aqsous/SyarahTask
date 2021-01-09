package com.qsous.syarah

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.RelativeLayout
import com.qsous.syarah.Vector2D.Companion.getAngle

class MultiTouchListener(
    parentView: RelativeLayout,
    photoEditImageView: ImageView
) : OnTouchListener {
    private val isRotateEnabled = true
    private val isTranslateEnabled = true
    private val isScaleEnabled = true
    private val minimumScale = 0.5f
    private val maximumScale = 10.0f
    private var mActivePointerId = INVALID_POINTER_ID
    private var mPrevX = 0f
    private var mPrevY = 0f
    private var mPrevRawX = 0f
    private var mPrevRawY = 0f
    private val mScaleGestureDetector: ScaleGestureDetector
    private val location = IntArray(2)
    private val outRect: Rect
    private val photoEditImageView: ImageView
    private val parentView: RelativeLayout
    private val mIsTextPinchZoomable = true

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(view, event)
        if (!isTranslateEnabled) {
            return true
        }
        val action = event.action
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()
        when (action and event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mPrevY = event.y
                mPrevRawX = event.rawX
                mPrevRawY = event.rawY
                mActivePointerId = event.getPointerId(0)
                view.bringToFront()
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndexMove = event.findPointerIndex(mActivePointerId)
                if (pointerIndexMove != -1) {
                    val currX = event.getX(pointerIndexMove)
                    val currY = event.getY(pointerIndexMove)
                    if (!mScaleGestureDetector.isInProgress) {
                        adjustTranslation(view, currX - mPrevX, currY - mPrevY)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> mActivePointerId = INVALID_POINTER_ID
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
                if (!isViewInBounds(photoEditImageView, x, y)) {
                    view.animate().translationY(0f).translationY(0f)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndexPointerUp =
                    action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndexPointerUp)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndexPointerUp == 0) 1 else 0
                    mPrevX = event.getX(newPointerIndex)
                    mPrevY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    private fun isViewInBounds(view: View, x: Int, y: Int): Boolean {
        view.getDrawingRect(outRect)
        view.getLocationOnScreen(location)
        outRect.offset(location[0], location[1])
        return outRect.contains(x, y)
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var mPivotX = 0f
        private var mPivotY = 0f
        private val mPrevSpanVector = Vector2D()
        override fun onScaleBegin(view: View?, detector: ScaleGestureDetector?): Boolean {
            detector?.let {
                mPivotX = it.focusX
                mPivotY = it.focusY
                mPrevSpanVector.set(it.currentSpanVector)
            }
            return mIsTextPinchZoomable
        }

        override fun onScale(view: View?, detector: ScaleGestureDetector?): Boolean {
            val info = TransformInfo()
            detector?.let {
                info.deltaScale = if (isScaleEnabled) it.scaleFactor else 1.0f
                info.deltaAngle = if (isRotateEnabled) getAngle(
                    mPrevSpanVector,
                    it.currentSpanVector
                ) else 0.0f
                info.deltaX = if (isTranslateEnabled) it.focusX - mPivotX else 0.0f
                info.deltaY = if (isTranslateEnabled) it.focusY - mPivotY else 0.0f
            }
            info.pivotX = mPivotX
            info.pivotY = mPivotY
            info.minimumScale = minimumScale
            info.maximumScale = maximumScale
            move(view, info)
            return !mIsTextPinchZoomable
        }
    }

    private inner class TransformInfo {
        var deltaX = 0f
        var deltaY = 0f
        var deltaScale = 0f
        var deltaAngle = 0f
        var pivotX = 0f
        var pivotY = 0f
        var minimumScale = 0f
        var maximumScale = 0f
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
        private fun adjustAngle(mDegrees: Float): Float {
            var degrees = mDegrees
            if (degrees > 180.0f) {
                degrees -= 360.0f
            } else if (degrees < -180.0f) {
                degrees += 360.0f
            }
            return degrees
        }

        private fun move(view: View?, info: TransformInfo) {
            computeRenderOffset(view, info.pivotX, info.pivotY)
            adjustTranslation(view, info.deltaX, info.deltaY)
            var scale = (view?.scaleX ?: 0f) * info.deltaScale
            scale = info.minimumScale.coerceAtLeast(Math.min(info.maximumScale, scale))
            view?.scaleX = scale
            view?.scaleY = scale
            val rotation = adjustAngle((view?.rotation ?: 0f) + info.deltaAngle)
            view?.rotation = rotation
        }

        private fun adjustTranslation(view: View?, deltaX: Float, deltaY: Float) {
            val deltaVector = floatArrayOf(deltaX, deltaY)
            view?.matrix?.mapVectors(deltaVector)
            view?.translationX = (view?.translationX ?: 0f) + deltaVector[0]
            view?.translationY = (view?.translationY ?: 0f) + deltaVector[1]
        }

        private fun computeRenderOffset(view: View?, pivotX: Float, pivotY: Float) {
            if (view?.pivotX == pivotX && view.pivotY == pivotY) {
                return
            }
            val prevPoint = floatArrayOf(0.0f, 0.0f)
            view?.matrix?.mapPoints(prevPoint)
            view?.pivotX = pivotX
            view?.pivotY = pivotY
            val currPoint = floatArrayOf(0.0f, 0.0f)
            view?.matrix?.mapPoints(currPoint)
            val offsetX = currPoint[0] - prevPoint[0]
            val offsetY = currPoint[1] - prevPoint[1]
            view?.translationX = (view?.translationX ?: 0f) - offsetX
            view?.translationY = (view?.translationY ?: 0f) - offsetY
        }
    }

    init {
        mScaleGestureDetector = ScaleGestureDetector(ScaleGestureListener())
        this.parentView = parentView
        this.photoEditImageView = photoEditImageView
        outRect = Rect(0, 0, 0, 0)
    }
}