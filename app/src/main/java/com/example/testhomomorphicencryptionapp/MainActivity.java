package com.example.testhomomorphicencryptionapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private TextView dec0View;
    private TextView dec1View;
    private TextView dec2View;

    private TextView EDPSRead;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private double longitude;
    private double latitude;
    private TextView longRead;
    private TextView latRead;

    private SEALConfig config;

    private String encryptedData;
    private double[] decryptedData;

    private Response.ErrorListener errorListener;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    ObjectMapper mapper;

    private static final String TAG = "MainActivity";
    private static final String reqUrl = "https://aokuer9jgd.execute-api.us-east-2.amazonaws.com/dev/nearest";
    private static final String reqUrl2 = "https://aokuer9jgd.execute-api.us-east-2.amazonaws.com/dev/GetLocationData";

    TimingLogger timings = new TimingLogger(TAG, "EDPS");

    static {
        System.loadLibrary("SealJavaWrapper");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.dec0View = findViewById(R.id.inputDec0);
        this.dec1View = findViewById(R.id.inputDec1);
        this.dec2View = findViewById(R.id.inputDec2);

        this.EDPSRead = findViewById(R.id.EDPSRead);

        this.longRead = findViewById(R.id.longRead);
        this.latRead = findViewById(R.id.latRead);

        this.locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        this.errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
            }
        };

        this.mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        SEALConfig tmp = loadSEALConfig("SEALConfig.txt");
        if (tmp == null) {
            this.config = new SEALConfig();
            this.config.setSerialVersionUID(UUID.randomUUID().toString());
            this.config.setParams(this.setParameters());
            this.config.setPrivateKey(this.getPrivateKey(this.config.getParams()));
            this.config.setPublicKey(this.getPublicKey(this.config.getParams(), this.config.getPrivateKey()));
            this.saveSEALConfig(this.config, "SEALConfig.txt");
        } else {
            String junk = this.setParameters();
            this.config = tmp;
        }
    }

    public void saveSEALConfig(SEALConfig SEALConfig, String fileName){
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(),"")+File.separator+fileName));
            out.writeObject(SEALConfig);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SEALConfig loadSEALConfig(String fileName){
        SEALConfig config = new SEALConfig();

        ObjectInputStream input;
        try {
            input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(),"")+File.separator+fileName)));
            config = (SEALConfig) input.readObject();
            //Log.v("serialization","Config Uid="+SEALConfig.getSerialVersionUID());
            input.close();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (config.isEmpty()) {
            return null;
        }
        return config;
    }

    public void encrypt(View view) {
        double dec0 = Double.parseDouble(this.dec0View.getText().toString());
        double dec1 = Double.parseDouble(this.dec1View.getText().toString());
        double dec2 = Double.parseDouble(this.dec2View.getText().toString());

        // Begin encryption
        double[] unencryptedData = new double[]{dec0, dec1, dec2};
        this.setEncryptedData(encryptDoubleArray(this.config.getParams(), this.config.getPublicKey(), unencryptedData));

        System.out.printf("Encrypted array: [ %f, %f, %f ]\n", unencryptedData[0],
                unencryptedData[1], unencryptedData[2]);
        System.out.println("Encrypted!");
        this.EDPSRead.setText(String.format("Encrypted array: [ %f, %f, %f ]\n", unencryptedData[0],
                unencryptedData[1], unencryptedData[2]));
    }

    public void decrypt(View view) {
        if (this.encryptedData.length() == 0) {
            System.out.println("Please enter values to encrypt before decrypting!");
            return;
        }

        // Begin decryption
        this.setDecryptedData(decryptDoubleArray(this.config.getParams(), this.config.getPrivateKey(), this.encryptedData));

        System.out.printf("Decrypted array: [ %f, %f, %f ]\n", this.decryptedData[0], this.decryptedData[1], this.decryptedData[2]);
        System.out.println("Decrypted!");
        this.EDPSRead.setText(String.format("Decrypted array: [ %f, %f, %f ]\n",
                this.decryptedData[0], this.decryptedData[1], this.decryptedData[2]));
    }

    public void calculateEDPS(View view) {
        // Set up unencrypted data
        double dec0 = Double.parseDouble(this.dec0View.getText().toString());
        double dec1 = Double.parseDouble(this.dec1View.getText().toString());
        double dec2 = Double.parseDouble(this.dec2View.getText().toString());

        double[] unencryptedData = new double[]{dec0, dec1, dec2};

        // Begin encrypt/decrypt per second calculation in s
        int max = 100;
        long startTime = System.nanoTime();
        for (int i = 0; i < max; i++){
            //timings.addSplit("start");
            this.setEncryptedData(encryptDoubleArray(this.config.getParams(), this.config.getPublicKey(), unencryptedData));
            //timings.addSplit("E " + Integer.toString(i));
            this.setDecryptedData(decryptDoubleArray(this.config.getParams(), this.config.getPrivateKey(), this.encryptedData));
            //timings.addSplit("D " + Integer.toString(i));
            //timings.addSplit("end");
        }
        //timings.dumpToLog();
        long endTime = System.nanoTime();
        double duration = (double)(endTime - startTime);
        double EDPS = ((double)max) / (duration/1000000000);
        this.EDPSRead.setText(String.format("%s EDPS", EDPS));
        System.out.printf("Encrypts/Decrypts per second = %f", EDPS);
    }

    public void getGPSLocation(View view) {
        boolean flag = displayGpsStatus();
        if (flag) {
            locationListener = new MyLocationListener();
            try {
                locationManager.requestLocationUpdates(LocationManager
                        .GPS_PROVIDER, 5000, 5, locationListener);
            } catch (SecurityException e){
                e.printStackTrace();
            }
        } else {
            alertbox("Gps Status", "Your GPS is: OFF");
        }
    }

    public void encryptGPSLocation(View view){
        // Begin encryption
        if (Double.isNaN(this.longitude) || Double.isNaN(this.latitude)){
            System.out.println("GPS coordinates have nor been logged. Press Get GPS Coordinates" +
                    " and wait.");
            this.EDPSRead.setText("Press Get GPS Coordinates and wait for display, then encrypt.");
            return;
        }
        double[] unencryptedData = new double[]{this.longitude, this.latitude};
        this.setEncryptedData(encryptDoubleArray(this.config.getParams(), this.config.getPublicKey(), unencryptedData));

        System.out.printf("Encrypted array: [ %f, %f ]\n", unencryptedData[0],
                unencryptedData[1]);
        System.out.println("Encrypted!");
        this.EDPSRead.setText(String.format("Encrypted array: [ %f, %f ]\n", unencryptedData[0],
                unencryptedData[1]));
    }

    public void decryptGPSLocation(View view){
        if (this.encryptedData.length() == 0) {
            System.out.println("Please encrypt GPS coordinates before decrypting!");
            return;
        }

        // Begin decryption
        this.setDecryptedData(decryptDoubleArray(this.config.getParams(), this.config.getPrivateKey(), this.encryptedData));

        System.out.printf("Decrypted array: [ %f, %f ]\n", this.decryptedData[0],
                this.decryptedData[1]);
        System.out.println("Decrypted!");
        this.EDPSRead.setText(String.format("Decrypted array: [ %f, %f ]\n", this.decryptedData[0],
                this.decryptedData[1]));
    }

    public void onRequest(View view){
        // Create request body
        if (!Double.isNaN(this.latitude) || !Double.isNaN(this.longitude)) {
            HashMap<String, String> params = new HashMap<>();
            params.put("params", this.config.getParams());
            params.put("key", this.config.getPrivateKey());
            params.put("latitude", this.encryptDoubleArray(this.config.getParams(),
                    this.config.getPublicKey(), new double[]{this.latitude}));
            params.put("longitude", this.encryptDoubleArray(this.config.getParams(),
                    this.config.getPublicKey(), new double[]{this.longitude}));
            params.put("device_id", this.config.getSerialVersionUID());
            this.EDPSRead.setText("Location sent!");

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, reqUrl, new JSONObject(params),
                        response -> EDPSRead.setText("Resp: " + response.toString()),
                        error -> {
                            // Handle error
                            EDPSRead.setText("Resp Err: " + error.getMessage());
                        }
                    );

            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
        } else {
            this.EDPSRead.setText("Location not sent, please select Get GPS Coordinates");
        }
    }

    public void onMap(View view){
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            reqUrl2,
            new JSONObject(),
            response -> {
                try {
                    String reqUrl3 = response.getString("url");
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (
                            Request.Method.GET,
                            reqUrl3,
                            new JSONObject(),
                            response1 -> {
                                //System.out.println(response1.toString());
                                PathsLocationData paths = null;
                                try {
                                    paths = mapper.readValue(response1.toString(), PathsLocationData.class);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (paths != null) {
                                    System.out.println("Got the paths");

                                    // Decrypt path 1
                                    ArrayList<LatLng> path1 = new ArrayList<>();
                                    int i;
                                    for (i = 0; i < paths.path1.path.size(); i++){
                                        double lat = decryptDoubleArray(this.config.getParams(),
                                            paths.path1.key,
                                            paths.path1.path.get(i).latitude
                                        )[0];
                                        double lng = decryptDoubleArray(this.config.getParams(),
                                                paths.path1.key,
                                                paths.path1.path.get(i).longitude
                                        )[0];
                                        LatLng temp = new LatLng(lat, lng);
                                        path1.add(temp);
                                    }

                                    // Decrypt path 2
                                    ArrayList<LatLng> path2 = new ArrayList<>();
                                    for (i = 0; i < paths.path2.path.size(); i++){
                                        double lat = decryptDoubleArray(this.config.getParams(),
                                                paths.path2.key,
                                                paths.path2.path.get(i).latitude
                                        )[0];
                                        double lng = decryptDoubleArray(this.config.getParams(),
                                                paths.path2.key,
                                                paths.path2.path.get(i).longitude
                                        )[0];
                                        LatLng temp = new LatLng(lat, lng);
                                        path2.add(temp);
                                    }

                                    System.out.println("Done");
                                    Intent myIntent = new Intent(MainActivity.this, MapsMarkerActivity.class);
                                    myIntent.putExtra("path1", path1);
                                    myIntent.putExtra("path2", path2);
                                    MainActivity.this.startActivity(myIntent);
                                }
                            },
                            error -> {
                                System.out.println(error.toString());
                            }
                        );
                    int socketTimeout = 30000;//30 seconds - change to what you want
                    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                    jsonObjectRequest.setRetryPolicy(policy);

                    // Access the RequestQueue through your singleton class.
                    MySingleton.getInstance(MainActivity.this.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            },
            error -> {
                // Handle error
                EDPSRead.setText("Resp Err: " + error.getMessage());
            }
        );
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);
        MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(request);
    }

//    public void startParsingTask() {
//        Thread threadA = new Thread() {
//            public void run() {
//                ThreadB threadB = new ThreadB(getApplicationContext());
//                JSONObject jsonObject = null;
//                try {
//                    jsonObject = threadB.execute().get(30, TimeUnit.SECONDS);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                } catch (TimeoutException e) {
//                    e.printStackTrace();
//                }
//                final JSONObject receivedJSONObject = jsonObject;
//                runOnUiThread(() -> {
//                    EDPSRead.setText("Response is: " + receivedJSONObject);
//                    if (receivedJSONObject != null) {
//                        try {
//                            reqUrl3 = receivedJSONObject.getString("url");
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//            }
//        };
//        threadA.start();
//    }
//    private class ThreadB extends AsyncTask<Void, Void, JSONObject> {
//        private Context mContext;
//        public ThreadB(Context ctx) {
//            mContext = ctx;
//        }
//        @Override
//        protected JSONObject doInBackground(Void... params) {
//            final RequestFuture<JSONObject> futureRequest = RequestFuture.newFuture();
//            int socketTimeout = 30000;//30 seconds - change to what you want
//            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
//            final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method
//                    .GET, reqUrl2,
//                    new JSONObject(), futureRequest, futureRequest);
//            jsonRequest.setTag(TAG);
//            jsonRequest.setRetryPolicy(policy);
//            MySingleton.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonRequest);
//            try {
//                return futureRequest.get(30, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            } catch (TimeoutException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//    }


//    private class UrlRet extends AsyncTask<String, Void, String>{
//        @Override
//        protected void onPreExecute() {
//            //super.onPreExecute();
//            EDPSRead.setText("Starting call for URL");
//        }
//
//        @Override
//        protected String doInBackground(String... strings) {
//            JSONObject urlObj = runJsonObjectRequest(Request.Method.GET, reqUrl2, new JSONObject());
//            if (urlObj != null){
//                try {
//                    String url = urlObj.getString("url");
//                    return url;
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    return null;
//                }
//            }
//            return null;
//        }
//    }

    /**
     * Runs a blocking Volley request
     *
     * @param method        get/put/post etc
     * @param url           endpoint
     * @param body          body of request
     * @return the JSONObject result or exception: NOTE returns null once the onErrorResponse listener has been called
     */
    public JSONObject runJsonObjectRequest(int method, String url, JSONObject body) {
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(method, url, body, future, future);
        request.setRetryPolicy(policy);
        MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(request);
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    // This method sets a parameter object and returns the object as a byte array
    public native String setParameters();

    // This method sets a private key given a parameters object and returns the object as a byte array
    public native String getPrivateKey(String parms);

    // This method sets a public key given a parameters object and private key and returns the object as a byte array
    public native String getPublicKey(String parms, String privateKey);

    // This method encrypts an array of doubles given parameters, public key, and data objects and returns the encrypted data as a byte array
    public native String encryptDoubleArray(String parms, String publicKey, double[] data);

    // This method decrypts an array of doubles given parameters, private key, and data objects and returns the decrypted data as a double array
    public native double[] decryptDoubleArray(String parms, String privateKey, String data);

    /*----------Method to create an AlertBox ------------- */
    protected void alertbox(String title, String mymessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your device's GPS is disabled")
                .setCancelable(false)
                .setTitle("** Gps Status **")
                .setPositiveButton("Gps On",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // finish the current activity
                                // AlertBoxAdvance.this.finish();
                                Intent myIntent = new Intent(
                                        Settings.ACTION_SECURITY_SETTINGS);
                                startActivity(myIntent);
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*----Method to Check GPS is enabled or disabled ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;
        } else {
            return false;
        }
    }

    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }

    public void setDecryptedData(double[] decryptedData) {
        this.decryptedData = decryptedData;
    }

    /*----------Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            longitude = loc.getLongitude();
            latitude = loc.getLatitude();
            longRead.setText(String.format("Longitude: %s", Double.toString(longitude)));
            latRead.setText(String.format("Latitude: %s", Double.toString(latitude)));
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }
}

