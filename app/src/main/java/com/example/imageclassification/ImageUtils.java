package com.example.imageclassification;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix.ScaleToFit;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;



public abstract class ImageUtils {

    private final Matrix decodeExifOrientation(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case 0:
            case 1:
                break;
            case 2:
                matrix.postScale(-1.0F, 1.0F);
                break;
            case 3:
                matrix.postRotate(180.0F);
                break;
            case 4:
                matrix.postScale(1.0F, -1.0F);
                break;
            case 5:
                matrix.postScale(-1.0F, 1.0F);
                matrix.postRotate(270.0F);
                break;
            case 6:
                matrix.postRotate(90.0F);
                break;
            case 7:
                matrix.postScale(-1.0F, 1.0F);
                matrix.postRotate(90.0F);
                break;
            case 8:
                matrix.postRotate(270.0F);
                break;

        }

        return matrix;
    }

    public final void setExifOrientation(String filePath, String value) throws IOException {
        ExifInterface exif = new ExifInterface(filePath);
        exif.setAttribute("Orientation", value);
        exif.saveAttributes();
    }

    public final int computeExifOrientation(int rotationDegrees, boolean mirrored) {
        return rotationDegrees == 0 && !mirrored ? 1 : (rotationDegrees == 0 && mirrored ? 2 : (rotationDegrees == 180 && !mirrored ? 3 : (rotationDegrees == 180 && mirrored ? 4 : (rotationDegrees == 270 && mirrored ? 7 : (rotationDegrees == 90 && !mirrored ? 6 : (rotationDegrees == 90 && mirrored ? 5 : (rotationDegrees == 270 && mirrored ? 8 : (rotationDegrees == 270 && !mirrored ? 7 : 0))))))));
    }

    public final Bitmap decodeBitmap(File file) throws IOException {
        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
        Matrix transformation = decodeExifOrientation(exif.getAttributeInt("Orientation", 6));
        Options options = new Options();
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        Bitmap var10000 = BitmapFactory.decodeFile(file.getAbsolutePath());
        var10000 = Bitmap.createBitmap(var10000, 0, 0, bitmap.getWidth(), bitmap.getHeight(), transformation, true);
        return var10000;
    }


    public static final Bitmap scaleBitmapAndKeepRatio(Bitmap targetBmp, int reqHeightInPixels, int reqWidthInPixels) {
        if (targetBmp.getHeight() == reqHeightInPixels && targetBmp.getWidth() == reqWidthInPixels) {
            return targetBmp;
        } else {
            Matrix matrix = new Matrix();
            matrix.setRectToRect(new RectF(0.0F, 0.0F, (float) targetBmp.getWidth(), (float) targetBmp.getWidth()), new RectF(0.0F, 0.0F, (float) reqWidthInPixels, (float) reqHeightInPixels), ScaleToFit.FILL);
            Bitmap var10000 = Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.getWidth(), targetBmp.getWidth(), matrix, true);
            return var10000;
        }
    }


    public static final ByteBuffer bitmapToByteBuffer(Bitmap bitmapIn, int width, int height, float mean, float std) {

        Bitmap bitmap = scaleBitmapAndKeepRatio(bitmapIn, width, height);
        ByteBuffer inputImage = ByteBuffer.allocateDirect(1 * width * height * 3 * 4);
        inputImage.order(ByteOrder.nativeOrder());
        inputImage.rewind();
        int[] intValues = new int[width * height];
        bitmap.getPixels(intValues, 0, width, 0, 0, width, height);
        int pixel = 0;
        int var10 = 0;

        for (int var11 = height; var10 < var11; ++var10) {
            int var12 = 0;

            for (int var13 = width; var12 < var13; ++var12) {
                int value = intValues[pixel++];
                inputImage.putFloat(((float) (value >> 16 & 255) - mean) / std);
                inputImage.putFloat(((float) (value >> 8 & 255) - mean) / std);
                inputImage.putFloat(((float) (value & 255) - mean) / std);
            }
        }

        inputImage.rewind();

        return inputImage;
    }

    // $FF: synthetic method
    public static ByteBuffer bitmapToByteBuffer$default( Bitmap var1, int var2, int var3, float var4, float var5, int var6, Object var7) {
        if ((var6 & 8) != 0) {
            var4 = 0.0F;
        }

        if ((var6 & 16) != 0) {
            var5 = 255.0F;
        }

        return bitmapToByteBuffer(var1, var2, var3, var4, var5);
    }

    public static final Bitmap createEmptyBitmap(int imageWidth, int imageHeigth, int color) {
        Bitmap ret = Bitmap.createBitmap(imageWidth, imageHeigth, Config.RGB_565);
        if (color != 0) {
            ret.eraseColor(color);
        }
        return ret;
    }

    // $FF: synthetic method
    public static Bitmap createEmptyBitmap$default(int var1, int var2, int var3, int var4, Object var5) {
        if ((var4 & 4) != 0) {
            var3 = 0;
        }

        return createEmptyBitmap(var1, var2, var3);
    }


}



