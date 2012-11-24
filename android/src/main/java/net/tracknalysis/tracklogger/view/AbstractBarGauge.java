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

import java.util.LinkedList;
import java.util.List;

import net.tracknalysis.tracklogger.view.CircularGauge.SweepDirection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;

/**
 * Base class for bar style gauge implementations.
 *
 * @author David Valeri
 */
public abstract class AbstractBarGauge extends AbstractGauge {
    
    /**
     * Configuration option enumeration for where to location the origin (0 value) within the gauge.
     */
    protected static enum SweepDirection {
        LEFT_TO_RIGHT,
        BOTTOM_TO_TOP
    }
    
    private final RectF workingRect;
    private final Paint barPaint;
    private final Paint valuePaint;
    private final Paint titlePaint;
    
    private final Paint majorScaleMarkLabelPaint;
    private final Paint majorScaleMarkPaint;
    private final Paint minorScaleMarkPaint;
    
    private Bitmap backgroundBitMap;
    private final Paint backgroundPaint;
    
    private final Paint facePaint;
    private final RectF faceRect;
    
    private LinearGradient alertGradient;
    
    private GaugeConfiguration configuration;
    
    // Configurable properties
    
    private int alertMinCriticalColor;
    private int alertMinWarningColor;
    private int alertOkColor;
    private int alertMaxWarningColor;
    private int alertMaxCriticalColor;
    
    private float minorScaleMarkSegmentsPerMajorScaleMark;
    
    // Calculated properties
    
    private float valueToPixelRatio;
    private float barHeight;
    private float alertHeight;
    private float titleCenterX;
    private float titleCenterY;
    private float valueCenterX;
    private float valueCenterY;
    
    public AbstractBarGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        workingRect = new RectF(0, 0, 0, 0);
        
        barPaint = new Paint();
        barPaint.setStyle(Style.FILL);
        
        
        valuePaint = new Paint();
        valuePaint.setTextAlign(Align.CENTER);
        valuePaint.setAntiAlias(true);
        
        
        titlePaint = new Paint();
        titlePaint.setTextAlign(Align.CENTER);
        titlePaint.setAntiAlias(true);
        
        
        majorScaleMarkLabelPaint = new Paint();
        majorScaleMarkLabelPaint.setStyle(Paint.Style.FILL);
        majorScaleMarkLabelPaint.setAntiAlias(true);
        majorScaleMarkLabelPaint.setTextAlign(Align.CENTER);
        
        majorScaleMarkPaint = new Paint();
        majorScaleMarkPaint.setStyle(Paint.Style.STROKE);
        majorScaleMarkPaint.setAntiAlias(true);
        
        minorScaleMarkPaint = new Paint();
        minorScaleMarkPaint.setStyle(Paint.Style.STROKE);
        minorScaleMarkPaint.setAntiAlias(true);
        
        
        backgroundPaint = new Paint();
        backgroundPaint.setFilterBitmap(true);
        
        
        facePaint = new Paint();
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setAntiAlias(true);
        
