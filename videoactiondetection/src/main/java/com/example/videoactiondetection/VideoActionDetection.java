package com.example.videoactiondetection;

import static com.example.videoactiondetection.LogUtil.*;
import ohos.global.resource.RawFileEntry;
import ohos.global.resource.Resource;
import ohos.global.resource.ResourceManager;
import org.apache.tvm.Device;
import org.apache.tvm.Function;
import org.apache.tvm.Module;
import org.apache.tvm.NDArray;
import org.apache.tvm.TVMType;
import org.apache.tvm.TVMValue;
import java.io.*;
import java.util.Arrays;


public class VideoActionDetection {
    private static final String TAG = VideoActionDetection.class.getName();
    private static final String MODEL_GRAPH_FILE_PATH    = "resources/rawfile/graph.json";
    private static final String MODEL_CPU_LIB_FILE_PATH    = "resources/rawfile/deploy_lib.so";
    private static final String MODEL_CPU_LIB_FILE_NAME    = "deploy_lib.so";

    private static final String MODEL_PARAMS_FILE_PATH    = "resources/rawfile/params.bin";
    private static final String LABEL_PATH    = "resources/rawfile/actions.txt";
    private static final String LABEL_NAME    = "actions.txt";

    // TVM constants
    private static final int OUTPUT_INDEX           = 0;
    private static final String INPUT_NAME          = "input0";

    private Module graphExecutorModule;
    ResourceManager resManager;
    File cacheD;

    public VideoActionDetection(ResourceManager resourceManager, File cacheDir) {
        // load json graph
        String modelGraph;
        this.resManager = resourceManager;
        this.cacheD = cacheDir;
        LogUtil.info(TAG, "Reading json graph from: " + MODEL_GRAPH_FILE_PATH);
        RawFileEntry rawFileEntryModel = resourceManager.getRawFileEntry(MODEL_GRAPH_FILE_PATH);
        try {
            modelGraph = new String(getBytesFromRawFile(rawFileEntryModel));
        } catch (IOException e) {
            LogUtil.error(TAG, "Problem reading json graph file!" + e);
            return; //failure
        }

        // create java tvm device
        Device tvmDev = Device.cpu();

        RawFileEntry rawFileEntryModelLib = resourceManager.getRawFileEntry(MODEL_CPU_LIB_FILE_PATH);
        File file = null;
        try {
            file = getFileFromRawFile(MODEL_CPU_LIB_FILE_NAME, rawFileEntryModelLib, cacheDir);
        } catch (IOException e) {
            LogUtil.error(TAG, e.getMessage());
        }

        // tvm module for compiled functions
        LogUtil.info(TAG, "VideoActionDetection: Model File Path: " + file.getAbsolutePath());
        Module modelLib = Module.load(file.getAbsolutePath());

        // get global function module for graph executor
        Function runtimeCreFun = Function.getFunction("tvm.graph_executor.create");
        TVMValue runtimeCreFunRes = runtimeCreFun.pushArg(modelGraph)
                .pushArg(modelLib)
                .pushArg(tvmDev.deviceType)
                .pushArg(tvmDev.deviceId)
                .invoke();
        LogUtil.info(TAG, runtimeCreFun.toString());
        graphExecutorModule = runtimeCreFunRes.asModule();
        LogUtil.info(TAG, String.valueOf(graphExecutorModule.handle));

        // load parameters
        byte[] modelParams;
        LogUtil.info(TAG, "Reading model params from: " + MODEL_PARAMS_FILE_PATH);
        RawFileEntry rawFileEntryModelParams = resourceManager.getRawFileEntry(MODEL_PARAMS_FILE_PATH);
        try {
            modelParams = getBytesFromRawFile(rawFileEntryModelParams);
        } catch (IOException e) {
            LogUtil.error(TAG, "Problem reading model param file!" + e);
            return; //failure
        }
        LogUtil.info(TAG, "Reading model params completed: ");
        // get the function from the module(load parameters)
        Function loadParamFunc = graphExecutorModule.getFunction("load_params");
        loadParamFunc.pushArg(modelParams).invoke();

    }

