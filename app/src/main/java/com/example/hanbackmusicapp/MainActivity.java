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
    private static final String SERVER_URL = "http://ec2-54-172-93-231.compute-1.amazonaws.com:3000/data"; // TODO: Update address
    private Boolean isRunning;
    private Boolean isMute;
    private Boolean isVisible;

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
        // Buttons
        ImageButton playPauseBtn = findViewById(R.id.playPauseButton);
        ImageButton prevBtn = findViewById(R.id.prevBtn);
        ImageButton nextBtn = findViewById(R.id.nextBtn);
        ImageButton muteBtn = findViewById(R.id.muteBtn);
        ImageButton visualBtn = findViewById(R.id.visualizerBtn);
        // WebView
        webVideo = findViewById(R.id.webVid);
        // Visualizer
        mVisualizer = findViewById(R.id.visualizer);

        /* Initialization */
        isRunning = true;
        isVisible = true;
        Log.d("Initialization", "timer complete");
        // webView
        webVideo.setWebViewClient(new WebViewClient());
        webVideo.getSettings().setJavaScriptEnabled(true);
        webVideo.setWebChromeClient(new WebChromeClient());
        Log.d("Initialization", "webView complete");
        Log.d("Initialization", "ALL COMPLETED");

        // Database connection
        new FetchDataFromServerTask().execute();

        // Visualizer start
        //startAudioRecording();

        /* Button Click Listeners*/
        // play & pause Button
        playPauseBtn.setOnClickListener(view -> {
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                webVideo.loadUrl("javascript:document.getElementById('audioPlayer').play();");
                Log.d("Pause/Play", "Pausing");
                isRunning = false;
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                webVideo.loadUrl("javascript:document.getElementById('audioPlayer').pause();");
                Log.d("Pause/Play", "Playing");
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
            }
        });

        // next Button
        nextBtn.setOnClickListener(view -> {
            if (jsonArray != null && jsonArray.length() > 0) {
                currentIndex++;
                if (currentIndex >= jsonArray.length()) {
                    currentIndex = 0;
                }
                displayCurrentItem();
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
            }
        });

        // mute Button
        muteBtn.setOnClickListener(view -> {
            if (isMute){
                isMute = false;
                muteBtn.setImageResource(R.drawable.baseline_volume_on_icon);
                //webVideo.loadUrl("javascript:unMuteVideo()");
            } else{
                isMute = true;
                muteBtn.setImageResource(R.drawable.baseline_volume_mute_icon);
                //webVideo.loadUrl("javascript:MuteVideo()");
            }
        });

        // visualizer visibility Button
        // TODO: add actual functions (to Jasper)
        visualBtn.setOnClickListener(view -> {
            if(isVisible){
                isVisible = false;
                visualBtn.setImageResource(R.drawable.baseline_visibility_off_visualizer_icon);
            } else{
                isVisible = true;
                visualBtn.setImageResource(R.drawable.baseline_visibility_visualizer_icon);
            }
        });
    }

    /* Database functions */
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

    /* Display current song's Title, Channel Name and video*/
    private void displayCurrentItem() {
        try {
            // Title and Channel Name Displays
            TextView titleDisplay = findViewById(R.id.titleDisplay);
            TextView channelNameDisplay = findViewById(R.id.channelNameDisplay);

            JSONObject jsonObject = jsonArray.getJSONObject(currentIndex);
            String title = jsonObject.getString("title");
            String channelName = jsonObject.getString("channelName");
            String videoID = jsonObject.getString("videoId");

            // Construct the HTML content with the dynamic video ID
            String htmlContent = "<html><head>" +
                    "<style> body, html { height: 100%; margin: 0; } </style>" +
                    "</head><body style=\"display:flex; align-items:center; justify-content:center; height:100%;\">" +
                    "<audio controls id=\"audioPlayer\" style=\"max-width: 100%; width: 100%;\">" +
                    "<source src=\"http://ec2-54-172-93-231.compute-1.amazonaws.com:3000/play/" + videoID + "\" type=\"audio/mpeg\">" +
                    "Your browser does not support the audio element.</audio></body></html>";

            // Load the HTML content into the WebView
            webVideo.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            isRunning = true;
            isMute = true;

            // set texts
            titleDisplay.setText(title);
            channelNameDisplay.setText(channelName);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }

    /* Visualizer functions */
    private void startAudioRecording() {
//        int sampleRate = 44100;
//        int sampleRate = 22050;
//        int sampleRate = 16000;
        int sampleRate = 11025;

//        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            return;
        } else{
//            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT, bufferSize);
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
                //startAudioRecording();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}