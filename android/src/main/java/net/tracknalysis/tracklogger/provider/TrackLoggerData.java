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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the TrackLogger content provider and its clients.
 * 
 * @author David Valeri
 */
public final class TrackLoggerData {
    public static final String AUTHORITY = "net.tracknalysis.trackLogger";

    /**
     * Hidden in non-instantiable class.
     */
    private TrackLoggerData() {
    }

    /**
     * Session table contract.
     */
    public static final class Session implements BaseColumns {

        // This class cannot be instantiated
        private Session() {}

        /**
         * The table name for Session.
         */
        public static final String TABLE_NAME = "session";
        
        // MIME ////////////
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of Sessions.
         */
        public static final String SESSION_TYPE = 
                "vnd.android.cursor.dir/net.tracknalysis.tracklogger.session";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * Session.
         */
        public static final String SESSION_ITEM_TYPE = 
                "vnd.android.cursor.item/net.tracknalysis.tracklogger.session";

        // URI ////////////

        /**
         * The scheme part for this provider's URI.
         */
        private static final String SCHEME = "content://";
        
        /**
         * Path part for the Session URI.
         */
        private static final String PATH_SESSION = "/session";

        /**
         * Path part for the Session ID URI.
         */
        private static final String PATH_SESSION_ID = "/session/";

        /**
         * 0-relative position of a Session ID segment in the path part of a Session ID URI.
         */
        public static final int ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_SESSION);

        /**
         * The content URI base for a single Session. Callers must
         * append a numeric Session ID to this URI to retrieve a Session.
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_SESSION_ID);

        /**
         * The content URI match pattern for a single Session, specified by its ID. Use this
         * to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_SESSION_ID + "/#");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "start_date DESC";

        // Columns ////////////

        /**
         * Column name for the start date of the session.
         * <P>Type: DATE</P>
         */
        public static final String COLUMN_NAME_START_DATE = "start_date";

        /**
         * Column name for the last modified date of the session.
         * <P>Type: DATE</P>
         */
        public static final String COLUMN_NAME_LAST_MODIFIED_DATE = "last_modified_date";
        
        /**
         * Column name for the split marker set ID of the split marker set in use with the session.
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_SPLIT_MARKER_SET_ID = "split_marker_set_id";
    }
    
    /**
     * Log entry table contract.
     */
    public static final class LogEntry implements BaseColumns {

        // This class cannot be instantiated
        private LogEntry() {}

        /**
         * The table name for a log entry.
         */
        public static final String TABLE_NAME = "log_entry";
        
        // MIME ////////////
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of log entry.
         */
        public static final String LOG_ENTRY_TYPE = 
                "vnd.android.cursor.dir/net.tracknalysis.tracklogger.logentry";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * log entry.
         */
        public static final String LOG_ENTRY_ITEM_TYPE = 
                "vnd.android.cursor.item/net.tracknalysis.tracklogger.logentry";
        
        // URI ////////////

        /**
         * The scheme part for this provider's URI.
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the LogEntry URI.
         */
        private static final String PATH_LOG_ENTRY = "/logentry";

        /**
         * Path part for the LogEntry ID URI.
         */
        private static final String PATH_LOG_ENTRY_ID = "/logentry/";

        /**
         * 0-relative position of a LogEntry ID segment in the path part of a LogEntry ID URI.
         */
        public static final int ID_PATH_POSITION = 1;


        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_LOG_ENTRY);

        /**
         * The content URI base for a single LogEntry. Callers must
         * append a numeric LogEntry ID to this URI to retrieve a LogEntry.
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_LOG_ENTRY_ID);

        /**
         * The content URI match pattern for a single LogEntry, specified by its ID. Use this
         * to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_LOG_ENTRY_ID + "/#");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        // Columns ////////////
        
        /**
         * Column name for the {@link Session} that this entry belongs to.
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_SESSION_ID = "session_id";
        
        /**
         * Column name for the millisecond in the day UTC time that this entry was generated.
         * This column is useful for synchronizing captured data in other tables.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_SYNCH_TIMESTAMP = "synch_timestamp";
        
        /**
         * Column name for the time stamp of the acceleration data capture in
         * milliseconds since January 1, 1970 00:00:00 UTC.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_ACCEL_CAPTURE_TIMESTAMP = "accel_capture_timestamp";

        /**
         * Column name for the start longitudinal acceleration of the entry in m/s^2.
         * <P>Type: FLOAT</P>
         */
        public static final String COLUMN_NAME_LONGITUDINAL_ACCEL = "longitudinal_accel";

