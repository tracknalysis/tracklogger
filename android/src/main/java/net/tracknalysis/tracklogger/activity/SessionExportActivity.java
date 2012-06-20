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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.android.notification.AndroidNotificationStrategy;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger.export.SessionExporter;
import net.tracknalysis.tracklogger.export.SessionExporter.ExportProgress;
import net.tracknalysis.tracklogger.export.android.AndroidSessionToTrackLoggerCsvExporter;
import net.tracknalysis.tracklogger.export.SessionToFileExporter;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * @author David Valeri
 */
public class SessionExportActivity extends Activity {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionExportActivity.class);

    private int laps;
    private int sessionId;
    private String createdDateString;
    private SessionExporter sessionExporter;
    private Dialog exportProgressDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        
        if (!TrackLogger.SESSION_EXPORT_ACTION.equals(intent.getAction())) {
            // TODO dialog and exit
        } else {
            Cursor sessionCursor = null;
            Cursor timingEntryCursor = null;
            
            Uri uri = intent.getData();
            
            sessionCursor = managedQuery(uri,
                    null, null, null, null);
            
            if (!sessionCursor.moveToFirst()) {
             // TODO bad URI, no data
            }
            
            sessionId = sessionCursor.getInt(sessionCursor
                    .getColumnIndex(TrackLoggerData.Session._ID));
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
            
            laps = timingEntryCursor.getCount();
            
            setContentView(R.layout.session_export);
            
            TextView sessionIdTextView = (TextView) findViewById(R.id.sessionId);
            sessionIdTextView.setText(String.valueOf(sessionId));
            
            TextView createdDateTextView = (TextView) findViewById(R.id.createdDate);
            createdDateTextView.setText(createdDateString);
            
            TextView totalLapsTextView = (TextView) findViewById(R.id.totalLaps);
            totalLapsTextView.setText(String.valueOf(laps));
            
            Button exportButton = (Button) findViewById(R.id.export);
            exportButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupSessionExporter();
                    startExport();
                }
            });
            
            ListView listView = (ListView) findViewById(R.id.lapList);
            
            listView.setAdapter(new SimpleCursorAdapter(
                    this,
                    R.layout.lap_list_item,
                    timingEntryCursor,
                    new String[] {
                            TrackLoggerData.TimingEntry.COLUMN_NAME_LAP,
                            TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME},
                    new int[] {
                            R.id.lapNumber,
                            R.id.lapTime}));
            
            exportProgressDialog = new Dialog(this);
            exportProgressDialog.setContentView(R.layout.export_progress);
            exportProgressDialog.setTitle("Session Export");
            exportProgressDialog.setCancelable(false);
            exportProgressDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            
            Button okButton = (Button) exportProgressDialog.findViewById(R.id.ok);
            okButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onExportProgressDialogOk();
                }
            });
            okButton.setClickable(false);
        }
    }
    
    @Override
    protected void onDestroy() {
        cleanupSessionExporter();
        
        super.onDestroy();
    }
    
    protected void setupSessionExporter() {
        
        cleanupSessionExporter();
        
        File outputDir = new File(Environment.getExternalStorageDirectory(), "TrackLogger");
        
        Handler sessionExporterHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                LOG.debug("Handling session exporter message: {}.", msg);
                
                SessionExporter.NotificationType type = 
                        SessionExporter.NotificationType.fromInt(msg.what);
                
                switch (type) {
                    case EXPORT_PROGRESS:
                        ExportProgress progress = (ExportProgress) msg.obj;
                        onExportProgress(progress);
                        break;
                    case EXPORT_FINISHED:
                        onExportFinished();
                        break;
                    case EXPORT_FAILED:
                        onExportFailed();
                        break;
                }
            }
        };
        
        SessionToFileExporter exporter = new AndroidSessionToTrackLoggerCsvExporter(this);
        exporter.setExportDir(outputDir);
        exporter.setNotificationStrategy(new AndroidNotificationStrategy(sessionExporterHandler));
        
        sessionExporter = exporter;
    }
    
    protected void startExport() {
        exportProgressDialog.show();
        
        Thread thread = new Thread() {
            public void run() {
                try {
                    sessionExporter.export(sessionId);
                } catch (Exception e) {
                    LOG.error("Error executing export.", e);
                }                
            }
        };
        
        thread.start();
    }
    
    protected void cleanupSessionExporter() {
        if (sessionExporter != null) {
            // make sure we don't get updates from the old one if it is still running for some reason
            sessionExporter.setNotificationStrategy(null);
            exportProgressDialog.dismiss();
            ProgressBar progressBar = (ProgressBar) exportProgressDialog.findViewById(R.id.exportProgressBar);
            progressBar.setMax(100);
            progressBar.setProgress(0);
            ((TextView) exportProgressDialog.findViewById(R.id.exportProgressMessage)).setText(null);
            exportProgressDialog.findViewById(R.id.ok).setClickable(false);
            sessionExporter = null;
        }
    }
    
    protected void onExportProgress(ExportProgress progress) {
        ProgressBar progressBar = (ProgressBar) exportProgressDialog
                .findViewById(R.id.exportProgressBar);
        
        progressBar.setMax(progress.getTotalRecords());
        progressBar.setProgress(progress.getCurrentRecordIndex());
        
        TextView exportProgressMessage = (TextView) exportProgressDialog
                .findViewById(R.id.exportProgressMessage);
        exportProgressMessage.setText("Exporting "
                + progress.getCurrentRecordIndex() + " of "
                + progress.getTotalRecords() + ".");
    }
    
    protected void onExportFinished() {
        TextView exportProgressMessage = (TextView) exportProgressDialog
                .findViewById(R.id.exportProgressMessage);
        
        exportProgressMessage.setText("Export complete.");
        exportProgressDialog.findViewById(R.id.ok).setClickable(true);
    }
    
    protected void onExportFailed() {
        TextView exportProgressMessage = (TextView) exportProgressDialog
                .findViewById(R.id.exportProgressMessage);
        
        exportProgressMessage.setText("Export failed.");
        exportProgressDialog.findViewById(R.id.ok).setClickable(true);
    }
    
    protected void onExportProgressDialogOk() {
        cleanupSessionExporter();
    }
}
