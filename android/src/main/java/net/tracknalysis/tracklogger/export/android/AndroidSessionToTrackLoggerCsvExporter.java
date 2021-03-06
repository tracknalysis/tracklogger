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

import static net.tracknalysis.tracklogger.export.android.AndroidSessionExporterHelper.*;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.notification.NotificationListener;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.export.AbstractSessionToTrackLoggerCsvExporter;
import net.tracknalysis.tracklogger.export.SessionExporter;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.provider.TrackLoggerDataUtil;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;

/**
 * Exports to a flat CSV file using the TrackLogger v1 CSV format.
 *
 * @author David Valeri
 */
public class AndroidSessionToTrackLoggerCsvExporter extends AbstractSessionToTrackLoggerCsvExporter {
    
    private static final Logger LOG = LoggerFactory.getLogger(AndroidSessionToTrackLoggerCsvExporter.class);
    
    private final Context context;
    private Date sessionStartDate;
    private String splitMarkerSetName;
    private int splitMarkerCount;

    public AndroidSessionToTrackLoggerCsvExporter(Context context,
            NotificationListener<SessionExporterNotificationType> notificationStrategy) {
        super(
                new File(ConfigurationFactory.getInstance().getConfiguration().getDataDirectory()),
                notificationStrategy);
        this.context = context;
    }
    
    @Override
    protected void init(int sessionId) {
        
        Cursor sessionCursor = null;
        Cursor splitMarkerSetCursor = null;
        Cursor splitMarkerCursor = null;
        
        try {
            sessionCursor = context.getContentResolver().query(TrackLoggerData.Session.CONTENT_URI,
                    null, 
                    TrackLoggerData.Session._ID + "= ?",
                    new String[] {Integer.toString(sessionId)},
                    null);
        
            if (!sessionCursor.moveToFirst()) {
                LOG.error("No session found for session ID {}.", sessionId);
                throw new IllegalStateException("No session found for ID " + sessionId);
            }
            
            String startDateString = sessionCursor.getString(sessionCursor
                    .getColumnIndex(TrackLoggerData.Session.COLUMN_NAME_START_DATE)); 
            
            sessionStartDate = TrackLoggerDataUtil.parseSqlDate(startDateString);
            int splitMarkerSetId = sessionCursor.getInt(sessionCursor
                    .getColumnIndex(TrackLoggerData.Session.COLUMN_NAME_SPLIT_MARKER_SET_ID));
            
            splitMarkerSetCursor = context
                    .getContentResolver()
                    .query(ContentUris
                            .withAppendedId(
                                    TrackLoggerData.SplitMarkerSet.CONTENT_URI,
                                    splitMarkerSetId),
                            null, null, null, null);
            
            if (!splitMarkerSetCursor.moveToFirst()) {
                LOG.error("No split marker set found for split marker set ID {}.", splitMarkerSetId);
                throw new IllegalStateException(
                        "No split marker set found for split marker set ID "
                                + splitMarkerSetId);
            }
            
            splitMarkerSetName = splitMarkerSetCursor
                    .getString(splitMarkerSetCursor
                            .getColumnIndex(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME));
            
            splitMarkerCursor = context.getContentResolver().query(
                    TrackLoggerData.SplitMarker.CONTENT_URI,
                    null,
                    TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID
                            + " = ?",
                    new String[] { String.valueOf(splitMarkerSetId) },
                    null);
            
            splitMarkerCount = splitMarkerCursor.getCount();
        } finally {
            if (sessionCursor != null) {
                sessionCursor.close();
            }
            
            if (splitMarkerCursor != null) {
                splitMarkerCursor.close();
            }
            
            if (splitMarkerSetCursor != null) {
                splitMarkerSetCursor.close();
            }
        }
    }
    
