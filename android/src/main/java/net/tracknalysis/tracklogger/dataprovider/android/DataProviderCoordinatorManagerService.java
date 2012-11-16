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

import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
public class DataProviderCoordinatorManagerService extends Service {
    
    static final int ONGOING_NOTIFICATION = 1;
    
    private final IBinder binder = new LocalBinder();
    private volatile DataProviderCoordinator instance;
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Initializes the service with the provided resources.
     *
     * @param context the context to use in the service
     * @param btAdapter the BT adapter to use in the service
     * @param splitMarkers the markers to use for timing data
     *
     * @throws IllegalStateException if the service is already initialized
     *
     * @see #uninitialize() 
     */
    public synchronized void initialize(Context context, BluetoothAdapter btAdapter,
            Uri splitMarkerSetUri) {
        getInstance(true);
        
        instance = DataProviderCoordinatorFactory.getInstance()
                .createDataProviderCoordinator(this, context,
                        btAdapter, splitMarkerSetUri);
    }
    
    public synchronized boolean isInitialized() {
        return instance != null;
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
            getInstance(false).stop();
        } finally {
            stopForeground(true);
        }
        instance = null;
    }
    
    /**
     * Returns the initialized instance being managed.
     *
     * @throws IllegalStateException
     *             if the service is not already initialized
     */
    public synchronized DataProviderCoordinator getInstance() {
        return getInstance(false);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public synchronized boolean onUnbind(Intent intent) {
        if (instance != null) {
            if (!instance.isRunning()) {
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
    
    private synchronized DataProviderCoordinator getInstance(boolean expectNull) {
        if (instance == null && !expectNull) {
            throw new IllegalStateException("Service not initialized.");
        } else if (instance != null && expectNull) {
            throw new IllegalStateException(
                    "Service already initialized.  The service cannot be initialized again" +
                    "until destroy has been called.");
        }
        
        return instance;
    }

    public class LocalBinder extends Binder {
        public DataProviderCoordinatorManagerService getService() {
            return DataProviderCoordinatorManagerService.this;
        }
    }
}
