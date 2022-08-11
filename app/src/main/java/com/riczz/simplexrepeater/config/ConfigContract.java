package com.riczz.simplexrepeater.config;

import android.provider.BaseColumns;

import java.util.Locale;

public final class ConfigContract {

    private ConfigContract() {
    }

    public static final class Entry implements BaseColumns {
        public static final String
                TABLE_NAME = "config",
                COLUMN_NAME_VOICE_INPUT_LEVEL = "voice_input_level",
                COLUMN_NAME_VOICE_CUTOFF_LEVEL = "voice_cutoff_level",
                COLUMN_NAME_VOICE_CUTOFF_DELAY_MS = "voice_cutoff_delay_ms",
                COLUMN_NAME_PLAYBACK_DELAY_MS = "playback_delay_ms";
    }

    public static final String SQL_INSERT_ROW = String
            .format("INSERT INTO %s VALUES ('%s','%s','%s','%s');",
                    Entry.TABLE_NAME,
                    Config.voiceInputLevel, Config.voiceCutoffLevel,
                    Config.voiceCutoffDelayMs, Config.playbackDelayMs);

    public static final String SQL_CREATE_TABLE = String
            .format(Locale.ROOT, "CREATE TABLE IF NOT EXISTS %s (%s REAL, %s REAL, %s REAL, %s REAL);",
                    Entry.TABLE_NAME, Entry.COLUMN_NAME_VOICE_INPUT_LEVEL,
                    Entry.COLUMN_NAME_VOICE_CUTOFF_LEVEL, Entry.COLUMN_NAME_VOICE_CUTOFF_DELAY_MS,
                    Entry.COLUMN_NAME_PLAYBACK_DELAY_MS);

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + Entry.TABLE_NAME + ";";
}
