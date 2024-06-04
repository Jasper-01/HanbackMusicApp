package com.example.hanbackmusicapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SERVER_URL = "http://ec2-3-80-50-248.compute-1.amazonaws.com:3000";
    private Boolean isRunning;
    private MediaPlayer mediaPlayer;
    private String currentAudioPath;
    private Context context;

    // Database
    private JSONArray jsonArray;
    private int currentIndex = 0;

    static {
        System.loadLibrary("hanbackmusicapp");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Request necessary permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            new FetchDataFromServerTask().execute();
        }

        ImageButton playPauseBtn = findViewById(R.id.playPauseButton);
        ImageButton prevBtn = findViewById(R.id.prevBtn);
        ImageButton nextBtn = findViewById(R.id.nextBtn);

        Log.d("Initialization", "ALL COMPLETED");

        playPauseBtn.setOnClickListener(view -> {
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                pauseAudio();
                isRunning = false;
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                playAudio();
                isRunning = true;
            }
        });

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new FetchDataFromServerTask().execute();
            } else {
                Toast.makeText(this, "Permissions required to download and play audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FetchDataFromServerTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder stringBuilder = new StringBuilder();
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(SERVER_URL + "/data");
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

    private void displayCurrentItem() {
        try {
            TextView titleDisplay = findViewById(R.id.titleDisplay);
            TextView channelNameDisplay = findViewById(R.id.channelNameDisplay);

            JSONObject jsonObject = jsonArray.getJSONObject(currentIndex);
            String title = jsonObject.getString("title");
            String channelName = jsonObject.getString("channelName");
            String videoID = jsonObject.getString("videoId");

            titleDisplay.setText(title);
            channelNameDisplay.setText(channelName);

            downloadAudioFile(videoID);

            isRunning = false;
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }

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
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download Failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void playAudio() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
            }

            File file = new File(currentAudioPath);
            if (!file.exists()) {
                Log.e(TAG, "File not found: " + currentAudioPath);
                Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Playing audio file: " + file.getAbsolutePath());

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getPath());

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    isRunning = true;

                    ImageButton playPauseBtn = findViewById(R.id.playPauseButton);
                    playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "MediaPlayer error: " + what + ", extra: " + extra);
                    Toast.makeText(MainActivity.this, "Error playing audio", Toast.LENGTH_SHORT).show();
                    return false; // Return true if the error is handled, false otherwise
                }
            });
            mediaPlayer.prepareAsync(); // Use prepareAsync for asynchronous preparation
        } catch (IOException e) {
            Log.e(TAG, "IOException while playing audio: " + e.getMessage());
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio: " + e.getMessage());
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }



    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }
}
