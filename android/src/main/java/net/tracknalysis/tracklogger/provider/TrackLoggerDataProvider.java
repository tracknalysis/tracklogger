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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to a database of Track Log data.
 * 
 * @author David Valeri
 */
public class TrackLoggerDataProvider extends ContentProvider {

    static final Logger LOG = LoggerFactory
            .getLogger(TrackLoggerDataProvider.class);

    static final String DATABASE_NAME = "TrackLog.db";
    static final int DATABASE_VERSION = 1;

    private static final HashMap<String, String> SESSION_PROJECTION_MAP;
    private static final HashMap<String, String> LOG_ENTRY_PROJECTION_MAP;
    private static final HashMap<String, String> TIMING_ENTRY_PROJECTION_MAP;

    private static final int SESSION = 1;
    private static final int SESSION_ID = 2;
    private static final int LOG_ENTRY = 3;
    private static final int LOG_ENTRY_ID = 4;
    private static final int TIMING_ENTRY = 5;
    private static final int TIMING_ENTRY_ID = 6;

    private static final UriMatcher URI_MATCHER;

    static {

        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        
        URI_MATCHER.addURI(TrackLoggerData.AUTHORITY, "session", SESSION);
        URI_MATCHER.addURI(TrackLoggerData.AUTHORITY, "session/#", SESSION_ID);

        URI_MATCHER.addURI(TrackLoggerData.AUTHORITY, "logentry/", LOG_ENTRY);
        URI_MATCHER.addURI(TrackLoggerData.AUTHORITY, "logentry/#", LOG_ENTRY_ID);
        
        URI_MATCHER.addURI(TrackLoggerData.AUTHORITY, "timingentry/", TIMING_ENTRY);
        URI_MATCHER.addURI(TrackLoggerData.AUTHORITY, "timingentry/#", TIMING_ENTRY_ID);

        SESSION_PROJECTION_MAP = new HashMap<String, String>();
        SESSION_PROJECTION_MAP.put(TrackLoggerData.Session._ID,
                TrackLoggerData.Session._ID);
        SESSION_PROJECTION_MAP.put(TrackLoggerData.Session.COLUMN_NAME_START_DATE,
                TrackLoggerData.Session.COLUMN_NAME_START_DATE);
        SESSION_PROJECTION_MAP.put(
                TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE,
                TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE);

        LOG_ENTRY_PROJECTION_MAP = new HashMap<String, String>();
        LOG_ENTRY_PROJECTION_MAP.put(TrackLoggerData.LogEntry._ID,
                TrackLoggerData.LogEntry._ID);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID,
                TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                TrackLoggerData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP,
                TrackLoggerData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL,
                TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_LATERAL_ACCEL,
                TrackLoggerData.LogEntry.COLUMN_NAME_LATERAL_ACCEL);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL,
                TrackLoggerData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP,
                TrackLoggerData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_LATITUDE,
                TrackLoggerData.LogEntry.COLUMN_NAME_LATITUDE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDE,
                TrackLoggerData.LogEntry.COLUMN_NAME_LONGITUDE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_ALTITUDE,
                TrackLoggerData.LogEntry.COLUMN_NAME_ALTITUDE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_SPEED,
                TrackLoggerData.LogEntry.COLUMN_NAME_SPEED);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_BEARING,
                TrackLoggerData.LogEntry.COLUMN_NAME_BEARING);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP,
                TrackLoggerData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_RPM,
                TrackLoggerData.LogEntry.COLUMN_NAME_RPM);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_MAP,
                TrackLoggerData.LogEntry.COLUMN_NAME_MAP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_TP,
                TrackLoggerData.LogEntry.COLUMN_NAME_TP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_AFR,
                TrackLoggerData.LogEntry.COLUMN_NAME_AFR);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_MAT,
                TrackLoggerData.LogEntry.COLUMN_NAME_MAT);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_CLT,
                TrackLoggerData.LogEntry.COLUMN_NAME_CLT);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE,
                TrackLoggerData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE,
                TrackLoggerData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE);
        
        TIMING_ENTRY_PROJECTION_MAP = new HashMap<String, String>();
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry._ID,
                TrackLoggerData.TimingEntry._ID);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID,
                TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                TrackLoggerData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP,
                TrackLoggerData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry.COLUMN_NAME_LAP,
                TrackLoggerData.TimingEntry.COLUMN_NAME_LAP);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME,
                TrackLoggerData.TimingEntry.COLUMN_NAME_LAP_TIME);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX,
                TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_INDEX);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_TIME,
                TrackLoggerData.TimingEntry.COLUMN_NAME_SPLIT_TIME);
    }

    private TrackLoggerDatabaseHelper databaseHelper;

    @Override
    public boolean onCreate() {
        databaseHelper = new TrackLoggerDatabaseHelper(getContext());
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code uri} is invalid
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int uriType = URI_MATCHER.match(uri); 
        switch (uriType) {
            case SESSION:
                qb.setTables(TrackLoggerData.Session.TABLE_NAME);
                qb.setProjectionMap(SESSION_PROJECTION_MAP);
                break;
            case SESSION_ID:
                qb.setTables(TrackLoggerData.Session.TABLE_NAME);
                qb.setProjectionMap(SESSION_PROJECTION_MAP);
                qb.appendWhere(TrackLoggerData.Session._ID + "="
                        + uri.getPathSegments().get(
                                TrackLoggerData.Session.SESSION_ID_PATH_POSITION));
                break;
            case LOG_ENTRY:
                qb.setTables(TrackLoggerData.LogEntry.TABLE_NAME);
                qb.setProjectionMap(LOG_ENTRY_PROJECTION_MAP);
                break;
            case LOG_ENTRY_ID:
                qb.setTables(TrackLoggerData.LogEntry.TABLE_NAME);
                qb.setProjectionMap(LOG_ENTRY_PROJECTION_MAP);
                qb.appendWhere(TrackLoggerData.LogEntry._ID + "="
                        + uri.getPathSegments().get(
                                TrackLoggerData.LogEntry.LOG_ENTRY_ID_PATH_POSITION));
                break;
                
            case TIMING_ENTRY:
                qb.setTables(TrackLoggerData.TimingEntry.TABLE_NAME);
                qb.setProjectionMap(TIMING_ENTRY_PROJECTION_MAP);
                break;
            case TIMING_ENTRY_ID:
                qb.setTables(TrackLoggerData.TimingEntry.TABLE_NAME);
                qb.setProjectionMap(TIMING_ENTRY_PROJECTION_MAP);
                qb.appendWhere(TrackLoggerData.LogEntry._ID + "="
                        + uri.getPathSegments().get(
                                TrackLoggerData.TimingEntry.TIMING_ENTRY_ID_PATH_POSITION));
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy = sortOrder;
        if (TextUtils.isEmpty(orderBy)) {
            orderBy = getDefaultSortOrder(uriType);
        }

        Cursor c = qb.query(databaseHelper.getReadableDatabase(),
                projection, 
                selection,
                selectionArgs,
                null,
                null,
                orderBy);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code uri} is invalid
     */
    @Override
    public String getType(Uri uri) {

        switch (URI_MATCHER.match(uri)) {
            case SESSION:
                return TrackLoggerData.Session.SESSION_TYPE;
            case SESSION_ID:
                return TrackLoggerData.Session.SESSION_ITEM_TYPE;
            case LOG_ENTRY:
                return TrackLoggerData.LogEntry.LOG_ENTRY_TYPE;
            case LOG_ENTRY_ID:
                return TrackLoggerData.LogEntry.LOG_ENTRY_ITEM_TYPE;
            case TIMING_ENTRY:
                return TrackLoggerData.TimingEntry.TIMING_ENTRY_TYPE;
            case TIMING_ENTRY_ID:
                return TrackLoggerData.TimingEntry.TIMING_ENTRY_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws SQLException If the insertion fails
     * @throws IllegalArgumentException if {@code uri} is invalid
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        ContentValues values = null;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long rowId;
        Uri newUri = null;
        
        switch (URI_MATCHER.match(uri)) {
            case SESSION:
                rowId = db.insert(TrackLoggerData.Session.TABLE_NAME, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            TrackLoggerData.Session.CONTENT_ID_URI_BASE, rowId);
                }
                break;
            case LOG_ENTRY:
                rowId = db.insert(TrackLoggerData.LogEntry.TABLE_NAME, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            TrackLoggerData.LogEntry.CONTENT_ID_URI_BASE, rowId);
                }
                break;
            case TIMING_ENTRY:
                rowId = db.insert(TrackLoggerData.TimingEntry.TABLE_NAME, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            TrackLoggerData.TimingEntry.CONTENT_ID_URI_BASE, rowId);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (rowId > 0) {
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code uri} is invalid
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int count;

        switch (URI_MATCHER.match(uri)) {

            case SESSION:
                count = db.delete(TrackLoggerData.Session.TABLE_NAME,
                        where,
                        whereArgs);
                break;
            case SESSION_ID:
                count = doSingleDelete(TrackLoggerData.Session._ID,
                        uri.getPathSegments().get(TrackLoggerData.Session.SESSION_ID_PATH_POSITION),
                        TrackLoggerData.Session.TABLE_NAME, where, whereArgs);
                break;
            case LOG_ENTRY:
                count = db.delete(TrackLoggerData.LogEntry.TABLE_NAME,
                        where,
                        whereArgs);
            case LOG_ENTRY_ID:
                count = doSingleDelete(TrackLoggerData.LogEntry._ID,
                        uri.getPathSegments().get(TrackLoggerData.LogEntry.LOG_ENTRY_ID_PATH_POSITION),
                        TrackLoggerData.LogEntry.TABLE_NAME, where, whereArgs);
                break;
            case TIMING_ENTRY:
                count = db.delete(TrackLoggerData.TimingEntry.TABLE_NAME,
                        where,
                        whereArgs);
            case TIMING_ENTRY_ID:
                count = doSingleDelete(TrackLoggerData.TimingEntry._ID,
                        uri.getPathSegments().get(TrackLoggerData.TimingEntry.TIMING_ENTRY_ID_PATH_POSITION),
                        TrackLoggerData.TimingEntry.TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code uri} is invalid
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int count;
        String tableName;
        String finalWhere;

        
        switch (URI_MATCHER.match(uri)) {
            case SESSION:  
                tableName = TrackLoggerData.Session.TABLE_NAME;
                finalWhere = where;
                break;
            case SESSION_ID:
                tableName = TrackLoggerData.Session.TABLE_NAME;
                
                finalWhere = TrackLoggerData.Session._ID + " = " + uri.getPathSegments().get(
                        TrackLoggerData.Session.SESSION_ID_PATH_POSITION);;
                
                if (where != null) {
                    finalWhere = finalWhere + " AND (" + where + ")";
                }
                break;
            case LOG_ENTRY:
                tableName = TrackLoggerData.LogEntry.TABLE_NAME;
                finalWhere = where;
                break;
            case LOG_ENTRY_ID:
                tableName = TrackLoggerData.LogEntry.TABLE_NAME;
    
                finalWhere = TrackLoggerData.Session._ID + " = " + uri.getPathSegments().get(
                        TrackLoggerData.LogEntry.LOG_ENTRY_ID_PATH_POSITION);
                
                if (where != null) {
                    finalWhere = finalWhere + " AND (" + where + ")";
                }
            case TIMING_ENTRY:
                tableName = TrackLoggerData.TimingEntry.TABLE_NAME;
                finalWhere = where;
                break;
            case TIMING_ENTRY_ID:
                tableName = TrackLoggerData.TimingEntry.TABLE_NAME;
    
                finalWhere = TrackLoggerData.TimingEntry._ID + " = " + uri.getPathSegments().get(
                        TrackLoggerData.TimingEntry.TIMING_ENTRY_ID_PATH_POSITION);
                
                if (where != null) {
                    finalWhere = finalWhere + " AND (" + where + ")";
                }
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
        count = db.update(tableName,
                values,
                where,
                whereArgs);

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
    
    protected int doSingleDelete(String idColumnName, String id, String tableName,
            String where, String[] whereArgs) {
        
        String finalWhere = idColumnName + " = " + id;

        if (where != null) {
            finalWhere = finalWhere + " AND (" + where + ")";
        }

        return databaseHelper.getWritableDatabase().delete(tableName,
                finalWhere,
                whereArgs);
    }
    
    protected String getDefaultSortOrder(int uriType) {
        switch (uriType) {
            case SESSION:
            case SESSION_ID:
                return TrackLoggerData.Session.DEFAULT_SORT_ORDER;
            case LOG_ENTRY:
            case LOG_ENTRY_ID:
                return TrackLoggerData.LogEntry.DEFAULT_SORT_ORDER;
            case TIMING_ENTRY:
            case TIMING_ENTRY_ID:
                return TrackLoggerData.TimingEntry.DEFAULT_SORT_ORDER;
            default:
                throw new IllegalArgumentException("Unknown URI type: " + uriType);
        }
    }
}