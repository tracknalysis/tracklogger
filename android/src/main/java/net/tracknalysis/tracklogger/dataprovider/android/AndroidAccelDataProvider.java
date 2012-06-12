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
package net.tracknalysis.tracklogger.dataprovider.android;

import net.tracknalysis.tracklogger.dataprovider.AbstractDataProvider;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.AccelData.AccelDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.AccelDataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * @author David Valeri
 */
public class AndroidAccelDataProvider extends AbstractDataProvider<AccelData>
        implements SensorEventListener, AccelDataProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(AndroidAccelDataProvider.class);

    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    private WindowManager windowManager;
    private Display display;
    
    /**
     * The number of times the sensor has been updated
     */
    private long sensorChangeCount = 0;
    
    /**
     * Time in nanoseconds since epoch of last received sensor update
     */
    private long lastEventTime = 0;
    
    /**
     * Average time in nanoseconds between sensor update events.
     */
    private long totalAccelDeltaEventTime = 0;
    
    /**
     * Average time in nanoseconds between now and the time when a sensor event was generated.
     */
    private long totalAccelDeltaSystemTime = 0;
    
    private volatile AccelData currentAccelData;
    
    public AndroidAccelDataProvider(SensorManager sensorManager, WindowManager windowManager) {
        this.sensorManager = sensorManager;
        this.windowManager = windowManager;
        
    }
    
    @Override
    public synchronized void start() {
        if (accelerometer == null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            display = windowManager.getDefaultDisplay();
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public synchronized void stop() {
        if (accelerometer != null) {
            sensorManager.unregisterListener(this, accelerometer);
        }
    }
    
    @Override
    public AccelData getCurrentData() {
        return currentAccelData;
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        
        long deltaSystemTime = -(System.nanoTime() - event.timestamp);
        long receviedTimestamp = (long) (System.currentTimeMillis() + (deltaSystemTime / 1000000d));
        
        if (LOG.isTraceEnabled()) {
            
            sensorChangeCount++;
            if (lastEventTime == 0) {
                lastEventTime = event.timestamp;
            }
            long deltaEventTime = -(event.timestamp - lastEventTime);
            lastEventTime = event.timestamp;
            totalAccelDeltaEventTime += deltaEventTime;
            
            
            long avgAccelDeltaEventTime = totalAccelDeltaEventTime / sensorChangeCount;
            long avgAccelDeltaSystemTime = totalAccelDeltaSystemTime / sensorChangeCount;
            
            LOG.trace(
                    "Got accel sensor update {}.  Delta T from now is {}ms.  "
                            + "Delta T from now is {}ms.  "
                            + "Average delta T from now is {}ms.  "
                            + "Average delta T between events is {}ms.  "
                            + "Received {} updates up to now.",
                    new Object[] {
                            event,
                            deltaSystemTime / (double) 1000000,
                            deltaEventTime / (double) 1000000,
                            avgAccelDeltaSystemTime / (double) 1000000,
                            avgAccelDeltaEventTime / (double) 1000000,
                            sensorChangeCount
                    });
        }
        
        float lateral;
        float vertical;
        float longitudinal;
        
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                lateral = event.values[0];
                vertical = event.values[1];
                longitudinal = -event.values[2];
                break;
            case Surface.ROTATION_90:
                lateral = -event.values[1];
                vertical = event.values[0];
                longitudinal = -event.values[2];
                break;
            case Surface.ROTATION_180:
                lateral = -event.values[0];
                vertical = -event.values[1];
                longitudinal = -event.values[2];
                break;
            case Surface.ROTATION_270:
                lateral = event.values[1];
                vertical = -event.values[0];
                longitudinal = -event.values[2];
                break;
            default:
                lateral = 0;
                vertical = 0;
                longitudinal = 0;
        }
        
        vertical = vertical - SensorManager.GRAVITY_EARTH;
        
        AccelDataBuilder builder = new AccelDataBuilder();
        builder.setDataRecivedTime(receviedTimestamp);
        builder.setLateral(lateral);
        builder.setLongitudinal(longitudinal);
        builder.setVertical(vertical);
        
        currentAccelData = builder.build();
        
        notifySynchronousListeners(currentAccelData);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
