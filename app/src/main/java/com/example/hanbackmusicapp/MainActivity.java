package com.example.hanbackmusicapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
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

import nl.changer.audiowife.AudioWife;

public class MainActivity extends AppCompatActivity {

    Context mContext = MainActivity.this;
    private static final String TAG = MainActivity.class.getSimpleName();
    // TODO: Update address
    private static final String SERVER_URL = "http://ec2-54-147-134-213.compute-1.amazonaws.com:3000";
    private Boolean isRunning;

    // Database
    private JSONArray jsonArray;
    private int currentIndex = 0;
    private String currentAudioPath;
    private Context context;


    private ActivityMainBinding binding;
    // player
    ImageView mPlayMedia;
    ImageView mPauseMedia;
    SeekBar mMediaSeekBar;
    TextView mRunTime;
    TextView mTotalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Request necessary permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            new FetchDataFromServerTask().execute();
        }

        // initialize the player controls
//        mMediaSeekBar = (SeekBar) findViewById(R.id.media_seekbar);
//        mRunTime = (TextView) findViewById(R.id.run_time);
//        mTotalTime = (TextView) findViewById(R.id.total_time);

        ImageButton playPauseBtn = findViewById(R.id.playPauseButton);
        ImageButton prevBtn = findViewById(R.id.prevBtn);
        ImageButton nextBtn = findViewById(R.id.nextBtn);

//        WebView webView = findViewById(R.id.webVid);
//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        webView.setWebViewClient(new WebViewClient());

//        AudioWife.getInstance().init(mContext, uri)
//                .useDefaultUi(mPlayerContainer, getLayoutInflater());

        // initialize the player controls
        ImageView mPlayMedia = findViewById(R.id.play);
        ImageView mPauseMedia = findViewById(R.id.pause);
        SeekBar mMediaSeekBar = (SeekBar) findViewById(R.id.media_seekbar);
        TextView mRunTime = (TextView) findViewById(R.id.run_time);
        TextView mTotalTime = (TextView) findViewById(R.id.total_time);

        Log.d("Initialization", "ALL COMPLETED");

        playPauseBtn.setOnClickListener(view -> {
            if (isRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
//                pauseAudio();
                AudioWife.getInstance().pause();
                isRunning = false;
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
//                playAudio();
                AudioWife.getInstance().play();
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
            }
        });

        nextBtn.setOnClickListener(view -> {
            if (jsonArray != null && jsonArray.length() > 0) {
                currentIndex++;
                if (currentIndex >= jsonArray.length()) {
                    currentIndex = 0;
                }
                displayCurrentItem();
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
//            WebView webView = findViewById(R.id.webVid);
            Log.d("Current item", "Trying to display items");
            JSONObject jsonObject = jsonArray.getJSONObject(currentIndex);
            String title = jsonObject.getString("title");
            String channelName = jsonObject.getString("channelName");
            String videoID = jsonObject.getString("videoId");

            titleDisplay.setText(title);
            channelNameDisplay.setText(channelName);

            String url = SERVER_URL + "/play/" + videoID;
//            webView.loadUrl(url);
            Log.d("DownloadAudioFile", "Video to download "+videoID);
            isRunning = false;
            downloadAudioFile(videoID);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying current item: " + e.getMessage());
        }
    }

    private void downloadAudioFile(String videoID) {
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

            Log.d(TAG, "File downloaded to: " + file.getPath());

//                runOnUiThread(() -> {
//                    currentAudioPath = file.getPath();
//                    initializeMediaPlayer(Uri.parse(currentAudioPath));
//                });

            currentAudioPath = file.getPath();
            initializeMediaPlayer(Uri.parse(currentAudioPath));

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Download failed: " + e.getMessage());
            Toast.makeText(MainActivity.this, "Download Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeMediaPlayer(Uri audioUri) {
        AudioWife.getInstance()
                .init(mContext, audioUri)
                .setPlayView(findViewById(R.id.play))
                .setPauseView(findViewById(R.id.pause))
                .setSeekBar((SeekBar) findViewById(R.id.media_seekbar))
                .setRuntimeView((TextView) findViewById(R.id.run_time))
                .setTotalTimeView((TextView) findViewById(R.id.total_time))
                .useDefaultUi(binding.getRoot(), getLayoutInflater());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioWife.getInstance().release();
    }
}