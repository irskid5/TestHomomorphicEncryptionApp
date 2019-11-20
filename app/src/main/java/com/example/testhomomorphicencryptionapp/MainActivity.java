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
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.TextView;

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

    private byte[] parms;
    private byte[] privKey;
    private byte[] pubKey;

    private byte[] encryptedData;
    private double[] decryptedData;

    private static final String TAG = "MainActivity";

    TimingLogger timings = new TimingLogger(TAG, "EDPS");

    // This method sets a parameter object and returns the object as a byte array
    public native byte[] setParameters();

    // This method sets a private key given a parameters object and returns the object as a byte array
    public native byte[] getPrivateKey(byte[] parms);

    // This method sets a public key given a parameters object and private key and returns the object as a byte array
    public native byte[] getPublicKey(byte[] parms, byte[] privateKey);

    // This method encrypts an array of doubles given parameters, public key, and data objects and returns the encrypted data as a byte array
    public native byte[] encryptDoubleArray(byte[] parms, byte[] publicKey, double[] data);

    // This method decrypts an array of doubles given parameters, private key, and data objects and returns the decrypted data as a double array
    public native double[] decryptDoubleArray(byte[] parms, byte[] privateKey, byte[] data);

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

        // Create parameters object
        this.setParms(setParameters());

        // Create private/public key pair
        this.setPrivKey(this.getPrivateKey(this.parms));
        this.setPubKey(this.getPublicKey(this.parms, this.privKey));
    }

    public void encrypt(View view) {
        double dec0 = Double.parseDouble(this.dec0View.getText().toString());
        double dec1 = Double.parseDouble(this.dec1View.getText().toString());
        double dec2 = Double.parseDouble(this.dec2View.getText().toString());

        // Begin encryption
        double[] unencryptedData = new double[]{dec0, dec1, dec2};
        this.setEncryptedData(encryptDoubleArray(this.parms, this.pubKey, unencryptedData));

        System.out.printf("Encrypted array: [ %f, %f, %f ]\n", unencryptedData[0],
                unencryptedData[1], unencryptedData[2]);
        System.out.println("Encrypted!");
        this.EDPSRead.setText(String.format("Encrypted array: [ %f, %f, %f ]\n", unencryptedData[0],
                unencryptedData[1], unencryptedData[2]));
    }

    public void decrypt(View view) {
        if (this.encryptedData.length == 0) {
            System.out.println("Please enter values to encrypt before decrypting!");
            return;
        }

        // Begin decryption
        this.setDecryptedData(decryptDoubleArray(this.parms, this.privKey, this.encryptedData));

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
        long startTime = System.nanoTime();
        int max = 100;
        for (int i = 0; i < max; i++){
            timings.addSplit("start");
            this.setEncryptedData(encryptDoubleArray(this.parms, this.pubKey, unencryptedData));
            timings.addSplit("E " + Integer.toString(i));
            this.setDecryptedData(decryptDoubleArray(this.parms, this.privKey, this.encryptedData));
            timings.addSplit("D " + Integer.toString(i));
            timings.addSplit("end");
        }
        timings.dumpToLog();
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
                        .GPS_PROVIDER, 5000, 10, locationListener);
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
        this.setEncryptedData(encryptDoubleArray(this.parms, this.pubKey, unencryptedData));

        System.out.printf("Encrypted array: [ %f, %f ]\n", unencryptedData[0],
                unencryptedData[1]);
        System.out.println("Encrypted!");
        this.EDPSRead.setText(String.format("Encrypted array: [ %f, %f ]\n", unencryptedData[0],
                unencryptedData[1]));
    }

    public void decryptGPSLocation(View view){
        if (this.encryptedData.length == 0) {
            System.out.println("Please encrypt GPS coordinates before decrypting!");
            return;
        }

        // Begin decryption
        this.setDecryptedData(decryptDoubleArray(this.parms, this.privKey, this.encryptedData));

        System.out.printf("Decrypted array: [ %f, %f ]\n", this.decryptedData[0],
                this.decryptedData[1]);
        System.out.println("Decrypted!");
        this.EDPSRead.setText(String.format("Decrypted array: [ %f, %f ]\n", this.decryptedData[0],
                this.decryptedData[1]));
    }

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

    public void setParms(byte[] parms) {
        this.parms = parms;
    }

    public void setPrivKey(byte[] privKey) {
        this.privKey = privKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public void setEncryptedData(byte[] encryptedData) {
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

