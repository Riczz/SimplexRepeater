package com.riczz.simplexrepeater;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.masoudss.lib.WaveformSeekBar;
import com.riczz.SettingsActivity;
import com.riczz.simplexrepeater.config.Config;
import com.riczz.simplexrepeater.config.ConfigHelper;

public final class MainActivity extends AppCompatActivity {

    private TextView noiseLevel;
    private ImageView settings;
    private WaveformSeekBar waveformSeekBar;
    private WavAudioRecorder recorder;
    private MediaPlayer mediaPlayer;

    private Thread listenerThread;
    private boolean threadRunning = false;
    private boolean isRecording = false;

    private static final int NOISE_POLLING_RATE_MS = 500;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 200;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 201;

    private static final String FOLDER_NAME = "SimplexReceiver";
    private static final String TEMP_FILE_NAME = "record_temp.raw";
    private static final String LOG_TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.noiseLevel = findViewById(R.id.noiseLevel);
        this.waveformSeekBar = findViewById(R.id.waveform_seekbar);
        this.settings = findViewById(R.id.settings);

        this.settings.setOnClickListener(button -> {
            threadRunning = false;
            listenerThread.interrupt();

            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        SettingsActivity.queryConfig(new ConfigHelper(this));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestWriteExternalStoragePermission();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestRecordAudioPermission();
            return;
        }

        startRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestRecordAudioPermission();
            } else {
                requestWriteExternalStoragePermission();
            }
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                requestRecordAudioPermission();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startRecording();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        listenerThread.interrupt();
        if (recorder != null && recorder.isRunning()) {
            recorder.stopRecording();
            recorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void startRecording() {
        listenerThread = new Thread(() -> {
            threadRunning = true;

            double cutoffTime = 0.0d;
            recorder = new WavAudioRecorder(this, FOLDER_NAME, TEMP_FILE_NAME);
            recorder.start();

            while (!isRecording) {
                if (!threadRunning) return;
                recorder.readBuffer();
                double amplitude = recorder.getMaxAmplitude();

                if (amplitude > Config.voiceInputLevel) {
                    runOnUiThread(() -> noiseLevel.setText(getString(R.string.record_audio)));
                    isRecording = true;
                    break;
                }

                runOnUiThread(() -> noiseLevel.setText(getString(R.string.noise_template,
                        String.valueOf(amplitude),
                        String.valueOf(Config.voiceInputLevel)
                )));
                SystemClock.sleep(NOISE_POLLING_RATE_MS);
            }

            while (isRecording) {
                if (!threadRunning) return;
                long startTime = System.nanoTime();

                recorder.writeToOutputFile();
                double amplitude = recorder.getMaxAmplitude();

                if (amplitude < Config.voiceCutoffLevel) {
                    long endTime = System.nanoTime();
                    cutoffTime += ((endTime - startTime) / 1000000.0d);

                    if (cutoffTime > Config.voiceCutoffDelayMs) {
                        recorder.saveFile();
                        break;
                    }
                } else {
                    cutoffTime = 0.0d;
                }

                recorder.fillIntBuffer();
                waveformSeekBar.setSampleFrom(recorder.getIntBuffer());
            }
            isRecording = false;
            playbackRecording();
        });
        listenerThread.start();
    }

    private void playbackRecording() {
        waveformSeekBar.setSample(new int[0]);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(recorder.getFilePath());
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        recorder.stopRecording();
        recorder = null;

        runOnUiThread(() -> new CountDownTimer((long) Config.playbackDelayMs, 1000) {
            @Override
            public void onTick(long l) {
                noiseLevel.setText(getString(R.string.playback_template,
                        String.valueOf((long) Math.ceil(l / 1000.0d))));
            }

            @Override
            public void onFinish() {
                if (mediaPlayer == null) return;
                noiseLevel.setText(getString(R.string.playback));
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(player -> {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    startRecording();
                });
            }
        }.start());
    }

    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private void requestWriteExternalStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }
}