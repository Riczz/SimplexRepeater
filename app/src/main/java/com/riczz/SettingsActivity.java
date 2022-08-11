package com.riczz;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.riczz.simplexrepeater.ConfigBarChangeListener;
import com.riczz.simplexrepeater.MainActivity;
import com.riczz.simplexrepeater.R;
import com.riczz.simplexrepeater.config.Config;
import com.riczz.simplexrepeater.config.ConfigContract;
import com.riczz.simplexrepeater.config.ConfigHelper;

public final class SettingsActivity extends AppCompatActivity {

    private static final String LOG_TAG = SettingsActivity.class.getName();
    private final ConfigHelper dbHelper = new ConfigHelper(this);

    private TextView voiceInputLevelTextView;
    private SeekBar voiceInputLevel;

    private TextView voiceCutoffLevelTextView;
    private SeekBar voiceCutoffLevel;

    private TextView voiceCutoffDelayMsTextView;
    private SeekBar voiceCutoffDelayMs;

    private TextView playbackDelayMsTextView;
    private SeekBar playbackDelayMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        this.voiceInputLevelTextView = findViewById(R.id.voiceInputLevelTextView);
        this.voiceCutoffLevelTextView = findViewById(R.id.voiceCutoffLevelTextView);
        this.voiceCutoffDelayMsTextView = findViewById(R.id.voiceCutoffDelayMsTextView);
        this.playbackDelayMsTextView = findViewById(R.id.playbackDelayMsTextView);

        this.voiceInputLevel = findViewById(R.id.voiceInputLevel);
        this.voiceCutoffLevel = findViewById(R.id.voiceCutoffLevel);
        this.voiceCutoffDelayMs = findViewById(R.id.voiceCutoffDelay);
        this.playbackDelayMs = findViewById(R.id.playbackDelayMs);

        this.voiceInputLevel.setOnSeekBarChangeListener(new ConfigBarChangeListener(this,
                voiceInputLevelTextView,
                R.string.voice_input_level
        ));

        this.voiceCutoffLevel.setOnSeekBarChangeListener(new ConfigBarChangeListener(this,
                voiceCutoffLevelTextView,
                R.string.voice_cutoff_level
        ));

        this.voiceCutoffDelayMs.setOnSeekBarChangeListener(new ConfigBarChangeListener(this,
                voiceCutoffDelayMsTextView,
                R.string.voice_cutoff_delay_ms
        ));

        this.playbackDelayMs.setOnSeekBarChangeListener(new ConfigBarChangeListener(this,
                playbackDelayMsTextView,
                R.string.playback_delay_ms
        ));

        queryConfig(dbHelper);
        setButtons();

    }

    public static void queryConfig(ConfigHelper dbHelper) {
        queryConfig(dbHelper, true);
    }

    @SuppressLint("Range")
    public static void queryConfig(ConfigHelper dbHelper, boolean insertRow) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                ConfigContract.Entry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );

        if (cursor.moveToLast()) {
            double voiceInputLevel = cursor.getDouble(cursor.getColumnIndex(ConfigContract.Entry.COLUMN_NAME_VOICE_INPUT_LEVEL));
            double voiceCutoffLevel = cursor.getDouble(cursor.getColumnIndex(ConfigContract.Entry.COLUMN_NAME_VOICE_CUTOFF_LEVEL));
            double voiceCutoffDelayMs = cursor.getDouble(cursor.getColumnIndex(ConfigContract.Entry.COLUMN_NAME_VOICE_CUTOFF_DELAY_MS));
            double playbackDelayMs = cursor.getDouble(cursor.getColumnIndex(ConfigContract.Entry.COLUMN_NAME_PLAYBACK_DELAY_MS));

            Config.voiceInputLevel = voiceInputLevel;
            Config.voiceCutoffLevel = voiceCutoffLevel;
            Config.voiceCutoffDelayMs = voiceCutoffDelayMs;
            Config.playbackDelayMs = playbackDelayMs;
        } else if (insertRow) {
            db.execSQL(ConfigContract.SQL_INSERT_ROW);
            queryConfig(dbHelper, false);
        }
        db.close();
        cursor.close();
    }

    public void updateConfig(View view) {
        final double voiceInputLevelValue = this.voiceInputLevel.getProgress();
        final double voiceCutoffLevelValue = this.voiceCutoffLevel.getProgress();
        final double voiceCutoffDelayMsValue = this.voiceCutoffDelayMs.getProgress();
        final double playbackDelayMsValue = this.playbackDelayMs.getProgress();

        if (voiceInputLevelValue == Config.voiceInputLevel && voiceCutoffLevelValue == Config.voiceCutoffLevel &&
                voiceCutoffDelayMsValue == Config.voiceCutoffDelayMs && playbackDelayMsValue == Config.playbackDelayMs) {
            Toast.makeText(this, "Nem történt változás.", Toast.LENGTH_SHORT).show();
            return;
        }

        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final ContentValues contentValues = new ContentValues();

        contentValues.put(ConfigContract.Entry.COLUMN_NAME_VOICE_INPUT_LEVEL, voiceInputLevelValue);
        contentValues.put(ConfigContract.Entry.COLUMN_NAME_VOICE_CUTOFF_LEVEL, voiceCutoffLevelValue);
        contentValues.put(ConfigContract.Entry.COLUMN_NAME_VOICE_CUTOFF_DELAY_MS, voiceCutoffDelayMsValue);
        contentValues.put(ConfigContract.Entry.COLUMN_NAME_PLAYBACK_DELAY_MS, playbackDelayMsValue);
        if (contentValues.size() == 0) return;

        int affectedRows =
                db.update(ConfigContract.Entry.TABLE_NAME, contentValues, null, null);

        String result = affectedRows > 0 ? "Adatok frissítve." : "Hiba az adatok frissítése közben!";
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        Log.d(LOG_TAG, result);
        queryConfig(dbHelper);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setButtons() {
        this.voiceInputLevelTextView.setText(getString(R.string.voice_input_level, String.valueOf((int) Config.voiceInputLevel)));
        this.voiceCutoffLevelTextView.setText(getString(R.string.voice_cutoff_level, String.valueOf((int) Config.voiceCutoffLevel)));
        this.voiceCutoffDelayMsTextView.setText(getString(R.string.voice_cutoff_delay_ms, String.valueOf((int) Config.voiceCutoffDelayMs)));
        this.playbackDelayMsTextView.setText(getString(R.string.playback_delay_ms, String.valueOf((int) Config.playbackDelayMs)));

        this.voiceInputLevel.setProgress((int) Config.voiceInputLevel);
        this.voiceCutoffLevel.setProgress((int) Config.voiceCutoffLevel);
        this.voiceCutoffDelayMs.setProgress((int) Config.voiceCutoffDelayMs);
        this.playbackDelayMs.setProgress((int) Config.playbackDelayMs);

    }
}
