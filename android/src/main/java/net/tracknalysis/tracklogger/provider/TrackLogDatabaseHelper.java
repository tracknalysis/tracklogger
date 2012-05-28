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
 * @author David Valeri
 */
public final class TrackLogDatabaseHelper extends SQLiteOpenHelper {

    public TrackLogDatabaseHelper(Context context) {
        super(context, TrackLogDataProvider.DATABASE_NAME, null, TrackLogDataProvider.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TrackLogData.Session.TABLE_NAME + " ("
                + TrackLogData.Session._ID + " INTEGER PRIMARY KEY,"
                + TrackLogData.Session.COLUMN_NAME_START_DATE + " DATE NOT NULL,"
                + TrackLogData.Session.COLUMN_NAME_LAST_MODIFIED_DATE + " DATE NOT NULL);");

        db.execSQL("CREATE TABLE " + TrackLogData.LogEntry.TABLE_NAME + " ("
                + TrackLogData.LogEntry._ID + " INTEGER PRIMARY KEY,"
                + TrackLogData.LogEntry.COLUMN_NAME_SESSION_ID + " INTEGER NOT NULL,"
                + TrackLogData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP + " BIGINT NOT NULL,"
                + TrackLogData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP + " BIGINT,"
                + TrackLogData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL + " FLOAT,"
                + TrackLogData.LogEntry.COLUMN_NAME_LATERAL_ACCEL + " FLOAT,"
                + TrackLogData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL + " FLOAT,"
                + TrackLogData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP + " BIGINT,"
                + TrackLogData.LogEntry.COLUMN_NAME_LATITUDE + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_LONGITUDE + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_ALTITUDE + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_SPEED + " FLOAT,"
                + TrackLogData.LogEntry.COLUMN_NAME_BEARING + " FLOAT,"
                + TrackLogData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP + " BIGINT,"
                + TrackLogData.LogEntry.COLUMN_NAME_MAP + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_TP + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_AFR + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_MAT + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_CLT + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE + " DOUBLE,"
                + TrackLogData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE + " DOUBLE"
                + ");");
        
        db.execSQL("CREATE TABLE " + TrackLogData.TimingEntry.TABLE_NAME + " ("
                + TrackLogData.TimingEntry._ID + " INTEGER PRIMARY KEY,"
                + TrackLogData.TimingEntry.COLUMN_NAME_SESSION_ID + " INTEGER NOT NULL,"
                + TrackLogData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP + " BIGINT NOT NULL,"
                + TrackLogData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP + " BIGINT NOT NULL,"
                + TrackLogData.TimingEntry.COLUMN_NAME_LAP + " INTEGER NOT NULL,"
                + TrackLogData.TimingEntry.COLUMN_NAME_LAP_TIME + " BIGINT,"
                + TrackLogData.TimingEntry.COLUMN_NAME_SPLIT_INDEX + " INTEGER NOT NULL,"
                + TrackLogData.TimingEntry.COLUMN_NAME_SPLIT_TIME + " BIGINT NOT NULL"
                + ");");
                
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TrackLogDataProvider.LOG.info(
                "Upgrading database from version {} to version {}.  Ladies and gentlemen, "
                        + "hold on to your hats.", oldVersion, newVersion);

        throw new UnsupportedOperationException(
                "This should never happen at this time.");
    }
}