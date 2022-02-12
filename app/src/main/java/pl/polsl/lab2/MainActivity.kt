package pl.polsl.lab2

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.os.Bundle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

var tilt = 0.0

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(MyGLSurfaceView(applicationContext))

        this.sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
            sensorManager.registerListener(
                    this,
                    gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
            sensorManager.registerListener(
                    this,
                    gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        tilt = tilt + event.values[0] / 100
    }
}

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    var ratio = 0.0f
    var radius = 0.1f

    init {
        val mediaPlayer = MediaPlayer.create(context, R.raw.pilka)
        val doublePi :Float = PI.toFloat()*2
        val numberOfTriangles = 100
        val array = FloatArray(3 * numberOfTriangles + 6) { -1.0f }
        array[0] = 0.0f
        array[1] = 0.0f

        val displayWidth = Resources.getSystem().displayMetrics.widthPixels.toFloat()
        val displayHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()
        ratio = displayWidth/displayHeight
        radius = ratio * (0.1f)

        var angle = 0.0
        for (i in 1..numberOfTriangles + 1) {
            array[3 * i] = (radius * cos(i * doublePi / numberOfTriangles))
            array[3 * i + 1] = (radius * sin(i * doublePi / numberOfTriangles)*ratio)
            angle = angle + (doublePi / numberOfTriangles)
        }
        setRenderer(MyRenderer(numberOfTriangles, array, radius, mediaPlayer))
    }

    class MyRenderer(val number: Int, var array: FloatArray, val radius: Float,val mediaPlayer: MediaPlayer) : Renderer {

        val size = 3 * number + 6
        var speed = 0.0

        override fun onDrawFrame(gl: GL10?) {
            val buffer = ByteBuffer.allocateDirect(size * 4)
            buffer.order(ByteOrder.nativeOrder())
            val arrayBuffer = buffer.asFloatBuffer()
            arrayBuffer.put(array)
            arrayBuffer.position(0)

            gl?.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            gl?.glColor4f(0.0f, 0.0f, 1.0f, 1.0f)
            gl?.glLoadIdentity()
            gl?.glEnableClientState(GL10.GL_VERTEX_ARRAY)
            gl?.glVertexPointer(3, GL10.GL_FLOAT, 0, arrayBuffer)
            gl?.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, size / 3)
            gl?.glDisableClientState(GL10.GL_VERTEX_ARRAY)


            speed = speed + tilt / 10
            var finalSpeed = speed

            if (array[0] + finalSpeed > abs(1-radius)){
                finalSpeed = (abs(1-radius) - array[0]).toDouble()
                speed = 0.0
            }

            if (array[0] + finalSpeed < -abs(1-radius)){
                finalSpeed = (-abs(1-radius) - array[0]).toDouble()
                speed = 0.0

            }

            if (abs(array[0]) == abs(1-radius)) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    mediaPlayer.prepare()
                }
                mediaPlayer.start()
            }

            for (i in 0..number + 1)
                array[3 * i] += finalSpeed.toFloat()

        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            gl?.glViewport(0, 0, width, height)
            gl?.glMatrixMode(GL10.GL_PROJECTION)
            gl?.glLoadIdentity()
            GLU.gluPerspective(gl, 45.0f, 0.5f, -1.0f, -10.0f)
            gl?.glClearColor(0.0f, 1.0f, 0.5f, 1.0f)
        }

        override fun onSurfaceCreated( gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig? ) {
        }
    }
}
