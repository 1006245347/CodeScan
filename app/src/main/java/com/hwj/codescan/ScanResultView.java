package com.hwj.codescan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.huawei.hms.ml.scan.HmsScan;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by jason-何伟杰，2023/1/9
 * des:多个矩形边框定位 多码识别
 */
public class ScanResultView extends View {

   private final Object lock = new Object();
   protected float widthScaleFactor = 1.0f;
   protected float heightScaleFactor = 1.0f;
   protected float previewWidth;
   protected float previewHeight;

   private final List<HmsScanGraphic> hmsScanGraphics = new ArrayList<>();

   public ScanResultView(Context context) {
      super(context);
   }

   public ScanResultView(Context context, @Nullable AttributeSet attrs) {
      super(context, attrs);
   }

   public void clear() {
      synchronized (lock) {
         hmsScanGraphics.clear();
      }
      postInvalidate();
   }

   public void add(HmsScanGraphic graphic) {
      synchronized (lock) {
         hmsScanGraphics.add(graphic);
      }
   }

   public void setCameraInfo(int previewWidth, int previewHeight) {
      synchronized (lock) {
         this.previewWidth = previewWidth;
         this.previewHeight = previewHeight;
      }
      postInvalidate();
   }

   /**
    * Draw MultiCodes on screen.
    */
   @Override
   protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      synchronized (lock) {
         if ((previewWidth != 0) && (previewHeight != 0)) {
            widthScaleFactor = (float) canvas.getWidth() / (float) previewWidth;
            heightScaleFactor = (float) canvas.getHeight() / (float) previewHeight;
         }

         for (HmsScanGraphic graphic : hmsScanGraphics) {
            graphic.drawGraphic(canvas);
         }
      }
   }

   public static class HmsScanGraphic {

      private static final int TEXT_COLOR = Color.WHITE;
      private static final float TEXT_SIZE = 35.0f;
      private static final float STROKE_WIDTH = 4.0f;

      private final Paint rectPaint;
      private final Paint hmsScanResult;
      private final HmsScan hmsScan;
      private ScanResultView scanResultView;

      public HmsScanGraphic(ScanResultView scanResultView, HmsScan hmsScan) {
         this(scanResultView, hmsScan, Color.WHITE);
      }

      public HmsScanGraphic(ScanResultView scanResultView, HmsScan hmsScan, int color) {
         this.scanResultView = scanResultView;
         this.hmsScan = hmsScan;

         rectPaint = new Paint();
         rectPaint.setColor(color);
         rectPaint.setStyle(Paint.Style.STROKE);
         rectPaint.setStrokeWidth(STROKE_WIDTH);

         hmsScanResult = new Paint();
         hmsScanResult.setColor(TEXT_COLOR);
         hmsScanResult.setTextSize(TEXT_SIZE);
      }


      public void drawGraphic(Canvas canvas) {
         if (hmsScan == null) {
            return;
         }

         RectF rect = new RectF(hmsScan.getBorderRect());
         RectF other = new RectF();
         other.left = canvas.getWidth()-scaleX(rect.top);
         other.top = scaleY(rect.left);
         other.right = canvas.getWidth()-scaleX(rect.bottom);
         other.bottom = scaleY(rect.right);
         canvas.drawRect(other, rectPaint);

         canvas.drawText(hmsScan.getOriginalValue(), other.right, other.bottom, hmsScanResult);
      }

      public float scaleX(float horizontal) {
         return horizontal * scanResultView.widthScaleFactor;
      }

      public float scaleY(float vertical) {
         return vertical * scanResultView.heightScaleFactor;
      }

   }
}
