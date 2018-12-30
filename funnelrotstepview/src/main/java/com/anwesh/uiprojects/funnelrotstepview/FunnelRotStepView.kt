package com.anwesh.uiprojects.funnelrotstepview

/**
 * Created by anweshmishra on 30/12/18.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.content.Context
import android.app.Activity
import android.util.Log

val funnelNodes : Int = 5
val ballNodes : Int = 3
val lines : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val sizeFactor : Float = 2.7f
val strokeFactor : Int = 90
val foreColor : Int = Color.parseColor("#2980b9")
val backColor : Int = Color.parseColor("#BDBDBD")
val ballColor : Int = Color.parseColor("#e74c3c")

val DELAY : Long = 25

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse()).toFloat()
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this/scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = ((1 - scaleFactor()) * a.inverse()) + (scaleFactor() * b.inverse())
fun Float.updateScale(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * scGap * dir

fun Float.updatePos(s : Float, scale : Float) : Float = this + (s - this) * scale

fun Canvas.drawFRSNode(i : Int, scale : Float, paint : Paint) {
    //Log.d("drawing node", "$i drawn")
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (funnelNodes + 1)
    val size : Float = gap / sizeFactor
    val kSize : Float = size/2
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    val ox : Float = -size/4
    val sx : Float = -size/2
    save()
    translate(gap * (i + 1), h/2)
    rotate(90f * sc2)
    for (j in 0..(lines - 1)) {
        val sc : Float = sc1.divideScale(j, lines)
        save()
        scale(1f - 2 * j, 1f)
        drawLine(ox, -size, ox, kSize, paint)
        drawLine(ox, kSize, ox.updatePos(sx, sc), size, paint)
        restore()
    }
    restore()
}

fun Canvas.drawBallNode(scale : Float, paint : Paint) {
    paint.color = ballColor
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (funnelNodes + 1)
    val size : Float = gap / sizeFactor
    val ballR : Float = size / 5
    val ox : Float = -ballR
    val dx : Float = w + ballR
    drawCircle(ox.updatePos(dx, scale), h/2, ballR, paint)
}

class FunnelRotStepView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f, var a : Int = 1, var b : Int = 1) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateScale(dir, a, b)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(DELAY)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    open class Node(var i : Int = 0, var nodes : Int, var state : State = State(), var cb : (Canvas, Paint, Int, Float) -> Unit,
                    var dfnNode : Node? = null) {

        protected var prev : Node? = null
        protected var next : Node? = null

        init {
            addNeighbor()
        }


        fun addNeighbor() {
            if (i < nodes - 1) {
                Log.d("adding neighbor for ", "$i")
                next = Node(i = i + 1, nodes = nodes, cb = cb, dfnNode = dfnNode)
                next?.prev = this
            } else {
                next = dfnNode
                next?.prev = this
                Log.d("next node is", "${next?.i}")
            }
        }

        open fun draw(canvas : Canvas, paint : Paint) {
            cb(canvas, paint, i, state.scale)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : Node {
            var curr : Node? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    class FRSNode : Node(nodes = funnelNodes, cb = {canvas, paint, i, scale ->
        Log.d("drawing ", "$i node")
        canvas.drawFRSNode(i, scale, paint)
    }, dfnNode = BallNode())

    class BallNode : Node(nodes = ballNodes, cb = {canvas, paint, i, scale ->
        canvas.drawBallNode(scale, paint)
    })

    data class FunnelRotStep(var i : Int) {

        private var root : Node = FRSNode()
        private var curr : Node = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : FunnelRotStepView) {
        private val frs : FunnelRotStep = FunnelRotStep(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            frs.draw(canvas, paint)
            animator.animate {
                frs.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            frs.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : FunnelRotStepView {
            val view : FunnelRotStepView = FunnelRotStepView(activity)
            activity.setContentView(view)
            return view
        }
    }
}