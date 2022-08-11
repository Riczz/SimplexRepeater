package com.riczz.simplexrepeater;

import android.content.Context;
import android.widget.SeekBar;
import android.widget.TextView;

public final class ConfigBarChangeListener implements SeekBar.OnSeekBarChangeListener {

    Context context;
    TextView displayText;
    int stringResource;

    public ConfigBarChangeListener(Context context, TextView displayText, int stringResource) {
        this.context = context;
        this.displayText = displayText;
        this.stringResource = stringResource;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        this.displayText.setText(context.getString(stringResource, String.valueOf(i)));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
