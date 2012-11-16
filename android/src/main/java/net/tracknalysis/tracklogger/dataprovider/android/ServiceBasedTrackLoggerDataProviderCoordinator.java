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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.android.io.BtSocketManager;
import net.tracknalysis.common.android.io.BtSocketManager.BtProfile;
import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.common.notification.NoOpNotificationStrategy;
import net.tracknalysis.location.LocationManager;
import net.tracknalysis.location.LocationManagerNotificationType;
import net.tracknalysis.location.Route;
import net.tracknalysis.location.Waypoint;
import net.tracknalysis.location.nmea.NmeaLocationManager;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.activity.LogActivity;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.dataprovider.AccelDataProvider;
import net.tracknalysis.tracklogger.dataprovider.EcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.LocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TimingDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.ecu.MegasquirtEcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.location.LocationManagerLocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.timing.RouteManagerTimingDataProvider;
import net.tracknalysis.tracklogger.model.LogEntry;
import net.tracknalysis.tracklogger.model.TimingEntry;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.provider.TrackLoggerDataUtil;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

/**
 * Implements the lowest level details of the coordinator specific to persisting data and managing
 * life cycle in the Android environment.  Serves as a delegate to a {@link DataProviderCoordinatorManagerService}.
 *  
 * @author David Valeri
 */