    public String detectAction(float[] data, int channel, int depth, int height, int width) {

        NDArray inputNdArray = NDArray.empty(new long[]{1, channel, depth, height, width}, new TVMType("float32"));
        inputNdArray.copyFrom(data);
        Function setInputFunc = graphExecutorModule.getFunction("set_input");

        try {
            setInputFunc.pushArg(INPUT_NAME).pushArg(inputNdArray).invoke();
        } catch (Exception e) {
            LogUtil.info(TAG, "Video Action Detection: pusherror" + e.getMessage());
            e.printStackTrace();
        }

        setInputFunc.release();

        // get the function from the module(run it)
        LogUtil.info(TAG, "run function on target");
        Function runFunc = graphExecutorModule.getFunction("run");
        runFunc.invoke();
        // release tvm local variables
        runFunc.release();

        // get the function from the module(get output data)
        LogUtil.info(TAG, "get output data");
        NDArray outputNdArray = NDArray.empty(new long[]{1, 400}, new TVMType("float32"));
        Function getOutputFunc = graphExecutorModule.getFunction("get_output");
        getOutputFunc.pushArg(OUTPUT_INDEX).pushArg(outputNdArray).invoke();
        // release tvm local variables
        inputNdArray.release();
        float[] output = outputNdArray.asFloatArray();
        outputNdArray.release();
        getOutputFunc.release();

        // display the result from extracted output data
        if (null != output) {
            LogUtil.info(TAG, "Output length: " + output.length);
            LogUtil.info(TAG, "Output: " + Arrays.toString(output));
            int maxPosition = -1;
            float maxValue = -1;
            for (int j = 0; j < output.length; ++j) {
                if (output[j] > maxValue) {
                    maxValue = output[j];
                    maxPosition = j;
                }
            }
            LogUtil.info(TAG, "Output predicted: " + maxPosition);
            //return maxPosition;
            RawFileEntry rawFileEntrylabelfile = resManager.getRawFileEntry(LABEL_PATH);
            FileInputStream fin = null;

            String outputLabel = null;
            File ifile = null;
            try {
                ifile = getFileFromRawFile(LABEL_NAME, rawFileEntrylabelfile, cacheD);
                fin = new FileInputStream(ifile);
                BufferedReader bufferedReader;
                int i = 0;
                String[] labelArray = new String[(int) ifile.length()];
                String line;
                try (InputStreamReader inputStreamReader = new InputStreamReader(fin)) {
                    bufferedReader = new BufferedReader(inputStreamReader);
                    while ((line = bufferedReader.readLine()) != null) {
                        labelArray[i++] = line;
                    }
                    outputLabel = labelArray[maxPosition];
                }
            } catch (IOException e) {
                LogUtil.error(TAG, e.getMessage());
            }
            LogUtil.info(TAG, "Output label predicted is : " + outputLabel);
            return outputLabel;
        }

        LogUtil.info(TAG, "prediction finished");
        return null;
    }

    private static File getFileFromRawFile(String filename, RawFileEntry rawFileEntry, File cacheDir)
        throws IOException {
        byte[] buf;
        File file;
        FileOutputStream output = null;

        try {
            file = new File(cacheDir, filename);
            output = new FileOutputStream(file);
            Resource resource = rawFileEntry.openRawFile();
            buf = new byte[(int) rawFileEntry.openRawFileDescriptor().getFileSize()];
            int bytesRead = resource.read(buf);
            if (bytesRead != buf.length) {
                throw new IOException("Video Action Detection: Asset Read failed!!!");
            }
            output.write(buf, 0, bytesRead);

            return file;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static byte[] getBytesFromRawFile(RawFileEntry rawFileEntry)
            throws IOException {
        byte[] buf;

        try {
            Resource resource = rawFileEntry.openRawFile();
            buf = new byte[(int) rawFileEntry.openRawFileDescriptor().getFileSize()];
            int bytesRead = resource.read(buf);
            if (bytesRead != buf.length) {
                throw new IOException("Video Action Detection: Asset Read failed!!!");
            }
            return buf;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
