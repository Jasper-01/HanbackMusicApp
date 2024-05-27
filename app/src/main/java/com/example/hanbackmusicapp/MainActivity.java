package com.example.hanbackmusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;

import org.json.JSONArray;

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
    private TextView resultTextView;
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

        // Displayed Objects
        TextView timerDisplay = findViewById(R.id.timeElapsedDisplay);
        ImageButton playPauseBtn = findViewById(R.id.playPauseButton);
        ImageButton prevBtn = findViewById(R.id.prevBtn);
        ImageButton nextBtn = findViewById(R.id.nextBtn);

        /* Initialization */
        timerDisplay.setText(R.string.reset_timer_display);
        isRunning = false;
        pausedTimeElapsed = 0;

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

        /* Button Click Listeners*/
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

    public native void startTimer();
    public native void stopTimer();
    public native void resetTimer();
    public native String getElapsedTime();
}