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
package net.tracknalysis.tracklogger.test;

import org.apache.log4j.Level;

import net.tracknalysis.common.notification.NoOpNotificationStrategy;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.location.LocationManagerNotificationType;
import net.tracknalysis.location.nmea.NmeaLocationManager;
import net.tracknalysis.location.nmea.NmeaTestSocketManager;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.activity.LogActivity;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.android.AndroidTrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorFactory;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorService;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorService.LocalBinder;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

/**
 * @author David Valeri
 */
public class LogActivityIntegrationTest extends ActivityInstrumentationTestCase2<LogActivity> {
    
    private NmeaTestSocketManager ntsm;
    private Context context;
    private ServiceConnection serviceConnection;
    private DataProviderCoordinatorService dpcs;
    private AndroidTrackLoggerDataProviderCoordinator dpcsDelegate;
    private Configuration configuration;
    
    private TextView elapsedLapTime;
    private TextView lapNumber;
    private TextView elapsedSessionTime;
    private TextView lastSplitTime;
    private TextView lastSplitTimeDelta;
    private TextView lastLapTime;
    private TextView lastLapTimeDelta;
    
    public LogActivityIntegrationTest() {
        super(LogActivity.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        context = this.getInstrumentation().getTargetContext().getApplicationContext();
        
        configuration = ConfigurationFactory.getInstance().getConfiguration();
        configuration.setRootLogLevel(Level.INFO);
        configuration.setLogToFile(false);
        
        ntsm = new NmeaTestSocketManager(
                getClass().getResourceAsStream(
                        "/NMEA-Test-Data.txt"), null);
        
        // Send teaser to get into the ready state
        ntsm.sendSentences(2, 0);
        
        DataProviderCoordinatorFactory.setInstance(
                new DataProviderCoordinatorFactory() {
                    @Override
                    public DataProviderCoordinator createDataProviderCoordinator(
                            DataProviderCoordinatorService dataProviderCoordinatorService,
                            NotificationStrategy<DataProviderCoordinator.NotificationType> notificationStrategy,
                            Context context, BluetoothAdapter btAdapter) {
                        
                        dpcsDelegate = new AndroidTrackLoggerDataProviderCoordinator(
                                dataProviderCoordinatorService, notificationStrategy, context, btAdapter) {
                            
                            @Override
                            protected void initLocationManager(Configuration config, BluetoothAdapter btAdapter) {
                                locationSocketManager = ntsm;
                                locationManager = new NmeaLocationManager(locationSocketManager,
                                        new NoOpNotificationStrategy<LocationManagerNotificationType>());
                            }
                        };
                        
                        return dpcsDelegate;
                    }
                }
        );
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder binder = (LocalBinder) service;
                dpcs = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        };
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (context != null) {
            context.unbindService(serviceConnection);
        }
    }
    
    public void testDefaultView() throws Throwable {
        
        configuration.setLogLayoutId(R.layout.log_default);
        
        LogActivity logActivity = initActivity();
        
        ContentResolver cr = logActivity.getApplication().getContentResolver();
        
        // Send at a pace of about 10Hz.  Pausing only to check on the state of the UI at key points.
        
        int delay = 43;
        ntsm.sendSentences(100, delay); // Starts trigger fires during
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("", lastSplitTime.getText());
                assertEquals("", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay); // First split fires during 16.359
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:16.359", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay); // Second split fires during 12.822
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:12.822", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay); // Third split fires during 45.077
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:45.077", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay); // Fourth split fires during 11.296
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:11.296", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay); // Fifth split / lap fires during and data finishes 36.016
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime));
                assertEquals("2:01.570", lastLapTime.getText());
                assertEquals("0:00.000", lastLapTimeDelta.getText());
                assertEquals("0:36.016", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("2", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        Thread.sleep(5000);
        
        // Finish the activity
        logActivity.finish();
        
        Thread.sleep(2000);
        
        // Ensure that the thing was shut down properly when the activity ended
        assertFalse(dpcs.isInitialized());
        assertFalse(dpcsDelegate.isRunning());
                
        // After all data is sent, we will want to check that the data ended up in the DB as appropriate.
        
        // Get the ID of the session that we just finished
        int sessionId = dpcsDelegate.getCurrentSessionId();
        
        // Check that the session is in the DB
        Uri currentSessionUri = ContentUris.withAppendedId(
                TrackLoggerData.Session.CONTENT_ID_URI_BASE, sessionId);
        Cursor sessionCursor = cr.query(currentSessionUri, null, null, null, null);
        assertEquals(1, sessionCursor.getCount());
        sessionCursor.close();
        
        // Check that all of the log entries were written
        Cursor logEntryCursor = cr.query(
                TrackLoggerData.LogEntry.CONTENT_URI,
                null, 
                TrackLoggerData.LogEntry.COLUMN_NAME_SESSION_ID + "= ?",
                new String[] { Integer.toString(sessionId) },
                TrackLoggerData.LogEntry.DEFAULT_SORT_ORDER);
        // Use a range because we can't be sure how many NMEA sentences get dropped due to
        // timing issues.
        assertTrue(logEntryCursor.getCount() > 1260 && logEntryCursor.getCount() < 1275);
        
        // TODO examine the data for several entries
        
        logEntryCursor.close();
        
        // Check that all of the timing entries were written
        Cursor timingEntryCursor = cr.query(
                TrackLoggerData.TimingEntry.CONTENT_URI, null,
                TrackLoggerData.TimingEntry.COLUMN_NAME_SESSION_ID + "= ?",
                new String[] { Integer.toString(sessionId) },
                TrackLoggerData.TimingEntry.DEFAULT_SORT_ORDER);
        assertEquals(6, timingEntryCursor.getCount());
        
        // TODO examine the data for each entry
        
        timingEntryCursor.close();
    }

    protected LogActivity initActivity() {
        LogActivity logActivity = getActivity();
        initializeUiFields(logActivity);
        
        assertTrue(context.bindService(
                new Intent(logActivity, DataProviderCoordinatorService.class),
                serviceConnection,
                Context.BIND_NOT_FOREGROUND));
        return logActivity;
    }
    
    protected void initializeUiFields(LogActivity logActivity) {
        elapsedLapTime = (TextView) logActivity.findViewById(R.id.log_elapsed_lap_time_value);
        lapNumber = (TextView) logActivity.findViewById(R.id.log_lap_number_value);
        elapsedSessionTime = (TextView) logActivity.findViewById(R.id.log_elapsed_session_time_value);
        lastSplitTime = (TextView) logActivity.findViewById(R.id.log_last_split_time_value);
        lastSplitTimeDelta = (TextView) logActivity.findViewById(R.id.log_last_split_time_delta_value);
        lastLapTime = (TextView) logActivity.findViewById(R.id.log_last_lap_time_value);
        lastLapTimeDelta = (TextView) logActivity.findViewById(R.id.log_last_lap_time_delta_value);
    }
}
