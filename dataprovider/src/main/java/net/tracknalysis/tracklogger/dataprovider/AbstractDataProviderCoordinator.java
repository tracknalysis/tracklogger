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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.tracknalysis.common.notification.NotificationListener;
import net.tracknalysis.common.notification.NotificationType;
import net.tracknalysis.tracklogger.model.AccelData;
import net.tracknalysis.tracklogger.model.EcuData;
import net.tracknalysis.tracklogger.model.LocationData;
import net.tracknalysis.tracklogger.model.TimingData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation providing basic lifecycle for {@link DataProvider}s and registration
 * of listeners for processing of data from the providers.
 *
 * @author David Valeri
 */
public abstract class AbstractDataProviderCoordinator implements DataProviderCoordinator {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataProviderCoordinator.class);
    
    private volatile boolean running;
    
    private DataListener<LocationData> locationListener;
    private DataListener<AccelData> accelListener;
    private DataListener<EcuData> ecuListener;
    private DataListener<TimingData> timingListener;
    
    private final List<WeakReference<NotificationListener<DataProviderCoordinatorNotificationType>>> notificationStrategies =
            new LinkedList<WeakReference<NotificationListener<DataProviderCoordinatorNotificationType>>>();
    private volatile DataProviderCoordinatorNotificationType lastNotificationType = 
            DataProviderCoordinatorNotificationType.STOPPED;
    private volatile Object lastNotificationBody;
    
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
        if (isEcuDataProviderEnabled()) {
            getEcuDataProvider().start();
        }
        
        running = true;
    }
    
    @Override
    public void startAsynch() {
        Thread t = new Thread() {
            public void run() {
                try {
                    AbstractDataProviderCoordinator.this.start();
                } catch (RuntimeException e) {
                    LOG.error("Error starting data provider coordinator asynchronously.", e);
                }
            };
        };
        t.start();
    }
    
    @Override
    public synchronized void stop() {
       cleanup();
       running = false;
    }
    
    @Override
    public final boolean isRunning() {
        return running;
    }
    
    @Override
    public final LocationData getCurrentLocationData() {
        return getLocationDataProvider().getCurrentData();
    }
    
    @Override
    public final double getLocationDataUpdateFrequency() {
        return getLocationDataProvider().getUpdateFrequency();
    }
    
    @Override
    public final AccelData getCurrentAccelData() {
        return getAccelDataProvider().getCurrentData();
    }
    
    @Override
    public final double getAccelDataUpdateFrequency() {
        return getAccelDataProvider().getUpdateFrequency();
    }
    
    @Override
    public final EcuData getCurrentEcuData() {
        return isEcuDataProviderEnabled() ? getEcuDataProvider().getCurrentData() : null;
    }
    
    @Override
    public final double getEcuDataUpdateFrequency() {
        return isEcuDataProviderEnabled() ? getEcuDataProvider().getUpdateFrequency() : 0d;
    }
    
    @Override
    public final TimingData getCurrentTimingData() {
        return getTimingDataProvider().getCurrentData();
    }
    
    @Override
    public final void register(
            NotificationListener<DataProviderCoordinatorNotificationType> notificationStrategy) {
        
        synchronized (notificationStrategies) {
            scrubStrategies(notificationStrategies);
            
            int index = findInWeakReferenceList(notificationStrategies, notificationStrategy);
            
            if (index == -1) {
                notificationStrategies.add(
                        new WeakReference<NotificationListener<DataProviderCoordinatorNotificationType>>(
                                notificationStrategy));
                
                if (lastNotificationBody != null) {
                    notificationStrategy.onNotification(lastNotificationType, lastNotificationBody);
                } else {
                    notificationStrategy.onNotification(lastNotificationType);
                }
            }
        }
    }
    
    @Override
    public final void unRegister(
            NotificationListener<DataProviderCoordinatorNotificationType> notificationStrategy) {
        
        synchronized (notificationStrategies) {
            scrubStrategies(notificationStrategies);
                
            int index = findInWeakReferenceList(notificationStrategies, notificationStrategy);
                
            if (index > -1) {
                notificationStrategies.remove(index);
            }
        }
    }
    
    protected final void sendNotification(DataProviderCoordinatorNotificationType notificationType) {
        sendNotification(notificationType, null);
    }
    
    protected final void sendNotification(DataProviderCoordinatorNotificationType notificationType, Object body) {
        synchronized (notificationStrategies) {
            lastNotificationType = notificationType;
            lastNotificationBody = body;
            
            sendNotificationInternal(notificationType, body);
        }
    }
    
    protected final boolean isEcuDataProviderEnabled() {
        return getEcuDataProvider() != null;
    }
    
    protected void cleanup() {
        
        try {
            if (getLocationDataProvider() != null) {
                getLocationDataProvider().removeSynchronousListener(locationListener);
                getLocationDataProvider().stop();
            }
        } catch (RuntimeException e) {
            LOG.error("Error cleaning up location data provider.", e);
        }
        
        try {
            if (getTimingDataProvider() != null) {
                getTimingDataProvider().removeSynchronousListener(timingListener);
                getTimingDataProvider().stop();
            }
        } catch (RuntimeException e) {
            LOG.error("Error cleaning up timing data provider.", e);
        }
        
        try {
            if (getAccelDataProvider() != null) {
                getAccelDataProvider().removeSynchronousListener(accelListener);
                getAccelDataProvider().stop();
            }
        } catch (RuntimeException e) {
            LOG.error("Error cleaning up accel data provider.", e);
        }
        
        try {
            if (getEcuDataProvider() != null) {
                getEcuDataProvider().removeSynchronousListener(ecuListener);
                getEcuDataProvider().stop();
            }
        } catch (RuntimeException e) {
            LOG.error("Error cleaning up ecu data provider.", e);
        }
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
    
    /**
     * Searches a list of references for a reference that points to {@code notificationStrategy}.
     *
     * @param strategies the list to search
     * @param notificationStrategy the strategy to look for in the list
     *
     * @return -1 if no matching entry was found or the index of the matching reference in the list
     */
    private int findInWeakReferenceList(
            List<? extends WeakReference<? extends NotificationListener<? extends NotificationType>>> strategies,
            NotificationListener<?> notificationStrategy) {
        
        int index = 0;
        
        for (WeakReference<? extends NotificationListener<?>> existingStrategy : strategies) {
            if (existingStrategy.get() == notificationStrategy) {
                return index;
            }
            
            index++;
        }
        
        return -1;
    }
    
    /**
     * Culls any references from the list which have had their target garbage collected.
     *
     * @param strategies the list to inspect for stale references
     */
    private void scrubStrategies(
            List<? extends WeakReference<? extends NotificationListener<? extends NotificationType>>> strategies) {
        
        Iterator<? extends WeakReference<? extends NotificationListener<? extends NotificationType>>> iterator =
                strategies.iterator();
        
        while (iterator.hasNext()) {
            WeakReference<?> strategyRef = iterator.next();
            if (strategyRef.get() == null) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Sends notifications to registered listeners.  Callers must synchronize on {@link #notificationStrategies}
     * externally.
     */
    private void sendNotificationInternal(DataProviderCoordinatorNotificationType notificationType, Object body) {
        for (WeakReference<NotificationListener<DataProviderCoordinatorNotificationType>> ref 
                : notificationStrategies) {
            
            NotificationListener<DataProviderCoordinatorNotificationType> strategy = ref.get();
            
            if (strategy != null) {
                if (body != null) {
                    strategy.onNotification(notificationType, body);
                } else {
                    strategy.onNotification(notificationType);
                }
            }
        }
    }
}