        /**
         * Column name for the lateral acceleration of the entry in m/s^2.
         * <P>Type: FLOAT</P>
         */
        public static final String COLUMN_NAME_LATERAL_ACCEL = "lateral_accel";
        
        /**
         * Column name for the vertical acceleration of the entry in m/s^2.
         * <P>Type: FLOAT</P>
         */
        public static final String COLUMN_NAME_VERTICAL_ACCEL = "vertical_accel";
        
        /**
         * Column name for the time stamp of the location data capture in
         * milliseconds since January 1, 1970 00:00:00 UTC.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_LOCATION_CAPTURE_TIMESTAMP = "location_capture_timestamp";
        
        /**
         * Column name for the millisecond offset into the day of the location data capture in
         * UTC time.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_LOCATION_TIME_IN_DAY = "location_time_in_day";
        
        /**
         * Column name for latitude in degrees.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        
        /**
         * Column name for longitude in degrees.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        
        /**
         * Column name for altitude above MSL in meters.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_ALTITUDE = "altitude";
        
        /**
         * Column name for speed in meters per second.
         * <p>Type: FLOAT</p>
         */
        public static final String COLUMN_NAME_SPEED = "speed";
        
        /**
         * Column name for bearing in degrees.
         * <p>Type: FLOAT</p>
         */
        public static final String COLUMN_NAME_BEARING = "bearing";
        
