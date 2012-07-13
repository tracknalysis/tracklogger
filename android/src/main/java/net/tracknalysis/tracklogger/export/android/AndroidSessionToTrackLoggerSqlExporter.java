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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.export.AbstractSessionToFileExporter;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.provider.TrackLoggerData.Session;

/**
 * Exporter for writing insert statements to populate a database equivalent to
 * the internal Tracklogger database structure. Useful if you want to use
 * structured relational data to perform some sort of analysis or for generating
 * SQL to populate test data in the database.
 * 
 * @author David Valeri
 */
public class AndroidSessionToTrackLoggerSqlExporter extends
        AbstractSessionToFileExporter {
    
    private static final Logger LOG = LoggerFactory.getLogger(AndroidSessionToTrackLoggerSqlExporter.class);
    
    private final Context context;
    
    public AndroidSessionToTrackLoggerSqlExporter(
            Context context,
            NotificationStrategy<SessionExporterNotificationType> notificationStrategy) {
        super(
                new File(ConfigurationFactory.getInstance().getConfiguration().getDataDirectory()),
                notificationStrategy);
        this.context = context;
    }

    @Override
    public String getMimeType() {
        return "text/sql";
    }

    @Override
    protected void export(OutputStream out, int sessionId, Integer startLap,
            Integer endLap) throws IOException {
        
        Writer writer = new OutputStreamWriter(out);
        
        ContentResolver cr = context.getContentResolver();
        Cursor logEntryCursor = null;
        Cursor timingEntryCursor = null;
        Cursor sessionCursor = null;
        
        try {
            sessionCursor = cr.query(TrackLoggerData.Session.CONTENT_URI,
                    null, 
                    TrackLoggerData.Session._ID + "= ?",
                    new String[] {Integer.toString(sessionId)},
                    null);
            
            timingEntryCursor = cr.query(
                    TrackLoggerData.TimingEntry.CONTENT_URI, null,
                    TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID + "= ?",
                    new String[] { Integer.toString(sessionId) },
                    TrackLoggerData.TimingEntry.DEFAULT_SORT_ORDER);
            
            logEntryCursor = cr.query(TrackLoggerData.LogEntry.CONTENT_URI,
                    null, TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID
                            + "= ?",
                    new String[] { Integer.toString(sessionId) },
                    TrackLoggerData.LogEntry.DEFAULT_SORT_ORDER);
            
            int totalRecords = sessionCursor.getCount() + timingEntryCursor.getCount() + logEntryCursor.getCount();
            int currentRecord = 1;
            
            if (!sessionCursor.moveToFirst()) {
                throw new IllegalStateException("No session found with ID " + sessionId + ".");
            }
            
            writer.write("INSERT INTO session(_id, start_date, last_modified_date)\r\n");
            writer.write("VALUES ("
                    + sessionId + ", '"
                    + writeSqlDate(parseSqlDate(sessionCursor.getString(sessionCursor
                            .getColumnIndexOrThrow(Session.COLUMN_NAME_START_DATE)))) + "', '"
                    + writeSqlDate(parseSqlDate(sessionCursor.getString(sessionCursor
                            .getColumnIndexOrThrow(Session.COLUMN_NAME_LAST_MODIFIED_DATE)))) +"');");
            
            writer.write("\r\n");
            writer.write("\r\n");
            
            sessionCursor.close();
            
            sendExportProgressNotification(currentRecord++, totalRecords);
            
            if (timingEntryCursor.moveToFirst()) {
            
                final int timingEntrySynchColumnIndex = timingEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
                final int timingEntryCaptureColumnIndex = timingEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP);
                final int timingEntryTimeInDayColumnIndex = timingEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_TIME_IN_DAY);
                final int timingEntryLapColumnIndex = timingEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP);
                final int timingEntryLapTimeColumnIndex = timingEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME);
                final int timingEntrySplitIndexColumnIndex = timingEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX);
                final int timingEntrySplitTimeColumnIndex = timingEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_TIME);
                
                while (!timingEntryCursor.isAfterLast()) {
                    writer.write("INSERT INTO timing_entry(session_id, synch_timestamp, capture_timestamp, time_in_day, lap, lap_time, split_index, split_time)\r\n");
                    writer.write("VALUES (");
                    writer.write(sessionId + ", ");
                    writer.write(toSqlFromString(timingEntryCursor, timingEntrySynchColumnIndex) + ", ");
                    writer.write(toSqlFromString(timingEntryCursor, timingEntryCaptureColumnIndex) + ", ");
                    writer.write(toSqlFromString(timingEntryCursor, timingEntryTimeInDayColumnIndex) + ", ");
                    writer.write(toSqlFromString(timingEntryCursor, timingEntryLapColumnIndex) + ", ");
                    writer.write(toSqlFromString(timingEntryCursor, timingEntryLapTimeColumnIndex) + ", ");
                    writer.write(toSqlFromString(timingEntryCursor, timingEntrySplitIndexColumnIndex) + ", ");
                    writer.write(toSqlFromString(timingEntryCursor, timingEntrySplitTimeColumnIndex));
                    writer.write(");");
                    
                    writer.write("\r\n");
                    writer.write("\r\n");
                    
                    timingEntryCursor.move(1);
                    sendExportProgressNotification(currentRecord++, totalRecords);
                }
            }
            
            timingEntryCursor.close();
            
            if (logEntryCursor.moveToFirst()) {
                final int logEntrySynchColumnIndex = logEntryCursor
                        .getColumnIndexOrThrow(TrackLoggerData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
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
                final int locationTimeColumnIndex = logEntryCursor
                        .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_TIME_IN_DAY);
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
                final int tpColumnIndex = logEntryCursor
                        .getColumnIndex(TrackLoggerData.LogEntry.COLUMN_NAME_TP);
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
                
                while (!logEntryCursor.isAfterLast()) {
                
                    writer.write("INSERT INTO log_entry(session_id, synch_timestamp, accel_capture_timestamp, longitudinal_accel, lateral_accel, vertical_accel, location_capture_timestamp, location_time_in_day, latitude, longitude, altitude, speed, bearing, ecu_capture_timestamp, rpm, map, tp, afr, mat, clt, ignition_advance, battery_voltage)\r\n");
                    writer.write("VALUES(");
                    writer.write(sessionId + ", ");
                    writer.write(logEntryCursor.getString(logEntrySynchColumnIndex) + ", ");
                    
                    writer.write(toSqlFromString(logEntryCursor, accelCaptureTimestampColumnIndex) + ", ");
                    writer.write(toSqlFromFloat(logEntryCursor, longitudinalAccelColumnIndex) + ", ");
                    writer.write(toSqlFromFloat(logEntryCursor, lateralAccelColumnIndex) + ", ");
                    writer.write(toSqlFromFloat(logEntryCursor, verticalAccelColumnIndex) + ", ");
                    writer.write(toSqlFromString(logEntryCursor, locationCaptureTimestampColumnIndex) + ", ");
                    writer.write(toSqlFromString(logEntryCursor, locationTimeColumnIndex) + ", "); 
                    writer.write(toSqlFromDouble(logEntryCursor, latitudeColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, longitudeColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, altitudeColumnIndex) + ", ");
                    writer.write(toSqlFromFloat(logEntryCursor, speedColumnIndex) + ", ");
                    writer.write(toSqlFromFloat(logEntryCursor, bearingColumnIndex) + ", ");
                    writer.write(toSqlFromString(logEntryCursor, ecuCaptureTimestampColumnIndex) + ", ");
                    writer.write(toSqlFromString(logEntryCursor, rpmColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, mapColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, tpColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, afrColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, matColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, cltColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, ignitionAdvanceColumnIndex) + ", ");
                    writer.write(toSqlFromDouble(logEntryCursor, batteryVoltageColumnIndex) + ");");
                    
                    writer.write("\r\n");
                    writer.write("\r\n");
                    
                    logEntryCursor.move(1);
                    sendExportProgressNotification(currentRecord++, totalRecords);
                }
            }
            
            logEntryCursor.close();
        } finally {
            writer.flush();
            
            if (sessionCursor != null) {
                sessionCursor.close();
            }
            
            if (timingEntryCursor != null) {
                timingEntryCursor.close();
            }
            
            if (logEntryCursor != null) {
                logEntryCursor.close();
            }
        }
    }
    
    protected String toSqlFromFloat(Cursor cursor, int columnIndex) {
        
        if (cursor.isNull(columnIndex)) {
            return "NULL";
        } else {
            return String.format("%.20f", cursor.getFloat(columnIndex));
        }
    }
    
    protected String toSqlFromDouble(Cursor cursor, int columnIndex) {
        
        if (cursor.isNull(columnIndex)) {
            return "NULL";
        } else {
            return String.format("%.20f", cursor.getDouble(columnIndex));
        }
    }
    
    protected String toSqlFromString(Cursor cursor, int columnIndex) {
        if (cursor.isNull(columnIndex)) {
            return "NULL";
        } else {
            return cursor.getString(columnIndex);
        }
    }

    @Override
    protected Date getSessionStartTime(int sessionId) {
        ContentResolver cr = context.getContentResolver();
        return AndroidSessionExporterHelper.getSessionStartTime(sessionId, cr, LOG);
    }

    @Override
    protected String getFileExtension() {
        return "sql";
    }
}
