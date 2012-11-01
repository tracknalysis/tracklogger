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

import java.io.IOException;

import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.common.notification.NoOpNotificationStrategy;
import net.tracknalysis.ecu.ms.io.DebugLogReaderMegasquirtIoManager;
import net.tracknalysis.ecu.ms.io.MegasquirtIoManager;
import net.tracknalysis.location.LocationManagerNotificationType;
import net.tracknalysis.location.nmea.NmeaLocationManager;
import net.tracknalysis.location.nmea.NmeaTestSocketManager;
import net.tracknalysis.tracklogger.activity.LogActivity;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorManagerService;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorManagerService.LocalBinder;
import net.tracknalysis.tracklogger.dataprovider.android.ServiceBasedTrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorFactory;
import net.tracknalysis.tracklogger.dataprovider.ecu.MegasquirtEcuDataProvider;
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

/**
 * @author David Valeri
 */
public class LogActivityIntegrationTest extends AbstractLogActivityTest {
    
    private NmeaTestSocketManager ntsm;
    private MegasquirtIoManager msiom;
    private ServiceConnection serviceConnection;
    private DataProviderCoordinatorManagerService dpcms;
    private ServiceBasedTrackLoggerDataProviderCoordinator dpcsDelegate;
    
    @Override
    protected void init() throws Exception {
        configuration = ConfigurationFactory.getInstance().getConfiguration();
        configuration.setEcuEnabled(true);
        
        ntsm = new NmeaTestSocketManager(
                getClass().getResourceAsStream(
                        "/NMEA-Test-Data.txt"), null);
        
        msiom = new DebugLogReaderMegasquirtIoManager(getClass()
                .getResourceAsStream("/MS-Test-Data.txt"));
        
        // Send teaser to get into the ready state
        ntsm.sendSentences(2, 0);
        
        // Rewire a factory that returns a subclass of the standard coordinator with our custom
        // source for location and ECU data.
        DataProviderCoordinatorFactory.setInstance(
                new DataProviderCoordinatorFactory() {
                    @Override
                    public DataProviderCoordinator createDataProviderCoordinator(
                            DataProviderCoordinatorManagerService dataProviderCoordinatorService,
                            Context context, BluetoothAdapter btAdapter,
                            Uri splitMarkerSetUri) {
                        
                        dpcsDelegate = new ServiceBasedTrackLoggerDataProviderCoordinator(
                                dataProviderCoordinatorService, context, btAdapter, splitMarkerSetUri) {
                            
                            @Override
                            protected void initLocationManager(Configuration config, BluetoothAdapter btAdapter) {
                                locationSocketManager = ntsm;
                                locationManager = new NmeaLocationManager(locationSocketManager,
                                        new NoOpNotificationStrategy<LocationManagerNotificationType>());
                            }
                            
                            protected void initEcuDataProvider(Configuration config, BluetoothAdapter btAdapter) {
                                if (config.isEcuEnabled()) {
                                    
                                    ecuDataProvider = new MegasquirtEcuDataProvider(ecuSocketManager, null) {
                                        @Override
                                        protected MegasquirtIoManager createIoManager(
                                                SocketManager socketManager) throws IOException {
                                            return msiom;
                                        }
                                    };
                                }
                            };
                        };
                        
                        return dpcsDelegate;
                    }
                }
        );
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, android.os.IBinder service) {
                LocalBinder binder = (LocalBinder) service;
                dpcms = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        };
    }
    
    public void testDefaultView() throws Throwable {
        
        LogActivity logActivity = initActivity();
        triggerStart(logActivity);
        
        // TODO have to sleep because of long startup time and hung UI
        Thread.sleep(2000);
        
        ContentResolver cr = logActivity.getApplication().getContentResolver();
        
        // Send at a pace of about 10Hz.  Pausing only to check on the state of the UI at key points.
        
        int delay = 43;
        ntsm.sendSentences(100, delay); // Start trigger fires during
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime.getText()));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("", lastSplitTime.getText());
                assertEquals("", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime.getText()));
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
                
                assertFalse("".equals(elapsedLapTime.getText()));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:16.359", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime.getText()));
            }
        });
        
        ntsm.sendSentences(100, delay);
        ntsm.sendSentences(100, delay); // Second split fires during 12.822
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
                
                assertFalse("".equals(elapsedLapTime.getText()));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:12.822", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime.getText()));
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
                
                assertFalse("".equals(elapsedLapTime.getText()));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:45.077", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime.getText()));
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
                
                assertFalse("".equals(elapsedLapTime.getText()));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:11.296", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime.getText()));
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
                
                assertFalse("".equals(elapsedLapTime.getText()));
                assertEquals("2:01.570", lastLapTime.getText());
                assertEquals("0:00.000", lastLapTimeDelta.getText());
                assertEquals("0:36.016", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                assertEquals("2", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime.getText()));
            }
        });
        
        Thread.sleep(5000);
        
        // Finish the activity
        getActivity().finish();
        
        Thread.sleep(2000);
        
        // Ensure that the thing was shut down properly when the activity ended
        assertFalse(dpcms.isInitialized());
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
                new Intent(logActivity, DataProviderCoordinatorManagerService.class),
                serviceConnection,
                Context.BIND_NOT_FOREGROUND));
        return logActivity;
    }

}
