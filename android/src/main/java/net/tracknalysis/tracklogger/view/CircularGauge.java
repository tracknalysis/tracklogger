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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;

/**
 * A circular gauge with a needle indicator.
 *
 * @author David Valeri
 */
public class CircularGauge extends AbstractGauge {
    
    /**
     * Configuration option enumeration for where to location the origin (0 degrees) within the gauge.
     */
    public static enum OriginOrientation {
        NORTH(0, 270),
        SOUTH(180, 90),
        EAST(90, 0),
        WEST(270, 180);
        
        private final int degreesToNorth;
        private final int degreesToEast;
        
        private OriginOrientation(int degreesToNorth, int degreesToEast) {
            this.degreesToNorth = degreesToNorth;
            this.degreesToEast = degreesToEast;
        }
        
        /**
         * Returns the number of degrees of rotation needed to orient the origin to the north (12:00).
         * Always positive.
         */
        public int getDegreesToNorth() {
            return degreesToNorth;
        }
        
        /**
         * Returns the number of degrees of roation needed to orient the origin to the east (3:00).
         * Always positive.
         */
        public int getDegreesToEast() {
            return degreesToEast;
        }
    }
    
    /**
     * Configuration option for the direction of sweep (direction of increasing values) of the gauge.
     */
    public static enum SweepDirection {
        CLOCKWISE,
        COUNTER_CLOCKWISE;
    }
    
    private final RectF faceRect;
    private final Paint facePaint;
    
    private final Paint majorScaleMarkLabelPaint;
    private final Paint majorScaleMarkPaint;
    private final Paint minorScaleMarkPaint;
    
    private final RectF alertRect;
    
    private final Paint titlePaint;
    
    private final Paint needlePaint;
    private final Path needlePath;
    
    private Bitmap backgroundBitMap;
    private final Paint backgroundPaint;
    
    private GaugeConfiguration configuration;
    
    private int radius;
    private int width;
    private int height;
    
    // Configurable properties
    
    private OriginOrientation originOrientation;
    private SweepDirection sweepDirection;
    
    // Always positive, start less than end when % 360
    private float startDegrees;
    private float endDegrees;
    
    private float majorScaleMarkLength;
    private float majorScaleMarkStartRadius;
    
    private float minorScaleMarkLength;
    private float minorScaleMarkStartRadius;
    private float minorScaleMarkSegmentsPerMajorScaleMark;
    
    private float majorScaleMarkLabelRadius;
    // The scale mark label value is divided by this value before rendering and
    // an indicator is placed in the gauge indicating that the value is X (times)
    // this value.
    private Float scaleMarkLabelScaleFactor; 
    
    private float alertRadius;
    private float alertWidth;
    private int alertOkColor;
    private int alertMaxWarnColor;
    private int alertMaxCriticalColor;
    
    // Calculated properties
    
    private float majorScaleMarkStartY;
    private float majorScaleMarkStopY;
    private float minorScaleMarkStartY;
    private float minorScaleMarkStopY;
    
    private float scaleMarkLabelY;
    
    private float valueToDegreeRatio;
    private float degreeIncrementPerMajorScaleMark;
    private float degreeIncrementPerMinorScaleMark;

    public CircularGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        faceRect = new RectF();
        
        facePaint = new Paint();
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setAntiAlias(true);
        
        
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
        
        alertRect = new RectF();
        
        titlePaint = new Paint();
        titlePaint.setStyle(Style.FILL);
        titlePaint.setAntiAlias(true);
        titlePaint.setTextAlign(Align.CENTER);
        
        
        needlePaint = new Paint();
        needlePaint.setStyle(Style.FILL);
        
