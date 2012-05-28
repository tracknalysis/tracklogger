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
public class TrackLogDataProvider extends ContentProvider {

    static final Logger LOG = LoggerFactory
            .getLogger(TrackLogDataProvider.class);

    static final String DATABASE_NAME = "TrackLog.db";
    static final int DATABASE_VERSION = 1;

    private static final HashMap<String, String> SESSION_PROJECTION_MAP;
    private static final HashMap<String, String> LOG_ENTRY_PROJECTION_MAP;
    private static final HashMap<String, String> TIMING_ENTRY_PROJECTION_MAP;

    private static final String[] READ_SESSION_PROJECTION = new String[] {
            TrackLogData.Session._ID,
            TrackLogData.Session.COLUMN_NAME_START_DATE,
            TrackLogData.Session.COLUMN_NAME_LAST_MODIFIED_DATE};

    private static final String[] READ_LOG_ENTRY_PROJECTION = new String[] {
            TrackLogData.LogEntry._ID,
            TrackLogData.LogEntry.COLUMN_NAME_SESSION_ID,
            TrackLogData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP,
            TrackLogData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL,
            TrackLogData.LogEntry.COLUMN_NAME_LATERAL_ACCEL,
            TrackLogData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL};

    private static final int SESSION = 1;
    private static final int SESSION_ID = 2;
    private static final int LOG_ENTRY = 3;
    private static final int LOG_ENTRY_ID = 4;
    private static final int TIMING_ENTRY = 5;
    private static final int TIMING_ENTRY_ID = 6;

    private static final UriMatcher URI_MATCHER;

