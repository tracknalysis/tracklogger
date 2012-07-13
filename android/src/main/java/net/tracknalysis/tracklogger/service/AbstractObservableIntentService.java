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
package net.tracknalysis.tracklogger.service;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.notification.NotificationType;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

/**
 * Simple service for handling the queuing and processing of requests. Displays
 * notifications of request progress and completion status for each intent
 * delivered in FIFO order.
 * <p/>
 * Based on {@link IntentService} but provides more control and situational
 * awareness to the handler. This implementation also allows rebinding to the
 * state of a previous request and also to the state of the entire service for
 * enhanced integration with the {@link Activity} lifecycle.
 * 
 * @param <T>
 *            the type of the request object.
 * @param <U>
 *            the type of the progress structure returned to notification
 *            strategies for a request. See {@link RequestLifecycleNotification}.
 * 
 * @author David Valeri
 */
public abstract class AbstractObservableIntentService<T, U> extends Service implements ObservableIntentService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractObservableIntentService.class);
    
    public static final String EXTRA_EXPORT_FORMAT = "exportFormat";
    public static final String EXTRA_EXPORT_START_LAP = "startLap";
    public static final String EXTRA_EXPORT_STOP_LAP = "stopLap";
    
    public static final String EXPORT_FORMAT_CSV_1 = "csv1";
    public static final String EXPORT_FORMAT_SQL_1 = "sql1";
    
    private final SparseArray<List<WeakReference<NotificationStrategy<RequestNotificationType>>>> requestNotificationStrategyMap = 
            new SparseArray<List<WeakReference<NotificationStrategy<RequestNotificationType>>>>();
    private final List<WeakReference<NotificationStrategy<ObservableIntentServiceNotificationType>>> serviceNotificationStrategyMap =
            new LinkedList<WeakReference<NotificationStrategy<ObservableIntentServiceNotificationType>>>();
    
    private final SparseArray<RequestState<T>> requestStateMap = new SparseArray<RequestState<T>>();
    private final IBinder binder = new LocalBinder(this);
    private final String name;
    
    private volatile Looper serviceLooper;
    private volatile ServiceHandler<T, U> serviceHandler;
    
    protected AbstractObservableIntentService(String name) {
        this.name = name;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + name + "]");
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler<T, U>(serviceLooper, this);
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        
        LOG.debug("Starting servicing of intent [{}] with startId [{}].");
        
        boolean willFail = onStartValidate(intent, startId);
        int requestId = getRequestId(intent, startId);
        
        if (requestId == -1) {
            willFail = true;
        }
        
        T request = initializeRequest(intent, startId);
        RequestState<T> requestState = new RequestState<T>(
                requestId, intent, startId, request, createHandler(),
                willFail ? RequestLifecycleStatus.FAILED : RequestLifecycleStatus.QUEUED);
        
        Message msg = serviceHandler.obtainMessage();
        msg.obj = requestState;
        synchronized (requestStateMap) {
            // TODO: This does not deal well with duplicate requests for the same
            // request ID.  It is therefore important that activities not enqueue
            // more than one request at a time for any given request ID until this is fixed.
            if (!willFail) {
                requestStateMap.put(requestId, requestState);
                sendDownstreamUpdates(requestId, RequestLifecycleStatus.QUEUED, null);
            }
            serviceHandler.sendMessage(msg);
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Created and enqueued request state [{}] from intent [{}] with startId [{}].",
                    new Object[] {requestState, intent, startId});
        } else if (LOG.isInfoEnabled()) {
            LOG.info("Enqueued request [{}].", requestState.getRequest());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // This will run until empty before terminating the loop.
        serviceLooper.quit();
        
        synchronized (requestStateMap) {
            requestStateMap.clear();
            // TODO probably should not dump this because the looper still runs and this is important for the handler to work right
        }
        
        synchronized (requestNotificationStrategyMap) {
            requestNotificationStrategyMap.clear();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void register(int sessionId,
            NotificationStrategy<RequestNotificationType> notificationStrategy) {
        
        synchronized (requestNotificationStrategyMap) {
            List<WeakReference<NotificationStrategy<RequestNotificationType>>> strategies = 
                    requestNotificationStrategyMap.get(sessionId);
            
            if (strategies == null) {
                strategies = new LinkedList<WeakReference<NotificationStrategy<RequestNotificationType>>>();
                requestNotificationStrategyMap.put(sessionId, strategies);
            } else {
                // Only scrub if it was an existing list.
                scrubStrategies(strategies);
            }
            
            int index = findInWeakReferenceList(strategies, notificationStrategy);
            
            if (index == -1) {
                strategies.add(
                        new WeakReference<NotificationStrategy<RequestNotificationType>>(notificationStrategy));
                
                // Always synch to the current state of the queued request for the session in question when registering.
                // This is synchronized so that we always get the latest state of the session and any subsequent updates
                // will not put the listener out of synch with the actual state of the export and we avoid race conditions
                // that result from the activity trying to setup a listener and poll the service to find the current state.
                synchronized (requestStateMap) {
                    RequestState<T> requestState = requestStateMap.get(sessionId); 
                    if (requestState == null) {
                        sendDownstreamUpdates(sessionId, RequestLifecycleStatus.NOT_QUEUED, null);
                    } else {
                        synchronized (requestState) {
                            sendDownstreamUpdates(sessionId, requestState.getStatus(), null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void unRegister(
            int sessionId,
            NotificationStrategy<RequestNotificationType> notificationStrategy) {
        
        synchronized (requestNotificationStrategyMap) {
            List<WeakReference<NotificationStrategy<RequestNotificationType>>> strategies = 
                    requestNotificationStrategyMap.get(sessionId);
            
            if (strategies != null) {
                scrubStrategies(strategies);
                
                int index = findInWeakReferenceList(strategies, notificationStrategy);
                
                if (index > -1) {
                    strategies.remove(index);
                }
            }
        }
    }
    
    @SuppressLint("UseSparseArrays")
    @Override
    public void register(
            NotificationStrategy<ObservableIntentServiceNotificationType> notificationStrategy) {
        
        synchronized (serviceNotificationStrategyMap) {
            scrubStrategies(serviceNotificationStrategyMap);
            
            int index = findInWeakReferenceList(serviceNotificationStrategyMap, notificationStrategy);
            
            if (index == -1) {
                serviceNotificationStrategyMap.add(
                        new WeakReference<NotificationStrategy<ObservableIntentServiceNotificationType>>(
                                notificationStrategy));
                
                synchronized (requestStateMap) {
                    Map<Integer, RequestLifecycleStatus> map = 
                            new HashMap<Integer, ObservableIntentService.RequestLifecycleStatus>();
                    
                    for (int i = 0; i < requestStateMap.size(); i++) {
                        map.put(requestStateMap.keyAt(i), requestStateMap.valueAt(i).getStatus());
                    }
                    
                    ObservableIntentServiceRequestStatusNotification oisrsn =
                            new ObservableIntentServiceRequestStatusNotification(map);
                    
                    notificationStrategy.sendNotification(
                            ObservableIntentServiceNotificationType.REQUEST_STATUS_NOTIFICATION,
                            oisrsn);
                }
            }
        }
    }
    
    @Override
    public void unRegister(
            NotificationStrategy<ObservableIntentServiceNotificationType> notificationStrategy) {
        
        synchronized (serviceNotificationStrategyMap) {
            scrubStrategies(serviceNotificationStrategyMap);
                
            int index = findInWeakReferenceList(serviceNotificationStrategyMap, notificationStrategy);
                
            if (index > -1) {
                serviceNotificationStrategyMap.remove(index);
            }
        }
    }
    
    /**
     * Perform quick validation of the incoming intent and determine if the request will fail.
     *
     * @param intent the intent being started
     * @param startId the start ID of the intent
     *
     * @return true if initial input validation indicates terminal failure of the intent processing
     */
    protected abstract boolean onStartValidate(Intent intent, int startId);
    
    /**
     * Extracts the unique request ID from the intent
     *
     * @param intent the intent being started
     * @param startId the start ID of the intent
     */
    protected abstract int getRequestId(Intent intent, int startId);
    
    /**
     * Parses the intent into a strongly typed object representing the incoming request to process
     *
     * @param intent the intent being started
     * @param startId the start ID of the intent
     */
    protected abstract T initializeRequest(Intent intent, int startId);
    
    /**
     * Creates the handler for use in responding to the lifecycle events of the request.
     */
    protected abstract ObservableIntentServiceHandler<T, U> createHandler();
    
    /**
     * Performs final initialization of the handler and performs the work associated with the request.
     */
    protected abstract void doWork(RequestState<T> requestState);
    
    /**
     * Handle a terminal failure in the processing of the request.  Typically, the failure is managed
     * by the handler; however, in the event of an unexpected error, this method must ensure that the
     * handler receives a message indicating that the request failed.
     * 
     * @param requestState the state of the request being processed
     * @param e the exception that caused the failure
     */
    protected abstract void handleDoWorkFailure(RequestState<T> requestState, Exception e);
    
    private void onHandleRequest(RequestState<T> requestState) {
        
        LOG.debug("Handling request with request state [{}].", requestState);
        try {
            if (requestState.status == RequestLifecycleStatus.FAILED) {
                LOG.debug(
                        "Request with request state [{}] is already failed, skipping further processing.",
                        requestState);
                return;
            }
            
            doWork(requestState);

            LOG.info("Finished for request with request state [{}].", requestState);
        } catch (Exception e) {
            LOG.error("Error for request with request state [{}].", requestState);
            handleDoWorkFailure(requestState, e);
        } finally {
            // Clean up any reference we have to the current request being serviced.
            synchronized (requestStateMap) {
                requestStateMap.remove(requestState.getRequestId());
            }
        }
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
     * Sends a notification, {@link SessionExportLifecycleNotification}, to any downstream listeners.
     *
     * @param requestId the ID of the session that the update is for
     * @param status the status of the request with ID {@code requestId}
     * @param progress the optional progress associated with the notification.  Expected when {@code status}
     * is {@link RequestLifecycleStatus#RUNNING}.
     */
    private void sendDownstreamUpdates(int requestId, RequestLifecycleStatus status, U progress) {
        synchronized (requestNotificationStrategyMap) {
            List<WeakReference<NotificationStrategy<RequestNotificationType>>> strategies = 
                    requestNotificationStrategyMap.get(requestId);
            
            if (strategies != null && strategies.size() != 0) {
                for (WeakReference<NotificationStrategy<RequestNotificationType>> strategyRef
                        : strategies) {
                    
                    NotificationStrategy<RequestNotificationType> strategy = strategyRef.get();
                    
                    if (strategy != null) {
                        strategy.sendNotification(RequestNotificationType.REQUEST_LIFECYCLE_NOTIFICATION,
                                new RequestLifecycleNotification<U>(status, progress));
                    }
                }
            }
        }
    }
    
    public static final class LocalBinder extends Binder {
        
        private ObservableIntentService service;
        
        private LocalBinder(ObservableIntentService service) {
            this.service = service;
        }
        
        public ObservableIntentService getService() {
            return service;
        }
    }
    
    protected static abstract class ObservableIntentServiceHandler<T, U> extends Handler {
        
        private final Context context;
        private WeakReference<AbstractObservableIntentService<T, U>> serviceRef;
        private Notification notification;
        private RequestState<T> requestState;
        private int notificationId;
        private boolean handleMessages = false;
        
        public ObservableIntentServiceHandler(AbstractObservableIntentService<T, U> service) {
            this.context = service.getApplicationContext();
            this.serviceRef = new WeakReference<AbstractObservableIntentService<T, U>>(service);
        }
        
        protected synchronized void init(RequestState<T> requestState) {
            this.requestState = requestState;
            handleMessages = true;
        }
        
        protected synchronized void destroy() {
            handleMessages = false;
            serviceRef.clear();
            notification = null;
            requestState = null;
        }
        
        @Override
        public void handleMessage(Message msg) {
            if (handleMessages) {
                LOG.debug("Handling message: {}.", msg);
                
                try {
                    doHandleMessage(msg);
                } catch (Exception e) {
                    LOG.error("Error handling message " + msg + ".", e);
                }
            }
        }
        
        /**
         * Handle mapping of internal notifications to lifecycle events for the request.
         *
         * @see #onStarting()
         * @see #onStarted()
         * @see #onRunning(Object)
         * @see #onFinished()
         * @see #onFailed()
         */
        protected abstract void doHandleMessage(Message msg);
        
        /**
         * Returns the ticker text displayed when the notification for a running request is presented.
         */
        protected abstract CharSequence getRunningNotificationTickerText(RequestState<T> requestState, Context context);
        
        /**
         * Returns the text displayed in the body of the notification for a
         * running request that has just started. This text should be updated by
         * {@link #updateRunningNotification(RequestState, Context, Object, Notification)} when progress updates
         * are received.  This method is called before any progress information is available.
         */
        protected abstract CharSequence getRunningNotificationContentText(
                RequestState<T> requestState, Context context);
        
        /**
         * Returns the {@link Intent} triggered when the user activates the running notification.
         */
        protected abstract Intent getRunningNotificationIntent(RequestState<T> requestState, Context context);
        
        /**
         * Update {@code notification} to reflect the provide {@code progress}.  The updated notification will
         * be refreshed if the method returns {@code true}.
         *
         * @param progress the progress information for the request, may be null
         * @param notification the notification to be updated
         * @param context the context to use for the update
         *
         * @return true if the notification was updated or false if no update was made
         */
        protected abstract boolean updateRunningNotification(
                RequestState<T> requestState, Context context, U progress,
                Notification notification);
        
        /**
         * Returns the ticker text displayed when the notification for a finished request is presented.
         */
        protected abstract CharSequence getFinishedNotificationTickerText(
                RequestState<T> requestState, Context context);
        
        /**
         * Returns the text displayed in the body of the notification for a finished request.
         */
        protected abstract CharSequence getFinishedNotificationContentText(
                RequestState<T> requestState, Context context);
        
        /**
         * Returns the {@link Intent} triggered when the user activates the finished notification.
         */
        protected abstract Intent getFinishedNotificationIntent(RequestState<T> requestState, Context context);
        
        /**
         * Returns the ticker text displayed when the notification for a failed request is presented.
         */
        protected abstract CharSequence getFailedNotificationTickerText(RequestState<T> requestState, Context context);
        
        /**
         * Returns the text displayed in the body of the notification for a failed request.
         */
        protected abstract CharSequence getFailedNotificationContentText(RequestState<T> requestState, Context context);

        /**
         * Returns the {@link Intent} triggered when the user activates the failed notification.
         */
        protected abstract Intent getFailedNotificationIntent(RequestState<T> requestState, Context context);

        /**
         * Updates the state of the handler and triggers downstream updates.  Also displays
         * the notification for the request.
         */
        protected final void onStarting() {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() < RequestLifecycleStatus.STARTING.getWeight()) {
                    requestState.setStatus(RequestLifecycleStatus.STARTING);
                    showRunningNotification();
                    
                    AbstractObservableIntentService<T, U> service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequestId(),
                                requestState.getStatus(), null);
                    }
                }
            }
        }
        
        /**
         * Updates the state of the handler and triggers downstream updates.
         */
        protected final void onStarted() {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() < RequestLifecycleStatus.STARTED.getWeight()) {
                    requestState.setStatus(RequestLifecycleStatus.STARTED);
                    
                    AbstractObservableIntentService<T, U> service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequestId(),
                                requestState.getStatus(), null);
                    }
                }
            }
        }
        
        /**
         * Updates the request progress view in the notification based on the provided progress data and
         * sends downstream updates.
         */
        protected final void onRunning(U progress) {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() <= RequestLifecycleStatus.RUNNING.getWeight()) {
                    requestState.setStatus(RequestLifecycleStatus.RUNNING);
    
                   if (updateRunningNotification(requestState, context, progress, notification)) {
                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        
                        notificationManager.notify(notificationId, notification);
                    }
                    
                    AbstractObservableIntentService<T, U> service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequestId(),
                                requestState.getStatus(), progress);
                    }
                }
            }
        }
        
        /**
         * Clears the in progress notification and displays a finished notification.  Finally sending
         * downstream updates and cleaning up this handler.
         */
        protected final void onFinished() {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() < RequestLifecycleStatus.FINISHED.getWeight()) {
                    requestState.setStatus(RequestLifecycleStatus.FINISHED);
                    try {
                        cancelNotification();
                        showFinishedNotification();
                        
                        AbstractObservableIntentService<T, U> service = serviceRef.get();
                        if (service != null) {
                            service.sendDownstreamUpdates(
                                    requestState.getRequestId(),
                                    requestState.getStatus(), null);
                        }
                    } finally {
                        destroy();
                    }
                }
            }
        }
        
        /**
         * Clears the in progress notification and displays a failed notification.  Finally sending
         * downstream updates and cleaning up this handler.
         */
        protected final void onFailed() {
            if (requestState.status.getWeight() < RequestLifecycleStatus.FINISHED.getWeight()) {
                requestState.status = RequestLifecycleStatus.FAILED;
                
                try {
                    cancelNotification();
                    showFailedNotification();
                
                    AbstractObservableIntentService<T, U> service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequestId(),
                                requestState.status, null);
                    }
                } finally {
                    destroy();
                }
            }
        }
        
        /**
         * Creates a new notification and displays it using the notification service to represent the running request.
         */
        @SuppressWarnings("deprecation") // Ignore as can't resolve until API 11
        private void showRunningNotification() {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);

            notification = new Notification(
                    R.drawable.icon,
                    getRunningNotificationTickerText(requestState, context),
                    System.currentTimeMillis());
            
            notification.flags = notification.flags
                    | Notification.FLAG_ONGOING_EVENT
                    | Notification.FLAG_NO_CLEAR;

            Intent notificationIntent = getRunningNotificationIntent(requestState, context);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, 0);

            notification.setLatestEventInfo(
                    context,
                    context.getText(R.string.app_name),
                    getRunningNotificationContentText(requestState, context),
                    pendingIntent);

            notificationId = TrackLogger.getUniqueNotificationId();
            notificationManager.notify(notificationId, notification);            
        }
        
        /**
         * Cancels the active notification managed by this handler.
         */
        private void cancelNotification() {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            
            notification = null;
            notificationId = -1;
        }
        
        /**
         * Shows a new notification indicating that the request finished.
         */
        @SuppressWarnings("deprecation") // Ignore as can't resolve until API 11
        private void showFinishedNotification() {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);
            
            
            notification = new Notification(
                    R.drawable.icon,
                    getFinishedNotificationTickerText(requestState, context),
                    System.currentTimeMillis());
            notification.flags = notification.flags
                    | Notification.FLAG_AUTO_CANCEL;

            Intent notificationIntent = getFinishedNotificationIntent(requestState, context);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            notification.setLatestEventInfo(
                    context,
                    context.getText(R.string.app_name),
                    getFinishedNotificationContentText(requestState, context),
                    pendingIntent);

            notificationId = TrackLogger.getUniqueNotificationId();
            notificationManager.notify(notificationId, notification);
        }
        
        /**
         * Shows a new notification indicating that the export failed.
         *
         * @param sessionId the ID of the session that was exported
         */
        @SuppressWarnings("deprecation") // Ignore as can't resolve until API 11
        private void showFailedNotification() {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);
            
            notification = new Notification(R.drawable.icon,
                    getFailedNotificationTickerText(requestState, context),
                    System.currentTimeMillis());
            notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
            
            Intent notificationIntent = getFailedNotificationIntent(requestState, context);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent, 0);
            
            notification.setLatestEventInfo(context,
                    context.getText(R.string.app_name),
                    getFailedNotificationContentText(requestState, context),
                    pendingIntent);
            
            notificationId = TrackLogger.getUniqueNotificationId();
            notificationManager.notify(notificationId, notification);
        }
    };
    
    /**
     * Simple structure for holding on to information about the export state.
     */
    protected static final class RequestState<T> {
        private final int requestId;
        private final Intent intent;
        private final int startId;
        private final T request;
        private final Handler handler;
        private RequestLifecycleStatus status;
        
        public RequestState(int requestId, Intent intent, int startId,
                T request, Handler handler, RequestLifecycleStatus status) {
            super();
            this.requestId = requestId;
            this.intent = intent;
            this.startId = startId;
            this.request = request;
            this.handler = handler;
            this.status = status;
        }
        
        public int getRequestId() {
            return requestId;
        }

        public Intent getIntent() {
            return intent;
        }

        public T getRequest() {
            return request;
        }

        public Handler getHandler() {
            return handler;
        }

        public RequestLifecycleStatus getStatus() {
            return status;
        }

        public void setStatus(RequestLifecycleStatus status) {
            this.status = status;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("RequestState [intent=");
            builder.append(intent);
            builder.append(", startId=");
            builder.append(startId);
            builder.append(", request=");
            builder.append(request);
            builder.append(", handler=");
            builder.append(handler);
            builder.append(", status=");
            builder.append(status);
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * The handler implementing the core worker loop of the service.
     */
    private static final class ServiceHandler<T, U> extends Handler {
        
        private WeakReference<AbstractObservableIntentService<T, U>> serviceRef;
        
        public ServiceHandler(Looper looper, AbstractObservableIntentService<T, U> service) {
            super(looper);
            serviceRef = new WeakReference<AbstractObservableIntentService<T, U>>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            AbstractObservableIntentService<T, U> service = serviceRef.get();
            if (service != null) {
                @SuppressWarnings("unchecked")
                RequestState<T> request = (RequestState<T>) msg.obj;
                try {
                    service.onHandleRequest(request);
                } finally {
                    service.stopSelf(request.startId);
                }
            }
        }
    }
}
