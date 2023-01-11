package com.hwj.codesearch.ocr;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public interface ImageTransactor {
    /**
     * Process a frame of image captured dynamically by a camera
     * @param data image data
     * @param frameMetadata metadata
     * @param graphicOverlay graphicOverlay
     */
    void process(ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay);

    /**
     * Process a still image
     * @param bitmap bitmap
     * @param graphicOverlay graphicOverlay
     */
    void process(Bitmap bitmap, GraphicOverlay graphicOverlay);

    /**
     * stop
     */
    void stop();
}
