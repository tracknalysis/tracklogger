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

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator.NotificationType;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;

/**
 * The default factory instance that provides a real coordinator implementation for production use.
 *
 * @author David Valeri
 */
public final class ServiceBasedDataProviderCoordinatorFactory
        extends DataProviderCoordinatorFactory {
    
    @Override
    public synchronized DataProviderCoordinator createDataProviderCoordinator(
            DataProviderCoordinatorService dataProviderCoordinatorService,
            NotificationStrategy<NotificationType> notificationStrategy,
            Context context, BluetoothAdapter btAdapter) {
        return new AndroidTrackLoggerDataProviderCoordinator(
                dataProviderCoordinatorService, notificationStrategy, context, btAdapter); 
    }
}