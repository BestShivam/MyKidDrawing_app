package com.example.kiddrawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

class DrawView(context : Context,attr : AttributeSet)
    : View(context,attr) {

    private lateinit var drawPath :CustomPath
    private var canvasBitmap : Bitmap? = null
    lateinit  var canvasPaint : Paint
    private var myCanvas :Canvas ? = null
    lateinit var drawPaint : Paint
    private var color = Color.RED
    private var mBrushSize = 0f
    private var mPaths = ArrayList<CustomPath>()
    private var mUndoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }


    private fun setUpDrawing(){
        drawPath = CustomPath(color,mBrushSize)
        drawPaint = Paint()
        drawPaint.color = color
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
        //mBrushSize = 20f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        myCanvas = Canvas(canvasBitmap!!)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!,0f,0f,canvasPaint)



        for (path in mPaths){
            drawPaint.color = path.color
            drawPaint.strokeWidth = path.brushThickness
            canvas.drawPath(path,drawPaint)
        }

        if(!drawPath.isEmpty){
            drawPaint.color = drawPath.color
            drawPaint.strokeWidth = drawPath.brushThickness
            canvas.drawPath(drawPath,drawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                drawPath.color = color
                drawPath.brushThickness = mBrushSize
                drawPath.reset()
                drawPath.moveTo(touchX!!,touchY!!)
            }
            MotionEvent.ACTION_MOVE ->{
                drawPath.lineTo(touchX!!,touchY!!)
                //drawPath.addCircle(touchX!!,touchY!!,10f,Path.Direction.CW)
                //drawPath.addRect(touchX!!,150f+touchX!!,200f+touchX!!,200f+touchY!!,Path.Direction.CW)
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(drawPath)
                drawPath = CustomPath(color,mBrushSize)
            }
            else -> return false
        }
        invalidate()

        return true
    }

    fun setBrushSize(newSize : Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize,resources.displayMetrics)
        drawPaint.strokeWidth = mBrushSize
    }

    internal inner class CustomPath(var color:Int,
                                    var brushThickness:Float): Path()

    fun getBrushColor():Int{
        return color
    }
    fun setBrushColor(newColor : String){
        color = Color.parseColor(newColor)
        drawPath.color = color

    }
    fun undo(){
        if(mPaths.isNotEmpty()){
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }

    }
    fun redo(){
        // mPath.add[mUndoPaths[lastElement]]
        if(mUndoPaths.isNotEmpty()){

            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size - 1))
            invalidate()
        }

    }
}