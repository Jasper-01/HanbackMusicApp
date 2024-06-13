
package com.example.hanbackmusicapp;

import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
    private static final String SERVER_URL = "http://ec2-204-236-202-165.compute-1.amazonaws.com:3000/data"; // TODO: Update address
    private Boolean isRunning;
    private Boolean isMute;
    private Boolean isVisible;

    // Timer
//    private Handler timerHandler;
//    private Runnable timerRunnable;
//    private long pausedTimeElapsed;  // Variable to store the paused elapsed time

    // Database
    private JSONArray jsonArray;
    private int currentIndex = 0;
    private WebView webVideo;

    // Visualizer
    private BarVisualizer mVisualizer;
    private AudioRecord audioRecord;
    private Thread audioThread;
    private boolean isRecording = false;
    private static final int[] SAMPLE_RATES = new int[]{8000, 11025, 16000, 22050, 44100, 48000, 96000};

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
        // TextView
//        TextView timerDisplay = findViewById(R.id.timeElapsedDisplay);
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
        isMute = true;
        isVisible = true;
        playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
        // webView
        webVideo.getSettings().setJavaScriptEnabled(true);
        webVideo.getSettings().setDomStorageEnabled(true);
        webVideo.setWebChromeClient(new WebChromeClient());
        webVideo.setWebViewClient(new WebViewClient());
        webVideo.getSettings().setLoadsImagesAutomatically(true);
        webVideo.getSettings().setAllowFileAccess(true);
        webVideo.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webVideo.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webVideo.getSettings().setPluginState(WebSettings.PluginState.ON);
        webVideo.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webVideo.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webVideo.requestFocus(View.FOCUS_DOWN);

        webVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Consume the touch event without passing it to the WebView
                return true;
            }
        });

        Log.d("Initialization", "webView complete");
        Log.d("Initialization", "ALL COMPLETED");

        // Timer handler and runnable to update the UI
//        timerHandler = new Handler(Looper.getMainLooper());
//        timerRunnable = new Runnable() {
//            @Override
//            public void run() {
//                timerDisplay.setText(getElapsedTime());
//                if (isRunning) {
//                    timerHandler.postDelayed(this, 1000);
//                }
//            }
//        };
//        playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
//        startTimer();
//        timerHandler.post(timerRunnable);
//        timerDisplay.setText(R.string.reset_timer_display);
//        pausedTimeElapsed = 0;

        // Database connection
        new FetchDataFromServerTask().execute();

        // Visualizer start
//        startAudioRecording();

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
//                pausedTimeElapsed = 0;
//                resetTimer();
//                startTimer();
//                timerDisplay.setText(R.string.reset_timer_display);
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
//                timerHandler.removeCallbacks(timerRunnable);
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
//                timerHandler.removeCallbacks(timerRunnable);
//                resetTimer();
//                startTimer();
//                timerDisplay.setText(R.string.reset_timer_display);
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

        // visualizer visibility Button
//        visualBtn.setOnClickListener(view -> {
//            if(isVisible){
//                isVisible = false;
//                visualBtn.setImageResource(R.drawable.baseline_visibility_off_visualizer_icon);
//                stopAudioRecording();
//                mVisualizer.setVisibility(View.INVISIBLE);
//                runDotMatrixInBackground("Visualizer - Off");
//            } else{
//                isVisible = true;
//                visualBtn.setImageResource(R.drawable.baseline_visibility_visualizer_icon);
//                startAudioRecording();
//                mVisualizer.setVisibility(View.VISIBLE);
//                runDotMatrixInBackground("Visualizer - On");
//            }
//        });
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

    /* Visualizer functions */
    // Start audio recording
