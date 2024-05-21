package com.example.hanbackmusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private Boolean timerIsRunning;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long pausedTimeElapsed;  // Variable to store the paused elapsed time

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

        // Initialize timer
        timerDisplay.setText(R.string.reset_timer_display);
        timerIsRunning = false;
        pausedTimeElapsed = 0;

        // Timer handler and runnable to update the UI
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timerDisplay.setText(getElapsedTime());
                if (timerIsRunning) {
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        // Button click listeners
        playPauseBtn.setOnClickListener(view -> {
            if (timerIsRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                stopTimer();
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_icon);
                startTimer();
                timerHandler.post(timerRunnable);
            }
            timerIsRunning = !timerIsRunning;
        });

        prevBtn.setOnClickListener(view -> {
            resetTimer();
            timerDisplay.setText(R.string.reset_timer_display);
            if (timerIsRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                timerHandler.removeCallbacks(timerRunnable);
                timerIsRunning = false;
            }
            pausedTimeElapsed = 0;
        });

        nextBtn.setOnClickListener(view -> {
            resetTimer();
            timerDisplay.setText(R.string.reset_timer_display);
            if (timerIsRunning) {
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_icon);
                timerHandler.removeCallbacks(timerRunnable);
                timerIsRunning = false;
            }
            pausedTimeElapsed = 0;
        });
    }

    public native void startTimer();
    public native void stopTimer();
    public native void resetTimer();
    public native String getElapsedTime();
}