package com.ssomlk.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;


public class WeatherController extends AppCompatActivity {

    
    final int REQUEST_CODE = 123;
    final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    final String APP_ID = "e72ca729af228beabd5d20e3b7749713";
    final long MIN_TIME = 5000;
    final float MIN_DISTANCE = 1000;

    String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;

    TextView lblCity;
    ImageView imgWeather;
    TextView tvTemperature;

    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_controller_layout);

        lblCity = (TextView) findViewById(R.id.locationTV);
        imgWeather = (ImageView) findViewById(R.id.weatherSymbolIV);
        tvTemperature = (TextView) findViewById(R.id.tempTV);
        ImageButton changeCityButton = (ImageButton) findViewById(R.id.changeCityButton);
        
        changeCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),ChangeCity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String city = intent.getStringExtra("city");
        if(city != null){
            getWeatherForNewCity(city);
        }else{
            getWeatherForCurrentLocation();
        }
    }
    
    private void getWeatherForNewCity(String city){
        RequestParams params = new RequestParams();
        params.put("q",city);
        params.put("appid",APP_ID);
        performNetworkingOperation(params);
    }
    
    private void getWeatherForCurrentLocation() {

        if(this.locationManager == null){
            this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        if(this.locationListener == null){
            this.locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    String longitude = String.valueOf(location.getLongitude());
                    String latitude = String.valueOf(location.getLatitude());
                    Log.d("ssomlk",longitude +" "+ latitude);

                    RequestParams params = new RequestParams();
                    params.put("lat",latitude);
                    params.put("lon",longitude);
                    params.put("appid",APP_ID);

                    performNetworkingOperation(params);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        this.locationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, this.locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getWeatherForCurrentLocation();
            }else{
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,permissions[0]);
                if (showRationale) {
                    Log.d("ssomlk","Access denied");
                }
            }
        }
    }

    private void performNetworkingOperation(RequestParams params){
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(WEATHER_URL,params,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("ssomlk","Success"+response.toString());
                WeatherDataModel weatherData = WeatherDataModel.fromJson(response);
                updateUI(weatherData);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                tvTemperature.setText("NA");
                int resourceId = getResources().getIdentifier("dunno","drawable",getPackageName());
                imgWeather.setImageResource(resourceId);
                lblCity.setText("No Data ...");
            }
        });
    }
    
    private void updateUI(WeatherDataModel model){
        tvTemperature.setText(model.getTemperature());
        lblCity.setText(model.getCity());
        int resourceId = getResources().getIdentifier(model.getIconName(),"drawable",getPackageName());
        imgWeather.setImageResource(resourceId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if(this.locationManager != null){
            this.locationManager.removeUpdates(this.locationListener);
        }
    }
}
