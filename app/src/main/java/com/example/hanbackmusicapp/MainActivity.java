package com.example.hanbackmusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    /* Tags */
    private static final String TAG = MainActivity.class.getSimpleName();

    /* variables */
    private static final String SERVER_URL = "http://ec2-54-196-174-136.compute-1.amazonaws.com:3000/data"; // TODO: Update address
    private Boolean isRunning;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long pausedTimeElapsed;  // Variable to store the paused elapsed time

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

        /* Displayed Objects */
        // TextView
        TextView timerDisplay = findViewById(R.id.timeElapsedDisplay);
        // Buttons
        ImageButton playPauseBtn = findViewById(R.id.playPauseButton);
        ImageButton prevBtn = findViewById(R.id.prevBtn);
        ImageButton nextBtn = findViewById(R.id.nextBtn);
        // WebView
        webVideo  = findViewById(R.id.webVid);

        /* Initialization */
        isRunning = false;
        // Timer
        timerDisplay.setText(R.string.reset_timer_display);
        pausedTimeElapsed = 0;
        Log.d("Initialization", "timer complete");
        // webView
        webVideo.setWebViewClient(new WebViewClient());
        webVideo.getSettings().setJavaScriptEnabled(true);
        webVideo.addJavascriptInterface(new WebAppInterface(), "Android");
        webVideo.setWebChromeClient(new WebChromeClient());
        Log.d("Initialization", "webView complete");
        Log.d("Initialization", "ALL COMPLETED");

        // Timer handler and runnable to update the UI
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timerDisplay.setText(getElapsedTime());
                if (isRunning) {
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        // Database connection
        new FetchDataFromServerTask().execute();

        /* Button Click Listeners*/     // TODO: To implement with database
        // play & pause Button
        playPauseBtn.setOnClickListener(view -> {
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                stopTimer();
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                startTimer();
                timerHandler.post(timerRunnable);
            }
            isRunning = !isRunning;
        });

        // previous Button
        prevBtn.setOnClickListener(view -> {
            resetTimer();
            timerDisplay.setText(R.string.reset_timer_display);
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                timerHandler.removeCallbacks(timerRunnable);
                isRunning = false;
            }
            pausedTimeElapsed = 0;
        });

        // next Button
        nextBtn.setOnClickListener(view -> {
            resetTimer();
            timerDisplay.setText(R.string.reset_timer_display);
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                timerHandler.removeCallbacks(timerRunnable);
                isRunning = false;
            }
            pausedTimeElapsed = 0;
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onPlayerReady() {
            webVideo.post(new Runnable() {
                @Override
                public void run() {
                    isRunning = true;
                    Toast.makeText(MainActivity.this, "Player is ready and video is playing", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void playVideo() {
            webVideo.post(new Runnable() {
                @Override
                public void run() {
                    webVideo.loadUrl("javascript:playVideo()");
                }
            });
        }

        @JavascriptInterface
        public void pauseVideo() {
            webVideo.post(new Runnable() {
                @Override
                public void run() {
                    webVideo.loadUrl("javascript:pauseVideo()");
                }
            });
        }
    }

    public void VideoDisplay(String videoID) {
//        String video = "<iframe width=\"100%\" height=\"100%\" " +
//                "src=\"https://www.youtube.com/embed/" + videoID + "?autoplay=1" +
//                "\" frameborder=\"0\" allowfullscreen></iframe>";

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
                //"  setTimeout(function() { event.target.unMute(); }, 1000);" + // test
                "}" +
                "function playVideo() {" +
                "  player.playVideo();" +
                "}" +
                "function pauseVideo() {" +
                "  player.pauseVideo();" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
        //webVideo.loadUrl("javascript:playVideo()");
        isRunning = true;
        Toast.makeText(this, "playing", Toast.LENGTH_SHORT).show();
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
            titleDisplay.setText(title);
            channelNameDisplay.setText(channelName);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }

    public native void startTimer();
    public native void stopTimer();
    public native void resetTimer();
    public native String getElapsedTime();
}