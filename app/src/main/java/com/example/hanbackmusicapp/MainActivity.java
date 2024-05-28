package com.example.hanbackmusicapp;
// TODO: Sync timer with sound detection

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;
import com.gauravk.audiovisualizer.visualizer.BarVisualizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.Manifest;

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
    private static final String SERVER_URL = "http://ec2-52-201-214-21.compute-1.amazonaws.com:3000/data"; // TODO: Update address
    private Boolean isRunning;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long pausedTimeElapsed;  // Variable to store the paused elapsed time

    // Database
    private JSONArray jsonArray;
    private int currentIndex = 0;
    private WebView webVideo;

    // Visualizer
    private BarVisualizer mVisualizer;
    private AudioRecord audioRecord;
    private Thread audioThread;
    private boolean isRecording = false;

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
        webVideo = findViewById(R.id.webVid);
        // Visualizer
        mVisualizer = findViewById(R.id.visualizer);

        /* Initialization */
        isRunning = true;
        Log.d("Initialization", "timer complete");
        // webView
        webVideo.setWebViewClient(new WebViewClient());
        webVideo.getSettings().setJavaScriptEnabled(true);
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
        playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
        startTimer();
        timerHandler.post(timerRunnable);
        timerDisplay.setText(R.string.reset_timer_display);
        pausedTimeElapsed = 0;

        // Database connection
        new FetchDataFromServerTask().execute();

        // Visualizer start
        startAudioRecording();

        /* Button Click Listeners*/
        // play & pause Button
        playPauseBtn.setOnClickListener(view -> {
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                webVideo.loadUrl("javascript:pauseVideo()");
                Log.d("Pause/Play", "Pausing");
                isRunning = false;
                stopTimer();
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                webVideo.loadUrl("javascript:playVideo()");
                Log.d("Pause/Play", "Playing");
                isRunning = true;
                timerHandler.post(timerRunnable);
                startTimer();
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
                pausedTimeElapsed = 0;
                resetTimer();
                startTimer();
                timerDisplay.setText(R.string.reset_timer_display);
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                timerHandler.removeCallbacks(timerRunnable);
            }
        });

        // next Button
        nextBtn.setOnClickListener(view -> {
            pausedTimeElapsed = 0;
            if (jsonArray != null && jsonArray.length() > 0) {
                currentIndex++;
                if (currentIndex >= jsonArray.length()) {
                    currentIndex = 0;
                }
                displayCurrentItem();
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                timerHandler.removeCallbacks(timerRunnable);
                resetTimer();
                startTimer();
                timerDisplay.setText(R.string.reset_timer_display);
            }
        });
    }

    public void VideoDisplay(String videoID) {

        // TODO: figure out how to {autoplay + unmute}
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
                "  player.unMute();" +
                "}" +
                "function pauseVideo() {" +
                "  player.pauseVideo();" +
                "  player.unMute();" +
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
            titleDisplay.setText(title);
            channelNameDisplay.setText(channelName);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }

    /* Visualizer functions */
    private void startAudioRecording() {
        int sampleRate = 44100;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            return;
        } else{
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        }

        audioThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            audioRecord.startRecording();
            isRecording = true;
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    mVisualizer.setRawAudioBytes(buffer);
                }
            }
        });

        audioThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }

    /* Permission Request */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioRecording();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public native void startTimer();
    public native void stopTimer();
    public native void resetTimer();
    public native String getElapsedTime();
}