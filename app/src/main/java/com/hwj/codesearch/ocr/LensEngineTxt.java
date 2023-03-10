package com.hwj.codesearch.ocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.huawei.hms.common.size.Size;

import java.io.IOException;
import java.lang.Thread.State;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

public class LensEngineTxt {
    private static final String TAG = "LensEngine";
    protected Activity activity;
    private Camera camera;
    private Thread transactingThread;
    private final FrameTransactingRunnable transactingRunnable;
    private final Object transactorLock = new Object();
    private ImageTransactor frameTransactor;
    private CameraSelector selector;
    private final Map<byte[], ByteBuffer> bytesToByteBuffer = new IdentityHashMap<>();
    private GraphicOverlay overlay;

    public LensEngineTxt(Activity activity, CameraConfiguration configuration, GraphicOverlay graphicOverlay) {
        this.activity = activity;
        this.transactingRunnable = new FrameTransactingRunnable();
        this.selector = new CameraSelector(activity, configuration);
        this.overlay = graphicOverlay;
        this.overlay.clear();
    }

    /**
     * Stop the camera and release the resources of the camera and analyzer.
     */
    public void release() {
        synchronized (this.transactorLock) {
            this.stop();
            this.transactingRunnable.release();
            if (this.frameTransactor != null) {
                this.frameTransactor.stop();
                this.frameTransactor = null;
            }
        }
    }

    /**
     * Turn on the camera and start sending preview frames to the analyzer for detection.
     *
     * @throws IOException IO Exception
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    public synchronized LensEngineTxt run() throws IOException {
        if (this.camera != null) {
            return this;
        }
        this.camera = this.createCamera();
        this.camera.startPreview();
        this.initializeOverlay();
        this.transactingThread = new Thread(this.transactingRunnable);
        this.transactingRunnable.setActive(true);
        this.transactingThread.start();
        return this;
    }

    /**
     * Take pictures.
     *
     * @param pictureCallback Callback function after obtaining photo data.
     */
    public synchronized void takePicture(android.hardware.Camera.PictureCallback pictureCallback) {
        synchronized (this.transactorLock) {
            if (this.camera != null) {
                this.camera.takePicture(null, null, null, pictureCallback);
            }
        }
    }

    public synchronized Camera getCamera() {
        return this.camera;
    }

    private void initializeOverlay() {
        if (this.overlay != null) {
            int min;
            int max;
            Size size = this.getPreviewSize();
            min = Math.min(size.getWidth(), size.getHeight());
            max = Math.max(size.getWidth(), size.getHeight());

            if (this.activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                this.overlay.setCameraInfo(min, max, this.getFacing());
            } else {
                this.overlay.setCameraInfo(max, min, this.getFacing());
            }
            this.overlay.clear();
        }
    }

    /**
     * Get camera preview size.
     *
     * @return Size Size of camera preview.
     */
    public Size getPreviewSize() {
        return this.selector.getPreviewSize();
    }

    public int getFacing() {
        return this.selector.getFacing();
    }

    /**
     * Turn off the camera and stop transmitting frames to the analyzer.
     */
    public synchronized void stop() {
        this.transactingRunnable.setActive(false);
        if (this.transactingThread != null) {
            try {
                this.transactingThread.join();
            } catch (InterruptedException e) {
                Log.d(LensEngineTxt.TAG, "Frame transacting thread interrupted on release.");
            }
            this.transactingThread = null;
        }
        if (this.camera != null) {
            this.camera.stopPreview();
            this.camera.setPreviewCallbackWithBuffer(null);
            try {
                this.camera.setPreviewDisplay(null);
            } catch (Exception e) {
                Log.e(LensEngineTxt.TAG, "Failed to clear camera preview: " + e);
            }
            this.camera.release();
            this.camera = null;
        }
        this.bytesToByteBuffer.clear();
    }

    @SuppressLint("InlinedApi")
    private Camera createCamera() throws IOException {
        Camera cameraCreat = this.selector.createCamera();
        cameraCreat.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
        cameraCreat.addCallbackBuffer(this.createPreviewBuffer(this.selector.getPreviewSize()));
        cameraCreat.addCallbackBuffer(this.createPreviewBuffer(this.selector.getPreviewSize()));
        cameraCreat.addCallbackBuffer(this.createPreviewBuffer(this.selector.getPreviewSize()));
        cameraCreat.addCallbackBuffer(this.createPreviewBuffer(this.selector.getPreviewSize()));
        return cameraCreat;
    }

