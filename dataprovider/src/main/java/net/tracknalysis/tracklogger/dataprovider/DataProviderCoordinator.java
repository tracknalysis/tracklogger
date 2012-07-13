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

import java.util.HashMap;
import java.util.Map;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.notification.NotificationType;
import net.tracknalysis.tracklogger.model.AccelData;
import net.tracknalysis.tracklogger.model.EcuData;
import net.tracknalysis.tracklogger.model.LocationData;
import net.tracknalysis.tracklogger.model.TimingData;

/**
 * @author David Valeri
 */
public interface DataProviderCoordinator {

    /**
     * Enumeration of notification types that this implementation may generate.
     */
    public static enum DataProviderCoordinatorNotificationType implements NotificationType {
        /**
         * Triggered when the the startup of the coordinator is begins.
         */
        STARTING,
        /**
         * Triggered when the startup of the coordinator is complete.
         */
        STARTED,
        /**
         * Triggered when the startup of the coordinator fails.  The notification contains the exception that
         * triggered the failure.
         */
        START_FAILED,
        /**
         * Triggered when an update from a subordinate data provider arrives and the coordinator is not
         * yet ready to start logging.  The notification contains an array of {@link LocationData}, {@link AccelData},
         * and {@link EcuData}.
         */
        READY_PROGRESS,
        /**
         * Triggered when an all subordinate data providers are ready and logging can start. 
         */
        READY,
        /**
         * Triggered when the condition for the start of timing is met.
         */
        TIMING_START_TRIGGER_FIRED,
        /**
         * Triggered when an update to timing data becomes available.  The notification contains
         * the new {@link TimingData}.
         */
        TIMING_DATA_UPDATE,
        /**
         * Triggered when there is a failure recording logged data.
         */
        LOGGING_FAILED,
        /**
         * Triggered when the the shutdown of the coordinator is begins.
         */
        STOPPING,
        /**
         * Triggered when the the shutdown of the coordinator completes.
         */
        STOPPED,
        /**
         * Triggered when the the shutdown of the coordinator fails.  The notification contains the exception that
         * triggered the failure.
         */
        STOP_FAILED;
        
        private static final Map<Integer, DataProviderCoordinatorNotificationType> intToTypeMap = new HashMap<Integer, DataProviderCoordinatorNotificationType>();
        
        static {
            for (DataProviderCoordinatorNotificationType type : DataProviderCoordinatorNotificationType.values()) {
                intToTypeMap.put(type.ordinal(), type);
            }
        }
    
        public static DataProviderCoordinatorNotificationType fromInt(int i) {
            DataProviderCoordinatorNotificationType type = intToTypeMap.get(Integer.valueOf(i));
            if (type == null) {
                throw new IllegalArgumentException(
                        "No enum const " + i);
            }
            return type;
        }
    
        @Override
        public int getNotificationTypeId() {
            return ordinal();
        }
    }

    void start();
    
    void startAsynch();

    void stop();

    boolean isRunning();
    
    LocationData getCurrentLocationData();
    
    double getLocationDataUpdateFrequency();

    AccelData getCurrentAccelData();

    double getAccelDataUpdateFrequency();

    EcuData getCurrentEcuData();

    double getEcuDataUpdateFrequency();

    TimingData getCurrentTimingData();

    boolean isLoggingStartTriggerFired();

    boolean isReady();
    
    /**
     * Register a listener for notifications regarding the overall state of the coordinator.  When the listener is
     * registered, it will immediately receive the last known notification sent by the coordinator.  Following
     * notifications will be delivered as they are produced by the coordinator.  This behavior allows for listeners
     * to synchronize immediately on registration.
     */
    void register(NotificationStrategy<DataProviderCoordinatorNotificationType> notificationStrategy);
    
    /**
     * Unregister a listener for notifications regarding the overall state of the service.
     */
    void unRegister(NotificationStrategy<DataProviderCoordinatorNotificationType> notificationStrategy);
}