    static {

        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        URI_MATCHER.addURI(TrackLogData.AUTHORITY, "session", SESSION);
        URI_MATCHER.addURI(TrackLogData.AUTHORITY, "session/#", SESSION_ID);

        URI_MATCHER.addURI(TrackLogData.AUTHORITY, "logentry/", LOG_ENTRY);
        URI_MATCHER.addURI(TrackLogData.AUTHORITY, "logentry/#", LOG_ENTRY_ID);
        
        URI_MATCHER.addURI(TrackLogData.AUTHORITY, "timingentry/", TIMING_ENTRY);
        URI_MATCHER.addURI(TrackLogData.AUTHORITY, "timingentry/#", TIMING_ENTRY_ID);

        SESSION_PROJECTION_MAP = new HashMap<String, String>();
        SESSION_PROJECTION_MAP.put(TrackLogData.Session._ID,
                TrackLogData.Session._ID);
        SESSION_PROJECTION_MAP.put(TrackLogData.Session.COLUMN_NAME_START_DATE,
                TrackLogData.Session.COLUMN_NAME_START_DATE);
        SESSION_PROJECTION_MAP.put(
                TrackLogData.Session.COLUMN_NAME_LAST_MODIFIED_DATE,
                TrackLogData.Session.COLUMN_NAME_LAST_MODIFIED_DATE);

        LOG_ENTRY_PROJECTION_MAP = new HashMap<String, String>();
        LOG_ENTRY_PROJECTION_MAP.put(TrackLogData.LogEntry._ID,
                TrackLogData.LogEntry._ID);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_SESSION_ID,
                TrackLogData.LogEntry.COLUMN_NAME_SESSION_ID);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                TrackLogData.LogEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP,
                TrackLogData.LogEntry.COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL,
                TrackLogData.LogEntry.COLUMN_NAME_LONGITUDINAL_ACCEL);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_LATERAL_ACCEL,
                TrackLogData.LogEntry.COLUMN_NAME_LATERAL_ACCEL);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL,
                TrackLogData.LogEntry.COLUMN_NAME_VERTICAL_ACCEL);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP,
                TrackLogData.LogEntry.COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_LATITUDE,
                TrackLogData.LogEntry.COLUMN_NAME_LATITUDE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_LONGITUDE,
                TrackLogData.LogEntry.COLUMN_NAME_LONGITUDE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_ALTITUDE,
                TrackLogData.LogEntry.COLUMN_NAME_ALTITUDE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_SPEED,
                TrackLogData.LogEntry.COLUMN_NAME_SPEED);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_BEARING,
                TrackLogData.LogEntry.COLUMN_NAME_BEARING);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP,
                TrackLogData.LogEntry.COLUMN_NAME_ECU_CAPTURE_TIMESTAMP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_MAP,
                TrackLogData.LogEntry.COLUMN_NAME_MAP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_TP,
                TrackLogData.LogEntry.COLUMN_NAME_TP);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_AFR,
                TrackLogData.LogEntry.COLUMN_NAME_AFR);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_MAT,
                TrackLogData.LogEntry.COLUMN_NAME_MAT);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_CLT,
                TrackLogData.LogEntry.COLUMN_NAME_CLT);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE,
                TrackLogData.LogEntry.COLUMN_NAME_IGNITION_ADVANCE);
        LOG_ENTRY_PROJECTION_MAP.put(
                TrackLogData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE,
                TrackLogData.LogEntry.COLUMN_NAME_BATTERY_VOLTAGE);
        
        TIMING_ENTRY_PROJECTION_MAP = new HashMap<String, String>();
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry._ID,
                TrackLogData.TimingEntry._ID);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry.COLUMN_NAME_SESSION_ID,
                TrackLogData.TimingEntry.COLUMN_NAME_SESSION_ID);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP,
                TrackLogData.TimingEntry.COLUMN_NAME_SYNCH_TIMESTAMP);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP,
                TrackLogData.TimingEntry.COLUMN_NAME_CAPTURE_TIMESTAMP);
        
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry.COLUMN_NAME_LAP,
                TrackLogData.TimingEntry.COLUMN_NAME_LAP);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry.COLUMN_NAME_LAP_TIME,
                TrackLogData.TimingEntry.COLUMN_NAME_LAP_TIME);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry.COLUMN_NAME_SPLIT_INDEX,
                TrackLogData.TimingEntry.COLUMN_NAME_SPLIT_INDEX);
        TIMING_ENTRY_PROJECTION_MAP.put(
                TrackLogData.TimingEntry.COLUMN_NAME_SPLIT_TIME,
                TrackLogData.TimingEntry.COLUMN_NAME_SPLIT_TIME);
    }

    private TrackLogDatabaseHelper databaseHelper;

    @Override
    public boolean onCreate() {
        databaseHelper = new TrackLogDatabaseHelper(getContext());
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
                qb.setTables(TrackLogData.Session.TABLE_NAME);
                qb.setProjectionMap(SESSION_PROJECTION_MAP);
                break;
            case SESSION_ID:
                qb.setTables(TrackLogData.Session.TABLE_NAME);
                qb.setProjectionMap(SESSION_PROJECTION_MAP);
                qb.appendWhere(TrackLogData.Session._ID + "="
                        + uri.getPathSegments().get(
                                TrackLogData.Session.SESSION_ID_PATH_POSITION));
                break;
            case LOG_ENTRY:
                qb.setTables(TrackLogData.LogEntry.TABLE_NAME);
                qb.setProjectionMap(LOG_ENTRY_PROJECTION_MAP);
                break;
            case LOG_ENTRY_ID:
                qb.setTables(TrackLogData.LogEntry.TABLE_NAME);
                qb.setProjectionMap(LOG_ENTRY_PROJECTION_MAP);
                qb.appendWhere(TrackLogData.LogEntry._ID + "="
                        + uri.getPathSegments().get(
                                TrackLogData.LogEntry.LOG_ENTRY_ID_PATH_POSITION));
                break;
                
            case TIMING_ENTRY:
                qb.setTables(TrackLogData.TimingEntry.TABLE_NAME);
                qb.setProjectionMap(TIMING_ENTRY_PROJECTION_MAP);
                break;
            case TIMING_ENTRY_ID:
                qb.setTables(TrackLogData.TimingEntry.TABLE_NAME);
                qb.setProjectionMap(TIMING_ENTRY_PROJECTION_MAP);
                qb.appendWhere(TrackLogData.LogEntry._ID + "="
                        + uri.getPathSegments().get(
                                TrackLogData.TimingEntry.TIMING_ENTRY_ID_PATH_POSITION));
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
                return TrackLogData.Session.SESSION_TYPE;
            case SESSION_ID:
                return TrackLogData.Session.SESSION_ITEM_TYPE;
            case LOG_ENTRY:
                return TrackLogData.LogEntry.LOG_ENTRY_TYPE;
            case LOG_ENTRY_ID:
                return TrackLogData.LogEntry.LOG_ENTRY_ITEM_TYPE;
            case TIMING_ENTRY:
                return TrackLogData.TimingEntry.TIMING_ENTRY_TYPE;
            case TIMING_ENTRY_ID:
                return TrackLogData.TimingEntry.TIMING_ENTRY_ITEM_TYPE;
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
                rowId = db.insert(TrackLogData.Session.TABLE_NAME, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            TrackLogData.Session.CONTENT_ID_URI_BASE, rowId);
                }
                break;
            case LOG_ENTRY:
                rowId = db.insert(TrackLogData.LogEntry.TABLE_NAME, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            TrackLogData.LogEntry.CONTENT_ID_URI_BASE, rowId);
                }
                break;
            case TIMING_ENTRY:
                rowId = db.insert(TrackLogData.TimingEntry.TABLE_NAME, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            TrackLogData.TimingEntry.CONTENT_ID_URI_BASE, rowId);
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
                count = db.delete(TrackLogData.Session.TABLE_NAME,
                        where,
                        whereArgs);
                break;
            case SESSION_ID:
                count = doSingleDelete(TrackLogData.Session._ID,
                        uri.getPathSegments().get(TrackLogData.Session.SESSION_ID_PATH_POSITION),
                        TrackLogData.Session.TABLE_NAME, where, whereArgs);
                break;
            case LOG_ENTRY:
                count = db.delete(TrackLogData.LogEntry.TABLE_NAME,
                        where,
                        whereArgs);
            case LOG_ENTRY_ID:
                count = doSingleDelete(TrackLogData.LogEntry._ID,
                        uri.getPathSegments().get(TrackLogData.LogEntry.LOG_ENTRY_ID_PATH_POSITION),
                        TrackLogData.LogEntry.TABLE_NAME, where, whereArgs);
                break;
            case TIMING_ENTRY:
                count = db.delete(TrackLogData.TimingEntry.TABLE_NAME,
                        where,
                        whereArgs);
            case TIMING_ENTRY_ID:
                count = doSingleDelete(TrackLogData.TimingEntry._ID,
                        uri.getPathSegments().get(TrackLogData.TimingEntry.TIMING_ENTRY_ID_PATH_POSITION),
                        TrackLogData.TimingEntry.TABLE_NAME, where, whereArgs);
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
                tableName = TrackLogData.Session.TABLE_NAME;
                finalWhere = where;
                break;
            case SESSION_ID:
                tableName = TrackLogData.Session.TABLE_NAME;
                
                finalWhere = TrackLogData.Session._ID + " = " + uri.getPathSegments().get(
                        TrackLogData.Session.SESSION_ID_PATH_POSITION);;
                
                if (where != null) {
                    finalWhere = finalWhere + " AND (" + where + ")";
                }
                break;
            case LOG_ENTRY:
                tableName = TrackLogData.LogEntry.TABLE_NAME;
                finalWhere = where;
                break;
            case LOG_ENTRY_ID:
                tableName = TrackLogData.LogEntry.TABLE_NAME;
    
                finalWhere = TrackLogData.Session._ID + " = " + uri.getPathSegments().get(
                        TrackLogData.LogEntry.LOG_ENTRY_ID_PATH_POSITION);
                
                if (where != null) {
                    finalWhere = finalWhere + " AND (" + where + ")";
                }
            case TIMING_ENTRY:
                tableName = TrackLogData.TimingEntry.TABLE_NAME;
                finalWhere = where;
                break;
            case TIMING_ENTRY_ID:
                tableName = TrackLogData.TimingEntry.TABLE_NAME;
    
                finalWhere = TrackLogData.TimingEntry._ID + " = " + uri.getPathSegments().get(
                        TrackLogData.TimingEntry.TIMING_ENTRY_ID_PATH_POSITION);
                
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
                return TrackLogData.Session.DEFAULT_SORT_ORDER;
            case LOG_ENTRY:
            case LOG_ENTRY_ID:
                return TrackLogData.LogEntry.DEFAULT_SORT_ORDER;
            default:
                throw new IllegalArgumentException("Unknown URI type: " + uriType);
        }
    }
}