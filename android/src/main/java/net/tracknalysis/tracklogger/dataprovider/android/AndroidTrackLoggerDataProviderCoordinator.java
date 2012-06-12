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
package net.tracknalysis.tracklogger.dataprovider.android;

import java.util.Date;

import net.tracknalysis.common.android.notification.AndroidNotificationStrategy;
import net.tracknalysis.tracklogger.dataprovider.AccelDataProvider;
import net.tracknalysis.tracklogger.dataprovider.EcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.LocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TimingData;
import net.tracknalysis.tracklogger.dataprovider.TimingDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;

/**
 * @author David Valeri
 */
public class AndroidTrackLoggerDataProviderCoordinator extends
        TrackLoggerDataProviderCoordinator {
    
    private final Context context;

    public AndroidTrackLoggerDataProviderCoordinator(Handler handler, Context context,
            AccelDataProvider accelDataProvider,
            LocationDataProvider gpsDataProvider, EcuDataProvider ecuDataProvider,
            TimingDataProvider timingDataProvider) {
        super(new AndroidNotificationStrategy(handler), accelDataProvider,
                gpsDataProvider, ecuDataProvider, timingDataProvider);
        this.context = context;
    }

    @Override
    protected int createSession() {
        Date now = new Date();
        
        ContentValues cv = new ContentValues();
        cv.put(TrackLoggerData.Session.COLUMN_NAME_START_DATE,
                formatAsSqlDate(now));
        cv.put(TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE,
                formatAsSqlDate(now));
        
        Uri currentSessionUri = context.getContentResolver().insert(
                TrackLoggerData.Session.CONTENT_URI, cv);
        return Integer.parseInt(currentSessionUri.getPathSegments().get(
                TrackLoggerData.Session.SESSION_ID_PATH_POSITION));
    }

    @Override
    protected void openSession(int sessionId) {
        Date now = new Date();
        
        Uri currentSessionUri = ContentUris.withAppendedId(
                TrackLoggerData.Session.CONTENT_ID_URI_BASE, sessionId);
        
        ContentValues cv = new ContentValues();
        cv.put(TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE,
                formatAsSqlDate(now));
        
        if (context.getContentResolver().update(
                currentSessionUri, cv, null, null) != 1) {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void storeLogEntry(int sessionId, LogEntry logEntry) {
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = new ContentValues();
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID,
                sessionId);
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                logEntry.locationData.getTime());
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP,
                logEntry.accelData.getDataRecivedTime());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL,
                logEntry.accelData.getLongitudinal());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LATERAL_ACCEL,
                logEntry.accelData.getLateral());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL,
                logEntry.accelData.getVertical());
        
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP,
                logEntry.locationData.getDataRecivedTime());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LATITUDE,
                logEntry.locationData.getLatitude());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDE,
                logEntry.locationData.getLongitude());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_ALTITUDE,
                logEntry.locationData.getAltitude());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_SPEED,
                logEntry.locationData.getSpeed());
        cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_BEARING,
                logEntry.locationData.getSpeed());
        
        if (logEntry.ecuData != null) {
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP,
                    logEntry.ecuData.getDataRecivedTime());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP,
                    logEntry.ecuData.getDataRecivedTime());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_MAP,
                    logEntry.ecuData.getManifoldAbsolutePressure());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_TP,
                    logEntry.ecuData.getThrottlePosition());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_AFR,
                    logEntry.ecuData.getAirFuelRatio());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_MAT,
                    logEntry.ecuData.getManifoldAirTemperature());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_CLT,
                    logEntry.ecuData.getCoolantTemperature());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE,
                    logEntry.ecuData.getIgnitionAdvance());
            cv.put(TrackLoggerData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE,
                    logEntry.ecuData.getBatteryVoltage());
        }
        
        cr.insert(TrackLoggerData.LogEntry.CONTENT_URI, cv);
    }

    @Override
    protected void storeTimingEntry(int sessionId, TimingData timingData) {
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = new ContentValues();
        
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID,
                sessionId);
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                timingData.getTime());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP,
                timingData.getDataRecivedTime());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP,
                timingData.getLap());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME,
                timingData.getLapTime());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX,
                timingData.getSplitIndex());
        cv.put(TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_TIME,
                timingData.getSplitTime());
        
        cr.insert(TrackLoggerData.TimingEntry.CONTENT_URI, cv);
    }
}
