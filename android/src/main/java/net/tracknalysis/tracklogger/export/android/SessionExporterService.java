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

import net.tracknalysis.common.android.notification.AndroidNotificationListener;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger.export.SessionExporter;
import net.tracknalysis.tracklogger.export.SessionExporter.ExportProgress;
import net.tracknalysis.tracklogger.export.SessionExporter.SessionExporterNotificationType;
import net.tracknalysis.tracklogger.export.SessionToFileExporter;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.service.AbstractObservableIntentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;

/**
 * Simple service for handling the queuing and processing of session exports.  Displays
 * notifications of export progress and completion status for each intent delivered in FIFO order.
 *
 * @author David Valeri
 */
public class SessionExporterService extends 
    AbstractObservableIntentService<SessionExportRequest, ExportProgress> {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionExporterService.class);
    private static final String NAME = "SessionExporterService";
    
    public static final String EXTRA_EXPORT_FORMAT = "exportFormat";
    public static final String EXTRA_EXPORT_START_LAP = "startLap";
    public static final String EXTRA_EXPORT_STOP_LAP = "stopLap";
    
    public static final String EXPORT_FORMAT_CSV_1 = "csv1";
    public static final String EXPORT_FORMAT_SQL_1 = "sql1";
    
    public SessionExporterService() {
        super(NAME);
    }
    
    @Override
    protected boolean onStartValidate(Intent intent, int startId) {
        
        LOG.debug("Starting servicing of intent [{}] with startId [{}].");
        
        Uri sessionUri = intent.getData();
        
        if (!TrackLogger.ACTION_SESSION_EXPORT.equals(intent.getAction())) {
            LOG.error(
                    "Intent [{}] has unknown action [{}].  Processing of intent will fail.",
                    intent, intent.getAction());
            return true;
        }
        
        if (sessionUri == null) {
            LOG.error("No data URI for intent [" + intent + "].  Processing of intent will fail.", intent);
            return true;
        }
        
        try {
            Integer.valueOf(sessionUri.getPathSegments().get(
                    TrackLoggerData.Session.ID_PATH_POSITION));
        } catch (NumberFormatException e) {
            LOG.error("Error parsing data URI for intent [" + intent + "].  Processing of intent will fail.", e);
            return true;
        }
        
        return false;
    }
    
    @Override
    protected int getRequestId(Intent intent, int startId) {
        return Integer.valueOf(intent.getData().getPathSegments().get(
                TrackLoggerData.Session.ID_PATH_POSITION));
    }
    
    @Override
    protected SessionExportRequest initializeRequest(Intent intent, int startId) {
        
        int sessionId = Integer.valueOf(intent.getData().getPathSegments().get(
                TrackLoggerData.Session.ID_PATH_POSITION));
        String exportFormat = intent.getStringExtra(EXTRA_EXPORT_FORMAT);
        Integer startLap = intent.getIntExtra(EXTRA_EXPORT_START_LAP, -1);
        Integer endLap = intent.getIntExtra(EXTRA_EXPORT_STOP_LAP, -1);
        
        if (startLap < 0) {
            startLap = null;
        }
        
        if (endLap < 0) {
            endLap = null;
        }
        
        return new SessionExportRequest(sessionId, exportFormat, startLap, endLap);
    }
    
    @Override
    protected ObservableIntentServiceHandler<SessionExportRequest, ExportProgress> createHandler() {
        return new SessionExportServiceHandler(this);
    }
    
    @Override
    protected void doWork(RequestState<SessionExportRequest> requestState) {
        
        String exportFormat = requestState.getRequest().getExportFormatIdentifier();
        
        if (exportFormat == null) {
            exportFormat = EXPORT_FORMAT_CSV_1;
        }
        
        SessionExporter exporter;
        
        if (EXPORT_FORMAT_CSV_1.equals(exportFormat)) {
            exporter = new AndroidSessionToTrackLoggerCsvExporter(
                    getApplicationContext(),
                    new AndroidNotificationListener<SessionExporterNotificationType>(requestState.getHandler()));
        } else if (EXPORT_FORMAT_SQL_1.equals(exportFormat)) {
            exporter = new AndroidSessionToTrackLoggerSqlExporter(
                    getApplicationContext(),
                    new AndroidNotificationListener<SessionExporterNotificationType>(requestState.getHandler()));
        } else {
            LOG.error(
                    "Export format identifier [{}] is not supported in request with request state [{}].",
                    exportFormat, requestState);
         
            // TODO unknown export format
            return;
        }
        
        ((SessionExportServiceHandler) requestState.getHandler()).init(requestState, exporter);
        
        exporter.export(requestState.getRequest().getSessionId(),
                requestState.getRequest().getStartLap(),
                requestState.getRequest().getStopLap());
    }
    
    @Override
    protected void handleDoWorkFailure(RequestState<SessionExportRequest> requestState,
            Exception e) {
        // Ensure that the handler gets the message that things failed as we could have
        // gotten here without the exporter sending the right notification to the handler before
        // it crashed.  If this is a duplicate, it will be ignored by the handler.
        Message message = requestState.getHandler().obtainMessage(
                SessionExporterNotificationType.EXPORT_FAILED.getNotificationTypeId(), e);
        requestState.getHandler().dispatchMessage(message);
    }
    
    private static final class SessionExportServiceHandler extends
        ObservableIntentServiceHandler<SessionExportRequest, ExportProgress> {
        
        private SessionExporter exporter;
        
        public SessionExportServiceHandler(
                AbstractObservableIntentService<SessionExportRequest, ExportProgress> service) {
            super(service);
        }
        
        protected synchronized void init(
                AbstractObservableIntentService.RequestState<SessionExportRequest> requestState,
                SessionExporter exporter) {
            super.init(requestState);
            this.exporter = exporter;
        }
        
        @Override
        protected void doHandleMessage(Message msg) {
            SessionExporter.SessionExporterNotificationType type = 
                    SessionExporter.SessionExporterNotificationType.fromInt(msg.what);
            
            switch (type) {
                case EXPORT_STARTING:
                    onStarting();
                    break;
                case EXPORT_STARTED:
                    onStarted();
                    break;
                case EXPORT_PROGRESS:
                    onRunning((ExportProgress) msg.obj);
                    break;
                case EXPORT_FINISHED:
                    onFinished();
                    break;
                case EXPORT_FAILED:
                    Exception e = (Exception) msg.obj;
                    LOG.error("Export failed.", e);
                    onFailed();
                    break;
            }
        }
        
        @Override
        protected CharSequence getRunningNotificationTickerText(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.export_progress_notification_running_ticker_message,
                            requestState.getRequest().getSessionId());
        }

        @Override
        protected CharSequence getRunningNotificationContentText(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.export_progress_notification_running_ticker_message,
                            requestState.getRequest().getSessionId());
        }

        @Override
        protected Intent getRunningNotificationIntent(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            return new Intent(TrackLogger.ACTION_SESSION_CONFIGURE_EXPORT, requestState.getIntent().getData());
        }
        
        @Override
        @SuppressWarnings("deprecation") // Ignore as can't resolve until API 11
        protected boolean updateRunningNotification(
                RequestState<SessionExportRequest> requestState,
                Context context, ExportProgress progress,
                Notification notification) {
            
            if (progress.getCurrentRecordIndex() % 10 == 0) {
                notification.setLatestEventInfo(
                        context,
                        null,
                        context.getString(
                                R.string.export_progress_notification_running_content_text,
                                progress.getCurrentRecordIndex(),
                                progress.getTotalRecords()),
                        notification.contentIntent);

                return true;
            } else {
                return false;
            }
        }
        
        @Override
        protected CharSequence getFinishedNotificationTickerText(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.export_progress_notification_finished_ticker_message,
                            requestState.getRequest().getSessionId());
        }

        @Override
        protected CharSequence getFinishedNotificationContentText(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.export_progress_notification_finished_content_text,
                            requestState.getRequest().getSessionId());
        }

        @Override
        protected Intent getFinishedNotificationIntent(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            
            Intent notificationIntent = null;
            
            
            String mimeType = exporter.getMimeType();
            
            Uri resultUri = null;
            if (exporter instanceof SessionToFileExporter) {
                resultUri = Uri.fromFile(new File(
                        ((SessionToFileExporter) exporter)
                                .getExportFileAbsolutePath()));
            }
            
            if (resultUri != null) {
                
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                shareIntent.putExtra(Intent.EXTRA_STREAM, resultUri);
                shareIntent.setType(mimeType);
                notificationIntent = Intent.createChooser(shareIntent, null);
            } 
            
            return notificationIntent;
        }
        
        @Override
        protected CharSequence getFailedNotificationTickerText(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            return context.getString(R.string.export_progress_notification_error_ticker_message, 
                    requestState.getRequest().getSessionId());
        }

        @Override
        protected CharSequence getFailedNotificationContentText(
                RequestState<SessionExportRequest> requestState,
                Context context) {
            return context.getString(R.string.export_progress_notification_error_content_text, 
                    requestState.getRequest().getSessionId());
        }
    };
}
