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
import android.graphics.Bitmap;
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
    private final Paint valueBackgroundPaint;
    private final Paint valuePaint;
    private final Paint titlePaint;
       
    private Bitmap backgroundBitMap;
    private final Paint backgroundPaint;
    
    private final Paint facePaint;
    private final RectF faceRect;

    private GaugeConfiguration configuration;
           
    // Calculated properties
    
    private float valueAreaHeight;
    private float titleCenterX;
    private float titleCenterY;
    private float valueCenterX;
    private float valueCenterY;

    public BoxGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        workingRect = new RectF(0, 0, 0, 0);
        
        valueBackgroundPaint = new Paint();
        valueBackgroundPaint.setStyle(Style.FILL);
        
        
        valuePaint = new Paint();
        valuePaint.setTextAlign(Align.CENTER);
        valuePaint.setAntiAlias(true);
        
        
        titlePaint = new Paint();
        titlePaint.setTextAlign(Align.CENTER);
        titlePaint.setAntiAlias(true);
            
        
        backgroundPaint = new Paint();
        backgroundPaint.setFilterBitmap(true);
        
        
        facePaint = new Paint();
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setAntiAlias(true);
        
        faceRect = new RectF();
    }

    @Override
    protected void reconfigure(int canvasWidth, int canvasHeight, GaugeConfiguration config) {
    	
    	this.configuration = config;
        
    	// Configured / configurable properties
        
        valuePaint.setColor(Color.WHITE);
        
        titlePaint.setColor(Color.WHITE);

        facePaint.setColor(Color.BLACK);
        
        faceRect.set(0, 0, canvasWidth, canvasHeight);
        
        // Calculated properties
        
        if (config.getTitle() != null) {
            valueAreaHeight = (int) (canvasHeight * 0.75f);
            
            int titleHeight = (int) (canvasHeight - valueAreaHeight);
            if (titleHeight <= 0) {
                titleHeight = 1;
            }
            
            titlePaint.setTextSize((float) (titleHeight * 0.8f));
            
            Rect titleBounds = new Rect();
            titlePaint.getTextBounds(configuration.getTitle(), 0,
            		configuration.getTitle().length(), titleBounds);

            titleCenterX = canvasWidth / 2;
            titleCenterY = valueAreaHeight + (titleHeight / 2)
                    + ((titleBounds.bottom - titleBounds.top) / 2);
        } else {
            valueAreaHeight = canvasHeight;
        }
        
        workingRect.set(0, 0, getCanvasWidth(), valueAreaHeight);
        
        valuePaint.setTextSize(valueAreaHeight * 0.7f);
        
		// Calculate size using a fake value so that we can cache the math and
		// not have to do it every update.
        Rect valueBounds = new Rect();
        valuePaint.getTextBounds("-1234567890", 0, 11, valueBounds);
        
        valueCenterX = canvasWidth / 2;
        valueCenterY = (valueAreaHeight / 2) + ((valueBounds.bottom - valueBounds.top) / 2);
        
        renderBackground();
    }
    
    protected void renderBackground() {
		backgroundBitMap = Bitmap.createBitmap(getCanvasWidth(),
				getCanvasHeight(), Bitmap.Config.ARGB_8888);
        Canvas backgroundCanvas = new Canvas(backgroundBitMap);
        
        backgroundCanvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
        
        drawFace(backgroundCanvas);
        drawTitle(backgroundCanvas);
    }

    protected void drawFace(Canvas backgroundCanvas) {
        backgroundCanvas.drawRect(faceRect, facePaint);
    }
    
    protected void drawTitle(Canvas backgroundCanvas) {
        if (getConfiguration().getTitle() != null) {
            backgroundCanvas.drawText(
                    configuration.getTitle(), 
                    titleCenterX,
                    titleCenterY,
                    titlePaint);
        }
    }
    
    @Override
    protected void draw(Canvas canvas, float valueToDraw) {
    	
    	canvas.drawBitmap(backgroundBitMap, 0, 0, backgroundPaint);
    	
    	if (getConfiguration().isShowValue()) {
			String valueText = String
					.format(
							Locale.US,
							"%." + configuration.getValuePrecision() + "f",
							valueToDraw);
            valuePaint.setColor(valueToAlertColor(valueToDraw));
            canvas.drawText(
                    valueText, 
                    valueCenterX,
                    valueCenterY,
                    valuePaint);
        } else {
            valueBackgroundPaint.setColor(valueToAlertColor(valueToDraw));
            canvas.drawRect(workingRect, valueBackgroundPaint);
        }
    }
}