        needlePath = new Path();
        needlePath.moveTo(0.48f, 0.65f);
        needlePath.lineTo(0.5f, 0.10f);
        needlePath.lineTo(0.52f, 0.65f);
        needlePath.lineTo(0.48f, 0.65f);
        
        
        backgroundPaint = new Paint();
        backgroundPaint.setFilterBitmap(true);
    }

    @Override
    protected void reconfigure(int canvasWidth, int canvasHeight,
            GaugeConfiguration configuration) {
        
        this.configuration = configuration;  
        
        // Environment properties
        width = canvasWidth;
        height = canvasHeight;
        
        // Configured/configurable properties
        
        faceRect.set(0f, 0f, 1f, 1f);
        
        facePaint.setColor(Color.BLACK);
        
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(0.05f);
        
        needlePaint.setColor(Color.RED);
        
        originOrientation = OriginOrientation.SOUTH;
        sweepDirection = SweepDirection.CLOCKWISE;
        
        startDegrees = 0;
        endDegrees = 270;
        
        majorScaleMarkLength = 0.075f;
        majorScaleMarkStartRadius = 0.35f;
        majorScaleMarkPaint.setStrokeWidth(0.005f);
        majorScaleMarkPaint.setColor(Color.WHITE);
        
        majorScaleMarkLabelPaint.setColor(Color.WHITE);
        majorScaleMarkLabelPaint.setTextSize(0.05f);
        majorScaleMarkLabelRadius = 0.44f;
        
        minorScaleMarkLength = 0.025f;
        minorScaleMarkStartRadius = 0.35f;
        minorScaleMarkPaint.setStrokeWidth(0.0025f);
        minorScaleMarkPaint.setColor(Color.LTGRAY);
        minorScaleMarkSegmentsPerMajorScaleMark = configuration.getMinorScaleMarkSegmentsPerMajorScaleMark();
        
        scaleMarkLabelScaleFactor = 1000f;
        
        alertRadius = 0.2f;
        alertWidth = 0.05f;
        alertOkColor = Color.GREEN;
        alertMaxWarnColor = Color.YELLOW;
        alertMaxCriticalColor = Color.RED;
        
        // Calculated properties.
        
        radius = Math.min(canvasHeight, canvasWidth) / 2;
                
        startDegrees = negativeAwareModulo(startDegrees, 360);
        endDegrees = negativeAwareModulo(endDegrees, 360);
        
        majorScaleMarkStartY = 0.5f - majorScaleMarkStartRadius;
        majorScaleMarkStopY = majorScaleMarkStartY - majorScaleMarkLength;
        minorScaleMarkStartY = 0.5f - minorScaleMarkStartRadius;
        minorScaleMarkStopY = minorScaleMarkStartY - minorScaleMarkLength;
        
        scaleMarkLabelY = 0.5f - majorScaleMarkLabelRadius;
        
        alertRect.set(alertRadius, alertRadius, 1 - alertRadius, 1 - alertRadius);
        
        float range;
        
        if (configuration.getMinValue() <= 0 && configuration.getMaxValue() <= 0) {
            range = -1 * (configuration.getMinValue() - configuration.getMaxValue());
        } else if (configuration.getMinValue() >= 0 && configuration.getMaxValue() >= 0) {
            range = configuration.getMaxValue() - configuration.getMinValue();
        } else {
            range = -configuration.getMinValue() + configuration.getMaxValue();
        }
        
        // X units of value per degree
        valueToDegreeRatio = range / (endDegrees - startDegrees);
        
        degreeIncrementPerMajorScaleMark = configuration.getMajorScaleMarkDelta() / valueToDegreeRatio;
        if (minorScaleMarkSegmentsPerMajorScaleMark <= 1) {
            degreeIncrementPerMinorScaleMark = degreeIncrementPerMajorScaleMark;
            minorScaleMarkSegmentsPerMajorScaleMark = 1;
        } else {
            degreeIncrementPerMinorScaleMark = degreeIncrementPerMajorScaleMark
                    / minorScaleMarkSegmentsPerMajorScaleMark;
        }
        
        renderBackground(configuration);
    }
    
    protected void renderBackground(GaugeConfiguration configuration) {
        
        backgroundBitMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas backgroundCanvas = new Canvas(backgroundBitMap);
        
        backgroundCanvas.scale(radius * 2, radius * 2);
        
        drawFace(backgroundCanvas);
        drawScale(backgroundCanvas);
        drawAlert(backgroundCanvas);
        drawTitle(backgroundCanvas);
        drawScaleMarkLabelScaleFactor(backgroundCanvas);
    }

    @Override
    protected void draw(Canvas canvas, float valueToDraw) {
        
        canvas.drawBitmap(backgroundBitMap, width / 2 - radius, height / 2 - radius, backgroundPaint);
        
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.translate(width / 2 - radius, height / 2 - radius);
        canvas.scale(radius * 2, radius * 2);
        
        drawValue(canvas, valueToDraw);
        drawNeedle(canvas, valueToDraw);
        
        canvas.restore();
    }
    
    /**
     * Draw the face of the gauge as part of the static background.  The resulting image
     * is only updated when the gauge is rescaled.
     *
     * @param backgroundCanvas the background canvas to draw on
     */
    protected void drawFace(Canvas backgroundCanvas) {
        backgroundCanvas.drawOval(faceRect, facePaint);
    }
    
    /**
     * Draw the scale marks on the static background.
     *
     * @param backgroundCanvas the background canvas to draw on
     */
    protected void drawScale(Canvas backgroundCanvas) {
        
        backgroundCanvas.save(Canvas.MATRIX_SAVE_FLAG);
        
        float currentDegree = startDegrees;
        float scaleTextValue = configuration.getMinValue();
        float degreesPerIncrement = degreeIncrementPerMinorScaleMark;
        
        if (sweepDirection == SweepDirection.COUNTER_CLOCKWISE) {
            // Negative rotation for a counter clockwise sweep.
            degreesPerIncrement *= -1;
            backgroundCanvas.rotate(originOrientation.getDegreesToNorth() - startDegrees, 0.5f, 0.5f);
        } else {
            // Positive rotation for a clockwise sweep.
            backgroundCanvas.rotate(originOrientation.getDegreesToNorth() + startDegrees, 0.5f, 0.5f);
        }

        int minorMarkCounter = 0;
        while (currentDegree <= endDegrees) {

            if (minorMarkCounter % minorScaleMarkSegmentsPerMajorScaleMark == 0) {
                backgroundCanvas.drawLine(0.5f, majorScaleMarkStartY, 0.5f,
                        majorScaleMarkStopY, majorScaleMarkPaint);
                
                float scaledScaleTextValue;
                
                if (scaleMarkLabelScaleFactor != null) {
                    scaledScaleTextValue = scaleTextValue / scaleMarkLabelScaleFactor;
                } else {
                    scaledScaleTextValue = scaleTextValue;
                }
                
                backgroundCanvas.drawText(String.valueOf(scaledScaleTextValue), 0.5f, scaleMarkLabelY, majorScaleMarkLabelPaint);
                scaleTextValue += configuration.getMajorScaleMarkDelta();
            } else {
                backgroundCanvas.drawLine(0.5f, majorScaleMarkStartY, 0.5f,
                        minorScaleMarkStopY, minorScaleMarkPaint);
            }

            minorMarkCounter++;
            currentDegree += degreeIncrementPerMinorScaleMark;
            backgroundCanvas.rotate(degreesPerIncrement, 0.5f, 0.5f);
        }
        
        backgroundCanvas.restore();
    }
    
    protected void drawAlert(Canvas backgroundCanvas) {
        
        if (configuration.isUseAlertColorGradient()
                && (configuration.isMaxCriticalEnabled() || configuration
                        .isMaxWarningEnabled())) {
            
            final float minValue = configuration.getMinValue();
            final float maxValue = configuration.getMaxValue();
            
            int lastColor = alertOkColor;
            List<Integer> colors = new LinkedList<Integer>();
            List<Float> positions = new LinkedList<Float>();

            addPairsForSweepGradient(
                    valueToSweepGradientPosition(minValue, maxValue, configuration.getMinValue()),
                    lastColor, colors, positions);
            
            if (configuration.isMaxWarningEnabled()) {
                addPairsForSweepGradient(
                        valueToSweepGradientPosition(minValue, maxValue, getMaxWarningThreshold()),
                        lastColor, colors, positions);
                addPairsForSweepGradient(
                        valueToSweepGradientPosition(minValue, maxValue, configuration.getMaxWarningValue()),
                        alertMaxWarnColor, colors, positions);
                lastColor = alertMaxWarnColor;
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
            
            Paint alertPaint = new Paint();
            alertPaint.setAntiAlias(true);
            alertPaint.setStrokeCap(Paint.Cap.BUTT);
            alertPaint.setStyle(Paint.Style.STROKE);
            alertPaint.setStrokeWidth(alertWidth);
            alertPaint.setColor(alertOkColor);
            alertPaint.setShader(new SweepGradient(0.5f, 0.5f, 
                    colorsArray,
                    positionsArray));
            
            backgroundCanvas.save(Canvas.MATRIX_SAVE_FLAG);
            // Rotate the min value on the scale to face east since that is where the origin is for
            // a sweep gradient.
            backgroundCanvas.rotate(valueToDegreesOfRotation(minValue) - 90, 0.5f, 0.5f);
            backgroundCanvas.drawArc(
                    alertRect,
                    0,
                    rangeToDegreesOfArcSweep(configuration.getMaxValue() - configuration.getMinValue()),
                    false,
                    alertPaint);
            backgroundCanvas.restore();

        } else if (configuration.isMaxCriticalEnabled() || configuration.isMaxWarningEnabled()){
        
            Paint alertPaint = new Paint();
            alertPaint.setAntiAlias(true);
            alertPaint.setStrokeCap(Paint.Cap.BUTT);
            alertPaint.setStyle(Paint.Style.STROKE);
            alertPaint.setStrokeWidth(alertWidth);
            alertPaint.setColor(alertOkColor);
                    
            float okSweep = 0;
            
            if (configuration.isMaxCriticalEnabled()) {
                okSweep = rangeToDegreesOfArcSweep(configuration
                        .getMaxCriticalValue() - configuration.getMinValue());
            }
            
            if (configuration.isMaxWarningEnabled()) {
                okSweep = rangeToDegreesOfArcSweep(configuration
                        .getMaxWarningValue() - configuration.getMinValue());
            }
            
            backgroundCanvas.drawArc(
                    alertRect,
                    valueToDegreesOfArcOrigin(configuration.getMinValue()),
                    okSweep,
                    false,
                    alertPaint);
            
                   
            if (configuration.isMaxWarningEnabled()) {
                alertPaint.setColor(alertMaxWarnColor);
    
                backgroundCanvas.drawArc(
                        alertRect,
                        valueToDegreesOfArcOrigin(configuration.getMaxWarningValue()),
                        rangeToDegreesOfArcSweep(configuration.getMaxValue()
                                - configuration.getMaxWarningValue()),
                        false,
                        alertPaint);
            }
            
            if (configuration.isMaxCriticalEnabled()) {
                alertPaint.setColor(alertMaxCriticalColor);
                
                backgroundCanvas.drawArc(
                        alertRect,
                        valueToDegreesOfArcOrigin(configuration.getMaxCriticalValue()),
                        rangeToDegreesOfArcSweep(configuration.getMaxValue()
                                - configuration.getMaxCriticalValue()),
                        false,
                        alertPaint);
            }
        }
    }
    
    /**
     * Draw the gauge title on the static background.  For example, the title and/or the units.
     *
     * @param backgroundCanvas the background canvas
     */
    private void drawTitle(Canvas backgroundCanvas) {
        if (configuration.getTitle() != null) {
            backgroundCanvas.drawText(configuration.getTitle(), 0.5f, 0.675f, titlePaint);
        }
    }

    /**
     * Draw the text that indicates the factor for the scale mark labels on the static background.
     *
     * @param backgroundCanvas the background canvas
     */
    private void drawScaleMarkLabelScaleFactor(Canvas backgroundCanvas) {
        if (scaleMarkLabelScaleFactor != null) {
            backgroundCanvas.drawText("x " + String.valueOf(scaleMarkLabelScaleFactor),
                    0.5f, 0.72f, titlePaint);
        }
    }
    
    /**
     * Draw the current value.
     *
     * @param canvas the canvas to draw to
     * @param valueToDraw the current value
     */
    protected void drawValue(Canvas canvas, float valueToDraw) {
        if (configuration.isShowValue()) {
            
        }
    }
    
    /**
     * Draw the needle indicator on the gauge.
     *
     * @param canvas the canvas to draw to
     * @param valueToDraw the current value
     * @param alertPaint the color of the alert paint to use for showing alert status
     */
    protected void drawNeedle(Canvas canvas, float valueToDraw) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        
        // Gets current value facing straight up.
        canvas.rotate(valueToDegreesOfRotation(valueToDraw), 0.5f, 0.5f);
        
        canvas.drawPath(needlePath, needlePaint);
        
        canvas.restore();
    }
    
    /**
     * Modulo operation that returns everything as a positive number.
     *
     * @param a the first operand
     * @param b the second operand
     */
    protected float negativeAwareModulo(float a, float b) {
        return (a % b + b) % b;
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
    protected void addPairsForSweepGradient(float position, int color, List<Integer> colors, List<Float> positions) {
        if (sweepDirection == SweepDirection.CLOCKWISE) {
            colors.add(color);
            positions.add(position);
        } else {
            colors.add(0, color);
            positions.add(0, 1 - position);
        }
    }
    
    /**
     * Maps a value within a defined range to a percentage of an arc that spans the full range of the gauge.
     *
     * @param minValue the minimum value of the gauge
     * @param maxValue the maximum value of the gauge
     * @param value the value to find the position before
     */
    protected float valueToSweepGradientPosition(float minValue, float maxValue, float value) {
        
        float range = maxValue - minValue;
        float degreeRangeScaleFactor = (endDegrees - startDegrees) / 360;
        
        return ((value - minValue) / range) * degreeRangeScaleFactor; 
    }
    
    /**
     * Return the degrees of sweep to use for an arc that spans a range on the scale.
     *
     * @param range the range to get the degrees for
     */
    protected float rangeToDegreesOfArcSweep(float range) {
        float degreesOfSweep = range / valueToDegreeRatio;
        
        if (sweepDirection == SweepDirection.COUNTER_CLOCKWISE) {
            // Negative rotation for a counter clockwise sweep.
            degreesOfSweep *= -1;
        }
        
        return degreesOfSweep;
    }
    
    /**
     * Returns the degrees of an arc origin to align it to a value on the gauge.
     * 
     * @param value the value to translate
     */
    protected float valueToDegreesOfArcOrigin(float value) {
        // Get the offset from the start location for the given value
        float degreesOfRotation = startDegrees + ((value - configuration.getMinValue()) / valueToDegreeRatio);
        
        if (sweepDirection == SweepDirection.COUNTER_CLOCKWISE) {
            // Negative rotation for a counter clockwise sweep.
            degreesOfRotation *= -1;
        }
        
        // Return the value relative to the origin for arcs witch is east.
        return originOrientation.getDegreesToEast() + degreesOfRotation;
    }
    
    /**
     * Returns the degrees of rotation to position a value straight up on the gauge.
     * 
     * @param value the value to translate
     */
    protected float valueToDegreesOfRotation(float value) {
        // Get the offset from the start location for the given value
        float degreesOfRotation = startDegrees + ((value - configuration.getMinValue()) / valueToDegreeRatio);
        
        if (sweepDirection == SweepDirection.COUNTER_CLOCKWISE) {
            // Negative rotation for a counter clockwise sweep.
            degreesOfRotation *= -1;
        }
        
        // Return the value relative to north so that we can use this value to draw values
        return originOrientation.getDegreesToNorth() + degreesOfRotation;
    }
}
