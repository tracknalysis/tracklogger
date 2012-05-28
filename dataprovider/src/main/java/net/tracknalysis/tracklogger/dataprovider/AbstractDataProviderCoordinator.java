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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Valeri
 */
public abstract class AbstractDataProviderCoordinator implements DataProviderCoordinator {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(AbstractDataProviderCoordinator.class);
    
    private volatile boolean running;
    
    private AccelDataProvider accelDataProvider;
    private LocationDataProvider locationDataProvider;
    private EcuDataProvider ecuDataProvider;
    private TimingDataProvider timingDataProvider;
    private DataListener<LocationData> locationListener;
    private DataListener<TimingData> timingListener;
    
    public AbstractDataProviderCoordinator(AccelDataProvider accelDataProvider,
            LocationDataProvider locationDataProvider, EcuDataProvider ecuDataProvider,
            TimingDataProvider timingDataProvider) {

        this.accelDataProvider = accelDataProvider;
        this.locationDataProvider = locationDataProvider;
        this.ecuDataProvider = ecuDataProvider;
        this.timingDataProvider = timingDataProvider;
    }

    @Override
    public synchronized void start() {
        
        try {
            locationDataProvider.start();
            accelDataProvider.start();
            ecuDataProvider.start();
            timingDataProvider.start();
            
            locationListener = new DataListener<LocationData>() {
                
                @Override
                public void receiveData(LocationData data) {
                    AccelData accelData = accelDataProvider.getCurrentData();
                    EcuData ecuData = ecuDataProvider.getCurrentData();
                    
                    handleUpdate(data, accelData, ecuData);
                }
            };
            
            timingListener = new DataListener<TimingData>() {
                
                @Override
                public void receiveData(TimingData data) {
                    handleUpdate(data);
                }
            };
            
            locationDataProvider.addSynchronousListener(locationListener);
            timingDataProvider.addSynchronousListener(timingListener);
            
            running = true;
        } catch (Exception e) {
            // TODO trigger event for an error here and try to clean up the mess
            LOG.error("BAD!", e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public synchronized void stop() {
        
        try {
            locationDataProvider.removeSynchronousListener(locationListener);
            timingDataProvider.removeSynchronousListener(timingListener);
            locationDataProvider.stop();
            timingDataProvider.stop();
            accelDataProvider.stop();
            ecuDataProvider.stop();
            
            running = false;
        } catch (Exception e) {
            // TODO trigger event for an error here and try to clean up the mess
            LOG.error("BAD!", e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    protected abstract void handleUpdate(LocationData gpsData, AccelData accelData, EcuData ecuData);
    
    protected abstract void handleUpdate(TimingData timingData);
}
