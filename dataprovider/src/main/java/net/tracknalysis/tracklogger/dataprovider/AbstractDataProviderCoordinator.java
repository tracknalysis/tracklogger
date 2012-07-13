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
package net.tracknalysis.tracklogger.dataprovider;

/**
 * @author David Valeri
 */
public abstract class AbstractDataProviderCoordinator implements DataProviderCoordinator {
    
    private volatile boolean running;
    
    private DataListener<LocationData> locationListener;
    private DataListener<AccelData> accelListener;
    private DataListener<EcuData> ecuListener;
    private DataListener<TimingData> timingListener;
    
    @Override
    public synchronized void start() {
        locationListener = new DataListener<LocationData>() {
            
            @Override
            public void receiveData(LocationData data) {
                handleUpdate(data);
            }
        };
        
        accelListener = new DataListener<AccelData>() {
            
            @Override
            public void receiveData(AccelData data) {
                handleUpdate(data);
            }
        };
        
        ecuListener = new DataListener<EcuData>() {
            
            @Override
            public void receiveData(EcuData data) {
                handleUpdate(data);
            }
        };
        
        timingListener = new DataListener<TimingData>() {
            
            @Override
            public void receiveData(TimingData data) {
                handleUpdate(data);
            }
        };
        
        getLocationDataProvider().addSynchronousListener(locationListener);
        getAccelDataProvider().addSynchronousListener(accelListener);
        if (isEcuDataProviderEnabled()) {
            getEcuDataProvider().addSynchronousListener(ecuListener);
        }
        getTimingDataProvider().addSynchronousListener(timingListener);
        
        getLocationDataProvider().start();
        getAccelDataProvider().start();
        if (getEcuDataProvider() != null) {
            getEcuDataProvider().start();
        }
        getTimingDataProvider().start();
        
        running = true;
    }
    
    @Override
    public synchronized void stop() {
        getLocationDataProvider().removeSynchronousListener(locationListener);
        getAccelDataProvider().removeSynchronousListener(accelListener);
        if (isEcuDataProviderEnabled()) {
            getEcuDataProvider().removeSynchronousListener(ecuListener);
        }
        getTimingDataProvider().removeSynchronousListener(timingListener);
        getLocationDataProvider().stop();
        getTimingDataProvider().stop();
        getAccelDataProvider().stop();
        if (getEcuDataProvider() != null) {
            getEcuDataProvider().stop();
        }
        
        running = false;
    }
    
    @Override
    public final boolean isRunning() {
        return running;
    }
    
    public final LocationData getCurrentLocationData() {
        return getLocationDataProvider().getCurrentData();
    }
    
    public final double getLocationDataUpdateFrequency() {
        return getLocationDataProvider().getUpdateFrequency();
    }
    
    public final AccelData getCurrentAccelData() {
        return getAccelDataProvider().getCurrentData();
    }
    
    public final double getAccelDataUpdateFrequency() {
        return getAccelDataProvider().getUpdateFrequency();
    }
    
    public final EcuData getCurrentEcuData() {
        return isEcuDataProviderEnabled() ? getEcuDataProvider().getCurrentData() : null;
    }
    
    public final double getEcuDataUpdateFrequency() {
        return isEcuDataProviderEnabled() ? getEcuDataProvider().getUpdateFrequency() : 0d;
    }
    
    public final TimingData getCurrentTimingData() {
        return getTimingDataProvider().getCurrentData();
    }
    
    protected final boolean isEcuDataProviderEnabled() {
        return getEcuDataProvider() != null;
    }
    
    /**
     * Called when new location data arrives.
     *
     * @param locationData the new location data
     */
    protected abstract void handleUpdate(LocationData data);
    
    /**
     * Called when new acceleration data arrives.
     *
     * @param data the new acceleration data
     */
    protected abstract void handleUpdate(AccelData data);
    
    /**
     * Called when new ECU data arrives.
     *
     * @param data the new ECU data
     */
    protected abstract void handleUpdate(EcuData data);
    
    /**
     * Called when new timing data arrives.
     *
     * @param data the new timing data
     */
    protected abstract void handleUpdate(TimingData data);
    
    protected abstract AccelDataProvider getAccelDataProvider();
    
    protected abstract LocationDataProvider getLocationDataProvider();
    
    protected abstract EcuDataProvider getEcuDataProvider();
    
    protected abstract TimingDataProvider getTimingDataProvider();
}
