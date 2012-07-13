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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import net.tracknalysis.tracklogger.dataprovider.TrackLoggerDataProviderCoordinator;

/**
 * A simple abstract factory to enable testing of activities.
 *
 * @author David Valeri
 */
public abstract class TrackLoggerDataProviderCoordinatorFactory {
    
    private static TrackLoggerDataProviderCoordinatorFactory instance;
    
    public static synchronized void setInstance(TrackLoggerDataProviderCoordinatorFactory instance) {
        if (instance == null) {
            throw new NullPointerException("instance cannot be null.");
        }
        
        TrackLoggerDataProviderCoordinatorFactory.instance = instance;
    }
    
    public static synchronized TrackLoggerDataProviderCoordinatorFactory getInstance() {
        if (instance == null) {
            instance = new DefaultTrackLoggerDataProviderCoordinatorFactory();
        }
        return instance;
    }
    
    public abstract TrackLoggerDataProviderCoordinator getTrackLoggerDataProviderCoordinator(
            Handler handler, Context context, BluetoothAdapter btAdapter);
    
    public static final class DefaultTrackLoggerDataProviderCoordinatorFactory
            extends TrackLoggerDataProviderCoordinatorFactory {

        @Override
        public TrackLoggerDataProviderCoordinator getTrackLoggerDataProviderCoordinator(
                Handler handler, Context context, BluetoothAdapter btAdapter) {
            return new AndroidTrackLoggerDataProviderCoordinator(handler, context, btAdapter);
        }
    }
}
