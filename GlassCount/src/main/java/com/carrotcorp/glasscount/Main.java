package com.carrotcorp.glasscount;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// TODO: implement offline mode

public class Main extends Activity {
    private String domain; // domain name and path to the PHP server
    private int offlineCount = 0; // how many counts have been created while offline?
    private String deviceID; // unique ID of this Glass device to differentiate between users on the website

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up view
        setContentView(R.layout.activity_main);

        // Set domain path
        domain = "http://tomthecarrot.com/glasscount/send.php";

        // Get device unique identifier (UUID)
        deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Calculate offline count
        offlineCount++;

        // Check for connection
        if (isNetworkAvailable()) {
            new Thread() {
                @Override
                public void run() {
                    // Send counts to the server
                    boolean success = sendServer(offlineCount, deviceID);
                    if (success) {
                        offlineCount = 0;
                    }
                }
            }.start();
        }
    }

    /**
     * Sends counts to server
     * @param count Number of counts to send to the server
     * @param deviceID This device's unique device identifier (UUID)
     * @return Whether the connection was successfully established
     */
    private boolean sendServer(int count, String deviceID) {
        HttpResponse response = null;
        try {
            // Create http client object to send request to server
            HttpClient client = new DefaultHttpClient();

            // Add parameters to the domain url
            String url = domain + "?count=" + count + "&deviceid=" + deviceID;
            URI uri = URI.create(url);

            // Create request to server and get response
            HttpGet httpget = new HttpGet();
            httpget.setURI(uri);
            response = client.execute(httpget);

            // Check for success (code 200)
            if (200 == response.getStatusLine().getStatusCode()) {

                // Print response
                String resultStr = EntityUtils.toString(response.getEntity());
                Log.i("glasscount", "HTTP RESPONSE: " + resultStr);

                return true;
            }
            else {
                return false;
            }
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