//    private void startAudioRecording() {
//        int sampleRate = findValidSampleRate();
//        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
////        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT);
//
//        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
//            Log.e(TAG, "Invalid buffer size: " + bufferSize);
//            Toast.makeText(this, "Invalid audio buffer size", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
//            return;
//        } else {
//            audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
////            audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT, bufferSize);
//        }
//
//        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
//            Log.e(TAG, "AudioRecord initialization failed");
//            Toast.makeText(this, "AudioRecord initialization failed", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        audioThread = new Thread(() -> {
//            byte[] buffer = new byte[bufferSize];
//            audioRecord.startRecording();
//            isRecording = true;
//            while (isRecording) {
//                int read = audioRecord.read(buffer, 0, buffer.length);
//                if (read > 0) {
//                    byte[] normalizedBuffer = normalizeAudioBuffer(buffer);
//                    byte[] smoothedBuffer = smoothAudioBuffer(normalizedBuffer);
//                    runOnUiThread(() -> mVisualizer.setRawAudioBytes(smoothedBuffer));
//                }
//            }
//        });
//
//        audioThread.start();
//
//    }
//
//    private boolean isAudioConfigSupported(int sampleRate, int channelConfig, int audioFormat) {
//        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
//        return !(bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE);
//    }
//
//    // Stop audio recording
//    private void stopAudioRecording() {
//        isRecording = false;
//        if (audioRecord != null) {
//            audioRecord.stop();
//            audioRecord.release();
//            audioRecord = null;
//        }
//    }
//
//    // Find a valid sample rate
//    // Find a valid sample rate
//    private int findValidSampleRate() {
//        int selectedSampleRate = -1; // Initialize with an invalid value
//        for (int rate : SAMPLE_RATES) {
//            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
//                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
//                    try {
//                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
//                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
//                            // Found a valid sample rate
//                            selectedSampleRate = rate;
//                            Log.d(TAG, "Selected sample rate: " + selectedSampleRate);
//                            return selectedSampleRate;
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, rate + "Exception, keep trying.", e);
//                    }
//                }
//            }
//        }
//        Log.e(TAG, "No valid sample rate found");
//        return selectedSampleRate;
//    }
//
//
//    // Permission request
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startAudioRecording(); // Start audio recording after permission is granted
//            } else {
//                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//
//    // Smooth the audio buffer
//    private byte[] smoothAudioBuffer(byte[] buffer) {
//        byte[] smoothedBuffer = new byte[buffer.length];
//        int windowSize = 10; // Adjust the window size as needed
//        for (int i = 0; i < buffer.length; i++) {
//            int sum = 0;
//            int count = 0;
//            for (int j = i - windowSize; j <= i + windowSize; j++) {
//                if (j >= 0 && j < buffer.length) {
//                    sum += buffer[j];
//                    count++;
//                }
//            }
//            smoothedBuffer[i] = (byte) (sum / count);
//        }
//        return smoothedBuffer;
//    }
//
//    // Normalize the audio buffer
//    private byte[] normalizeAudioBuffer(byte[] buffer) {
//        int maxAmplitude = 0;
//        for (byte b : buffer) {
//            int amplitude = Math.abs(b);
//            if (amplitude > maxAmplitude) {
//                maxAmplitude = amplitude;
//            }
//        }
//        if (maxAmplitude == 0) {
//            return buffer;
//        }
//        float normalizationFactor = 180.0f / maxAmplitude;
//        byte[] normalizedBuffer = new byte[buffer.length];
//        for (int i = 0; i < buffer.length; i++) {
//            normalizedBuffer[i] = (byte) (buffer[i] * normalizationFactor);
//        }
//        return normalizedBuffer;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        isRecording = false;
//        stopAudioRecording();
//    }

    private void runDotMatrixInBackground(final String message) {
        new Thread(() -> dotMatrixOut(message)).start();
    }

//    public native void startTimer();
//    public native void stopTimer();
//    public native void resetTimer();
//    public native String getElapsedTime();

    // textLCD
    public native void textLCDout(String str1, String str2);

    // dotMatrix
    public native void dotMatrixOut(String str1);
}