    @Override
    protected void exportEntries(Writer writer, int sessionId,
            Integer startLap, Integer endLap) throws IOException {
        
        ContentResolver cr = context.getContentResolver();
        Cursor logEntryCursor = null;
        Cursor timingEntryCursor = null;
        
        try {
            logEntryCursor = cr.query(TrackLoggerData.LogEntry.CONTENT_URI,
                    null, TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID
                            + "= ?",
                    new String[] { Integer.toString(sessionId) },
                    TrackLoggerData.LogEntry.DEFAULT_SORT_ORDER);
            
            timingEntryCursor = cr.query(
                    TrackLoggerData.TimingEntry.CONTENT_URI, null,
                    TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID + "= ?",
                    new String[] { Integer.toString(sessionId) },
                    TrackLoggerData.TimingEntry.DEFAULT_SORT_ORDER);
            
            // There is nothing in the cursor so we give up
            if (!timingEntryCursor.moveToFirst()) {
                LOG.error("No timing entries found for session with ID {}.",
                        new Object[] { sessionId, startLap, endLap });

                throw new IllegalStateException(
                        "No timing entries found for session.");
            }
            
            // There is nothing in the cursor so we give up
            if (logEntryCursor.getCount() == 0) {
                LOG.error("No log entries found for session with ID {}.",
                        new Object[] { sessionId, startLap, endLap });

                throw new IllegalStateException(
                        "No log entries found for session.");
            }
            
            final int logEntrySynchColumnIndex = logEntryCursor
                    .getColumnIndexOrThrow(TrackLoggerData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
            final int timingEntrySynchColumnIndex = timingEntryCursor
                    .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
            
            final int accelCaptureTimestampColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP);
            final int longitudinalAccelColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL);
            final int lateralAccelColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_LATERAL_ACCEL);
            final int verticalAccelColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL);
            final int locationCaptureTimestampColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP);
            final int latitudeColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_LATITUDE);
            final int longitudeColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDE);
            final int altitudeColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_ALTITUDE);
            final int speedColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_SPEED);
            final int bearingColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_BEARING);
            final int ecuCaptureTimestampColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP);
            final int rpmColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_RPM);
            final int mapColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_MAP);
            final int mgpColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_MGP);
            final int tpColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_THROTTLE_POSITION);
            final int afrColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_AFR);
            final int matColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_MAT);
            final int cltColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_CLT);
            final int ignitionAdvanceColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE);
            final int batteryVoltageColumnIndex = logEntryCursor
                    .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE);
            final int timingCaptureTimestampColumnIndex = timingEntryCursor
                    .getColumnIndex(TrackLoggerData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP);
            
            // For notifications of progress
            int recordCount = logEntryCursor.getCount();
            
            // For synching the two data sets
            long timingSynchTimestamp = timingEntryCursor
                    .getLong(timingEntrySynchColumnIndex);
            long logSynchTimestamp = Long.MAX_VALUE;
            
            LOG.debug("Looking for timing to log entry synch...");
            while (!logEntryCursor.isAfterLast()
                    && timingSynchTimestamp != logSynchTimestamp) {
                logEntryCursor.move(1);
                logSynchTimestamp = logEntryCursor
                        .getLong(logEntrySynchColumnIndex);
                
                getNotificationStrategy().onNotification(
                        SessionExporter.SessionExporterNotificationType.EXPORT_PROGRESS,
                        new ExportProgress(logEntryCursor.getPosition(), recordCount));
            }
            
            // We never found a synch between the two data sets
            if (logEntryCursor.isAfterLast()) {
                LOG.error(
                        "No synchronization between log and timing entries found for session with "
                                + "ID {} and laps {} to {}.", new Object[] {
                                sessionId, startLap, endLap });

                throw new IllegalStateException(
                        "No synchronization between log and timing entries found for session.");
            } else {
                LOG.debug(
                        "Found synchronization between log and timing entries.  Log entry at position {} "
                                + "matched with timing entry at position {}.",
                        logEntryCursor.getPosition() - 1,
                        timingEntryCursor.getPosition());
            }
            
            int lap = timingEntryCursor
                    .getInt(timingEntryCursor
                            .getColumnIndex(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP));
            int splitIndex = timingEntryCursor
                    .getInt(timingEntryCursor
                            .getColumnIndex(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX));
            long timingCaptureTimestamp = timingEntryCursor
                    .getLong(timingCaptureTimestampColumnIndex);
            
            // Until we run out of log data
            while (!logEntryCursor.isAfterLast()) {

                logSynchTimestamp = logEntryCursor
                        .getLong(logEntrySynchColumnIndex);
                
                // The events in the DB are logged on the completion of a lap/split; however, they also
                // represent the instant at which the next lap/split is beginning.  In the export, we want
                // the alignment to be in tune with the later arrangement rather than the former.
                // For instance, the first timing entry should be for lap 1 and not the last time in lap 0.
                if (logSynchTimestamp == timingSynchTimestamp) {
                    splitIndex++;
                    if (splitIndex == splitMarkerCount) {
                        splitIndex = 0;
                        lap++;
                    }
                }

                if ((startLap == null || startLap >= lap) && (endLap == null || lap <= endLap)) {
                    LOG.trace("Writing entry for lap {} and split index {}.",
                            lap, splitIndex);
                    
                    writeEntry(
                            writer, logSynchTimestamp,
                            
                            getLongOrNull(accelCaptureTimestampColumnIndex, logEntryCursor),
                            getFloatOrNull(longitudinalAccelColumnIndex, logEntryCursor),
                            getFloatOrNull(lateralAccelColumnIndex, logEntryCursor),
                            getFloatOrNull(verticalAccelColumnIndex, logEntryCursor),
                            
                            getLongOrNull(locationCaptureTimestampColumnIndex, logEntryCursor),
                            getDoubleOrNull(latitudeColumnIndex, logEntryCursor),
                            getDoubleOrNull(longitudeColumnIndex, logEntryCursor),
                            getDoubleOrNull(altitudeColumnIndex, logEntryCursor),
                            getFloatOrNull(speedColumnIndex, logEntryCursor),
                            getFloatOrNull(bearingColumnIndex, logEntryCursor),
                            
                            getLongOrNull(ecuCaptureTimestampColumnIndex, logEntryCursor),
                            getIntegerOrNull(rpmColumnIndex, logEntryCursor),
                            getDoubleOrNull(mapColumnIndex, logEntryCursor),
                            getDoubleOrNull(mgpColumnIndex, logEntryCursor),
                            getDoubleOrNull(tpColumnIndex, logEntryCursor),
                            getDoubleOrNull(afrColumnIndex, logEntryCursor),
                            getDoubleOrNull(matColumnIndex, logEntryCursor),
                            getDoubleOrNull(cltColumnIndex, logEntryCursor),
                            getDoubleOrNull(ignitionAdvanceColumnIndex, logEntryCursor),
                            getDoubleOrNull(batteryVoltageColumnIndex, logEntryCursor),
                            
                            timingCaptureTimestamp,
                            lap,
                            splitIndex);
                } else {
                    LOG.trace("Skipping entry for lap {} and split index {}.",
                            lap, splitIndex);
                }

                // This entry represents a split/segment or lap event so we are now waiting for the next event
                if (logSynchTimestamp == timingSynchTimestamp) {
                    timingEntryCursor.move(1);
                    if (!timingEntryCursor.isAfterLast()) {
                        timingSynchTimestamp = timingEntryCursor
                                .getLong(timingEntrySynchColumnIndex);
                        
                        lap = timingEntryCursor
                                .getInt(timingEntryCursor
                                        .getColumnIndex(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP));
                        splitIndex = timingEntryCursor
                                .getInt(timingEntryCursor
                                        .getColumnIndex(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX));
                        timingCaptureTimestamp = timingEntryCursor
                                .getLong(timingCaptureTimestampColumnIndex);
                    } else {
                        // We are already sitting on the right values for the remainder of the log data
                        // so just move the timing synch out of the way so we will never match it again.
                        timingSynchTimestamp = Long.MAX_VALUE;
                    }
                }
                
                logEntryCursor.move(1);

                sendExportProgressNotification(logEntryCursor.getPosition(), recordCount);
            }
        } finally {
            if (timingEntryCursor != null) {
                timingEntryCursor.close();
            }
            
            if (logEntryCursor != null) {
                logEntryCursor.close();
            }
        }
    }

    @Override
    protected Date getSessionStartTime() {
        return sessionStartDate;
    }
    
    @Override
    protected String getSplitMarkerSetName() {
        return splitMarkerSetName;
    }
}
