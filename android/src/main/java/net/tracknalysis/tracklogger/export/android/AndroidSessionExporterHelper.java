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

import java.util.Date;

import org.slf4j.Logger;

import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.provider.TrackLoggerDataUtil;
import android.content.ContentResolver;
import android.database.Cursor;

/**
 * Utility class providing helper methods to exporter implementations.
 *
 * @author David Valeri
 */
class AndroidSessionExporterHelper {
    
    private AndroidSessionExporterHelper() {
    }
    
    static Double getDoubleOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getDouble(columnIndex);
    }
    
    static Float getFloatOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getFloat(columnIndex);
    }
    
    static Integer getIntegerOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getInt(columnIndex);
    }
    
    static Long getLongOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getLong(columnIndex);
    }
    
    /**
     * Returns the date at which the session with the given ID started.
     *
     * @param sessionId the ID of the session to get the start time for
     * @param cr the content resolver to use for querying the DB
     * @param log the logger to use for writing log output
     *
     * @return the date that the session started.
     */
    static Date getSessionStartTime(int sessionId, ContentResolver cr, Logger log) {
        Cursor sessionCursor = null;
        
        try {
            sessionCursor = cr.query(TrackLoggerData.Session.CONTENT_URI,
                    new String[] {TrackLoggerData.Session.COLUMN_NAME_START_DATE}, 
                    TrackLoggerData.Session._ID + "= ?",
                    new String[] {Integer.toString(sessionId)},
                    null);
        
            if (!sessionCursor.moveToFirst()) {
                log.error("No session found for session ID '{}'.", sessionId);
                throw new IllegalStateException("No session found for ID " + sessionId);
            }
            
            String startDateString = sessionCursor.getString(sessionCursor
                    .getColumnIndex(TrackLoggerData.Session.COLUMN_NAME_START_DATE)); 
            
            return TrackLoggerDataUtil.parseSqlDate(startDateString);
        } finally {
            if (sessionCursor != null) {
                sessionCursor.close();
            }
        }
    }

}
