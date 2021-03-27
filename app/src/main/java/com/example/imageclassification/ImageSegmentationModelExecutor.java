package com.example.imageclassification;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.graphics.ColorUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Interpreter.Options;

import org.tensorflow.lite.gpu.GpuDelegate;


public final class ImageSegmentationModelExecutor {
    private GpuDelegate gpuDelegate;
    private ByteBuffer segmentationMasks;
    private Interpreter interpreter;
    private int numberThreads;
    private boolean useGPU;

    public static final String TAG = "SegmentationInterpreter";
    private static final String imageSegmentationModel = "deeplabv3_257_mv_gpu.tflite";
    private static final int imageSize = 257;
    public static final int NUM_CLASSES = 21;
    private static final float IMAGE_MEAN = 127.5F;
    private static final float IMAGE_STD = 127.5F;

    private static final int[] segmentColors = new int[21];

    private static final String[] labelsArrays = new String[]{"background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair", "cow", "dining table", "dog", "horse", "motorbike", "person", "potted plant", "sheep", "sofa", "train", "tv"};

    public ImageSegmentationModelExecutor(Context context) {
        this.numberThreads = 4;
        try {
            Options tfliteOptions = new Options();
            tfliteOptions.setNumThreads(this.numberThreads);
            interpreter = new Interpreter((ByteBuffer) this.loadModelFile(context, imageSegmentationModel), tfliteOptions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4 * imageSize * imageSize);
        this.segmentationMasks = byteBuffer;
        this.segmentationMasks.order(ByteOrder.nativeOrder());
        Random random = new Random(System.currentTimeMillis());
        segmentColors[0] = Color.TRANSPARENT;
        for (int i = 1;i< NUM_CLASSES ; i++) {
            segmentColors[i] = Color.argb(128, getRandomRGBInt(random),getRandomRGBInt(random),getRandomRGBInt(random));
        }
    }


    public  Recognition segmentImage(Bitmap data) {
        Bitmap emptyBitmap;
        try {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(data, imageSize, imageSize, false);
            ByteBuffer contentArray = ImageUtils.bitmapToByteBuffer(scaledBitmap, imageSize, imageSize, IMAGE_MEAN, IMAGE_STD);
            this.interpreter.run(contentArray, this.segmentationMasks);
            this.convertBytebufferMaskToBitmap(this.segmentationMasks, imageSize, imageSize, scaledBitmap, segmentColors,scaledBitmap);
            return new Recognition(resultBitmap, scaledBitmap, maskBitmap,transparentImage,  itemsFound);
        } catch (Exception var8) {
            emptyBitmap =ImageUtils.createEmptyBitmap$default( 257, 257, 0, 4, (Object) null);
            return new Recognition(emptyBitmap, emptyBitmap, emptyBitmap,emptyBitmap,  (Map) (new HashMap()));
        }
    }


    private final MappedByteBuffer loadModelFile(Context context, String modelFile) throws IOException {
        AssetFileDescriptor var10000 = context.getAssets().openFd(modelFile);
        AssetFileDescriptor fileDescriptor = var10000;
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer retFile = fileChannel.map(MapMode.READ_ONLY, startOffset, declaredLength);
        fileDescriptor.close();
        return retFile;
    }


    HashMap itemsFound;
    Bitmap maskBitmap,resultBitmap,scaledBackgroundImage,transparentImage;
    private void convertBytebufferMaskToBitmap(ByteBuffer inputBuffer, int imageWidth, int imageHeight, Bitmap backgroundImage, int[] colors,Bitmap scaledBitmap) {
        Config conf = Config.ARGB_8888;
        maskBitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf);
        transparentImage = Bitmap.createBitmap(imageWidth, imageHeight, conf);
        resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf);
        scaledBackgroundImage = ImageUtils.scaleBitmapAndKeepRatio(backgroundImage, imageWidth, imageHeight);

        int[][] mSegmentBits = new int[imageWidth][imageHeight];
        itemsFound = new HashMap();
        inputBuffer.rewind();
        for (int y = 0; y < imageHeight; y++) {
            for (int x=0;x< imageWidth; x++) {
                float maxVal = 0.0F;
                mSegmentBits[x][y] = 0;
                for (int c= 0;c<NUM_CLASSES;c++) {
                    float value = inputBuffer.getFloat((y * imageWidth * NUM_CLASSES + x * NUM_CLASSES + c) * 4);
                    if (c == 0 || value > maxVal) {
                        maxVal = value;
                        mSegmentBits[x][y] = c;
                    }
                }
                String label = labelsArrays[mSegmentBits[x][y]];
                int color = colors[mSegmentBits[x][y]];
                itemsFound.put(label, color);
                int newPixelColor = ColorUtils.compositeColors(colors[mSegmentBits[x][y]], scaledBackgroundImage.getPixel(x, y));
                resultBitmap.setPixel(x, y, newPixelColor);
                if(mSegmentBits[x][y] != 0){
                    transparentImage.setPixel(x, y, scaledBitmap.getPixel(x,y));
                }
                maskBitmap.setPixel(x, y, colors[mSegmentBits[x][y]]);
            }
        }
    }



    private final int getRandomRGBInt(Random random) {
        return (int)((float)255 * random.nextFloat());
    }


    public class Recognition{
        Bitmap bitmapResult;
        Bitmap bitmapOriginal;
        Bitmap bitmapMaskOnly;
        Bitmap transparentImage;
        // A map between labels and colors of the items found.
        Map<String, Integer> itemsFound;

        public Recognition(Bitmap bitmapResult, Bitmap bitmapOriginal, Bitmap bitmapMaskOnly,Bitmap transparentImage,  Map<String, Integer> itemsFound) {
            this.bitmapResult = bitmapResult;
            this.bitmapOriginal = bitmapOriginal;
            this.bitmapMaskOnly = bitmapMaskOnly;
            this.transparentImage = transparentImage;
            this.itemsFound = itemsFound;
        }

        public Recognition() {
        }

        public Bitmap getBitmapResult() {
            return bitmapResult;
        }

        public void setBitmapResult(Bitmap bitmapResult) {
            this.bitmapResult = bitmapResult;
        }

        public Bitmap getBitmapOriginal() {
            return bitmapOriginal;
        }

        public void setBitmapOriginal(Bitmap bitmapOriginal) {
            this.bitmapOriginal = bitmapOriginal;
        }

        public Bitmap getBitmapMaskOnly() {
            return bitmapMaskOnly;
        }

        public void setBitmapMaskOnly(Bitmap bitmapMaskOnly) {
            this.bitmapMaskOnly = bitmapMaskOnly;
        }

        public Bitmap getTransparentImage() {
            return transparentImage;
        }

        public void setTransparentImage(Bitmap transparentImage) {
            this.transparentImage = transparentImage;
        }

        public Map<String, Integer> getItemsFound() {
            return itemsFound;
        }

        public void setItemsFound(Map<String, Integer> itemsFound) {
            this.itemsFound = itemsFound;
        }
    }

}