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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.concurrent.GracefulShutdownThread;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger.dataprovider.AbstractDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.AccelDataProvider;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.EcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.LocationData;
import net.tracknalysis.tracklogger.dataprovider.LocationDataProvider;

/**
 * @author David Valeri
 */
public abstract class TrackLoggerDataProviderCoordinator extends
        AbstractDataProviderCoordinator {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(TrackLoggerDataProviderCoordinator.class);
    
    private final NotificationStrategy notificationStrategy;
    private volatile boolean ready;
    private volatile int sessionId;
    private volatile boolean logging;
    private volatile int currentSessionId;
    private volatile boolean loggingStartTriggerFired;
    private LogThread logThread;
    
    private final SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final BlockingQueue<Object> dataQueue = new ArrayBlockingQueue<Object>(100);
    
    /**
     * Enumeration of notification types that this implementation may generate.
     */
    public static enum NotificationType implements net.tracknalysis.common.notification.NotificationType {
        /**
         * Triggered when an update from a subordinate data provider arrives and the coordinator is not
         * yet ready to start logging.  The notification contains an array of {@link LocationData}, {@link AccelData},
         * and {@link EcuData}.
         */
        READY_PROGRESS(0),
        /**
         * Triggered when an all subordinate data providers are ready and logging can start. 
         */
        READY(1),
        /**
         * Triggered when the condition for the start of logging is met.
         */
        LOGGING_START_TRIGGER_FIRED(2),
        /**
         * Triggered when there is a failure recording logged data.
         */
        LOGGING_FAILED(3);

        private final int notificationTypeId;
        
        private NotificationType(int notificationTypeId) {
            this.notificationTypeId = notificationTypeId;
        }

        @Override
        public int getNotificationTypeId() {
            return notificationTypeId;
        }
    }

    public TrackLoggerDataProviderCoordinator(NotificationStrategy notificationStrategy,
            AccelDataProvider accelDataProvider,
            LocationDataProvider gpsDataProvider, EcuDataProvider ecuDataProvider,
            TimingDataProvider timingDataProvider) {
        super(accelDataProvider, gpsDataProvider, ecuDataProvider, timingDataProvider);
        this.notificationStrategy = notificationStrategy;
    }
    
    @Override
    public final synchronized void start() {
        super.start();
        ready = false;
    }
    
    @Override
    public final synchronized void stop() {
        // Call super first so we stop receiving updates immediately.  Otherwise
        // if we shutdown logging first, the state gets out of whack because we
        // emit messages that logging is ready again, even though we are shutting down.
        super.stop();
        stopLogging();
        ready = false;
    }
    
    public final boolean isReady() {
        return ready && isRunning();
    }
    
    public final synchronized void startLogging() {
        if (!logging) {
            if (ready && isRunning()) {
                
                loggingStartTriggerFired = false;
                
                if (sessionId == 0) {
                    currentSessionId = createSession();
                } else {
                    currentSessionId = sessionId;
                    
                    openSession(sessionId);
                }
                
                dataQueue.clear();
                logThread = new LogThread();
                logThread.start();
                
                logging = true;
                
            } else {
                throw new IllegalStateException();
            }
        }
    }
    
    public final synchronized void stopLogging() {
        if (logging) {
            logging = false;
            loggingStartTriggerFired = false;
            logThread.cancel();
            logThread = null;
        }
    }
    
    public final boolean isLogging() {
        return logging;
    }
    
    public final boolean isLoggingStartTriggerFired() {
        return loggingStartTriggerFired;
    }
    
    public final synchronized void setSessionId(int sessionId) {
        if (isRunning()) {
            throw new IllegalStateException();
        } else {
            this.sessionId = sessionId;
        }
    }
    
    @Override
    protected final synchronized void handleUpdate(LocationData gpsData,
            AccelData accelData, EcuData ecuData) {
    	
    	long startTime = System.currentTimeMillis();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Handling updated.  Current status is: running: {}, ready: {}, "
                            + " logging: {}, logging start trigger fired: {}",
                    new Object[] {isRunning(), isReady(), isLogging(), 
                                    isLoggingStartTriggerFired()});
        }
        
        if (!ready && gpsData != null && accelData != null && ecuData != null) {
            LOG.debug("Ready condition met.  Received non-null data from all data providers.");
            ready = true;
            
            notificationStrategy.sendNotification(
                    NotificationType.READY_PROGRESS, new Object[] {
                            gpsData, accelData, ecuData});
            
            notificationStrategy.sendNotification(
                    NotificationType.READY);
        } else if (!ready) {
            notificationStrategy.sendNotification(
                    NotificationType.READY_PROGRESS, new Object[] {
                            gpsData, accelData, ecuData });
        }
        
        if (ready && logging) {
            
            if (loggingStartTriggerFired) {
            	
                LogEntry logEntry = new LogEntry();
                logEntry.accelData = accelData;
                logEntry.locationData = gpsData;
                logEntry.ecuData = ecuData;
                
                if (!dataQueue.offer(logEntry)) {
                    LOG.error("No space on the data queue.  Discarding current data.");
                    notificationStrategy.sendNotification(NotificationType.LOGGING_FAILED);
                }
            }
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Handled update in {}.  Current status is: running: {}, ready: {}, "
                            + " logging: {}, logging start trigger fired: {}",
                    new Object[] {System.currentTimeMillis() - startTime, 
                            		isRunning(), isReady(), isLogging(), 
                                    isLoggingStartTriggerFired()});
        }
    }
    
    @Override
    protected final void handleUpdate(TimingData timingData) {
        if (ready && logging) {
            if (!loggingStartTriggerFired) {
                // TODO This is split marker based.  Handle start based on configuration options for immediately, movement, etc.
                LOG.debug("Log trigger start condition met.");
                loggingStartTriggerFired = true;
                notificationStrategy.sendNotification(NotificationType.LOGGING_START_TRIGGER_FIRED);
            }
            
            if (loggingStartTriggerFired) {
                if (!dataQueue.offer(timingData)) {
                    LOG.error("No space on the data queue.  Discarding current data.");
                    notificationStrategy.sendNotification(NotificationType.LOGGING_FAILED);
                }
            }
        }
    }
    
    /**
     * Creates a new session in the data store and returns the ID of the session.
     */
    protected abstract int createSession();
    
    /**
     * "Opens" an existing session in the data store for the addition of more data.
     */
    protected abstract void openSession(int sessionId);
    
    /**
     * Store a log entry in the data store.
     *
     * @param sessionId the ID of the session that the log entry belongs to
     * @param logEntry the data to store
     */
    protected abstract void storeLogEntry(int sessionId, LogEntry logEntry);

    /**
     * Store timing data in the data store.
     *
     * @param sessionId the ID of the session that the data belongs to
     * @param timingData the data to store
     */
    protected abstract void storeTimingEntry(int sessionId, TimingData timingData);
    
    protected final synchronized String formatAsSqlDate(Date date) {
        return sqlDateFormat.format(date);
    }
    
    protected static final class LogEntry {
        public AccelData accelData;
        public LocationData locationData;
        public EcuData ecuData;
    }

    private class LogThread extends GracefulShutdownThread {
        
        @Override
        public void run() {
            
            try {

                List<Object> logObjects = new ArrayList<Object>(20);
                long startTime;
                int numRead;
    
                while (run) {
    
                    numRead = dataQueue.drainTo(logObjects, 20);
                    startTime = System.currentTimeMillis();
                    
                    
                    for (int i = 0; i < numRead; i++) {
                        
                        Object o = logObjects.get(i);
                        
                        if (o instanceof LogEntry) {
                            LogEntry logEntry = (LogEntry) o;
                            
                            storeLogEntry(currentSessionId, logEntry);
                            
                        } else if (o instanceof TimingData) {
                            TimingData timingData = (TimingData) o;
                            
                            storeTimingEntry(currentSessionId, timingData);
                        } else {
                            LOG.error("Fatal error while logging data.  Unknown data type {}.", o.getClass());
                            notificationStrategy.sendNotification(NotificationType.LOGGING_FAILED);
                        }
                    }
                    
                    logObjects.clear();
    
                    if (numRead != 0 && LOG.isDebugEnabled()) {
                        long time = System.currentTimeMillis() - startTime;
                        LOG.debug(
                                "Wrote {} entries in {}ms.  Avg. time per entry is {}ms.",
                                new Object[] { numRead, time, time / numRead });
                    }
                }
            } catch (Exception e) {
                LOG.error("Fatal error while logging data.", e);
                notificationStrategy.sendNotification(NotificationType.LOGGING_FAILED);
            }
        }
    }
}
