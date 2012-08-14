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

import net.tracknalysis.common.android.notification.SwapableAndroidNotificationStrategy;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.LocationData;
import net.tracknalysis.tracklogger.dataprovider.TimingData;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

/**
 * Service to manage active singleton instance of a {@link DataProviderCoordinator} in the application.
 * See {@link #initialize(Handler, Context, BluetoothAdapter)} and {@link #uninitialize()} for more
 * details.
 *
 * @author David Valeri
 */
public class DataProviderCoordinatorService extends Service implements DataProviderCoordinator{
    
    static final int ONGOING_NOTIFICATION = 1;
    
    private final IBinder binder = new LocalBinder();
    private volatile DataProviderCoordinator delegate;
    private volatile SwapableAndroidNotificationStrategy<NotificationType> notificationStrategy;
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Initializes the service with the provided resources.
     *
     * @param handler the handler for receiving events from the service
     * @param context the context to use in the service
     * @param btAdapter the BT adapter to use in the service
     *
     * @throws IllegalStateException if the service is already initialized
     *
     * @see #uninitialize() 
     */
    public synchronized void initialize(Handler handler, Context context, BluetoothAdapter btAdapter) {
        getDelegate(true);
        
        notificationStrategy = new SwapableAndroidNotificationStrategy<NotificationType>(handler);
        delegate = DataProviderCoordinatorFactory.getInstance()
                .createDataProviderCoordinator(this, notificationStrategy, context,
                        btAdapter);
    }
    
    public synchronized boolean isInitialized() {
        return delegate != null;
    }
    
    public synchronized void clearHandler() {
        notificationStrategy.setHandler(null);
    }
    
    public synchronized void replaceHandler(Handler handler) {
        notificationStrategy.setHandler(handler);
    }
    
    /**
     * Destroys the service configuration, stopping the coordinator if needed,
     * allowing clients to re-initialize it later.
     * 
     * @throws IllegalStateException
     *             if the service is not already initialized
     * 
     * @see #initialize(Handler, Context, BluetoothAdapter)
     */
    public synchronized void uninitialize() {
        try {
            getDelegate(false).stop();
        } finally {
            stopForeground(true);
        }
        delegate = null;
    }
    
    @Override
    public void start() {
        getDelegate(false).start();
    }
    
    @Override
    public void startAsynch() {
        getDelegate(false).startAsynch();
    }

    @Override
    public void stop() {
        getDelegate(false).stop();
    }

    @Override
    public boolean isRunning() {
        return getDelegate(false).isRunning();
    }

    @Override
    public LocationData getCurrentLocationData() {
        return getDelegate(false).getCurrentLocationData();
    }

    @Override
    public double getLocationDataUpdateFrequency() {
        return getDelegate(false).getLocationDataUpdateFrequency();
    }

    @Override
    public AccelData getCurrentAccelData() {
        return getDelegate(false).getCurrentAccelData();
    }

    @Override
    public double getAccelDataUpdateFrequency() {
        return getDelegate(false).getAccelDataUpdateFrequency();
    }

    @Override
    public EcuData getCurrentEcuData() {
        return getDelegate(false).getCurrentEcuData();
    }

    @Override
    public double getEcuDataUpdateFrequency() {
        return getDelegate(false).getEcuDataUpdateFrequency();
    }

    @Override
    public TimingData getCurrentTimingData() {
        return getDelegate(false).getCurrentTimingData();
    }

    @Override
    public boolean isLoggingStartTriggerFired() {
        return getDelegate(false).isLoggingStartTriggerFired();
    }

    @Override
    public boolean isReady() {
        return getDelegate(false).isReady();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public synchronized boolean onUnbind(Intent intent) {
        if (delegate != null) {
            if (!delegate.isRunning()) {
                stopSelf();
            }
        }
        return false;
    }
    
    @Override
    public synchronized void onDestroy() {
        if (isInitialized()) {
            uninitialize();
        }
        super.onDestroy();
    }
    
    private synchronized DataProviderCoordinator getDelegate(boolean expectNull) {
        if (delegate == null && !expectNull) {
            throw new IllegalStateException("Service not initialized.");
        } else if (delegate != null && expectNull) {
            throw new IllegalStateException(
                    "Service already initialized.  The service cannot be initialized again" +
                    "until destroy has been called.");
        }
        
        return delegate;
    }

    public class LocalBinder extends Binder {
        public DataProviderCoordinatorService getService() {
            return DataProviderCoordinatorService.this;
        }
    }
}
