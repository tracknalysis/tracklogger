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

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.activity.LogActivity;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

/**
 * @author David Valeri
 */
public class DataProviderCoordinatorFactoryService extends Service {
    
    private static final int ONGOING_NOTIFICATION = 1;
    
    private final IBinder binder = new LocalBinder();
    private volatile DataProviderCoordinator instance;
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    public synchronized DataProviderCoordinator createDataProviderCoordinator(
            Handler handler, Context context, BluetoothAdapter btAdapter) {
        if (instance != null) {
            throw new IllegalStateException("Cannot instantiate multiple data provider coordinators.");
        }
        
        instance = new ServiceBasedAndroidTrackLoggerDataProviderCoordinator(handler, context, btAdapter); 
        return instance;
    }
    
    public synchronized DataProviderCoordinator getDataProviderCoordinator() {
        return instance;
    }
    
    public synchronized void removeDataProviderCoordinator() {
        instance = null;
        stopForeground(true);
    }
    
    public class LocalBinder extends Binder {
        public DataProviderCoordinatorFactoryService getService() {
            return DataProviderCoordinatorFactoryService.this;
        }
    }
    
    protected class ServiceBasedAndroidTrackLoggerDataProviderCoordinator extends
        AndroidTrackLoggerDataProviderCoordinator {
        
        public ServiceBasedAndroidTrackLoggerDataProviderCoordinator(Handler handler, Context context, BluetoothAdapter btAdapter) {
            super(handler, context, btAdapter);
        }
        
        @Override
        protected void preStart() {
            Notification notification = new Notification(R.drawable.icon, getText(R.string.log_notification_message),
                    System.currentTimeMillis());
            Intent notificationIntent = new Intent(getContext(), LogActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);
            notification.setLatestEventInfo(getContext(), getText(R.string.log_notification_title),
                    getText(R.string.log_notification_message), pendingIntent);
            startForeground(ONGOING_NOTIFICATION, notification);

            super.preStart();
        }
        
        @Override
        protected void postStop() {
            stopForeground(true);
            super.postStop();
        }
    }
}
