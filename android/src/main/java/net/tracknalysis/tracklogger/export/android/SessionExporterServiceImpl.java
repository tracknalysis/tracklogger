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
package net.tracknalysis.tracklogger.export.android;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.tracknalysis.common.android.notification.AndroidNotificationStrategy;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger.export.ExportProgress;
import net.tracknalysis.tracklogger.export.SessionExportRequest;
import net.tracknalysis.tracklogger.export.SessionExporter;
import net.tracknalysis.tracklogger.export.SessionExporter.SessionExporterNotificationType;
import net.tracknalysis.tracklogger.export.SessionExporterService;
import net.tracknalysis.tracklogger.export.SessionToFileExporter;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

/**
 * Simple service for handling the queuing and processing of session exports.  Displays
 * notifications of export progress and completion status for each intent delivered in FIFO order.
 * <p/>
 * Based on {@link IntentService} but provides more control and situational awareness to the
 * handler.
 *
 * @author David Valeri
 */
public class SessionExporterServiceImpl extends Service implements SessionExporterService {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionExporterServiceImpl.class);
    private static final String NAME = "SessionExporterService";
    
    public static final String EXTRA_EXPORT_FORMAT = "exportFormat";
    public static final String EXTRA_EXPORT_START_LAP = "startLap";
    public static final String EXTRA_EXPORT_STOP_LAP = "stopLap";
    
    public static final String EXPORT_FORMAT_CSV_1 = "csv1";
    public static final String EXPORT_FORMAT_SQL_1 = "sql1";
    
    private final SparseArray<List<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>>> notificationStrategyMap = 
            new SparseArray<List<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>>>();
    
    private final SparseArray<RequestState> requestStateMap = new SparseArray<RequestState>();
    
    private final IBinder binder = new LocalBinder();
    
    private volatile Looper serviceLooper;
    private volatile ServiceHandler serviceHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + NAME + "]");
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper, this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        
        LOG.debug("Starting servicing of intent [{}] with startId [{}].");
        
        Uri sessionUri = intent.getData();
        int sessionId = 0;
        boolean willFail = false;
        
        if (sessionUri == null) {
            LOG.error("No data URI for intent [" + intent + "].  Processing of intent will fail.", intent);
            willFail = true;
        }
        
        if (!TrackLogger.ACTION_SESSION_EXPORT.equals(intent.getAction())) {
            LOG.error(
                    "Intent [{}] has unknown action [{}].  Processing of intent will fail.",
                    intent, intent.getAction());
            willFail = true;
        }
        
        try {
            sessionId = Integer.valueOf(sessionUri.getPathSegments().get(
                    TrackLoggerData.Session.SESSION_ID_PATH_POSITION));
        } catch (NumberFormatException e) {
            LOG.error("Error parsing data URI for intent [" + intent + "].  Processing of intent will fail.", e);
            willFail = true;
        }
        
        String exportFormat = intent.getStringExtra(EXTRA_EXPORT_FORMAT);
        Integer startLap = intent.getIntExtra(EXTRA_EXPORT_START_LAP, -1);
        Integer endLap = intent.getIntExtra(EXTRA_EXPORT_STOP_LAP, -1);
        
        if (startLap < 0) {
            startLap = null;
        }
        
        if (endLap < 0) {
            endLap = null;
        }
        
        SessionExportRequest request = new SessionExportRequest(sessionId, exportFormat, startLap, endLap);
        
        
        RequestState requestState = new RequestState(
                intent, startId, request, new SessionExporterHandler(this),
                willFail ? SessionExportStatus.FAILED : SessionExportStatus.QUEUED);
        
        Message msg = serviceHandler.obtainMessage();
        msg.obj = requestState;
        synchronized (requestStateMap) {
            // Note: This does not deal well with duplicate requests for the same
            // session ID.  It is therefore important that activities not enqueue
            // more than one request at a time for any given sessionID.
            if (!willFail) {
                requestStateMap.put(sessionId, requestState);
                sendDownstreamUpdates(sessionId, SessionExportStatus.QUEUED, null);
            }
            serviceHandler.sendMessage(msg);
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Created and enqueued request state [{}] from intent [{}] with startId [{}].",
                    new Object[] {requestState, intent, startId});
        } else if (LOG.isInfoEnabled()) {
            LOG.info("Enqueuing export of session for request [{}].", requestState.getRequest());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        synchronized (requestStateMap) {
            requestStateMap.clear();
        }
        
        synchronized (notificationStrategyMap) {
            notificationStrategyMap.clear();
        }
        
        serviceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void enqueue(SessionExportRequest request) {
        // Delegate to the intent mechanism such that we can let the service
        // manage itself as an intent service.  This just gets mapped back to a SessionExportRequest
        // in onStart.
        Intent intent = new Intent(TrackLogger.ACTION_SESSION_EXPORT, 
                ContentUris.withAppendedId(TrackLoggerData.Session.CONTENT_URI, request.getSessionId()));
        intent.putExtra(EXTRA_EXPORT_FORMAT, request.getExportFormatIdentifier());
        intent.putExtra(EXTRA_EXPORT_START_LAP, request.getStartLap());
        intent.putExtra(EXTRA_EXPORT_START_LAP, request.getStopLap());
        
        LOG.debug("Created intent [{}] from session export request [{}],", intent, request);
        
        getApplicationContext().startService(intent);
    }

    @Override
    public void register(int sessionId,
            NotificationStrategy<SessionExporterServiceNotificationType> notificationStrategy) {
        
        synchronized (notificationStrategyMap) {
            List<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>> strategies = notificationStrategyMap
                    .get(sessionId);
            
            if (strategies == null) {
                strategies = new LinkedList<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>>();
                notificationStrategyMap.put(sessionId, strategies);
            } else {
                // Only scrub if it was an existing list.
                scrubStrategies(strategies);
            }
            
            int index = findInWeakReferenceList(strategies, notificationStrategy);
            
            if (index == -1) {
                strategies.add(
                        new WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>(notificationStrategy));
                
                // Always synch to the current state of the queued request for the session in question when registering.
                // This is synchronized so that we always get the latest state of the session and any subsequent updates
                // will not put the listener out of synch with the actual state of the export and we avoid race conditions
                // that result from the activity trying to setup a listener and poll the service to find the current state.
                synchronized (requestStateMap) {
                    RequestState requestState = requestStateMap.get(sessionId); 
                    if (requestState == null) {
                        sendDownstreamUpdates(sessionId, SessionExportStatus.NOT_QUEUED, null);
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
            NotificationStrategy<SessionExporterServiceNotificationType> notificationStrategy) {
        
        synchronized (notificationStrategyMap) {
            List<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>> strategies = 
                    notificationStrategyMap.get(sessionId);
            
            if (strategies != null) {
                scrubStrategies(strategies);
                
                int index = findInWeakReferenceList(strategies, notificationStrategy);
                
                if (index > -1) {
                    strategies.remove(index);
                }
            }
        }
    }
    
    private void onHandleRequest(RequestState requestState) {
        
        LOG.debug("Handling request with request state [{}].", requestState);
        try {
            if (requestState.status == SessionExportStatus.FAILED) {
                LOG.debug(
                        "Request with request state [{}] is already failed, skipping further processing.",
                        requestState);
                return;
            }
        
        
            // The general idea here is that we get the exporter ready and then let it run to completion
            // on this thread.  The handler assigned to the exporter deals with all UI updates and
            // cleanup.  In the event that the exporter execution fails really badly, there is 
            // a catch clause here that simply sends another message to the handler indicating the failure.
            // The handler can deal with multiple messages for the same state transition.
            
            String exportFormat = requestState.getRequest().getExportFormatIdentifier();
            
            if (exportFormat == null) {
                exportFormat = EXPORT_FORMAT_CSV_1;
            }
            
            SessionExporter exporter;
            
            if (EXPORT_FORMAT_CSV_1.equals(exportFormat)) {
                exporter = new AndroidSessionToTrackLoggerCsvExporter(
                        getApplicationContext(),
                        new AndroidNotificationStrategy<SessionExporterNotificationType>(requestState.getHandler()));
            } else if (EXPORT_FORMAT_SQL_1.equals(exportFormat)) {
                exporter = new AndroidSessionToTrackLoggerSqlExporter(
                        getApplicationContext(),
                        new AndroidNotificationStrategy<SessionExporterNotificationType>(requestState.getHandler()));
            } else {
                LOG.error(
                        "Export format identifier [{}] is not supported in request with request state [{}].",
                        exportFormat, requestState);
                requestState.getHandler().init(requestState, null);
                
                // Ensure that the handler gets the message that things failed as the exporter is not
                // going to get a chance to run.
                Message message = requestState.getHandler().obtainMessage(
                        SessionExporterNotificationType.EXPORT_FAILED
                                .getNotificationTypeId());
                requestState.getHandler().dispatchMessage(message);
                return;
            }
            
            requestState.getHandler().init(requestState, exporter);
            exporter.export(requestState.getRequest().getSessionId(),
                    requestState.getRequest().getStartLap(),
                    requestState.getRequest().getStopLap());

            LOG.info("Export finished for request with request state [{}].", requestState);
        } catch (Exception e) {
            LOG.error("Error executing export for request with request state [" + requestState + "].", e);
            
            // Ensure that the handler gets the message that things failed as we could have
            // gotten here without the exporter sending the right notification to the handler before
            // it crashed.  If this is a duplicate, it will be ignored by the handler.
            Message message = requestState.getHandler().obtainMessage(
                    SessionExporterNotificationType.EXPORT_FAILED
                            .getNotificationTypeId(), e);
            requestState.getHandler().dispatchMessage(message);
        } finally {
            // Clean up any reference we have to the current request being serviced.
            synchronized (requestStateMap) {
                requestStateMap.remove(requestState.getRequest().getSessionId());
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
            List<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>> strategies,
            NotificationStrategy<SessionExporterServiceNotificationType> notificationStrategy) {
        
        int index = 0;
        
        for (WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>> existingStrategy : strategies) {
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
            List<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>> strategies) {
        
        Iterator<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>> iterator = strategies
                .iterator();
        
        while (iterator.hasNext()) {
            WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>> strategyRef = iterator.next();
            if (strategyRef.get() == null) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Sends a notification, {@link SessionExportLifecycleNotification}, to any downstream listeners.
     *
     * @param sessionId the ID of the session that the update is for
     * @param status the status of the session export for the session with ID {@code sessionId}
     * @param progress the optional progress associated with the notification.  Expected when {@code status}
     * is {@link SessionExportStatus#EXPORTING}.
     */
    private void sendDownstreamUpdates(int sessionId, SessionExportStatus status, ExportProgress progress) {
        synchronized (notificationStrategyMap) {
            List<WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>>> strategies = 
                    notificationStrategyMap.get(sessionId);
            
            if (strategies != null && strategies.size() != 0) {
                for (WeakReference<NotificationStrategy<SessionExporterServiceNotificationType>> strategyRef
                        : strategies) {
                    
                    NotificationStrategy<SessionExporterServiceNotificationType> strategy = strategyRef.get();
                    
                    if (strategy != null) {
                        strategy.sendNotification(SessionExporterServiceNotificationType.SESSION_EXPORT_LIFECYCLE_NOTIFICATION,
                                new SessionExportLifecycleNotification(status, progress));
                    }
                }
            }
        }
    }
    
    public class LocalBinder extends Binder {
        public SessionExporterService getService() {
            return SessionExporterServiceImpl.this;
        }
    }
    
    private static final class SessionExporterHandler extends Handler {
        
        private final Context context;
        private WeakReference<SessionExporterServiceImpl> serviceRef;
        // Intentionally not a weak reference because we might otherwise lose the notification as it is
        // not held anywhere else but in this handler after creation.
        private Notification notification;
        private SessionExporter exporter;
        private RequestState requestState;
        private int notificationId;
        private boolean handleMessages = false;
        
        public SessionExporterHandler(SessionExporterServiceImpl service) {
            this.context = service.getApplicationContext();
            this.serviceRef = new WeakReference<SessionExporterServiceImpl>(service);
        }
        
        public synchronized void init(RequestState requestState, SessionExporter exporter) {
            this.exporter = exporter;
            this.requestState = requestState;
            handleMessages = true;
        }
        
        public synchronized void destroy() {
            handleMessages = false;
            serviceRef.clear();
            notification = null;
            exporter = null;
            requestState = null;
        }
        
        @Override
        public void handleMessage(Message msg) {
            if (handleMessages) {
                LOG.debug("Handling session exporter message: {}.", msg);
                
                try {
                    SessionExporter.SessionExporterNotificationType type = 
                            SessionExporter.SessionExporterNotificationType.fromInt(msg.what);
                    
                    switch (type) {
                        case EXPORT_STARTING:
                            onExportStarting();
                            break;
                        case EXPORT_STARTED:
                            onExportStarted();
                            break;
                        case EXPORT_PROGRESS:
                            onExportProgress((ExportProgress) msg.obj);
                            break;
                        case EXPORT_FINISHED:
                            onExportFinished();
                            break;
                        case EXPORT_FAILED:
                            onExportFailed();
                            break;
                    }
                } catch (Exception e) {
                    LOG.error("Error handling message " + msg + ".", e);
                }
            }
        }

        /**
         * Updates the state of the handler and triggers downstream updates.  Also displays
         * the notification for the export.
         */
        private void onExportStarting() {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() < SessionExportStatus.STARTING.getWeight()) {
                    requestState.setStatus(SessionExportStatus.STARTING);
                    showExportingNotification(
                            requestState.getIntent().getData(),
                            requestState.getRequest().getSessionId());
                    
                    SessionExporterServiceImpl service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequest().getSessionId(),
                                requestState.getStatus(), null);
                    }
                }
            }
        }
        
        /**
         * Updates the state of the handler and triggers downstream updates.
         */
        private void onExportStarted() {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() < SessionExportStatus.STARTED.getWeight()) {
                    requestState.setStatus(SessionExportStatus.STARTED);
                    
                    SessionExporterServiceImpl service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequest().getSessionId(),
                                requestState.getStatus(), null);
                    }
                }
            }
        }
        
        /**
         * Updates the export progress view in the notification based on the provided progress data and
         * sends downstream updates.
         */
        private void onExportProgress(ExportProgress progress) {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() <= SessionExportStatus.EXPORTING.getWeight()) {
                    requestState.setStatus(SessionExportStatus.EXPORTING);
    
                    if (progress.getCurrentRecordIndex() % 10 == 0) {
                        notification.setLatestEventInfo(
                                context,
                                context.getText(R.string.app_name),
                                context.getString(
                                        R.string.export_progress_notification_running_content_text,
                                        progress.getCurrentRecordIndex(),
                                        progress.getTotalRecords()),
                                notification.contentIntent);
    
                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        
                        notificationManager
                                .notify(notificationId,
                                        notification);
                    }
                    
                    SessionExporterServiceImpl service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequest().getSessionId(),
                                requestState.getStatus(), progress);
                    }
                }
            }
        }
        
        /**
         * Clears the in progress notification and displays a finished notification.  Finally sending
         * downstream updates and cleaning up this handler.
         */
        private void onExportFinished() {
            synchronized (requestState) {
                if (requestState.getStatus().getWeight() < SessionExportStatus.FINISHED.getWeight()) {
                    requestState.setStatus(SessionExportStatus.FINISHED);
                    try {
                        cancelExportingNotification();
                        
                        Uri resultUri = null;
                        if (exporter instanceof SessionToFileExporter) {
                            resultUri = Uri.fromFile(new File(
                                    ((SessionToFileExporter) exporter)
                                            .getExportFileAbsolutePath()));
                        }
                        
                        showFinishedNotification(resultUri, exporter.getMimeType(),
                                requestState.getRequest().getSessionId());
                        
                        SessionExporterServiceImpl service = serviceRef.get();
                        if (service != null) {
                            service.sendDownstreamUpdates(
                                    requestState.getRequest().getSessionId(),
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
        private void onExportFailed() {
            if (requestState.status.getWeight() < SessionExportStatus.FINISHED.getWeight()) {
                requestState.status = SessionExportStatus.FAILED;
                
                try {
                    cancelExportingNotification();
                    
                    showFailedNotification(requestState.getRequest().getSessionId());
                
                    SessionExporterServiceImpl service = serviceRef.get();
                    if (service != null) {
                        service.sendDownstreamUpdates(
                                requestState.getRequest().getSessionId(),
                                requestState.status, null);
                    }
                } finally {
                    destroy();
                }
            }
        }
        
        /**
         * Creates a new notification and displays it using the notification service.
         *
         * @param sessionId the ID of the session being exported
         */
        private void showExportingNotification(Uri sessionUri, int sessionId) {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);

            notification = new Notification(
                    R.drawable.icon,
                    context.getString(
                            R.string.export_progress_notification_running_ticker_message,
                            sessionId), System.currentTimeMillis());
            
            notification.flags = notification.flags
                    | Notification.FLAG_ONGOING_EVENT
                    | Notification.FLAG_NO_CLEAR;

            Intent notificationIntent = new Intent(
                    TrackLogger.ACTION_SESSION_EXPORT_CONFIG, sessionUri);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, 0);

            notification.setLatestEventInfo(
                    context,
                    context.getText(R.string.app_name),
                    context.getString(
                            R.string.export_progress_notification_running_ticker_message,
                            sessionId), pendingIntent);

            notificationId = TrackLogger.getUniqueNotificationId();
            notificationManager.notify(notificationId, notification);            
        }
        
        /**
         * Cancels the exporting notification.
         */
        private void cancelExportingNotification() {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            
            notification = null;
            notificationId = -1;
        }
        
        /**
         * Shows a new notification indicating that the export finished.
         *
         * @param resultUri the optional URI of the output from the export
         * @param mimeType the optional mime type of the output from the export
         * @param sessionId the ID of the session that was exported
         */
        private void showFinishedNotification(Uri resultUri, String mimeType, int sessionId) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            
            
            notification = new Notification(
                    R.drawable.icon,
                    context.getString(
                            R.string.export_progress_notification_finished_ticker_message,
                            sessionId), System.currentTimeMillis());
            notification.flags = notification.flags
                    | Notification.FLAG_AUTO_CANCEL;

            Intent notificationIntent = null;

            if (resultUri != null) {
                // TODO https://tracknalysis.atlassian.net/browse/TRKLGR-30
                notificationIntent = new Intent(Intent.ACTION_VIEW);
                if (mimeType != null) {
                    notificationIntent.setDataAndType(resultUri, mimeType);
                } else {
                    notificationIntent.setData(resultUri);
                }
            } else {
                // TODO https://tracknalysis.atlassian.net/browse/TRKLGR-30
                notificationIntent = new Intent(Intent.ACTION_MAIN);
                notificationIntent.addCategory(Intent.CATEGORY_HOME);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            notification.setLatestEventInfo(
                    context,
                    context.getText(R.string.app_name),
                    context.getString(
                            R.string.export_progress_notification_finished_content_text,
                            sessionId), pendingIntent);

            notificationId = TrackLogger.getUniqueNotificationId();
            notificationManager.notify(notificationId, notification);
        }
        
        /**
         * Shows a new notification indicating that the export failed.
         *
         * @param sessionId the ID of the session that was exported
         */
        private void showFailedNotification(int sessionId) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            
            notification = new Notification(R.drawable.icon,
                    context.getString(R.string.export_progress_notification_error_ticker_message, sessionId),
                    System.currentTimeMillis());
            notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
            
            // TODO https://tracknalysis.atlassian.net/browse/TRKLGR-30
            Intent notificationIntent = new Intent(Intent.ACTION_MAIN);
                notificationIntent.addCategory(Intent.CATEGORY_HOME);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent, 0);
            
            notification.setLatestEventInfo(context,
                    context.getText(R.string.app_name),
                    context.getText(R.string.export_progress_notification_finished_content_text),
                    pendingIntent);
            
            notificationId = TrackLogger.getUniqueNotificationId();
            notificationManager.notify(notificationId, notification);
        }
    };
    
    /**
     * Simple structure for holding on to information about the export state.
     */
    private static final class RequestState {
        private final Intent intent;
        private final int startId;
        private final SessionExportRequest request;
        private final SessionExporterHandler handler;
        private SessionExportStatus status;
        
        public RequestState(Intent intent, int startId,
                SessionExportRequest request, SessionExporterHandler handler,
                SessionExportStatus status) {
            super();
            this.intent = intent;
            this.startId = startId;
            this.request = request;
            this.handler = handler;
            this.status = status;
        }
        
        public Intent getIntent() {
            return intent;
        }

        public SessionExportRequest getRequest() {
            return request;
        }

        public SessionExporterHandler getHandler() {
            return handler;
        }

        public SessionExportStatus getStatus() {
            return status;
        }

        public void setStatus(SessionExportStatus status) {
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
    private static final class ServiceHandler extends Handler {
        
        private WeakReference<SessionExporterServiceImpl> serviceRef;
        
        public ServiceHandler(Looper looper, SessionExporterServiceImpl service) {
            super(looper);
            serviceRef = new WeakReference<SessionExporterServiceImpl>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            SessionExporterServiceImpl service = serviceRef.get();
            if (service != null) {
                RequestState request = (RequestState) msg.obj;
                try {
                    service.onHandleRequest(request);
                } finally {
                    service.stopSelf(request.startId);
                }
            }
        }
    }
}
