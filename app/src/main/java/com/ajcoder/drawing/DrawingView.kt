package com.ajcoder.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    // An variable of CustomPath inner class to use it further.
    private var mDrawPath : CustomPath? = null

    private var mCanvasBitmap: Bitmap? = null // An instance of the Bitmap.
    private var mDrawPaint: Paint? = null  // The Paint class holds the style and color information about how to draw geometries, text and bitmaps.
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    /**
     * A variable for canvas which will be initialized later and used.
     *
     *The Canvas class holds the "draw" calls. To draw something, you need 4 basic components: A Bitmap to hold the pixels, a Canvas to host
     * the draw calls (writing into the bitmap), a drawing primitive (e.g. Rect,
     * Path, text, Bitmap), and a paint (to describe the colors and styles for the
     * drawing)
     */
    private var canvas:Canvas? = null
    // TODO(Step 2 : A variable for array list of undo paths.)
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

   fun onClickUndo(){

       if(mPaths.size > 0){
           mUndoPaths.add(mPaths.removeAt(mPaths.size -1))
           invalidate()
       }
    }
    private fun setUpDrawing() {

        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint?.color = color
        mDrawPaint?.style = Paint.Style.STROKE// This is to draw a STROKE style
        mDrawPaint?.strokeJoin =Paint.Join.ROUND // This is for store join
        mDrawPaint?.strokeCap = Paint.Cap.ROUND// This is for stroke Cap
        mCanvasPaint =Paint(Paint.DITHER_FLAG) // Paint flag that enables dithering when blitting.
//        mBrushSize = 10.toFloat()
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap =Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)


    }

//       /**
//     * This method is called when a stroke is drawn on the canvas
//     * as a part of the painting.
//     */

    override fun onDraw(canvas: Canvas) {
        Log.i("AJay","onDraw")
        super.onDraw(canvas)

        mCanvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, mCanvasPaint)
        }


        for(path in mPaths){
            mDrawPaint?.strokeWidth = path.brushThickness
            mDrawPaint?.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        if (!mDrawPath!!.isEmpty) {
            mDrawPaint?.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint?.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchx = event?.x
        val touchy = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath?.color =color
                mDrawPath?.brushThickness = mBrushSize

                mDrawPath?.reset()
                if(touchx!=null){
                    if(touchy!=null){
                        mDrawPath!!.moveTo(touchx, touchy)
                    }
                }

            }
            MotionEvent.ACTION_MOVE ->{
                if(touchx != null){
                    if(touchy != null){
                        mDrawPath!!.lineTo(touchx, touchy)
                    }
                }
            }

            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)
                mDrawPath =CustomPath(color, mBrushSize)
            }
            else -> return false
        }
            invalidate()

        return true
    }

    fun setSizeForBrush(newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics)
        mDrawPaint?.strokeWidth = mBrushSize

    }

    fun setColor(newColor:String){
        color = Color.parseColor(newColor)
        mDrawPaint?.color = color
    }

    internal inner class CustomPath(var color: Int,
        var brushThickness: Float): Path(){

        }

}