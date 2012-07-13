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
package net.tracknalysis.tracklogger.activity;

import java.lang.ref.WeakReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.android.notification.AndroidNotificationStrategy;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.util.TimeUtil;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger.export.SessionExporter.ExportProgress;
import net.tracknalysis.tracklogger.export.android.SessionExporterService;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.service.AbstractObservableIntentService.LocalBinder;
import net.tracknalysis.tracklogger.service.ObservableIntentService;
import net.tracknalysis.tracklogger.service.ObservableIntentService.RequestLifecycleNotification;
import net.tracknalysis.tracklogger.service.ObservableIntentService.RequestNotificationType;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for configuring, triggering, and monitoring the progress of session exports.
 *
 * @author David Valeri
 */
public class SessionConfigureExportActivity extends Activity {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionConfigureExportActivity.class);

    private int sessionId;
    private String createdDateString;
    private Dialog initErrorDialog;
    
    private TextView sessionIdTextView;
    private TextView createdDateTextView;
    private TextView totalLapsTextView;
    private Button exportButton;
    private LinearLayout exportBarEnqueueLayout;
    private LinearLayout exportBarInProgressLayout;
    private TextView exportProgressMessageTextView;
    private ProgressBar exportProgressBar;
    
    private ServiceConnection serviceConnection;
    private ObservableIntentService sessionExporterService;
    private volatile boolean bound;
    private NotificationStrategy<RequestNotificationType> notificationStrategy;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        notificationStrategy = new AndroidNotificationStrategy<RequestNotificationType>(
                new SessionExporterServiceHandler(this));
        
        setContentView(R.layout.session_export);
        
        sessionIdTextView = (TextView) findViewById(R.id.sessionId);
        
        createdDateTextView = (TextView) findViewById(R.id.createdDate);
        
        totalLapsTextView = (TextView) findViewById(R.id.totalLaps);
        
        exportButton = (Button) findViewById(R.id.export_button);
        exportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startExport();
            }
        });
        
        exportBarEnqueueLayout = (LinearLayout) findViewById(R.id.export_action_bar_enqueue);
        exportBarInProgressLayout = (LinearLayout) findViewById(R.id.export_action_bar_in_progress);
        
        exportProgressMessageTextView = (TextView) findViewById(R.id.export_progress_text);
        exportProgressBar = (ProgressBar) findViewById(R.id.export_progress_bar);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // TODO deal with rebind
                LocalBinder binder = (LocalBinder) service;
                bound = true;
                sessionExporterService = binder.getService();
                finishResume();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                bound = false;
            }
        };
        
        if (!bindService(new Intent(getApplicationContext(), SessionExporterService.class),
                serviceConnection,
                BIND_AUTO_CREATE)) {
            LOG.error("The Activity could not bind to the SessionExporterService.");
            onInitError(R.string.app_name, R.string.general_error);
        }
    }
    
    /**
     * Finish view setup only after we have bound to the session exporter service.
     */
    protected void finishResume() {
        
        Intent intent = getIntent();
        
        if (!TrackLogger.ACTION_SESSION_EXPORT_CONFIG.equals(intent.getAction())) {
            LOG.error(
                    "The Activity was launched with action {}, but the activity does not understand this action.",
                    intent.getAction());
            onInitError(R.string.app_name, R.string.general_error);
            return;
        } else {
            Cursor sessionCursor = null;
            Cursor timingEntryCursor = null;
            
            Uri uri = intent.getData();
            
            sessionCursor = managedQuery(uri,
                    null, null, null, null);
            
            if (!sessionCursor.moveToFirst()) {
                onInitError(R.string.app_name, R.string.export_error_session_not_found);
                return;
            }
            
            sessionId = sessionCursor.getInt(sessionCursor
                    .getColumnIndex(TrackLoggerData.Session._ID));
            
            sessionExporterService.register(sessionId, notificationStrategy);
            
            createdDateString = sessionCursor
                    .getString(sessionCursor
                            .getColumnIndex(TrackLoggerData.Session.COLUMN_NAME_START_DATE));
            
            timingEntryCursor = managedQuery(
                    TrackLoggerData.TimingEntry.CONTENT_URI,
                    null, 
                    TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID
                            + "= ? AND " + TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME
                            + " IS NOT NULL AND " + TrackLoggerData.TimingEntry.COLUMN_NAME_LAP
                            + " != 0",
                    new String[] {Integer.toString(sessionId)},
                    TrackLoggerData.TimingEntry.DEFAULT_SORT_ORDER);
            
            final int laps = timingEntryCursor.getCount();
            
            sessionIdTextView.setText(String.valueOf(sessionId));
            createdDateTextView.setText(createdDateString);
            totalLapsTextView.setText(String.valueOf(laps));
            
            ListView listView = (ListView) findViewById(R.id.lapList);
            
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.lap_list_item,
                    timingEntryCursor,
                    new String[] {
                            TrackLoggerData.TimingEntry.COLUMN_NAME_LAP,
                            TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME},
                    new int[] {
                            R.id.lapNumber,
                            R.id.lapTime});
            
            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                    int lapTimeColumnIndex = cursor.getColumnIndex(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME);
                    boolean bound = false;
                    
                    if (lapTimeColumnIndex == columnIndex) {
                        long duration = cursor.getLong(lapTimeColumnIndex);
                        
                        if (view instanceof TextView) {
                            ((TextView) view).setText(TimeUtil.formatDuration(duration, false, true));
                        } else {
                            throw new IllegalStateException("This binder can only bind the lap time to a text view.");
                        }
                        
                        bound = true;
                    }
                    
                    return bound;
                }
            });
            
            listView.setAdapter(adapter);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        sessionExporterService.unRegister(sessionId, notificationStrategy);
        
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
    
    @Override
    protected void onDestroy() {      
        if (initErrorDialog != null) {
            initErrorDialog.dismiss();
        }
        
        super.onDestroy();
    }
    
    /**
     * Enqueue the session for export with the current export settings.
     */
    protected void startExport() {
        Intent intent = new Intent(TrackLogger.ACTION_SESSION_EXPORT, getIntent().getData());
        // Only used for testing until https://tracknalysis.atlassian.net/browse/TRKLGR-23 is implemented
        // intent.putExtra("exportFormat", "sql1");
        startService(intent);
        
        Toast.makeText(this, getString(R.string.export_queued), Toast.LENGTH_LONG).show();
    }
    
    /**
     * Displays the form for configuring and triggering the export of the
     * session while hiding the export progress display.
     */
    protected void showExportForm() {
        exportBarInProgressLayout.setVisibility(View.GONE);
        exportBarEnqueueLayout.setVisibility(View.VISIBLE);
        
        exportProgressBar.setMax(100);
        exportProgressBar.setProgress(0);
    }
    
    /**
     * Hides the form for triggering a session export while displaying the form for queued / running
     * export progress.
     */
    protected void showExportProgress(ExportProgress progress) {
        if (exportBarEnqueueLayout.getVisibility() != View.GONE) {
            exportBarEnqueueLayout.setVisibility(View.GONE);
        }
        
        if (exportBarInProgressLayout.getVisibility() != View.VISIBLE) {
            exportBarInProgressLayout.setVisibility(View.VISIBLE);
        }
        
        if (progress == null) {
            exportProgressBar.setIndeterminate(true);
            exportProgressMessageTextView.setText(getString(R.string.export_queued));
            exportProgressBar.setMax(100);
            exportProgressBar.setProgress(0);
        } else {
            exportProgressBar.setIndeterminate(false);
            exportProgressMessageTextView.setText(getString(
                    R.string.export_progress_notification_running_content_text,
                    progress.getCurrentRecordIndex(), progress.getTotalRecords()));
            exportProgressBar.setMax(progress.getTotalRecords());
            exportProgressBar.setProgress(progress.getCurrentRecordIndex());
        }
        
    }
    
    /**
     * Display an alert dialog with the title text containing the string with ID {@code title}
     * and an error message containing the string with ID {@code errorMessage} and optional arguments 
     * {@code args}.  The OK buttom on the dialog triggers the finishing of this activity.
     *
     * @param title the string ID for the dialog title
     * @param errorMessage the string ID for the dialog message
     * @param args the optional arguments for token replacement in the string with ID {@code errorMessage}
     */
    protected void onInitError(int title, int errorMessage, Object... args) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(errorMessage, args))
                .setTitle(title)
                .setPositiveButton(R.string.general_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        });

        if (!isFinishing()) {
            initErrorDialog = builder.create();
            initErrorDialog.show();
        }
    }
    
    private static class SessionExporterServiceHandler extends Handler {
        
        private final WeakReference<SessionConfigureExportActivity> activityRef;
        
        private SessionExporterServiceHandler(
                SessionConfigureExportActivity activity) {
            super();
            this.activityRef = new WeakReference<SessionConfigureExportActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg) {
            
            SessionConfigureExportActivity activity = activityRef.get();
            
            if (activity != null) {
                LOG.debug("{}", msg);
                
                RequestNotificationType notificationType = 
                        RequestNotificationType.fromInt(msg.what);
                
                switch (notificationType) {
                    case REQUEST_LIFECYCLE_NOTIFICATION:
                        
                        @SuppressWarnings("unchecked")
                        RequestLifecycleNotification<ExportProgress> exportNotification = 
                                (RequestLifecycleNotification<ExportProgress>) msg.obj;
                        
                        switch (exportNotification.getStatus()) {
                            case NOT_QUEUED:
                                activity.showExportForm();
                                break;
                            case QUEUED:
                                activity.showExportProgress(null);
                                break;
                            case RUNNING:
                                activity.showExportProgress(exportNotification.getProgress());
                                break;
                            case FINISHED:
                                activity.showExportForm();
                                break;
                            case FAILED:
                                activity.showExportForm();
                                break;
                            default:
                                // Ignore all others
                        }
                        break;
                    default:
                        // Ignore all others
                }
            }
        }
    }
}
