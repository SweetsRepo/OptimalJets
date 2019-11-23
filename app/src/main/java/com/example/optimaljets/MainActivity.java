package com.example.optimaljets;

import java.lang.Math;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Shared preferences store for baseline data
    private SharedPreferences baseline;
    private boolean blSet = false;
    private int blJetSize;
    private float blAirDensity;
    EditText baselineJetSize;
    EditText baselineAirDensity;

    // Each of the required sensor types for computing air density as a percentage
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private Sensor humiditySensor;
    private Sensor temperatureSensor;
    private float pressure;
    private float humidity;
    private float temperature;
    private double p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get previously stored baselines from shared preferences
        // Note that the code will not output a jet size until a baseline has been entered
        baselineJetSize = (EditText)findViewById(R.id.baselineJetSize);
        baselineAirDensity = (EditText)findViewById(R.id.baselineAirDensity);
        baseline = getApplicationContext().getSharedPreferences("data",0);
        System.out.println(baseline);
        if(baseline.contains("JetSize")){
            blSet = true;
            blJetSize = baseline.getInt("JetSize",0);
            baselineJetSize.setText(String.valueOf(blJetSize));
        }
        if(baseline.contains("AirDensity")){
            blSet = true;
            blAirDensity = baseline.getFloat("AirDensity",0);
            baselineAirDensity.setText(String.valueOf(blAirDensity));
        }

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
        // Convert humidity into a decimal
        humidity /= 100;
        double a = Math.log(humidity) + (M * temperature) / (TN + temperature);
        double dp = TN * a / (M - a);
        double p1 = 6.1078 * Math.pow(10, 7.5 * dp / (dp + 237.3));
        double pv = p1 * humidity;
        double pd = pressure - pv;
        // Pressure in kg/m^3
        p = 100*(pd / (RD * (temperature+273.15))) + (pv / (RV * (temperature+273.15)));

        // ISA states that pressure at sea level is 1.225kg/m^3. Divide and multiply by 100 to get %
        p /= 1.225;
        p *= 100;

        // Finally compute the optimal jet size using a 3rd order polynomial fit if baseline is set
        if(blSet) {
            int optimalJetSize = jetSizeLookup(p);
            TextView jetSize = (TextView) findViewById(R.id.jetSize);
            jetSize.setText(String.valueOf(optimalJetSize));
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(
                this,
                pressureSensor,
                SensorManager.SENSOR_DELAY_NORMAL
        );
        sensorManager.registerListener(
                this,
                humiditySensor,
                SensorManager.SENSOR_DELAY_NORMAL
        );
        sensorManager.registerListener(
                this,
                temperatureSensor,
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * Accepts baseline parameter settings when clicking on button in gui and stores results
     * @param view
     */
    public void acceptBaseline(View view){
        String jetSizeString = baselineJetSize.getText().toString();
        String airDensityString = baselineAirDensity.getText().toString();

        // Do nothing further if jet size baseline hasn't been specified
        if(!jetSizeString.isEmpty()) {
            SharedPreferences.Editor editor = baseline.edit();
            // Remove old values for baseline
            editor.clear();
            editor.apply();

            blJetSize = Integer.valueOf(jetSizeString);
            // Air Density can either be entered, or current value can be taken
            if (!airDensityString.isEmpty()) {
                blAirDensity = Float.valueOf(airDensityString);
            } else {
                blAirDensity = (float) p;
                baselineAirDensity.setText(String.valueOf(Math.round(p)));
            }

            editor.putInt("JetSize", blJetSize);
            editor.putFloat("AirDensity", blAirDensity);
            editor.apply();
            blSet = true;
        }
    }

    /**
     * Given the baseline jet-size and air density values and the current air density, computes
     * the new optimal jet size to use.
     * @param p - Relative Air Density (%)
     * @return Jet Size
     */
    protected int jetSizeLookup(double p){
        double featureSpace = jetSizeFunc(blJetSize);
        featureSpace += Math.abs(airDensityFunc(blAirDensity) - airDensityFunc(p));
        return jetSizeInv(featureSpace);
    }

    /**
     * Uses the coefficients resulting from a 3rd order polynomial fit to map changes in air density
     * to a feature space shared with jet sizing
     * @param x - Air Density in %
     * @return Feature space encoded value
     */
    private double airDensityFunc(double x){
        return 0.0000566765*Math.pow(x,3)-0.030350453*Math.pow(x,2)+5.67665149*x-243.679494541;
    }

    /**
     * Uses the coefficients resulting from a 3rd order polynomial fit to map changes in jet sizes
     * to a feature space shared with air density
     * @param x - Jet size in mm
     * @return Feature space encoded value
     */
    private double jetSizeFunc(int x){
        return 0.0000010303*Math.pow(x,3)-0.001901179*Math.pow(x,2)+1.4186186963*x-159.3889027293;
    }

    /**
     * Uses the coefficients resulting from a 3rd order polynomial fit to map changes in the shared
     * feature space into a physical jet size
     * @param x - Feature space values
     * @return Jet Size to use
     */
    private int jetSizeInv(double x){
        double js;
        js = 0.0000175017*Math.pow(x,3)+0.0002019229*Math.pow(x,2)+1.0232220687*x+137.4920938274;
        return (int)js;
    }
}