    /**
     * Create a buffer for the camera preview callback. The size of the buffer is based on the camera preview size and the camera image format.
     *
     * @param previewSize preview size
     * @return Image data from the camera
     */
    @SuppressLint("InlinedApi")
    private byte[] createPreviewBuffer(Size previewSize) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        long sizeInBits = (long) previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

        byte[] byteArray = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        if (!buffer.hasArray() || (buffer.array() != byteArray)) {
            throw new IllegalStateException("Failed to create valid buffer for lensEngine.");
        }
        this.bytesToByteBuffer.put(byteArray, buffer);
        return byteArray;
    }

    private class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            LensEngineTxt.this.transactingRunnable.setNextFrame(data, camera);
        }
    }

    public void setMachineLearningFrameTransactor(ImageTransactor transactor) {
        synchronized (this.transactorLock) {
            if (this.frameTransactor != null) {
                this.frameTransactor.stop();
            }
            this.frameTransactor = transactor;
        }
    }

    /**
     * It is used to receive the frame captured by the camera and pass it to the analyzer.
     */
    private class FrameTransactingRunnable implements Runnable {
        private final Object lock = new Object();
        private boolean active = true;
        private ByteBuffer pendingFrameData;

        FrameTransactingRunnable() {
        }

        /**
         * Frees the transactor and can safely perform this operation only after the associated thread has completed.
         */
        @SuppressLint("Assert")
        void release() {
            synchronized (this.lock) {
                assert (LensEngineTxt.this.transactingThread.getState() == State.TERMINATED);
            }
        }

        void setActive(boolean active) {
            synchronized (this.lock) {
                this.active = active;
                this.lock.notifyAll();
            }
        }

        /**
         * Sets the frame data received from the camera. Adds a previously unused frame buffer (if exit) back to the camera.
         */
        void setNextFrame(byte[] data, Camera camera) {
            synchronized (this.lock) {
                if (this.pendingFrameData != null) {
                    camera.addCallbackBuffer(this.pendingFrameData.array());
                    this.pendingFrameData = null;
                }
                if (!LensEngineTxt.this.bytesToByteBuffer.containsKey(data)) {
                    Log.d(LensEngineTxt.TAG, "Skipping frame. Could not find ByteBuffer associated with the image "
                            + "data from the camera.");
                    return;
                }
                this.pendingFrameData = LensEngineTxt.this.bytesToByteBuffer.get(data);
                this.lock.notifyAll();
            }
        }

        @SuppressLint("InlinedApi")
        @SuppressWarnings("GuardedBy")
        @Override
        public void run() {
            ByteBuffer data;

            while (true) {
                synchronized (this.lock) {
                    while (this.active && (this.pendingFrameData == null)) {
                        try {
                            // Waiting for next frame.
                            this.lock.wait();
                        } catch (InterruptedException e) {
                            Log.w(LensEngineTxt.TAG, "Frame transacting loop terminated.", e);
                            return;
                        }
                    }
                    if (!this.active) {
                        this.pendingFrameData = null;
                        return;
                    }
                    data = this.pendingFrameData;
                    this.pendingFrameData = null;
                }
                try {
                    synchronized (LensEngineTxt.this.transactorLock) {
                        Log.d(LensEngineTxt.TAG, "Process an image");
                        LensEngineTxt.this.frameTransactor.process(
                                data,
                                new FrameMetadata.Builder()
                                        .setWidth(LensEngineTxt.this.selector.getPreviewSize().getWidth())
                                        .setHeight(LensEngineTxt.this.selector.getPreviewSize().getHeight())
                                        .setRotation(LensEngineTxt.this.selector.getRotation())
                                        .setCameraFacing(LensEngineTxt.this.selector.getFacing())
                                        .build(),
                                LensEngineTxt.this.overlay
                        );
                    }
                } catch (Throwable t) {
                    Log.e(LensEngineTxt.TAG, "Exception thrown from receiver.", t);
                } finally {
                    LensEngineTxt.this.camera.addCallbackBuffer(data.array());
                }
            }
        }
    }
}
