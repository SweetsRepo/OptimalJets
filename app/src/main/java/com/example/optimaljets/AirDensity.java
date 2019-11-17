package com.example.optimaljets;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

public class AirDensity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor pressure;

    @Override
    public final void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assign instances of the sensor manager and pressure sensor on the device
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        TextView jetSize = (TextView)findViewById(R.id.jetSize);
        jetSize.setText(String.valueOf(1000.0));
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy){
        // Just needs to be implemented to make SensorEventListener Interface happy
    }

    @Override
    public final void onSensorChanged(SensorEvent event){
        if(event.sensor.getType() != Sensor.TYPE_PRESSURE)
            return;
        float millibarsOfPressure = event.values[0];
        // TODO: Map conversion of pressure im mBar to %
        TextView jetSize = (TextView)findViewById(R.id.jetSize);
        jetSize.setText(String.valueOf(100.0));
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }


}
