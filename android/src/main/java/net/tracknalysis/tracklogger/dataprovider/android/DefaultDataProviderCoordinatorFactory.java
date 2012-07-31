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
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;

/**
 * @author David Valeri
 */
public final class DefaultDataProviderCoordinatorFactory
        extends DataProviderCoordinatorFactory {
    
    private final DataProviderCoordinatorFactoryService service;
    
    public DefaultDataProviderCoordinatorFactory(
            DataProviderCoordinatorFactoryService service) {
        this.service = service;
    }
    
    @Override
    public synchronized DataProviderCoordinator createDataProviderCoordinator(
            Handler handler, Context context, BluetoothAdapter btAdapter) {
        return service.createDataProviderCoordinator(handler, context, btAdapter);
    }
    
    public synchronized DataProviderCoordinator getDataProviderCoordinator() {
        return service.getDataProviderCoordinator();
    }
    
    public synchronized void removeDataProviderCoordinator() {
        service.removeDataProviderCoordinator();
    }
}