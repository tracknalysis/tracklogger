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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;

/**
 * @author David Valeri
 *
 * @param <T> the type of data that the provider returns
 */
public abstract class AbstractDataProvider<T extends AbstractData> implements DataProvider<T> {

    protected List<DataListener<T>> listeners = 
            new CopyOnWriteArrayList<DataListener<T>>();
    
    protected volatile double frequency;
    protected T lastData;
    protected long frequencyLoggedTime = 0;
    
    @Override
    public double getUpdateFrequency() {
        return frequency;
    }

    @Override
    public void addSynchronousListener(DataListener<T> listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeSynchronousListener(DataListener<T> listener) {
        listeners.remove(listener);
    }
    
    protected void notifySynchronousListeners(T data) {
        
        Logger log = getLogger();
        
        if (lastData != null) {
            frequency = 1000d / (double) (data.getDataRecivedTime() - lastData
                    .getDataRecivedTime());
            long now = System.currentTimeMillis(); 
            if (now - frequencyLoggedTime > 5000) {
                frequencyLoggedTime = now;
                log.info("Update frequency is {}Hz.", frequency);
            }
            
            log.debug("Update frequency is {}Hz.", frequency);
        }
        
        lastData = data;
        
        for (DataListener<T> listener : listeners) {
            try {
                listener.receiveData(data);
            } catch (Exception e) {
                log.error("Error in data listener " + listener + ".",
                        e);
            }
        }
    }
    
    protected abstract Logger getLogger();
}
