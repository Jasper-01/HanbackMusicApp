package com.example.hanbackmusicapp;
// TODO: Sync timer with sound detection

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
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
    private static final String SERVER_URL = "http://ec2-52-55-190-29.compute-1.amazonaws.com:3000/data"; // TODO: Update address
    private Boolean isRunning;
    private Boolean isMute;
    private Boolean isVisible;

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
    private static int[] mSampleRates = new int[] { 44100, 22050, 16000, 11025, 8000 };


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

        // TODO: test textLCD without database
        textLCDout("title", "channelName");

        /* Displayed Objects */
        // TextView
        TextView timerDisplay = findViewById(R.id.timeElapsedDisplay);
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
//        isMute = true;
        isVisible = true;
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
        // TODO: watch out cus the device hates my ass
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
                // TODO: phone X
                 dotMatrixOut("Pausing");
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                webVideo.loadUrl("javascript:playVideo()");
                Log.d("Pause/Play", "Playing");
                isRunning = true;
                timerHandler.post(timerRunnable);
                startTimer();
//                 TODO: phone X
                 dotMatrixOut("Playing");
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
                // TODO: phone X
                 dotMatrixOut("Previous");
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
                // TODO: phone X
                 dotMatrixOut("Next");
            }
        });

        // mute Button
        muteBtn.setOnClickListener(view -> {
            if (isMute){
                isMute = false;
                muteBtn.setImageResource(R.drawable.baseline_volume_on_icon);
                webVideo.loadUrl("javascript:unMuteVideo()");
                // TODO: phone X
                 dotMatrixOut("Unmute");
            } else{
                isMute = true;
                muteBtn.setImageResource(R.drawable.baseline_volume_mute_icon);
                webVideo.loadUrl("javascript:MuteVideo()");
                // TODO: phone X
                 dotMatrixOut("Mute");
            }
        });

        // visualizer visibility Button
        // TODO: add actual functions (to Jasper)
        visualBtn.setOnClickListener(view -> {
            if(isVisible){
                isVisible = false;
                visualBtn.setImageResource(R.drawable.baseline_visibility_off_visualizer_icon);
                // TODO: phone X
                 dotMatrixOut("Visualizer - Off");
            } else{
                isVisible = true;
                visualBtn.setImageResource(R.drawable.baseline_visibility_visualizer_icon);
                // TODO: phone X
                 dotMatrixOut("Visualizer - On");
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
        isMute = true;
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
            textLCDout(title, channelName);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }

    /* Visualizer functions */
    private void startAudioRecording() {
        int sampleRate = 11025;

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size: " + bufferSize);
            Toast.makeText(this, "Invalid audio buffer size", Toast.LENGTH_SHORT).show();
            return;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (!audioManager.isMicrophoneMute()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                return;
            } else {
                audioRecord = findAudioRecord();
            }

            if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                Toast.makeText(this, "AudioRecord initialization failed", Toast.LENGTH_SHORT).show();
                return;
            }

            Handler mainHandler = new Handler(Looper.getMainLooper());

            audioThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                audioRecord.startRecording();
                isRecording = true;
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        byte[] normalizedBuffer = normalizeAudioBuffer(buffer);
                        byte[] smoothedBuffer = smoothAudioBuffer(normalizedBuffer);
                        mainHandler.post(() -> mVisualizer.setRawAudioBytes(smoothedBuffer));
                    }
                }
            });

            audioThread.start();
            Log.d(TAG, "MIC WORKS");
        } else {
            Toast.makeText(this, "No audio input hardware found", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No audio input hardware found");
        }
    }

    private byte[] normalizeAudioBuffer(byte[] buffer) {
        int maxAmplitude = 0;
        for (byte b : buffer) {
            int amplitude = Math.abs(b);
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude;
            }
        }
        if (maxAmplitude == 0) {
            return buffer;
        }
        // TODO: phone X
        float normalizationFactor = 280.0f / maxAmplitude;
//        float normalizationFactor = 120.0f / maxAmplitude;
        byte[] normalizedBuffer = new byte[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            normalizedBuffer[i] = (byte) (buffer[i] * normalizationFactor);
        }
        return normalizedBuffer;
    }

    private byte[] smoothAudioBuffer(byte[] buffer) {
        byte[] smoothedBuffer = new byte[buffer.length];
        int windowSize = 10; // Adjust the window size as needed
        for (int i = 0; i < buffer.length; i++) {
            int sum = 0;
            int count = 0;
            for (int j = i - windowSize; j <= i + windowSize; j++) {
                if (j >= 0 && j < buffer.length) {
                    sum += buffer[j];
                    count++;
                }
            }
            smoothedBuffer[i] = (byte) (sum / count);
        }
        return smoothedBuffer;
    }

    // Find sample rates
    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                            return null;
                        } else {
                            Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                    + channelConfig);
                            int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                            if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                                // check if we can instantiate and have a success
                                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                                if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                    return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
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

    // textLCD
    public native void textLCDout(String str1, String str2);

    // dotMatrix
    // TODO: phone X
    public native void dotMatrixOut(String str1);
}