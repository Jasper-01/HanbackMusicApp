package com.example.hanbackmusicapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/*
TODO : Before starting app
1. WiFi connection
2. AWS activity
3. Database activity
4. Turning on server
5. Check from browser
6. Check if address is updated

CHECK STATES DIPSHIT
 */
public class MainActivity extends AppCompatActivity {

    /* Tags */
    private static final String TAG = MainActivity.class.getSimpleName();

    /* Permission codes */
    private static final int PERMISSION_REQUEST_CODE = 1;

    /* variables */
    private static final String SERVER_URL = "http://ec2-54-226-172-53.compute-1.amazonaws.com:3000/data"; // TODO: Update address
    private Boolean isRunning;
    private Boolean isMute;

    // Database
    private JSONArray jsonArray;
    private int currentIndex = 0;
    private WebView webVideo;

    // Used to load the 'hanbackmusicapp' library on application startup.
    static {
        System.loadLibrary("hanbackmusicapp");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        textLCDout("title", "channelName");

        /* Displayed Objects */
        // Buttons
        ImageButton playPauseBtn = findViewById(R.id.playPauseButton);
        ImageButton prevBtn = findViewById(R.id.prevBtn);
        ImageButton nextBtn = findViewById(R.id.nextBtn);
        ImageButton muteBtn = findViewById(R.id.muteBtn);
        // WebView
        webVideo = findViewById(R.id.webVid);

        /* Initialization */
        isRunning = true;
        isMute = true;
        playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
        // webView
        webVideo.getSettings().setJavaScriptEnabled(true);
        webVideo.getSettings().setDomStorageEnabled(true);
        webVideo.setWebChromeClient(new WebChromeClient());
        webVideo.setWebViewClient(new WebViewClient());

        webVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Consume the touch event without passing it to the WebView
                return true;
            }
        });

        Log.d("Initialization", "webView complete");
        Log.d("Initialization", "ALL COMPLETED");

        // Database connection
        new FetchDataFromServerTask().execute();

        /* Button Click Listeners*/
        // play & pause Button
        playPauseBtn.setOnClickListener(view -> {
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                webVideo.post(() -> webVideo.loadUrl("javascript:pauseVideo()"));
                isRunning = false;
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                webVideo.post(() -> webVideo.loadUrl("javascript:playVideo()"));
                isRunning = true;
            }
        });


        // previous Button
        prevBtn.setOnClickListener(view -> {
            if (jsonArray != null && jsonArray.length() > 0) {
                currentIndex--;
                if (currentIndex < 0) {
                    currentIndex = jsonArray.length() - 1;
                }
                displayCurrentItem();
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                runDotMatrixInBackground("Previous");
            }
        });

        // next Button
        nextBtn.setOnClickListener(view -> {
//            pausedTimeElapsed = 0;
            if (jsonArray != null && jsonArray.length() > 0) {
                currentIndex++;
                if (currentIndex >= jsonArray.length()) {
                    currentIndex = 0;
                }
                displayCurrentItem();
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                runDotMatrixInBackground("Next");
            }
        });

        // mute Button
        muteBtn.setOnClickListener(view -> {
            if (isMute){
                isMute = false;
                muteBtn.setImageResource(R.drawable.baseline_volume_on_icon);
                webVideo.loadUrl("javascript:unMuteVideo()");
                runDotMatrixInBackground("Unmute");
            } else{
                isMute = true;
                muteBtn.setImageResource(R.drawable.baseline_volume_mute_icon);
                webVideo.loadUrl("javascript:MuteVideo()");
                runDotMatrixInBackground("Mute");
            }
        });
    }

    public void VideoDisplay(String videoID) {
        String video = "<html>" +
                "<body style='margin:0;padding:0;'>" +
                "<iframe id=\"player\" width=\"100%\" height=\"100%\" " +
                "src=\"https://www.youtube.com/embed/" + videoID + "?enablejsapi=1&autoplay=1&mute=1" +
                "\" frameborder=\"0\" allowfullscreen></iframe>" +
                "<script src=\"https://www.youtube.com/iframe_api\"></script>" +
                "<script type=\"text/javascript\">" +
                "var player;" +
                "function onYouTubeIframeAPIReady() {" +
                "  player = new YT.Player('player', {" +
                "    events: {" +
                "      'onReady': onPlayerReady" +
                "    }" +
                "  });" +
                "}" +
                "function onPlayerReady(event) {" +
                "  event.target.playVideo();" +
                "}" +
                "function playVideo() {" +
                "  player.playVideo();" +
                "}" +
                "function pauseVideo() {" +
                "  player.pauseVideo();" +
                //"  player.unMute();" +
                "}" +
                "function unMuteVideo() {" +
                "  player.unMute();" +
                "}" +
                "function MuteVideo() {" +
                "  player.mute();" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
        isRunning = true;
        webVideo.loadDataWithBaseURL("https://www.youtube.com", video, "text/html", "utf-8", null);
    }

    /* Database functions */
    // AsyncTask to fetch data from server
    private class FetchDataFromServerTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder stringBuilder = new StringBuilder();
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(SERVER_URL);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

            } catch (IOException e) {
                Log.e(TAG, "Error fetching data from server: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing stream: " + e.getMessage());
                    }
                }
            }

            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String jsonData) {
            super.onPostExecute(jsonData);
            if (jsonData != null && !jsonData.isEmpty()) {
                try {
                    jsonArray = new JSONArray(jsonData);
                    if (jsonArray.length() > 0) {
                        currentIndex = 0;
                        displayCurrentItem();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON: " + e.getMessage());
                }
            }
        }
    }

    // Display current song's Title and Channel Name
    private void displayCurrentItem() {
        try {
            // Title and Channel Name Displays
            TextView titleDisplay = findViewById(R.id.titleDisplay);
            TextView channelNameDisplay = findViewById(R.id.channelNameDisplay);

            JSONObject jsonObject = jsonArray.getJSONObject(currentIndex);
            String title = jsonObject.getString("title");
            String channelName = jsonObject.getString("channelName");
            String videoID = jsonObject.getString("videoId");
            VideoDisplay(videoID);

            // set texts
            textLCDout(title, channelName);
            titleDisplay.setText(title);
            channelNameDisplay.setText(channelName);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }

    private void runDotMatrixInBackground(final String message) {
        new Thread(() -> dotMatrixOut(message)).start();
    }

    // textLCD
    public native void textLCDout(String str1, String str2);

    // dotMatrix
    public native void dotMatrixOut(String str1);
};