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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Utility class providing for the create of SQLite DB schema and migration of data on updates.
 *
 * @author David Valeri
 */
public final class TrackLoggerDatabaseHelper extends SQLiteOpenHelper {

    public TrackLoggerDatabaseHelper(Context context) {
        super(context, TrackLoggerDataProvider.DATABASE_NAME, null, TrackLoggerDataProvider.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TrackLoggerData.Session.TABLE_NAME + " ("
                + TrackLoggerData.Session._ID + " INTEGER PRIMARY KEY,"
                + TrackLoggerData.Session.COLUMN_NAME_START_DATE + " DATE NOT NULL,"
                + TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE + " DATE NOT NULL,"
                + TrackLoggerData.Session.COLUMN_NAME_SPLIT_MARKER_SET_ID + " INTEGER NOT NULL"
                + ");");

        db.execSQL("CREATE TABLE " + TrackLoggerData.LogEntry.TABLE_NAME + " ("
                + TrackLoggerData.LogEntry._ID + " INTEGER PRIMARY KEY,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID + " INTEGER NOT NULL,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP + " BIGINT NOT NULL,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP + " BIGINT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL + " FLOAT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_LATERAL_ACCEL + " FLOAT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL + " FLOAT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP + " BIGINT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_TIME_IN_DAY + " BIGINT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_LATITUDE + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDE + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_ALTITUDE + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_SPEED + " FLOAT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_BEARING + " FLOAT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP + " BIGINT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_RPM + " BIGINT,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_MAP + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_TP + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_AFR + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_MAT + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_CLT + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE + " DOUBLE,"
                + TrackLoggerData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE + " DOUBLE"
                + ");");
        
        db.execSQL("CREATE TABLE " + TrackLoggerData.TimingEntry.TABLE_NAME + " ("
                + TrackLoggerData.TimingEntry._ID + " INTEGER PRIMARY KEY,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID + " INTEGER NOT NULL,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP + " BIGINT NOT NULL,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP + " BIGINT NOT NULL,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_TIME_IN_DAY + " BIGINT NOT NULL,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_LAP + " INTEGER NOT NULL,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME + " BIGINT,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX + " INTEGER NOT NULL,"
                + TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_TIME + " BIGINT"
                + ");");
        
        db.execSQL("CREATE TABLE " + TrackLoggerData.SplitMarkerSet.TABLE_NAME + " ("
                + TrackLoggerData.SplitMarkerSet._ID + " INTEGER PRIMARY KEY,"
                + TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME + " VARCHAR(50) NOT NULL"
                + ");");
        
        db.execSQL("CREATE TABLE " + TrackLoggerData.SplitMarker.TABLE_NAME + " ("
                + TrackLoggerData.SplitMarker._ID + " INTEGER PRIMARY KEY,"
                + TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID + " INTEGER NOT NULL,"
                + TrackLoggerData.SplitMarker.COLUMN_NAME_NAME + " VARCHAR(50) NULL,"
                + TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX + " INTEGER NOT NULL,"
                + TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE + " DOUBLE NOT NULL,"
                + TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE + " DOUBLE NOT NULL"
                + ");");
                
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TrackLoggerDataProvider.LOG.info(
                "Upgrading database from version {} to version {}.  Ladies and gentlemen, "
                        + "hold on to your hats.", oldVersion, newVersion);

        throw new UnsupportedOperationException(
                "This should never happen at this time.");
    }
}