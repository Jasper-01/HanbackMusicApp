package com.example.hanbackmusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.hanbackmusicapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'hanbackmusicapp' library on application startup.
    static {
        System.loadLibrary("hanbackmusicapp");
    }

    private ActivityMainBinding binding;

    private TextView timerDisplay;
    private Boolean timerIsRunning;
    private Button playPauseBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        timerDisplay = findViewById(R.id.timerDisplay);
        playPauseBtn = findViewById(R.id.playPauseBtn);

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(timerIsRunning){
                    playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_pause_icon);
                    timerPause();
                } else{
                    playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_play_icon);
                    timerStart();
                }
            }
        });
    }

    public void timerStart(){
        timerIsRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                startTimerNative(timerDisplay);
            }
        });
    }

    public void timerPause(){
        timerIsRunning = false;
        pauseTimerNative();
    }

    public void timerReset(){
        timerIsRunning = false;
        resetTimerNative();
        timerDisplay.setText(R.string.timerRstDisplayVal);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timerIsRunning){
            timerPause();
        }
    }

    public native void startTimerNative(View textView);
    public native void pauseTimerNative();
    public native void resetTimerNative();
}