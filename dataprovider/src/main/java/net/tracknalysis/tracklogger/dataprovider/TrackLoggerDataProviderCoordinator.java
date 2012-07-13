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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.concurrent.GracefulShutdownThread;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.notification.NotificationType;
import net.tracknalysis.tracklogger.dataprovider.AbstractDataProviderCoordinator;
import net.tracknalysis.tracklogger.model.AccelData;
import net.tracknalysis.tracklogger.model.EcuData;
import net.tracknalysis.tracklogger.model.LocationData;
import net.tracknalysis.tracklogger.model.LogEntry;
import net.tracknalysis.tracklogger.model.TimingData;
import net.tracknalysis.tracklogger.model.TimingEntry;

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
    
    private final List<WeakReference<NotificationStrategy<DataProviderCoordinatorNotificationType>>> notificationStrategies =
            new LinkedList<WeakReference<NotificationStrategy<DataProviderCoordinatorNotificationType>>>();
    
    private volatile boolean ready;
    private volatile int sessionId;
    private volatile boolean logging;
    private volatile int currentSessionId;
    private volatile boolean loggingStartTriggerFired;
    private volatile DataProviderCoordinatorNotificationType lastNotificationType = 
            DataProviderCoordinatorNotificationType.STOPPED;
    private volatile Object lastNotificationBody;
    private LogThread logThread;
    
    private final BlockingQueue<Object> dataQueue = new ArrayBlockingQueue<Object>(100);
    private volatile int logEntriesOffered;
    private volatile int timingEntriesOffered;
    
    @Override
    public final synchronized void start() {
        if (logThread == null) {
            sendNotification(DataProviderCoordinatorNotificationType.STARTING);
            logThread = new LogThread();
            
            preStart();
            try {
                dataQueue.clear();
                logEntriesOffered = 0;
                timingEntriesOffered = 0;
                
                ready = false;
                logThread.start();
                loggingStartTriggerFired = false;
                logging = true;
                
                super.start();
                postStart();
                sendNotification(DataProviderCoordinator.DataProviderCoordinatorNotificationType.STARTED);
            } catch (RuntimeException e) {
                LOG.error("Error during startup.", e);
                sendNotification(DataProviderCoordinator.DataProviderCoordinatorNotificationType.START_FAILED, e);
                
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
            sendNotification(DataProviderCoordinatorNotificationType.STOPPING);
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
                sendNotification(DataProviderCoordinatorNotificationType.STOPPED);
            } catch (RuntimeException e) {
                logThread = null;
                LOG.error("Error during shutdown.", e);
                sendNotification(DataProviderCoordinator.DataProviderCoordinatorNotificationType.STOP_FAILED, e);
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
    
    @Override
    public final void register(
            NotificationStrategy<DataProviderCoordinatorNotificationType> notificationStrategy) {
        
        synchronized (notificationStrategies) {
            scrubStrategies(notificationStrategies);
            
            int index = findInWeakReferenceList(notificationStrategies, notificationStrategy);
            
            if (index == -1) {
                notificationStrategies.add(
                        new WeakReference<NotificationStrategy<DataProviderCoordinatorNotificationType>>(
                                notificationStrategy));
                
                if (lastNotificationBody != null) {
                    notificationStrategy.sendNotification(lastNotificationType, lastNotificationBody);
                } else {
                    notificationStrategy.sendNotification(lastNotificationType);
                }
            }
        }
    }
    
    @Override
    public final void unRegister(
            NotificationStrategy<DataProviderCoordinatorNotificationType> notificationStrategy) {
        
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
    
    protected final void handleReady() {
        if (!ready) {
        
            LocationData locationData = getLocationDataProvider().getCurrentData();
            AccelData accelData = getAccelDataProvider().getCurrentData();
            EcuData ecuData = isEcuDataProviderEnabled() ? getEcuDataProvider().getCurrentData() : null;
            
            if (!ready && locationData != null && accelData != null
                    && (!isEcuDataProviderEnabled() || ecuData != null)) {
                LOG.debug("Ready condition met.  Received non-null data from all data providers.");
                
                // Create/open session only once we are ready so that we don't get a session recorded
                // with no data in it.
                if (sessionId == 0) {
                    currentSessionId = createSession();
                } else {
                    currentSessionId = sessionId;
                    
                    openSession(sessionId);
                }
                
                ready = true;
                
                sendNotification(
                        DataProviderCoordinatorNotificationType.READY_PROGRESS,
                        new Object[] {locationData, accelData, ecuData});
                
                // Start only once everyone is ready so we don't get timing events before
                // the other data providers are ready.
                getTimingDataProvider().start();
                
                sendNotification(
                        DataProviderCoordinatorNotificationType.READY);
            } else if (!ready) {
                sendNotification(
                        DataProviderCoordinatorNotificationType.READY_PROGRESS,
                        new Object[] {locationData, accelData, ecuData });
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
            LogEntry logEntry = new LogEntry(locationData.getTime(),
                    currentSessionId, accelData, locationData, ecuData);
            
            if (!dataQueue.offer(logEntry)) {
                LOG.error("No space on the data queue.  Discarding current data.");
                sendNotification(DataProviderCoordinatorNotificationType.LOGGING_FAILED);
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
                sendNotification(DataProviderCoordinatorNotificationType.TIMING_START_TRIGGER_FIRED);
            }
            
            if (loggingStartTriggerFired) {
                TimingEntry timingEntry = new TimingEntry(timingData.getTime(), currentSessionId, timingData);
                
                sendNotification(
                                DataProviderCoordinatorNotificationType.TIMING_DATA_UPDATE,
                                timingData);
                
                if (!dataQueue.offer(timingEntry)) {
                    LOG.error("No space on the data queue.  Discarding current data.");
                    sendNotification(DataProviderCoordinatorNotificationType.LOGGING_FAILED);
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
     * @param logEntry the data to store
     */
    protected abstract void storeLogEntry(LogEntry logEntry);

    /**
     * Store timing data in the data store.
     *
     * @param timingEntry the data to store
     */
    protected abstract void storeTimingEntry(TimingEntry timingEntry);
    
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
    
    /**
     * Searches a list of references for a reference that points to {@code notificationStrategy}.
     *
     * @param strategies the list to search
     * @param notificationStrategy the strategy to look for in the list
     *
     * @return -1 if no matching entry was found or the index of the matching reference in the list
     */
    private int findInWeakReferenceList(
            List<? extends WeakReference<? extends NotificationStrategy<? extends NotificationType>>> strategies,
            NotificationStrategy<?> notificationStrategy) {
        
        int index = 0;
        
        for (WeakReference<? extends NotificationStrategy<?>> existingStrategy : strategies) {
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
            List<? extends WeakReference<? extends NotificationStrategy<? extends NotificationType>>> strategies) {
        
        Iterator<? extends WeakReference<? extends NotificationStrategy<? extends NotificationType>>> iterator =
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
        for (WeakReference<NotificationStrategy<DataProviderCoordinatorNotificationType>> ref 
                : notificationStrategies) {
            
            NotificationStrategy<DataProviderCoordinatorNotificationType> strategy = ref.get();
            
            if (strategy != null) {
                if (body != null) {
                    strategy.sendNotification(notificationType, body);
                } else {
                    strategy.sendNotification(notificationType);
                }
            }
        }
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
                            
                            storeLogEntry(logEntry);
                            logEntriesWritten++;
                        } else if (o instanceof TimingEntry) {
                            TimingEntry timingEntry = (TimingEntry) o;
                            
                            storeTimingEntry(timingEntry);
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
                    sendNotification(DataProviderCoordinatorNotificationType.LOGGING_FAILED);
                } else {
                    LOG.info(logMessage, e);
                }
            }
        }
    }
}
