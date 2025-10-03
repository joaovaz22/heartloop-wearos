/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.pixelapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.pixelapp.presentation.theme.PixelAppTheme
import android.os.Vibrator
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Scaffold
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.em
import androidx.wear.compose.material.ButtonDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        if (checkSelfPermission(android.Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.BODY_SENSORS), 1)
        }
        setContent {
            WearApp()
        }
    }

}

@Composable
fun WearApp() {

    val isVibrating = remember { mutableStateOf(false) } // Track vibration state
    val context = LocalContext.current
    var bpm by remember { mutableIntStateOf(60) }   // fallback until first read
    val highbpm = (bpm * 1.3).coerceIn(80.0, 105.0)
    val lowbpm = (bpm * 0.7).coerceIn(40.0, 65.0)

    fun readHeartRateOnce() {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val hrSensor = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE) ?: return

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val value = event?.values?.getOrNull(0)?.toInt() ?: return
                if (value > 0) {
                    bpm = value
                    sm.unregisterListener(this)   // stop listening after first valid read
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, hrSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    PixelAppTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101820)) // dark background
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape) // subtle ring
                .padding(20.dp) // inner margin for round watch
        ) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val screenHeight = configuration.screenHeightDp.dp

            // 🔹 scale sizes relative to screen
            val buttonHeight = screenHeight * 0.12f   // slim top/bottom buttons
            val circleSize = screenWidth * 0.20f     // circle buttons size

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Read HR button
                Button(
                    onClick = { readHeartRateOnce() },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3F51B5)),
                ) {
                    Text("Read HR", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Heart rate display
                Text(
                    fontSize = 3.5.em,
                    text = "${bpm.toInt()}",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
                Text(
                    fontSize = 2.0.em,
                    text = "bpm",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Vibration buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            triggerVibration(context, lowbpm)
                            isVibrating.value = true
                        },
                        modifier = Modifier.size(circleSize),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3))
                    ) {
                        Text("–30%", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            triggerVibration(context, highbpm)
                            isVibrating.value = true
                        },
                        modifier = Modifier.size(circleSize),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800))
                    ) {
                        Text("+30%", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stop button
                Button(
                    onClick = {
                        stopVibration(context)
                        isVibrating.value = false
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isVibrating.value) Color.Red else Color.DarkGray
                    )
                ) {
                    Text("Stop", fontSize = 14.sp)
                }
            }
        }
    }
}

fun triggerVibration(context: Context, bpm: Double) {
    stopVibration(context)
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val intervalMs = (60000 / bpm).toLong()
    val pattern = longArrayOf(0, 50, intervalMs - 50)
    vibrator.vibrate(
        VibrationEffect.createWaveform(pattern, 0)
    )
}

fun stopVibration(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.cancel()
}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}