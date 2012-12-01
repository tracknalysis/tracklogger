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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Base class for gauge implementations.
 *
 * @author David Valeri
 */
public abstract class AbstractGauge extends SurfaceView implements SurfaceHolder.Callback, Gauge {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBarGauge.class);
    
    private volatile boolean haveSurface = false;
    private volatile boolean haveSurfaceDetails = false;
    
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    
    private final Paint alertPaint;
    private final Paint clearPaint;
    
    private float alertThresholdRangePercentage = 0.15f;
    
    private float minCriticalThreshold;
    private float minCriticalColorRatio;
    
    private float minWarningThreshold;
    private float minWarningColorRatio;
    
    private float maxWarningThreshold;
    private float maxWarningColorRatio;
    
    private float maxCriticalThreshold;
    private float maxCriticalColorRatio;
    
    private float currentValue;
    
    private GaugeConfiguration config;
    
    public AbstractGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        if (!isInEditMode()) {
            // Set us up for transparent backgrounds.
            setZOrderOnTop(true);
            getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }
        
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        
        alertPaint = new Paint();
        alertPaint.setAntiAlias(true);
        
        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
    }
    
    @Override
    public final void init(GaugeConfiguration configuration) {
        synchronized (getHolder()) {
            config = configuration;
            rescale();
            update();
        }
    }

    @Override
    public final  void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
        update();
    }

    @Override
    public final void surfaceCreated(SurfaceHolder holder) {
        synchronized (getHolder()) {
            haveSurface = true;
        }
    }

    @Override
    public final void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        synchronized (getHolder()) {
            canvasWidth = width;
            canvasHeight = height;
            haveSurfaceDetails = true;
            rescale();
            update();
        }
    }

    @Override
    public final void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (getHolder()) {
            haveSurface = false;
            haveSurfaceDetails = false;
        }
    }
    
    /**
     * Returns the width of the current canvas such that sub-classes don't have to track this on their own.
     */
    protected final int getCanvasWidth() {
        return canvasWidth;
    }

    /**
     * Returns the height of the current canvas such that sub-classes don't have to track this on their own.
     */
    protected final int getCanvasHeight() {
        return canvasHeight;
    }
    
    /**
     * Returns the percentage of the next alert value at which alert color blending should occur when using
     * gradients.
     */
    protected final float getAlertThresholdFactor() {
        return alertThresholdRangePercentage;
    }
    
    /**
     * Gets the threshold for blending gradients on the min critical alert value.
     *
     * @see #getAlertThresholdFactor()
     */
    public float getMinCriticalThreshold() {
        return minCriticalThreshold;
    }
    
    /**
     * Gets the threshold for blending gradients on the min warning alert value.
     *
     * @see #getAlertThresholdFactor()
     */
    public float getMinWarningThreshold() {
        return minWarningThreshold;
    }

    /**
     * Gets the threshold for blending gradients on the max warning alert value.
     *
     * @see #getAlertThresholdFactor()
     */
    protected final float getMaxWarningThreshold() {
        return maxWarningThreshold;
    }

    /**
     * Gets the threshold for blending gradients on the max critical alert value.
     *
     * @see #getAlertThresholdFactor()
     */
    protected final float getMaxCriticalThreshold() {
        return  maxCriticalThreshold;
    }

    protected final GaugeConfiguration getConfiguration() {
        return config;
    }

    /**
     * Calculates the alert color based on the configured ranges and a given value.
     *
     * @param value the value to calculate the color for
     *
     * @return the alert color
     */
    protected int valueToAlertColor(float value) {
        int red = 0;
        int green = 255;
        boolean matched = false;
        
        if (config.isMinCriticalEnabled()) {
            if (value <= config.getMinCriticalValue()) {
                red = 255;
                green = 0;
                matched = true;
            }
            
            if (config.isUseAlertColorGradient() && value <= minCriticalThreshold) {
                red = 255;
                green = 255 - (int) (minCriticalColorRatio * (maxCriticalThreshold - value));
                matched = true;
            }
        }
        
        if (config.isMinWarningEnabled() && !matched) {
            if (value <= config.getMinWarningValue()) {
                red = 255;
                green = 255;
                matched = true;
            }
            
            if (config.isUseAlertColorGradient() && value <= minWarningThreshold) {
                red = (int) (minWarningColorRatio * (maxWarningThreshold - value));
                green = 255;
                matched = true;
            }
        }
        
        if (config.isMaxCriticalEnabled() && !matched) {
            if (value >= config.getMaxCriticalValue()) {
                red = 255;
                green = 0;
                matched = true;
            }
            
            if (config.isUseAlertColorGradient() && value >= maxCriticalThreshold) {
                red = 255;
                green = 255 - (int) (maxCriticalColorRatio * (value - maxCriticalThreshold));
                matched = true;
            }
        }
        
        if (config.isMaxWarningEnabled() && !matched) {
            if (value >= config.getMaxWarningValue()) {
                red = 255;
                green = 255;
                matched = true;
            }
            
            if (config.isUseAlertColorGradient() && value >= maxWarningThreshold) {
                green = 255;
                red = (int) (maxWarningColorRatio * (value - maxWarningThreshold));
            }
        }
        
        return Color.rgb(red, green, 0);
    }
    
    /**
     * Sanitize {@code value} to be between the minimum and maximum values for
     * the gauge.
     * 
     * @param value
     *            the value to sanitize
     *
     * @return the minimum value if {@code value} is less than the minimum
     *         value, the maximum value if {@code value} is greater than the
     *         maximum value, otherwise {@code value}
     *         
     * @see GaugeConfiguration#getMinValue()
     * @see GaugeConfiguration#getMaxValue()
     */
    protected final float restrictValueRange(float value) {
        float restrictedvalue = value;
        if (currentValue < config.getMinValue()) {
            restrictedvalue = config.getMinValue();
        } else if (currentValue > config.getMaxValue()) {
            restrictedvalue = config.getMaxValue();
        }
        
        return restrictedvalue;
    }
    
    /**
     * Called when the gauge is provided with a new configuration or when the underlying surface changes.
     * Subclasses should perform any rescaling and construction of cached resources in the implementation of
     * this method.  Note that the getters on this class reflect the new state of the gauge when this method
     * is called.
     *
     * @param canvasWidth the new canvas width
     * @param canvasHeight the new canvas height
     * @param configuration the new configuration
     */
    protected abstract void reconfigure(int canvasWidth,
            int canvasHeight, GaugeConfiguration configuration);
    
    /**
     * Called when a new value should be drawn on the gauge.
     *
     * @param canvas the canvas to draw on
     * @param valueToDraw the sanitized, per {@link #restrictValueRange(float)}, value to draw
     */
    protected abstract void draw(Canvas canvas, float valueToDraw);
    
    /**
     * Clear the canvas and attempt to draw the gauge if and only if the gauge is in a state where it can
     * be drawn. 
     */
    private void update() {
        SurfaceHolder holder = getHolder();
        
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas(null);
            synchronized (holder) {
                if (haveSurface && config != null && haveSurfaceDetails) {
                    canvas.drawPaint(clearPaint);
                    draw(canvas, restrictValueRange(currentValue));
                }
            }
        } catch (RuntimeException e) {
            if (!haveSurface) {
                LOG.warn("Error attempting to draw gauge during shutdown.", e);
            } else {
                throw e;
            }
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
    
    /**
     * Handle updates to the size and configuration of the gauge by recalculating derived values and then
     * calling {@link #reconfigure(int, int, GaugeConfiguration)} to allow sub-classes to do the same. 
     */
    private void rescale() {
        synchronized (getHolder()) {
            if (config != null && haveSurfaceDetails && haveSurface) {
                if (config.isMinCriticalEnabled()) {
                    minCriticalThreshold = config.getMinCriticalValue()
                            + ((getNextAlertValue(config
                                    .getMinCriticalValue()) - config
                                    .getMinCriticalValue()) * (alertThresholdRangePercentage));
                    
                    minCriticalColorRatio = 255 / (minCriticalThreshold - config.getMinCriticalValue());
                }
                
                if (config.isMinWarningEnabled()) {
                    minWarningThreshold = config.getMinWarningValue()
                            + ((getNextAlertValue(config
                                    .getMinWarningValue()) - config
                                    .getMinWarningValue()) * (alertThresholdRangePercentage));
                    
                    minWarningColorRatio = 255 / (minWarningThreshold - config.getMinWarningValue());
                }
                
                if (config.isMaxWarningEnabled()) {
                    maxWarningThreshold = config.getMaxWarningValue()
                            - ((config.getMaxWarningValue() - getPreviousAlertValue(config
                                    .getMaxWarningValue())) * (alertThresholdRangePercentage));
                    
                    maxWarningColorRatio = 255 / (config.getMaxWarningValue() - maxWarningThreshold);
                }
                
                if (config.isMaxCriticalEnabled()) {
                    maxCriticalThreshold = config.getMaxCriticalValue()
                            - ((config.getMaxCriticalValue() - getPreviousAlertValue(config
                                    .getMaxCriticalValue())) * (alertThresholdRangePercentage));
                    
                    maxCriticalColorRatio = 255 / (config.getMaxCriticalValue() - maxCriticalThreshold);
                }
                
                reconfigure(canvasWidth, canvasHeight, config);
            }
        }
    }
    
    private float getPreviousAlertValue(Float value) {
        if (config.isMaxCriticalEnabled() && value > config.getMaxCriticalValue()) {
            return config.getMaxCriticalValue();
        } else if (config.isMaxWarningEnabled() && value > config.getMaxWarningValue()) {
            return config.getMaxWarningValue();
        } else if (config.isMinWarningEnabled() && value > config.getMinWarningValue()) {
            return config.getMinWarningValue();
        } else if (config.isMinCriticalEnabled() && value > config.getMinCriticalValue()) {
            return config.getMinCriticalValue();
        }
        
        return config.getMinValue();
    }
    
    private float getNextAlertValue(Float value) {
        if (config.isMinCriticalEnabled() && value < config.getMinCriticalValue()) {
            return config.getMinCriticalValue();
        } else if (config.isMinWarningEnabled() && value < config.getMinWarningValue()) {
            return config.getMinWarningValue();
        } else if (config.isMaxWarningEnabled() && value < config.getMaxWarningValue()) {
            return config.getMaxWarningValue();
        } else if (config.isMaxCriticalEnabled() && value < config.getMaxCriticalValue()) {
            return config.getMaxCriticalValue();
        }
        
        return config.getMaxValue();
    }
}