public class ServiceBasedTrackLoggerDataProviderCoordinator extends
        TrackLoggerDataProviderCoordinator {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceBasedTrackLoggerDataProviderCoordinator.class);
    
    private final Context context;
    private final BluetoothAdapter btAdapter;
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DataProviderCoordinatorManagerService dataProviderCoordinatorService;
    private Uri splitMarkerSetUri;
    private AtomicLong logEntryCounter = new AtomicLong();
    
    // Accel
    protected AccelDataProvider accelDataProvider;
    
    // Location
    protected SocketManager locationSocketManager;
    protected LocationManager locationManager;
    protected LocationDataProvider locationDataProvider;
    
    // ECU
    protected SocketManager ecuSocketManager;
    protected EcuDataProvider ecuDataProvider;
    
    // Timing
    protected TimingDataProvider timingDataProvider;

    public ServiceBasedTrackLoggerDataProviderCoordinator(DataProviderCoordinatorManagerService dataProviderCoordinatorService, 
            Context context, BluetoothAdapter btAdapter, Uri splitMarkerSetUri) {
        this.context = context;
        this.dataProviderCoordinatorService = dataProviderCoordinatorService;
        this.splitMarkerSetUri = splitMarkerSetUri;
        this.btAdapter = btAdapter;
    }
    
    /**
     * Initializes the data providers and other resources used by the coordinator.
     */
    protected void initProviders(Context context, BluetoothAdapter btAdapter) {
        Configuration config = ConfigurationFactory.getInstance().getConfiguration();
        
        try {
            initLocationManager(config, btAdapter);
            initLocationDataProvider(config, btAdapter);
            initAccelDataProvider(context, config, btAdapter);
            initEcuDataProvider(config, btAdapter);
            initTimingDataProvider(config, btAdapter);
        } catch (RuntimeException e) {
            cleanup();
            throw e;
        }
    }
    
    /**
     * Initializes the location manager.  By default, initializes an NMEA based location manager
     * reading from a BT serial port.
     *
     * @param config the application configuration
     * @param btAdapter the adapter to use
     */
    protected void initLocationManager(Configuration config, BluetoothAdapter btAdapter) {
        locationSocketManager = new BtSocketManager(config.getLocationBtAddress(),
                btAdapter, BtProfile.SPP);
        locationManager = new NmeaLocationManager(locationSocketManager,
                new NoOpNotificationStrategy<LocationManagerNotificationType>());
    }
    
    /**
     * Initializes the location data provider.  By default, initializes one based on the location manager.
     * 
     * @param config the application configuration
     * @param btAdapter the adapter to use
     * 
     * @see #initLocationManager(Configuration, BluetoothAdapter)
     */
    protected void initLocationDataProvider(Configuration config, BluetoothAdapter btAdapter) {
        locationDataProvider = new LocationManagerLocationDataProvider(locationManager);
    }
    
    /**
     * Initializes the accel data provider.  By default, initializes one based on the OS provided
     * acceleration data.
     * 
     * @param context the context for the app
     * @param config the application configuration
     * @param btAdapter the adapter to use
     */
    protected void initAccelDataProvider(Context context, Configuration config, BluetoothAdapter btAdapter) {
        accelDataProvider = new AndroidAccelDataProvider(context);
    }
    
    /**
     * Initializes the ECU data provider.  By default, initializes one based on MegaCom using a BT
     * serial port.
     *
     * @param config the application configuration
     * @param btAdapter the adapter to use
     */
    protected void initEcuDataProvider(Configuration config, BluetoothAdapter btAdapter) {
        if (config.isEcuEnabled()) {
            ecuSocketManager = new BtSocketManager(config.getEcuBtAddress(),
                    btAdapter, BtProfile.SPP);
            
            String dataDir = config.getDataDirectory();
            // TODO configurability for IO logging.
            ecuDataProvider = new MegasquirtEcuDataProvider(ecuSocketManager, null); //new File(dataDir, "megasquirt.log"));
        }
    }
    
    /**
     * Initializes the timing data provider.  By default, initializes one based on the route manager
     * from the previously initialized location manager.
     *
     * @param config the application configuration
     * @param btAdapter the adapter to use
     * 
     * @see #initLocationManager(Configuration, BluetoothAdapter)
     */
    protected void initTimingDataProvider(Configuration config, BluetoothAdapter btAdapter) {
        timingDataProvider = new RouteManagerTimingDataProvider(
                locationManager.getRouteManager(), getRoute(config));
    }
    
    protected Route getRoute(Configuration config) {
        
        if (splitMarkerSetUri == null) {
            throw new IllegalStateException("No split marker set URI provided.");
        }
        
        Cursor cursor = null;
        
        List<Waypoint> waypoints = new LinkedList<Waypoint>();
        int counter = 1;
        try {
            int splitMarkerSetId = Integer.valueOf(splitMarkerSetUri
                    .getPathSegments().get(
                            TrackLoggerData.SplitMarkerSet.ID_PATH_POSITION));
            cursor = context.getContentResolver().query(
                    TrackLoggerData.SplitMarker.CONTENT_URI,
                    null,
                    TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID + " = ?",
                    new String[] {String.valueOf(splitMarkerSetId)},
                    null);
            
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    
                    double lat = cursor
                            .getDouble(cursor
                                    .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE));
                    
                    double lon = cursor
                            .getDouble(cursor
                                    .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE));
                    
                    waypoints.add(
                            new Waypoint(
                                    String.valueOf(counter++),
                                    lat,
                                    lon));
                    
                    cursor.moveToNext();
                }
            } else {
                throw new IllegalStateException(
                        "No split markers found in split marker set with URI [" + splitMarkerSetUri + "].");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return new Route("Split Markers", waypoints);
    }   
    
    @Override
    @SuppressWarnings("deprecation") // Fix not available until API 11
    protected void preStart() {
        initProviders(context, btAdapter);
        
        Notification notification = new Notification(R.drawable.icon, this.dataProviderCoordinatorService.getText(R.string.log_notification_message),
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(getContext(), LogActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(getContext(), this.dataProviderCoordinatorService.getText(R.string.log_notification_title),
                this.dataProviderCoordinatorService.getText(R.string.log_notification_message), pendingIntent);
        this.dataProviderCoordinatorService.startForeground(DataProviderCoordinatorManagerService.ONGOING_NOTIFICATION, notification);
        super.preStart();
    }
    
    /**
     * Starting the location manager after the coordinator is ready so we don't emit timing events
     * that don't get logged.
     */
    @Override
    protected void postStart() {
        locationManager.start();
    }
    
    protected void cleanup() {
        super.cleanup();
        
        try {
            if (locationManager != null) {
                locationManager.stop();
            }
        } catch(Exception e) {
            LOG.error("Error cleaning up location manager.", e);
        } finally {
            silentSocketManagerDisconnect(locationSocketManager);
        }
        
        silentSocketManagerDisconnect(ecuSocketManager);
    }
    
    @Override
    protected void postStop() {
        this.dataProviderCoordinatorService.stopForeground(true);
        super.postStop();
    }
    
    protected final synchronized String formatAsSqlDate(Date date) {
        return sqlDateFormat.format(date);
    }
    
    protected void silentSocketManagerDisconnect(SocketManager socketManager) {
        if (socketManager != null) {
            try {
                socketManager.disconnect();
            } catch (Exception e1) {
                LOG.warn("Error disconnecting BlueTooth connection with manager.", e1);
            }
        }
    }
    
    protected Context getContext() {
        return context;
    }
    
    @Override
    protected AccelDataProvider getAccelDataProvider() {
        return accelDataProvider;
    }
    
    @Override
    protected LocationDataProvider getLocationDataProvider() {
        return locationDataProvider;
    }
    
    @Override
    protected EcuDataProvider getEcuDataProvider() {
        return ecuDataProvider;
    }
    
    @Override
    protected TimingDataProvider getTimingDataProvider() {
        return timingDataProvider;
    }

    @Override
    protected int createSession() {
        Date now = new Date();
        
        ContentValues cv = new ContentValues();
        cv.put(TrackLoggerData.Session.COLUMN_NAME_START_DATE,
                formatAsSqlDate(now));
        cv.put(TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE,
                formatAsSqlDate(now));
        cv.put(TrackLoggerData.Session.COLUMN_NAME_SPLIT_MARKER_SET_ID,
                Integer.valueOf(splitMarkerSetUri
                        .getPathSegments().get(
                                TrackLoggerData.SplitMarkerSet.ID_PATH_POSITION)));
        
        Uri currentSessionUri = context.getContentResolver().insert(
                TrackLoggerData.Session.CONTENT_URI, cv);
        return Integer.parseInt(currentSessionUri.getPathSegments().get(
                TrackLoggerData.Session.ID_PATH_POSITION));
    }

    @Override
    protected void openSession(int sessionId) {
        Date now = new Date();
        
        Uri currentSessionUri = ContentUris.withAppendedId(
                TrackLoggerData.Session.CONTENT_ID_URI_BASE, sessionId);
        
        Cursor cursor = null;
        
        try {
            cursor = context.getContentResolver().query(currentSessionUri,
                    null, null, null, null);
            
            if (cursor.getCount() != 1) {
                throw new IllegalStateException("Session with URI [" + currentSessionUri + "] not found.");
            } else {
                cursor.moveToFirst();
                splitMarkerSetUri = ContentUris.withAppendedId(
                        TrackLoggerData.SplitMarkerSet.CONTENT_URI,
                        cursor.getInt(cursor
                                .getColumnIndex(
                                        TrackLoggerData.Session.COLUMN_NAME_SPLIT_MARKER_SET_ID)));
            }
            
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        ContentValues cv = new ContentValues();
        cv.put(TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE,
                formatAsSqlDate(now));
        
        if (context.getContentResolver().update(
                currentSessionUri, cv, null, null) != 1) {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void storeLogEntry(LogEntry logEntry) {
        LOG.debug("Writing log entry {} to session with ID {}.",
                logEntryCounter.getAndIncrement(), logEntry.getSessionId());
        
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = TrackLoggerDataUtil.toContentValues(logEntry); 
        cr.insert(TrackLoggerData.LogEntry.CONTENT_URI, cv);
    }

    @Override
    protected void storeTimingEntry(TimingEntry timingEntry) {
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = TrackLoggerDataUtil.toContentValues(timingEntry);
        cr.insert(TrackLoggerData.TimingEntry.CONTENT_URI, cv);
    }
}
