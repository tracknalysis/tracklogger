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
package net.tracknalysis.tracklogger.provider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.tracklogger.model.AccelData;
import net.tracknalysis.tracklogger.model.EcuData;
import net.tracknalysis.tracklogger.model.LocationData;
import net.tracknalysis.tracklogger.model.LogEntry;
import net.tracknalysis.tracklogger.model.TimingData;
import net.tracknalysis.tracklogger.model.TimingEntry;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

/**
 * Utility for performing common data transformations related to the TrackLogger data provider.
 * 
 * TODO This all needs to be moved into a business logic tier
 *
 * @author David Valeri
 */
public final class TrackLoggerDataUtil {
    
    private static final Logger LOG = LoggerFactory.getLogger(TrackLoggerDataUtil.class);
    private static final DateFormat SQL_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
    
    static {
        TrackLoggerDataUtil.SQL_DATE_FORMATTER.setLenient(false);
    }
    
    private TrackLoggerDataUtil() {        
    }
    
    /**
     * Attempts to make a unique variant of the given name
     * 
     * @param cr resolver to use for inspecting existing names
     * @param name the base name to try from
     *
     * @return a unique name or {@code null} if one could not be calculated
     */
    public static String createUniqueSplitMarkerSetName(ContentResolver cr, String name) {
        String newName = name;
        int i = 2;
        
        while (TrackLoggerDataUtil.isDuplicateSplitMarkerSetName(cr, newName)
                && i < 100) {
            newName = name + " " + i++;
        }
        
        if (i >= 100) {
            LOG.error(
                    "Exhausted unique name generation attemps after [{}] tries, ending with [{}].",
                    i - 2, newName);
            return null;
        } else {
            return newName;
        }
    }
    
    /**
     * Returns true if the name meets validation criteria for format.
     */
    public static boolean isValidSplitMarkerSetName(String name) {
        return name.matches(".*[^\\s-]+.*") || name.length() <= 50;
    }
    
    /**
     * Returns true if {@code name} is already used by an existing split marker set.
     * @param cr the resolver to use
     * @param name the name to check
     */
    public static boolean isDuplicateSplitMarkerSetName(ContentResolver cr, String name) {
        Cursor cursor = null;
        
        try {
            cursor = cr.query(
                    TrackLoggerData.SplitMarkerSet.CONTENT_URI, null,
                    TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME + " = ?",
                    new String[] {name}, null);
            
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    /**
     * Converts the fields in a {@link LogEntry} into content values for insertion
     * into the TrackLogger database under {@link TrackLoggerData.LogEntry}.
     *
     * @param logEntry the entry to convert
     *
     * @return the mapped content values
     */
    public static ContentValues toContentValues(LogEntry logEntry) {
        ContentValues cv  = new ContentValues();
        
        LocationData locationData = logEntry.getLocationData();
        AccelData accelData = logEntry.getAccelData();
        EcuData ecuData = logEntry.getEcuData();
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID,
                logEntry.getSessionId());
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                logEntry.getSynchTimestamp());
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP,
                accelData.getDataRecivedTime());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL,
                accelData.getLongitudinal());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LATERAL_ACCEL,
                accelData.getLateral());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL,
                accelData.getVertical());
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP,
                locationData.getDataRecivedTime());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_TIME_IN_DAY,
                locationData.getTime());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LATITUDE,
                locationData.getLatitude());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDE,
                locationData.getLongitude());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_ALTITUDE,
                locationData.getAltitude());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_SPEED,
                locationData.getSpeed());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_BEARING,
                locationData.getBearing());
        
        if (ecuData != null) {
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP,
                    ecuData.getDataRecivedTime());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_RPM,
                    ecuData.getRpm());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_MAP,
                    ecuData.getManifoldAbsolutePressure());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_TP,
                    ecuData.getThrottlePosition());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_AFR,
                    ecuData.getAirFuelRatio());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_MAT,
                    ecuData.getManifoldAirTemperature());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_CLT,
                    ecuData.getCoolantTemperature());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE,
                    ecuData.getIgnitionAdvance());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE,
                    ecuData.getBatteryVoltage());
        }
        
        return cv;
    }
    
    public static ContentValues toContentValues(TimingEntry timingEntry) {
        ContentValues cv = new ContentValues();
        
        TimingData timingData = timingEntry.getTimingData();
        
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID,
                timingEntry.getSessionId());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                timingEntry.getSynchTimestamp());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP,
                timingData.getDataRecivedTime());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_TIME_IN_DAY,
                timingData.getTime());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP,
                timingData.getLap());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME,
                timingData.getLapTime());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX,
                timingData.getSplitIndex());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_TIME,
                timingData.getSplitTime());
        
        return cv;
    }

    public static synchronized String writeSqlDate(Date date) {
        return SQL_DATE_FORMATTER.format(date);
    }

    public static synchronized Date parseSqlDate(String dateString) {
        try {
            return SQL_DATE_FORMATTER.parse(dateString);
        } catch (ParseException e) {
            throw new IllegalStateException("Error parsing SQL formatted date.", e);
        }
    }
}
