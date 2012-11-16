/**
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this software except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tracknalysis.tracklogger.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.PixelXorXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;

/**
 * A bar gauge oriented such that the bar moves left and right.
 *
 * @author David Valeri
 */
public class HorizontalBarGauge extends AbstractBarGauge {
    
    private float valueToPixelRatio;
    
    private final RectF workingRect;
    private final Paint barPaint;
    private final Paint valuePaint;
    private final Paint titlePaint;
    
    private float barHeight;
    private float titleCenterX;
    private float titleCenterY;
    private float valueCenterX;
    private float valueCenterY;

    public HorizontalBarGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        workingRect = new RectF(0, 0, 0, 0);
        
        barPaint = new Paint();
        barPaint.setStyle(Style.FILL);
        
        valuePaint = new Paint();
        valuePaint.setXfermode(new PixelXorXfermode(Color.WHITE));
        valuePaint.setTextAlign(Align.CENTER);
        
        titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Align.CENTER);
    }

    @Override
    protected void reconfigure(int canvasWidth, int canvasHeight, GaugeConfiguration config) {
        super.reconfigure(canvasWidth, canvasHeight, config);
        
        valueToPixelRatio = canvasWidth / (config.getMaxValue() - config.getMinValue());
        
        if (config.getTitle() != null) {
            barHeight = (int) (canvasHeight * 0.75f);
            
            int titleHeight = (int) (canvasHeight - barHeight);
            if (titleHeight <= 0) {
                titleHeight = 1;
            }
            
            titlePaint.setTextSize((float) (titleHeight * 0.7f));
            
            Rect titleBounds = new Rect();
            titlePaint.getTextBounds(config.getTitle(), 0, config.getTitle().length(), titleBounds);
            
            titleCenterX = canvasWidth / 2;
            titleCenterY = barHeight + (titleHeight / 2) + ((titleBounds.bottom - titleBounds.top) / 2);
        } else {
            barHeight = canvasHeight;
        }
        
        
        int valueHeight = (int) (barHeight * 0.7f);
        if (valueHeight <= 0) {
            valueHeight = 1;
        }
        
        valuePaint.setTextSize(valueHeight);
        
        // Calculate size using a fake value so that we can cache the math and not have to do it every update.
        Rect valueBounds = new Rect();
        valuePaint.getTextBounds("0.0", 0, 3, valueBounds);
        
        valueCenterX = canvasWidth / 2;
        valueCenterY = (barHeight / 2) + ((valueBounds.bottom - valueBounds.top) / 2);
    }

    @Override
    protected void draw(Canvas canvas, float valueToDraw) {
        
        barPaint.setColor(valueToAlertColor(valueToDraw));
        
        int widthToDrawTo = (int) (valueToPixelRatio * (valueToDraw - getConfiguration().getMinValue()));
        workingRect.set(0, 0, widthToDrawTo, barHeight);
        canvas.drawRect(workingRect, barPaint);
        
        if (getConfiguration().isShowValue()) {
            String valueText = String.format("%.1f", valueToDraw);
            
            canvas.drawText(
                    valueText, 
                    valueCenterX,
                    valueCenterY,
                    valuePaint);
        }
        
        if (getConfiguration().getTitle() != null) {
            canvas.drawText(
                    getConfiguration().getTitle(), 
                    titleCenterX,
                    titleCenterY,
                    titlePaint);
        }
    }
}
