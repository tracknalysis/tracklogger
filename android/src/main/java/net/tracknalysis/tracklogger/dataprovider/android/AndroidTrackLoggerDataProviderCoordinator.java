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
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.android.io.BtSocketManager;
import net.tracknalysis.common.android.io.BtSocketManager.BtProfile;
import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.common.notification.NoOpNotificationStrategy;
import net.tracknalysis.common.notification.NotificationStrategy;
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
import net.tracknalysis.tracklogger.dataprovider.TimingData;
import net.tracknalysis.tracklogger.dataprovider.TimingDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.ecu.MegasquirtEcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.location.LocationManagerLocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.timing.RouteManagerTimingDataProvider;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.view.WindowManager;

/**
 * Implements the lowest level details of the coordinator specific to persisting data and managing
 * life cycle in the Android environment.
 *  
 * @author David Valeri
 */
public class AndroidTrackLoggerDataProviderCoordinator extends
        TrackLoggerDataProviderCoordinator {
    
    private static final Logger LOG = LoggerFactory.getLogger(AndroidTrackLoggerDataProviderCoordinator.class);
    
    private final Context context;
    private final SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DataProviderCoordinatorService dataProviderCoordinatorService;
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

    public AndroidTrackLoggerDataProviderCoordinator(DataProviderCoordinatorService dataProviderCoordinatorService, 
            NotificationStrategy<NotificationType> notificationStrategy, Context context, BluetoothAdapter btAdapter) {
        super(notificationStrategy);
        this.context = context;
        this.dataProviderCoordinatorService = dataProviderCoordinatorService;
        initProviders(context, btAdapter);
    }
    
    /**
     * Initializes the data providers and other resources used by the coordinator.
     */
    protected void initProviders(Context context, BluetoothAdapter btAdapter) {
        Configuration config = ConfigurationFactory.getInstance().getConfiguration();
        
        try {
            initLocationManager(config, btAdapter);
            initLocationDataProvider(config, btAdapter);
            initAccelDataProvider(config, btAdapter);
            initEcuDataProvider(config, btAdapter);
            initTimingDataProvider(config, btAdapter);
        } catch (RuntimeException e) {
            cleanup();
            throw e;
        }
    }
    
    /**
     * Initializes the location manager.  By default, initializes an NMEA based location manager
     * reading from the a BT serial port.
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
     * @param config the application configuration
     * @param btAdapter the adapter to use
     */
    protected void initAccelDataProvider(Configuration config, BluetoothAdapter btAdapter) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        accelDataProvider = new AndroidAccelDataProvider(sensorManager, windowManager);
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
            ecuDataProvider = new MegasquirtEcuDataProvider(ecuSocketManager);
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
        return // TODO route from external source
                new Route("My Route", Arrays.asList(
                        new Waypoint("1", 38.979896545410156d, -77.54102325439453d),
                        new Waypoint("2", 38.98295974731445d, -77.53973388671875d),
                        new Waypoint("3", 38.982906341552734d, -77.54007720947266d),
                        new Waypoint("4", 38.972618103027344d, -77.54145050048828d),
                        new Waypoint("5", 38.97257995605469d, -77.5412826538086d)));
    }   
    
    @Override
    protected void preStart() {
        Notification notification = new Notification(R.drawable.icon, this.dataProviderCoordinatorService.getText(R.string.log_notification_message),
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(getContext(), LogActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(getContext(), this.dataProviderCoordinatorService.getText(R.string.log_notification_title),
                this.dataProviderCoordinatorService.getText(R.string.log_notification_message), pendingIntent);
        this.dataProviderCoordinatorService.startForeground(DataProviderCoordinatorService.ONGOING_NOTIFICATION, notification);
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
        LOG.debug("Writing log entry {} to session with ID {}.", logEntryCounter.getAndIncrement(), sessionId);
        
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
                logEntry.locationData.getBearing());
        
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
