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
    
    private float maxWarningThreshold;
    private float maxWarningColorRatio;
    
    private float maxCriticalThreshold;
    private float maxCriticalColorRatio;
    
    private float currentValue;
    
    private GaugeConfiguration config;
    
    public AbstractGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        
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
        }
    }

    @Override
    public final void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (getHolder()) {
            haveSurface = false;
            haveSurfaceDetails = false;
        }
    }
    
    protected int getCanvasWidth() {
        return canvasWidth;
    }

    protected int getCanvasHeight() {
        return canvasHeight;
    }

    protected float getMaxWarningThreshold() {
        return maxWarningThreshold;
    }

    protected float getMaxWarningColorRatio() {
        return maxWarningColorRatio;
    }

    protected float getMaxCriticalThreshold() {
        return maxCriticalThreshold;
    }

    protected float getMaxCriticalColorRatio() {
        return maxCriticalColorRatio;
    }
    
    protected GaugeConfiguration getConfiguration() {
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
        
        if (config.isMaxWarningEnabled()) {
            if (value < config.getMaxWarningValue()) {
                green = 255;
                red = 0;
                
                if (config.isUseAlertColorGradient() && value > maxWarningThreshold) {
                    red = (int) (maxWarningColorRatio * (value - maxWarningThreshold));
                }
            } else {
                green = 255;
                red = 255;
            }
        }
        
        if (config.isMaxCriticalEnabled()) {
            if (config.isMaxWarningEnabled()
                    && value > config.getMaxWarningValue()
                    && value < config.getMaxCriticalValue()) {
                green = 255;
                red = 255;
                
                if (config.isUseAlertColorGradient() && value > maxCriticalThreshold) {
                    green = 255 - (int) (maxCriticalColorRatio * (value - maxCriticalThreshold));
                }
            } else if (value >= config.getMaxCriticalValue()) {
                green = 0;
                red = 255;
            }
        }
        
        return Color.rgb(red, green, 9);
    }
    
    protected final float restrictValueRange(float value) {
        float restrictedvalue = value;
        if (currentValue < config.getMinValue()) {
            restrictedvalue = config.getMinValue();
        } else if (currentValue > config.getMaxValue()) {
            restrictedvalue = config.getMaxValue();
        }
        
        return restrictedvalue;
    }
    
    protected abstract void reconfigure(int canvasWidth,
            int canvasHeight, GaugeConfiguration configuration);
    
    protected abstract void draw(Canvas canvas, float valueToDraw);
    
    private void update() {
        SurfaceHolder holder = getHolder();
        
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas(null);
            synchronized (holder) {
                if (!haveSurface && config != null && haveSurfaceDetails) {
                    return;
                }
                
                canvas.drawPaint(clearPaint);
                
                draw(canvas, restrictValueRange(currentValue));
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
    
    private void rescale() {
        synchronized (getHolder()) {
            if (config != null && haveSurfaceDetails) {
                if (config.isMaxWarningEnabled()) {
                    maxWarningThreshold = config.getMaxWarningValue() * 0.75f;
                    maxWarningColorRatio = 255 / (config.getMaxWarningValue() - maxWarningThreshold);
                }
                
                if (config.isMaxCriticalEnabled()) {
                    if (config.isMaxWarningEnabled()) {
                        maxCriticalThreshold = config.getMaxWarningValue() + 
                                ((config.getMaxCriticalValue() - config.getMaxWarningValue()) * 0.75f);
                    } else {
                        maxCriticalThreshold = config.getMaxCriticalValue() * 0.75f;
                    }
                    maxCriticalColorRatio = 255 / (config.getMaxCriticalValue() - maxCriticalThreshold);
                }
                
                reconfigure(canvasWidth, canvasHeight, config);
            }
        }
    }
}
