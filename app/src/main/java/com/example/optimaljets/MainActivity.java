package com.example.optimaljets;

import java.lang.Math;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    // Each of the required sensor types for computing air density as a percentage
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private Sensor humiditySensor;
    private Sensor temperatureSensor;
    private float pressure;
    private float humidity;
    private float temperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Assign instances of the sensor manager and pressure sensor on the device
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy){
        // Just needs to be implemented to make SensorEventListener Interface happy
    }

    @Override
    public final void onSensorChanged(SensorEvent event){
        int sensorType = event.sensor.getType();
        if(sensorType == Sensor.TYPE_PRESSURE) {
            pressure = event.values[0];
        } else if(sensorType == Sensor.TYPE_RELATIVE_HUMIDITY) {
            humidity = event.values[0];
        } else if(sensorType == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            temperature = event.values[0];
        } else {
            return;
        }
        // https://www.omnicalculator.com/physics/air-density#how-to-calculate-the-air-density
        // https://developer.android.com/guide/topics/sensors/sensors_environment.html#java
        // Constants used in the calculation
        final double RD = 287.058;
        final double RV = 461.495;
        final double M = 17.62;
        final double TN = 243.12;
        double dp_comp = Math.log(humidity/100) + (M * temperature) / (TN + temperature);
        double dp = TN * dp_comp / (M - dp_comp);
        double p1 = 6.1078 * Math.pow(10, 7.5 * dp / (dp + 237.3));
        double pv = p1 * humidity;
        double pd = pressure - pv;
        double p = (pd / (RD * (temperature+273.15))) + (pv / (RV * (temperature+273.15)));

        TextView jetSize = (TextView)findViewById(R.id.jetSize);
        jetSize.setText(String.valueOf(p));
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
