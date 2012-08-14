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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.concurrent.GracefulShutdownThread;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger.dataprovider.AbstractDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.LocationData;

/**
 * Implements data collection and coordination logic, delegating to sub-classes only for persistence and
 * creation of the {@link DataProvider}s being coordinated.
 *
 * @author David Valeri
 */
public abstract class TrackLoggerDataProviderCoordinator extends
        AbstractDataProviderCoordinator {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(TrackLoggerDataProviderCoordinator.class);
    
    private static final AtomicInteger logThreadCounter = new AtomicInteger();
    
    private final NotificationStrategy<NotificationType> notificationStrategy;
    private volatile boolean ready;
    private volatile int sessionId;
    private volatile boolean logging;
    private volatile int currentSessionId;
    private volatile boolean loggingStartTriggerFired;
    private LogThread logThread;
    
    private final BlockingQueue<Object> dataQueue = new ArrayBlockingQueue<Object>(100);
    private volatile int logEntriesOffered;
    private volatile int timingEntriesOffered;
    
    public TrackLoggerDataProviderCoordinator(NotificationStrategy<NotificationType> notificationStrategy) {
        this.notificationStrategy = notificationStrategy;
    }
    
    @Override
    public final synchronized void start() {
        if (logThread == null) {
            notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.STARTING);
            logThread = new LogThread();
            
            preStart();
            try {
                
                if (sessionId == 0) {
                    currentSessionId = createSession();
                } else {
                    currentSessionId = sessionId;
                    
                    openSession(sessionId);
                }
                
                dataQueue.clear();
                logEntriesOffered = 0;
                timingEntriesOffered = 0;
                
                ready = false;
                logThread.start();
                loggingStartTriggerFired = false;
                logging = true;
                
                super.start();
                postStart();
                notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.STARTED);
            } catch (RuntimeException e) {
                LOG.error("Error during startup.", e);
                notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.START_FAILED, e);
                
                try {
                    stop();
                } catch (Exception e2) {
                    LOG.warn("Error trying to cleanup after failed start.", e2);
                    // Ignore
                }
            }
        }
    }
    
    @Override
    public final synchronized void stop() {
        if (logThread != null) {
            notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.STOPPING);
            try {
                // Call super first so we stop receiving updates immediately.  Otherwise
                // if we shutdown logging first, the state gets out of whack because we
                // emit messages that logging is ready again, even though we are shutting down.
                super.stop();
                
                logging = false;
                loggingStartTriggerFired = false;
                logThread.cancel();
                logThread = null;
                ready = false;
                
                LOG.info(
                        "Offered {} log entries and {} timing entries in total.",
                        logEntriesOffered, timingEntriesOffered);
                
                postStop();
                notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.STOPPED);
            } catch (RuntimeException e) {
                logThread = null;
                LOG.error("Error during shutdown.", e);
                notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.STOP_FAILED, e);
            }
        }
    }
    
    @Override
    public final boolean isLoggingStartTriggerFired() {
        return loggingStartTriggerFired;
    }
    
    protected final boolean isLogging() {
        return logging;
    }
    
    public final synchronized void setSessionId(int sessionId) {
        if (isRunning()) {
            throw new IllegalStateException();
        } else {
            this.sessionId = sessionId;
        }
    }
    
    @Override
    public final boolean isReady() {
        return ready && isRunning();
    }
    
    public int getCurrentSessionId() {
        return currentSessionId;
    }

    protected final void handleReady() {
        if (!ready) {
        
            LocationData locationData = getLocationDataProvider().getCurrentData();
            AccelData accelData = getAccelDataProvider().getCurrentData();
            EcuData ecuData = isEcuDataProviderEnabled() ? getEcuDataProvider().getCurrentData() : null;
            
            if (!ready && locationData != null && accelData != null
                    && (!isEcuDataProviderEnabled() || ecuData != null)) {
                LOG.debug("Ready condition met.  Received non-null data from all data providers.");
                ready = true;
                
                notificationStrategy.sendNotification(
                        DataProviderCoordinator.NotificationType.READY_PROGRESS, new Object[] {
                                locationData, accelData, ecuData});
                
                // Start only once everyone is ready so we don't get timing events before
                // the other data providers are ready.
                getTimingDataProvider().start();
                
                notificationStrategy.sendNotification(
                        DataProviderCoordinator.NotificationType.READY);
            } else if (!ready) {
                notificationStrategy.sendNotification(
                        DataProviderCoordinator.NotificationType.READY_PROGRESS, new Object[] {
                                locationData, accelData, ecuData });
            }
        }
    }
    
    @Override
    protected final synchronized void handleUpdate(AccelData data) {
        handleReady();
    }
    
    @Override
    protected final synchronized void handleUpdate(EcuData data) {
        handleReady();
    }
    
    @Override
    protected final synchronized void handleUpdate(LocationData locationData) {
        
        AccelData accelData = getAccelDataProvider().getCurrentData();
        EcuData ecuData = getEcuDataProvider() == null ? null : getEcuDataProvider().getCurrentData();
    	
    	long startTime = System.currentTimeMillis();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Handling updated.  Current status is: running: {}, ready: {}, "
                            + " logging: {}, logging start trigger fired: {}",
                    new Object[] {isRunning(), isReady(), isLogging(), 
                                    isLoggingStartTriggerFired()});
        }
        
        handleReady();
        
        if (ready && logging) {
            
            // Note: Not waiting for start trigger here as timing
            // can be asynchronous from the other data logging events and
            // we need to already be logging those events when the first timing
            // event triggers.
            	
            LogEntry logEntry = new LogEntry();
            logEntry.accelData = accelData;
            logEntry.locationData = locationData;
            logEntry.ecuData = ecuData;
            
            if (!dataQueue.offer(logEntry)) {
                LOG.error("No space on the data queue.  Discarding current data.");
                notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.LOGGING_FAILED);
            } else {
                logEntriesOffered++;
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
    protected final synchronized void handleUpdate(TimingData timingData) {
        handleReady();
        
        if (ready && logging) {
            if (!loggingStartTriggerFired) {
                // TODO This is split marker based.  Handle start based on configuration options for immediately, movement, etc.
                LOG.debug("Log trigger start condition met.");
                loggingStartTriggerFired = true;
                notificationStrategy
                        .sendNotification(DataProviderCoordinator.NotificationType.TIMING_START_TRIGGER_FIRED);
            }
            
            if (loggingStartTriggerFired) {
                notificationStrategy
                        .sendNotification(
                                DataProviderCoordinator.NotificationType.TIMING_DATA_UPDATE,
                                timingData);
                
                if (!dataQueue.offer(timingData)) {
                    LOG.error("No space on the data queue.  Discarding current data.");
                    notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.LOGGING_FAILED);
                } else {
                    timingEntriesOffered++;
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
    
    /**
     * Hook to allow sub-classes to do extra initialization before the main startup.
     */
    protected void preStart() {
    }
    
    /**
     * Hook to allow sub-classes to do extra initialization after the main startup.
     */
    protected void postStart() {
    }
    
    /**
     * Hook to allow sub-classes to do extra cleanup after the main shutdown.
     */
    protected void postStop() {
    }
    
    protected static final class LogEntry {
        public AccelData accelData;
        public LocationData locationData;
        public EcuData ecuData;
    }

    private class LogThread extends GracefulShutdownThread {
        
        private int logEntriesWritten = 0;
        private int timingEntriesWritten = 0;
        
        public LogThread() {
            setName("TrackLoggerDataProviderCoordinator-LogThread-" + logThreadCounter.getAndIncrement());
        }
        
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
                            logEntriesWritten++;
                        } else if (o instanceof TimingData) {
                            TimingData timingData = (TimingData) o;
                            
                            storeTimingEntry(currentSessionId, timingData);
                            timingEntriesWritten++;
                        } else {
                            LOG.warn("Error while logging data.  Unknown data type {}.", o.getClass());
                        }
                    }
                    
                    logObjects.clear();
    
                    if (numRead != 0 && LOG.isDebugEnabled()) {
                        long time = System.currentTimeMillis() - startTime;
                        LOG.debug(
                                "Wrote {} entries in {}ms.  Avg. time per entry in this cycle is {}ms.  "
                                        + "Wrote {} timing entries and {} log entries in total.",
                                new Object[] {numRead, time, time / numRead,
                                        timingEntriesWritten, logEntriesWritten});
                    }
                }
                
                LOG.info("Wrote {} timing entries and {} log entries in total.",
                        timingEntriesWritten, logEntriesWritten);
                
            } catch (Exception e) {
                String logMessage = "Exception while logging data.  Data queue depth is '" + dataQueue.size()
                        + "' running is " + run + ".";
                
                if (run) {
                    LOG.error(logMessage, e);
                    notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.LOGGING_FAILED);
                } else {
                    LOG.info(logMessage, e);
                }
            }
        }
    }
}