        faceRect = new RectF();
    }
    
    @Override
    protected final void reconfigure(int canvasWidth, int canvasHeight,
            GaugeConfiguration configuration) {
        
        this.configuration = configuration;
        
        // Configured / configurable properties
        
        valuePaint.setColor(Color.WHITE);
        
        titlePaint.setColor(Color.WHITE);
        
        majorScaleMarkPaint.setColor(Color.WHITE);
        
        minorScaleMarkPaint.setColor(Color.WHITE);
        minorScaleMarkSegmentsPerMajorScaleMark = configuration.getMinorScaleMarkSegmentsPerMajorScaleMark();
        
        facePaint.setColor(Color.BLACK);
        
        faceRect.set(0, 0, canvasWidth, canvasHeight);
        
        alertMinCriticalColor = Color.RED;
        alertMinWarningColor = Color.YELLOW;
        alertOkColor = Color.GREEN;
        alertMaxWarningColor = Color.YELLOW;
        alertMaxCriticalColor = Color.RED;
        
        // Calculated properties
        
        int workingWidth = 0;
        int workingHeight = 0;
        String titleText = getTitleText();
        
        switch (getSweepDirection()) {
            case LEFT_TO_RIGHT:
                workingWidth = canvasWidth;
                workingHeight = canvasHeight;
                break;
            case BOTTOM_TO_TOP:
                workingWidth = canvasHeight;
                workingHeight = canvasWidth;
                break;
        }
        
        valueToPixelRatio = workingWidth / (configuration.getMaxValue() - configuration.getMinValue());
        
        if (configuration.getTitle() != null) {
            
            float titleHeight = 0;
            
            if (configuration.isAlertEnabled()) {
                barHeight = workingHeight * 0.70f;
                alertHeight = workingHeight * 0.10f;
                titleHeight =  workingHeight * 0.20f;
            } else {
                barHeight = (int) (workingHeight * 0.75f);
                alertHeight = 0;
                titleHeight = (workingHeight - barHeight);
            }
            
            if (titleHeight <= 0) {
                titleHeight = 1;
            }

            titlePaint.setTextSize((float) (titleHeight * 0.8f));

            Rect titleBounds = new Rect();
            titlePaint.getTextBounds(titleText, 0,
                    titleText.length(), titleBounds);

            titleCenterX = workingWidth / 2;
            titleCenterY = barHeight + alertHeight + (titleHeight / 2)
                    + ((titleBounds.bottom - titleBounds.top) / 2);
        } else {
            if (configuration.isAlertEnabled()) {
                barHeight = workingHeight * 0.90f;
                alertHeight = workingHeight * 0.10f;
            } else {
                barHeight = workingHeight;
            }
        }
        
        float valueHeight = barHeight * 0.7f;
        if (valueHeight <= 0) {
            valueHeight = 1;
        }
        
        valuePaint.setTextSize(valueHeight);
        
        // Calculate size using a fake value so that we can cache the math and not have to do it every update.
        Rect valueBounds = new Rect();
        valuePaint.getTextBounds("0.0", 0, 3, valueBounds);
        
        valueCenterX = workingWidth / 2;
        valueCenterY = (barHeight / 2) + ((valueBounds.bottom - valueBounds.top) / 2);
        
        alertGradient = calculateAlertGradient();
        
        renderBackground();
    }
    
    protected void renderBackground() {
        backgroundBitMap = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        Canvas backgroundCanvas = new Canvas(backgroundBitMap);
        
        backgroundCanvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
        
        drawFace(backgroundCanvas);
        drawScale(backgroundCanvas);
        drawAlert(backgroundCanvas);
        drawTitle(backgroundCanvas);
    }
    
    @Override
    protected void draw(Canvas canvas, float valueToDraw) {
        
        canvas.drawBitmap(backgroundBitMap, 0f, 0f, backgroundPaint);
        
        drawBar(canvas, valueToDraw);
        drawValue(canvas, valueToDraw);
    }
    
    protected void drawFace(Canvas backgroundCanvas) {
        backgroundCanvas.drawRect(faceRect, facePaint);
    }
    
    protected void drawScale(Canvas backgroundCanvas) {
        if (isDrawScale()) {
            // TODO implement drawing of scale marks and labels
        }
    }
    
    protected void drawAlert(Canvas backgroundCanvas) {
        if (configuration.isAlertEnabled()) {
            backgroundCanvas.save();
            translateCanvasForSweepDirection(backgroundCanvas);
            
            final float top = barHeight + alertHeight * 0.2f;
            final float bottom = barHeight + alertHeight;
            
            if (configuration.isUseAlertColorGradient()) {
                
                Paint alertPaint = new Paint();
                alertPaint.setAntiAlias(true);
                alertPaint.setStyle(Paint.Style.FILL);
                alertPaint.setShader(alertGradient);
                
                backgroundCanvas.drawRect(
                        valueToXPosition(configuration.getMinValue()),
                        top,
                        valueToXPosition(configuration.getMaxValue()),
                        bottom,
                        alertPaint);
                
            } else {
                // Draw the alert without gradients
                
                Paint alertPaint = new Paint();
                alertPaint.setAntiAlias(false);
                alertPaint.setStyle(Paint.Style.FILL);
                
                float okStart = configuration.getMinValue();
                float okEnd = configuration.getMaxValue();
                
                if (configuration.isMinCriticalEnabled()) {
                    alertPaint.setColor(alertMinCriticalColor);
                    
                    backgroundCanvas.drawRect(
                            valueToXPosition(configuration.getMinValue()),
                            top,
                            0,
                            bottom,
                            alertPaint);
                    okStart = configuration.getMinCriticalValue();
                }
                
                if (configuration.isMinWarningEnabled()) {
                    alertPaint.setColor(alertMinWarningColor);
                    
                    backgroundCanvas.drawRect(
                            valueToXPosition(okStart),
                            top,
                            valueToXPosition(configuration.getMinWarningValue()),
                            bottom,
                            alertPaint);
                    
                    okStart = configuration.getMinWarningValue();
                }
                
                if (configuration.isMaxCriticalEnabled()) {
                    alertPaint.setColor(alertMaxCriticalColor);
                    
                    backgroundCanvas.drawRect(
                            valueToXPosition(configuration.getMaxCriticalValue()),
                            top,
                            valueToXPosition(configuration.getMaxValue()),
                            bottom,
                            alertPaint);
                    
                    okEnd = configuration.getMaxCriticalValue();
                }
                
                if (configuration.isMaxWarningEnabled()) {
                    alertPaint.setColor(alertMaxWarningColor);
                    
                    backgroundCanvas.drawRect(
                            valueToXPosition(configuration.getMaxWarningValue()),
                            top,
                            valueToXPosition(okEnd),
                            bottom,
                            alertPaint);
        
                    okEnd = configuration.getMaxWarningValue();
                }
                
                alertPaint.setColor(alertOkColor);
                backgroundCanvas.drawRect(
                        valueToXPosition(okStart),
                        top,
                        valueToXPosition(okEnd),
                        bottom,
                        alertPaint);
            }
            
            backgroundCanvas.restore();
        }
    }
    
    protected void drawTitle(Canvas backgroundCanvas) {
        if (getConfiguration().getTitle() != null) {
            
            backgroundCanvas.save();
            translateCanvasForSweepDirection(backgroundCanvas);
            
            backgroundCanvas.drawText(
                    getTitleText(), 
                    titleCenterX,
                    titleCenterY,
                    titlePaint);
            
            backgroundCanvas.restore();
        }
    }
    
    protected void drawBar(Canvas canvas, float valueToDraw) {
        
        if (configuration.isUseAlertColorGradient()) {
            barPaint.setShader(alertGradient);
        } else {
            barPaint.setShader(null);
            barPaint.setColor(valueToAlertColor(valueToDraw));
        }
        
        canvas.save();
        translateCanvasForSweepDirection(canvas);
        
        workingRect.set(0, 0, valueToXPosition(valueToDraw), barHeight);
        canvas.drawRect(workingRect, barPaint);
        
        canvas.restore();
    }
    
    protected void drawValue(Canvas canvas, float valueToDraw) {
        if (configuration.isShowValue()) {
            canvas.save();
            translateCanvasForSweepDirection(canvas);
            
            canvas.drawText(
                    String.valueOf(valueToDraw), 
                    valueCenterX,
                    valueCenterY,
                    valuePaint);
            
            canvas.restore();
        }
    }
    
    protected final LinearGradient calculateAlertGradient() {
        final float minValue = configuration.getMinValue();
        final float maxValue = configuration.getMaxValue();
        
        int lastColor = alertOkColor;
        float lastThreshold = minValue;
        List<Integer> colors = new LinkedList<Integer>();
        List<Float> positions = new LinkedList<Float>();
        
        if (configuration.isMinCriticalEnabled()) {
            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, configuration.getMinCriticalValue()),
                    alertMinCriticalColor, colors, positions);
            lastColor = alertMinCriticalColor;
            lastThreshold = getMinCriticalThreshold();
        }
        
        if (configuration.isMinWarningEnabled()) {
            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, lastThreshold),
                    alertMinWarningColor, colors, positions);
            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, configuration.getMinWarningValue()),
                    alertMinWarningColor, colors, positions);
            lastColor = alertMinWarningColor;
            lastThreshold = getMinWarningThreshold();
        }

        addPairsForSweepGradient(
                valueToSweepGradientPosition(minValue, maxValue, lastThreshold),
                alertOkColor, colors, positions);
        lastColor = alertOkColor;
        
        if (configuration.isMaxWarningEnabled()) {
            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, getMaxWarningThreshold()),
                    lastColor, colors, positions);
            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, configuration.getMaxWarningValue()),
                    alertMaxWarningColor, colors, positions);
            lastColor = alertMaxWarningColor;
        }
        
        if (configuration.isMaxCriticalEnabled()) {
            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, getMaxCriticalThreshold()),
                    lastColor, colors, positions);
            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, configuration.getMaxCriticalValue()),
                    alertMaxCriticalColor, colors, positions);
            lastColor = alertMaxCriticalColor;
        }
        
        addPairsForSweepGradient(
                valueToSweepGradientPosition(minValue, maxValue, configuration.getMaxValue()),
                lastColor, colors, positions);
        
        int[] colorsArray = new int[colors.size()];
        int counter = 0;
        for (Integer color : colors) {
            colorsArray[counter++] = color;
        }
        
        float[] positionsArray = new float[positions.size()];
        counter = 0;
        for (Float position : positions) {
            positionsArray[counter++] = position;
        }
        
        return new LinearGradient(
                valueToXPosition(configuration.getMinValue()),
                0,
                valueToXPosition(configuration.getMaxValue()),
                0,
                colorsArray,
                positionsArray,
                TileMode.REPEAT);
    }
    
    protected final String getTitleText() {
        if (configuration.getScaleMarkLabelScaleFactor() != null) {
            return configuration.getTitle() + " x " + String.valueOf(configuration.getScaleMarkLabelScaleFactor());
        } else {
            return configuration.getTitle();
        }
    }
    
    protected final void translateCanvasForSweepDirection(Canvas canvas) {
        switch (getSweepDirection()) {
            case LEFT_TO_RIGHT:
                // Do nothing
                break;
            case BOTTOM_TO_TOP:
                canvas.translate(0, getHeight());
                canvas.rotate(-90);
                break;
        }
    }
    
    protected final float valueToXPosition(float value) {
        return (valueToPixelRatio * (value - getConfiguration().getMinValue()));
    }
    
    /**
     * Adds the color at position to the lists while respecting the sweep direction of the gauge.
     *
     * @param position the calculated position
     * @param color the color for the position
     * @param colors the list of colors so far
     * @param positions the list of positions so far
     *
     * @see #valueToSweepGradientPosition(float, float, float)
     */
    protected final void addPairsForSweepGradient(float position, int color,
            List<Integer> colors, List<Float> positions) {
        
        colors.add(color);
        positions.add(position);
    }
    
    /**
     * Maps a value within a defined range to a percentage of an arc that spans the full range of the gauge.
     *
     * @param minValue the minimum value of the gauge
     * @param maxValue the maximum value of the gauge
     * @param value the value to find the position before
     */
    protected final float valueToSweepGradientPosition(float minValue, float maxValue, float value) {
        float range = maxValue - minValue;
        return ((value - minValue) / range); 
    }
    
    /**
     * Returns true if the gauge should render scale marks. The default
     * implementation returns {@code true}.
     */
    protected boolean isDrawScale() {
        return true;
    }

    protected abstract SweepDirection getSweepDirection();
    
    
}
