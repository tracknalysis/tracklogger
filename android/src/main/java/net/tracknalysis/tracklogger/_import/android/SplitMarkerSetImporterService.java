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
package net.tracknalysis.tracklogger._import.android;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import net.tracknalysis.common.android.notification.AndroidNotificationStrategy;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger._import.SplitMarkerSetImporter;
import net.tracknalysis.tracklogger._import.SplitMarkerSetImporter.ImportProgress;
import net.tracknalysis.tracklogger._import.SplitMarkerSetImporter.SplitMarkerSetImporterNotificationType;
import net.tracknalysis.tracklogger.activity.SplitMarkerSetListActivity;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.service.AbstractObservableIntentService;

/**
 * @author David Valeri
 */
public class SplitMarkerSetImporterService extends
        AbstractObservableIntentService<SplitMarkerSetImportRequest, ImportProgress> {
    
    public static final String EXTRA_IMPORT_FORMAT = "importFormat";
    public static final String EXTRA_NAME = "name";

    public SplitMarkerSetImporterService() {
        super("SplitMarkerSetImporterService");
    }

    @Override
    protected boolean onStartValidate(Intent intent, int startId) {

        if (!TrackLogger.ACTION_SPLIT_MARKER_SET_IMPORT.equals(intent.getAction())) {
            return true;
        }
        
        try {
            SplitMarkerSetFileFormat.fromId(intent.getIntExtra(EXTRA_IMPORT_FORMAT, -1));
        } catch (IllegalArgumentException e) {
            return true;
        }
        
        return intent.getData() == null;
    }

    @Override
    protected int getRequestId(Intent intent, int startId) {
        return startId;
    }

    @Override
    protected SplitMarkerSetImportRequest initializeRequest(Intent intent, int startId) {
        
        String resource = intent.getDataString();
        String name = intent.getStringExtra(EXTRA_NAME);
        SplitMarkerSetFileFormat format = SplitMarkerSetFileFormat.fromId(intent.getIntExtra(EXTRA_IMPORT_FORMAT, 0));
        
        return new SplitMarkerSetImportRequest(format, resource, name);
    }

    @Override
    protected ObservableIntentServiceHandler<SplitMarkerSetImportRequest, ImportProgress> createHandler() {
        return new SplitMarkerSetImporterServiceHandler(this);
    }

    @Override
    protected void doWork(RequestState<SplitMarkerSetImportRequest> requestState) {
        
        SplitMarkerSetImporter importer = null;
        
        switch (requestState.getRequest().getFormat()) {
            case CSV_1_0:
                try {
                    importer = new AndroidSplitMarkerSetCsvImporter(
                            new AndroidNotificationStrategy<SplitMarkerSetImporterNotificationType>(
                                    requestState.getHandler()),
                            getApplicationContext(),
                            
                            new File(new URI(requestState.getRequest().getResource())));
                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
        }
        
        if (importer == null) {
            // TODO unknown import format
        } else {
            ((SplitMarkerSetImporterServiceHandler) requestState.getHandler()).init(requestState, importer);
            importer.doImport();
        }
    }

    @Override
    protected void handleDoWorkFailure(RequestState<SplitMarkerSetImportRequest> requestState,
            Exception e) {
        // Ensure that the handler gets the message that things failed as we could have
        // gotten here without the exporter sending the right notification to the handler before
        // it crashed.  If this is a duplicate, it will be ignored by the handler.
        Message message = requestState.getHandler().obtainMessage(
                SplitMarkerSetImporterNotificationType.IMPORT_FAILED.getNotificationTypeId(), e);
        requestState.getHandler().dispatchMessage(message);
    }
    
    private static class SplitMarkerSetImporterServiceHandler
            extends
            ObservableIntentServiceHandler<SplitMarkerSetImportRequest, ImportProgress> {
        
        private static final Logger LOG = LoggerFactory.getLogger(SplitMarkerSetImporterServiceHandler.class);
        
        private SplitMarkerSetImporter importer;
        private int splitMarkerSetId = -1;

        public SplitMarkerSetImporterServiceHandler(
                AbstractObservableIntentService<SplitMarkerSetImportRequest, ImportProgress> service) {
            super(service);
        }
        
        protected synchronized void init(
                RequestState<SplitMarkerSetImportRequest> requestState, SplitMarkerSetImporter importer) {
            super.init(requestState);
            this.importer = importer;
        }

        @Override
        protected void doHandleMessage(Message msg) {
            LOG.debug("Handling split marker set importer message: {}.", msg);
            
            SplitMarkerSetImporterNotificationType type = 
                    SplitMarkerSetImporterNotificationType.fromInt(msg.what);
            
            switch (type) {
                case IMPORT_STARTING:
                    onStarting();
                    break;
                case IMPORT_STARTED:
                    onStarted();
                    break;
                case IMPORT_PROGRESS:
                    onRunning((ImportProgress) msg.obj);
                    break;
                case IMPORT_FINISHED:
                    splitMarkerSetId = (Integer) msg.obj;
                    onFinished();
                    break;
                case IMPORT_FAILED:
                    onFailed();
                    break;
            }
        }

        @Override
        protected CharSequence getRunningNotificationTickerText(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.split_marker_set_import_notification_running_ticker_message,
                            importer.getName());
        }

        @Override
        protected CharSequence getRunningNotificationContentText(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            
            return context
                    .getString(
                            R.string.split_marker_set_import_notification_running_ticker_message,
                            importer.getName());
        }

        @Override
        protected Intent getRunningNotificationIntent(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            return new Intent(context, SplitMarkerSetListActivity.class);
        }

        @Override
        @SuppressWarnings("deprecation") // Ignore as can't resolve until API 11
        protected boolean updateRunningNotification(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context, ImportProgress progress,
                Notification notification) {
            
            String text = null;
            if (progress.getTotalRecords() != null) {
                text = context.getString(
                        R.string.split_marker_set_import_notification_running_content_text,
                        progress.getCurrentRecord(),
                        progress.getTotalRecords());
            } else {
                text = context.getString(
                        R.string.split_marker_set_import_notification_running_indeterminate_content_text,
                        progress.getCurrentRecord()); 
            }

            notification.setLatestEventInfo(
                    context,
                    context.getText(R.string.app_name),
                    text,
                    notification.contentIntent);

            // Split marker sets are small so we will just update for all progress events and hope that
            // we don't bog the device down.
            return true;
        }

        @Override
        protected CharSequence getFinishedNotificationTickerText(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.split_marker_set_import_notification_finished_ticker_message);
        }

        @Override
        protected CharSequence getFinishedNotificationContentText(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.split_marker_set_import_notification_finished_content_text,
                            importer.getName());
        }

        @Override
        protected Intent getFinishedNotificationIntent(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            
            Uri splitMarkerSetUri = ContentUris.withAppendedId(
                    TrackLoggerData.SplitMarkerSet.CONTENT_ID_URI_BASE, splitMarkerSetId);
            
            // TODO handle back stack
            return new Intent(Intent.ACTION_VIEW, splitMarkerSetUri);
        }

        @Override
        protected CharSequence getFailedNotificationTickerText(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.split_marker_set_import_notification_error_ticker_message);
        }

        @Override
        protected CharSequence getFailedNotificationContentText(
                RequestState<SplitMarkerSetImportRequest> requestState,
                Context context) {
            return context
                    .getString(
                            R.string.split_marker_set_import_notification_error_content_text,
                            importer.getName());
        }
    };
}
