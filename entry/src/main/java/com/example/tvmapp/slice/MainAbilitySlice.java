package com.example.tvmapp.slice;


import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.global.resource.RawFileEntry;
import ohos.global.resource.Resource;
import com.example.tvmapp.ResourceTable;
import com.example.videoactiondetection.VideoActionDetection;
import java.io.*;

public class MainAbilitySlice extends AbilitySlice {
    private static final String TAG = MainAbilitySlice.class.getName();

    private static final int IMG_CHANNEL            = 3;
    private static final int IMG_DEPTH              = 240;
    private static final int MODEL_INPUT_HEIGHT       = 90;
    private static final int MODEL_INPUT_WIDTH       = 90;


    private static final String IPATH    = "entry/resources/rawfile/climbing-90-90-30-8sec.txt";
    private static final String IMAGE_NAME    = "climbing-90-90-30-8sec.txt";

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);

        RawFileEntry rawFileEntryImage = getResourceManager().getRawFileEntry(IPATH);
        File ifile = null;
        FileInputStream fin = null;
        LogUtil.info(TAG, "Start Reading image file ");
        try {
            ifile = getFileFromRawFile(IMAGE_NAME, rawFileEntryImage, getCacheDir());
        } catch (IOException e) {
            LogUtil.error(TAG, e.getMessage());
            return;
        }
        try {
            fin = new FileInputStream(ifile);
        } catch (FileNotFoundException e) {
            LogUtil.error(TAG, e.getMessage());
            return;
        }
        InputStreamReader inputStreamReader = new InputStreamReader(fin);
        int size = IMG_CHANNEL * MODEL_INPUT_HEIGHT * MODEL_INPUT_WIDTH * IMG_DEPTH;
        float[] data = new float[size];
        int i = 0;
        LogUtil.info(TAG, "Assigning float completed: ");
        try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                data[i++] = Float.parseFloat(line);
            }
        } catch (IOException e) {
            LogUtil.error(TAG, e.getMessage());
            return;
        }

        // get the function from the module(set input data)
        // pre-process the image rgb data transpose based on the provided parameters.
        VideoActionDetection detect = new VideoActionDetection(getResourceManager(), getCacheDir());
        String action = detect.detectAction(data, IMG_CHANNEL, IMG_DEPTH, MODEL_INPUT_HEIGHT, MODEL_INPUT_WIDTH);

        LogUtil.info(TAG, "prediction finished : " + action);

    }

    private static File getFileFromRawFile(String filename, RawFileEntry rawFileEntry, File cacheDir)
            throws IOException {
        byte[] buf1 = null;
        File file1;
        file1 = new File(cacheDir, filename);
        try (FileOutputStream output1 = new FileOutputStream(file1)) {
            Resource resource = rawFileEntry.openRawFile();
            buf1 = new byte[(int) rawFileEntry.openRawFileDescriptor().getFileSize()];
            int bytesRead = resource.read(buf1);
            if (bytesRead != buf1.length) {
                throw new IOException("Asset Read failed!!!");
            }
            output1.write(buf1, 0, bytesRead);
            return file1;
        }
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }
}
