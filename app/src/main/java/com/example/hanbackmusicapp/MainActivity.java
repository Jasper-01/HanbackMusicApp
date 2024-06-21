package com.example.hanbackmusicapp;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
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
    private static final String SERVER_URL = "http://ec2-54-226-172-53.compute-1.amazonaws.com:3000"; // TODO: Update address
    private Boolean isRunning;
    private Boolean isMute;

    // Database
    private JSONArray jsonArray;
    private int currentIndex = 0;
    private String currentAudioPath;
    private WebView webVideo;
    private TextView timeElapsedDisplay;

    // Used to load the 'hanbackmusicapp' library on application startup.
    static {
        System.loadLibrary("hanbackmusicapp");
    }

    private ActivityMainBinding binding;
    private MediaPlayer mediaPlayer;
    private int pausedPosition = 0; // Added to store the paused position
    private Handler handler;
    private Runnable runnable;

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
        timeElapsedDisplay = findViewById(R.id.timeElapsedDisplay);
        // WebView
        webVideo = findViewById(R.id.webVid);

        /* Initialization */
        isRunning = true;
        isMute = false;
        playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);

        Log.d("Initialization", "ALL COMPLETED");

        // Database connection
        new FetchDataFromServerTask().execute();

        /* Button Click Listeners*/
        // play & pause Button
        playPauseBtn.setOnClickListener(view -> {
            if (isRunning) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    pausedPosition = mediaPlayer.getCurrentPosition(); // Store the current position
                    handler.removeCallbacks(runnable); // Stop updating the timer
                }
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                runDotMatrixInBackground("Pausing");
                isRunning = false;
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(pausedPosition); // Resume from the stored position
                    mediaPlayer.start();
                    updateSeekBar(); // Start updating the timer again
                }
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                isRunning = true;
                runDotMatrixInBackground("Playing");
            }
        });

        // previous Button
        prevBtn.setOnClickListener(view -> {
            pausedPosition = 0;
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
            pausedPosition = 0;
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
            if (isMute) {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }
                isMute = false;
                muteBtn.setImageResource(R.drawable.baseline_volume_on_icon);
                runDotMatrixInBackground("Unmute");
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0.0f, 0.0f);
                }
                isMute = true;
                muteBtn.setImageResource(R.drawable.baseline_volume_mute_icon);
                runDotMatrixInBackground("Mute");
            }
        });
    }

    /* mp3 downloads */
    private void downloadAudioFile(String videoID) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/play/" + videoID);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                File sdcard = Environment.getExternalStorageDirectory();
                File file = new File(sdcard, videoID + ".mp3");

                FileOutputStream fileOutput = new FileOutputStream(file);
                InputStream inputStream = urlConnection.getInputStream();

                byte[] buffer = new byte[1024];
                int bufferLength;

                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                }
                fileOutput.close();

                Log.d("DownloadAudioFile", "File downloaded to: " + file.getPath());

                runOnUiThread(() -> {
                    currentAudioPath = file.getPath();
                    playAudio();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void playAudio() {
        if (currentAudioPath != null) {
            if (mediaPlayer != null) {
                mediaPlayer.reset(); // Reset the MediaPlayer to ensure new source is set
            } else {
                mediaPlayer = new MediaPlayer();
            }
            try {
                mediaPlayer.setDataSource(currentAudioPath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                updateSeekBar(); // Start updating the timer
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("MP3", "No audio to play");
        }
    }

    private void updateSeekBar() {
        handler = new Handler();
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    timeElapsedDisplay.setText(millisecondsToTime(currentPosition));
                    handler.postDelayed(this, 1000); // Update every second
                }
            }
        }, 1000);
    }

    private String millisecondsToTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
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
                URL url = new URL(SERVER_URL+"/data");
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
            downloadAudioFile(videoID);

            // set texts
            textLCDout(title, channelName);
            titleDisplay.setText(title);
            channelNameDisplay.setText(channelName);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
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