        /**
         * Column name for the time stamp of the ECU data capture in
         * milliseconds since January 1, 1970 00:00:00 UTC.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_ECU_CAPTURE_TIMESTAMP = "ecu_capture_timestamp";
        
        /**
         * Column name for RPMs.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_RPM = "rpm";
        
        /**
         * Column name for MAP in kPa.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_MAP = "map";
        
        /**
         * Column name for manifold gauge pressure in kPa.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_MGP = "mgp";
        
        /**
         * Column name for throttle position % (0-100).
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_THROTTLE_POSITION = "throttle_position";
        
        /**
         * Column name for air fuel ratio.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_AFR = "afr";
        
        /**
         * Column name for the intake manifold air temperature in degrees Celsius.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_MAT = "mat";
        
        /**
         * Column name for the coolant temperature in degrees Celsius.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_CLT = "clt";
        
        /**
         * Column name for the ignition advance in degrees BTDC.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_IGNITION_ADVANCE = "ignition_advance";
        
        /**
         * Column name for the battery voltage in Volts.
         * <p>Type: DOUBLE</p>
         */
        public static final String COLUMN_NAME_BATTERY_VOLTAGE = "battery_voltage";
    }
    
    /**
     * Timing entry table contract.
     */
    public static final class TimingEntry implements BaseColumns {

        // This class cannot be instantiated
        private TimingEntry() {}

        /**
         * The table name.
         */
        public static final String TABLE_NAME = "timing_entry";
        
        // MIME ////////////
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of instances.
         */
        public static final String TYPE = 
                "vnd.android.cursor.dir/net.tracknalysis.tracklogger.timingentry";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * instance.
         */
        public static final String ITEM_TYPE = 
                "vnd.android.cursor.item/net.tracknalysis.tracklogger.timingentry";
        
        // URI ////////////

        /**
         * The scheme part for this provider's URI.
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the instance URI.
         */
        private static final String PATH = "/timingentry";

        /**
         * Path part for the instance ID URI.
         */
        private static final String PATH_ID = "/timingentry/";

        /**
         * 0-relative position of a instance ID segment in the path part of a instance ID URI.
         */
        public static final int ID_PATH_POSITION = 1;


        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH);

        /**
         * The content URI base for a single instance. Callers must
         * append a numeric instance ID to this URI to retrieve an instance.
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID);

        /**
         * The content URI match pattern for a single instance, specified by its ID. Use this
         * to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID + "/#");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        // Columns ////////////
        
        /**
         * Column name for the {@link Session} that this entry belongs to.
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_SESSION_ID = "session_id";
        
        /**
         * Column name for the millisecond in the day UTC time that this entry was generated.
         * This column is useful for synchronizing captured data in other tables.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_SYNCH_TIMESTAMP = "synch_timestamp";
        
        /**
         * Column name for the time stamp of the timing data capture in
         * milliseconds since January 1, 1970 00:00:00 UTC.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_CAPTURE_TIMESTAMP = "capture_timestamp";
        
        /**
         * Column name for the millisecond offset into the day of the timing data capture in
         * UTC time.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_TIME_IN_DAY = "time_in_day";
        
        /**
         * Column name for the lap number.
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_LAP = "lap";
        
        /**
         * Column name for the lap elapsed time.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_LAP_TIME = "lap_time";
        
        /**
         * Column name for the split index.
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_SPLIT_INDEX = "split_index";
        
        /**
         * Column name for the split elapsed time.
         * <P>Type: BIGINT</P>
         */
        public static final String COLUMN_NAME_SPLIT_TIME = "split_time";
    }
    
    /**
     * Split Marker Set table contract.
     */
    public static final class SplitMarkerSet implements BaseColumns {

        // This class cannot be instantiated
        private SplitMarkerSet() {}

        /**
         * The table name.
         */
        public static final String TABLE_NAME = "split_marker_set";
        
        // MIME ////////////
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of instances.
         */
        public static final String TYPE = 
                "vnd.android.cursor.dir/net.tracknalysis.tracklogger.splitmarkerset";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * instance.
         */
        public static final String ITEM_TYPE = 
                "vnd.android.cursor.item/net.tracknalysis.tracklogger.splitmarkerset";
        
        // URI ////////////

        /**
         * The scheme part for this provider's URI.
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the Split Marker Set URI.
         */
        private static final String PATH = "/splitmarkerset";

        /**
         * Path part for the Split Marker Set ID URI.
         */
        private static final String PATH_ID = "/splitmarkerset/";

        /**
         * 0-relative position of the ID segment in the path part of an ID URI.
         */
        public static final int ID_PATH_POSITION = 1;


        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH);

        /**
         * The content URI base for a single instance. Callers must
         * append a numeric ID to this URI to retrieve an instance.
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID);

        /**
         * The content URI match pattern for a single instance, specified by its ID. Use this
         * to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID + "/#");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        // Columns ////////////
        
        /**
         * Column name for the name of this instance.
         * <P>Type: VARCHAR(50)</P>
         */
        public static final String COLUMN_NAME_NAME = "name";
    }
    
    /**
     * Split Marker table contract.
     */
    public static final class SplitMarker implements BaseColumns {

        // This class cannot be instantiated
        private SplitMarker() {}

        /**
         * The table name.
         */
        public static final String TABLE_NAME = "split_marker";
        
        // MIME ////////////
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of instances.
         */
        public static final String TYPE = 
                "vnd.android.cursor.dir/net.tracknalysis.tracklogger.splitmarker";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * instance.
         */
        public static final String ITEM_TYPE = 
                "vnd.android.cursor.item/net.tracknalysis.tracklogger.splitmarker";
        
        // URI ////////////

        /**
         * The scheme part for this provider's URI.
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the Split Marker URI.
         */
        private static final String PATH = "/splitmarker";

        /**
         * Path part for the Split Maker ID URI.
         */
        private static final String PATH_ID = "/splitmarker/";

        /**
         * 0-relative position of the ID segment in the path part of an ID URI.
         */
        public static final int ID_PATH_POSITION = 1;


        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH);

        /**
         * The content URI base for a single instance. Callers must
         * append a numeric ID to this URI to retrieve an instance.
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID);

        /**
         * The content URI match pattern for a single instance, specified by its ID. Use this
         * to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID + "/#");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "order_index ASC";

        // Columns ////////////
        
        /**
         * Column name for the {@link SplitMarkerSet} ID that this split marker belongs to.
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_SPLIT_MARKER_SET_ID = "split_marker_set_id";
        
        /**
         * Column name for the name of this instance.
         * <P>Type: VARCHAR(50)</P>
         */
        public static final String COLUMN_NAME_NAME = "name";
        
        /**
         * Column name for the index of this split marker in the route.
         * <P>Type: INTEGER</P>
         */
        public static final String COLUMN_NAME_ORDER_INDEX = "order_index";
        
        /**
         * Column name for the latitude of this instance.
         * <P>Type: DOUBLE</P>
         */
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        
        /**
         * Column name for the longitude of this instance.
         * <P>Type: DOUBLE</P>
         */
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
    }
}
