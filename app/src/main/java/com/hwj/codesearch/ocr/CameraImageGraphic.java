package com.hwj.codesearch.ocr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class CameraImageGraphic extends BaseGraphic {

    private final Bitmap bitmap;

    private boolean isFill = true;

    public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap) {
        super(overlay);
        this.bitmap = bitmap;
    }

    public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap, boolean isFill) {
        super(overlay);
        this.bitmap = bitmap;
        this.isFill = isFill;
    }

    @Override
    public void draw(Canvas canvas) {
        int width;
        int height;
        if (this.isFill) {
            width = canvas.getWidth();
            height = canvas.getHeight();
        } else {
            width = this.bitmap.getWidth();
            height = this.bitmap.getHeight();
        }
        canvas.drawBitmap(this.bitmap, null, new Rect(0, 0, width, height), null);
    }
}
