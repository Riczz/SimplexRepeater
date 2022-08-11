package com.riczz.simplexrepeater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavAudioRecorder extends AudioRecord {

    private static final int AUDIO_BPP = 16;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final String LOG_TAG = WavAudioRecorder.class.getName();

    private final Context context;
    private final String folderName;
    private final String tempFileName;
    private final String tempFilePath;
    private FileOutputStream outputStream;

    private final int bufferSize;
    private boolean isRunning = false;

    private byte[] byteBuffer;
    private short[] shortBuffer;
    private int[] intBuffer;

    @SuppressLint("MissingPermission")
    public WavAudioRecorder(Context context, String folderName, String tempFileName) {
        super(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 3);

        this.context = context;
        this.folderName = folderName;
        this.tempFileName = tempFileName;
        this.tempFilePath = getTempFilePath();

        this.bufferSize = getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 3;
        this.byteBuffer = new byte[bufferSize * 2];
        this.shortBuffer = new short[bufferSize];
        this.intBuffer = new int[bufferSize];
    }

    public void start() {
        if (isRunning) return;

        try {
            outputStream = new FileOutputStream(tempFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        startRecording();
        isRunning = true;
    }

    public void writeToOutputFile() {
        try {
            int readSize = readBuffer();
            ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer().put(shortBuffer);
            if (readSize != AudioRecord.ERROR_INVALID_OPERATION) {
                outputStream.write(byteBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (!isRunning) {
            return;
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        stop();
        release();
        isRunning = false;
    }

    public int readBuffer() {
        return read(shortBuffer, 0, bufferSize);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int[] getIntBuffer() {
        return intBuffer;
    }

    public short[] getShortBuffer() {
        return shortBuffer;
    }

    public byte[] getByteBuffer() {
        return byteBuffer;
    }

    public void fillIntBuffer() {
        for (int i = 0; i < bufferSize; i++) {
            intBuffer[i] = shortBuffer[i];
        }
    }

    public double getMaxAmplitude() {
        double sDataMax = 0;
        for (short sDatum : shortBuffer) {
            if (Math.abs(sDatum) >= sDataMax) {
                sDataMax = Math.abs(sDatum);
            }
        }
        return sDataMax;
    }

    public void saveFile() {
        copyWaveFile(getTempFilePath(), getFilePath());
        new File(getTempFilePath()).delete();
    }

    public String getFilePath() {
        String filepath = context.getExternalFilesDir(null).getPath();
        File file = new File(filepath, folderName);

        if (!file.exists()) file.mkdirs();
        return (file.getAbsolutePath() + File.separator + "recording" + ".wav");
    }

    public String getTempFilePath() {
        String filepath = context.getExternalFilesDir(null).getPath();
        File file = new File(filepath, folderName);
        if (!file.exists()) file.mkdirs();

        File tempFile = new File(filepath, tempFileName);
        if (tempFile.exists()) tempFile.delete();
        return (file.getAbsolutePath() + File.separator + tempFileName);
    }

    public void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = SAMPLE_RATE;
        int channels = 2;
        long byteRate = AUDIO_BPP * SAMPLE_RATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.i(LOG_TAG, "File size: " + totalDataLen);

            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeWaveFileHeader(
            FileOutputStream out,
            long totalAudioLen, long totalDataLen,
            long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        header[34] = AUDIO_BPP;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
