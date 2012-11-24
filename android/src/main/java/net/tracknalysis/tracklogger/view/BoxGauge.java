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

import java.util.Locale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;

/**
 * A simple numerical display with or without alert color coding.
 *
 * @author David Valeri
 */
public class BoxGauge extends AbstractGauge {

    private final RectF workingRect;
    private final Paint barPaint;
    private final Paint titlePaint;
    
    private float valueAreaHeight;
    private float valueCenterX;
    private float valueCenterY;
    private int valueHeight;
    private float titleCenterX;
    private float titleCenterY;

    public BoxGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        workingRect = new RectF(0, 0, 0, 0);
        
        barPaint = new Paint();
        barPaint.setStyle(Style.FILL);
        
        titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Align.CENTER);
        titlePaint.setAntiAlias(true);
    }

    @Override
    protected void reconfigure(int canvasWidth, int canvasHeight, GaugeConfiguration config) {
        
        if (config.getTitle() != null) {
            valueAreaHeight = (int) (canvasHeight * 0.75f);
            
            int titleHeight = (int) (canvasHeight - valueAreaHeight);
            if (titleHeight <= 0) {
                titleHeight = 1;
            }
            
            titlePaint.setTextSize((float) (titleHeight * 0.7f));
            
            titleCenterX = canvasWidth / 2;
            titleCenterY = valueAreaHeight + (titleHeight / 2); //((titleBounds.bottom - titleBounds.top) / 2);
        } else {
            valueAreaHeight = canvasHeight;
        }
        
        valueHeight = (int) (valueAreaHeight * 0.7f);
        if (valueHeight <= 0) {
            valueHeight = 1;
        }
        
        Paint tempValuePaint = new Paint();
        tempValuePaint.setTextAlign(Align.CENTER);
        
        tempValuePaint.setTextSize(valueHeight);
        
        // Calculate size using a fake value so that we can cache the math and not have to do it every update.
        Rect valueBounds = new Rect();
        tempValuePaint.getTextBounds("-123456789.0", 0, 3, valueBounds);
        
        valueCenterX = canvasWidth / 2;
        valueCenterY = (valueAreaHeight / 2) + ((valueBounds.bottom - valueBounds.top) / 2);
    }

    @Override
    protected void draw(Canvas canvas, float valueToDraw) {
        
        barPaint.setColor(valueToAlertColor(valueToDraw));
        
        if (getConfiguration().isShowValue()) {
            String valueText = String.format(Locale.US, "%.1f", valueToDraw);
            Paint valuePaint = new Paint(barPaint);
            valuePaint.setTextAlign(Align.CENTER);
            valuePaint.setTextSize(valueHeight);
            
            canvas.drawText(
                    valueText, 
                    valueCenterX,
                    valueCenterY,
                    valuePaint);
        } else {
            workingRect.set(0, 0, getCanvasWidth(), valueAreaHeight);
            canvas.drawRect(workingRect, barPaint);
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
