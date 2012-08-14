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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Level;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.activity.LogActivity;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.LocationData;
import net.tracknalysis.tracklogger.dataprovider.AccelData.AccelDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.LocationData.LocationDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.TimingData;
import net.tracknalysis.tracklogger.dataprovider.TimingData.TimingDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorFactory;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorService;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

/**
 * Tests the log activity views with a mock {@link DataProviderCoordinator} instance for fine grained unit testing.
 * @author David Valeri
 */
public class LogActivityViewTest extends ActivityInstrumentationTestCase2<LogActivity> {
    
    private Configuration configuration;
    
    private NotificationStrategy<DataProviderCoordinator.NotificationType> notificationStrategy;
    private boolean started;
    private LocationData locationData;
    private AccelData accelData;
    private EcuData ecuData;
    private TimingData timingData;
    
    private TextView locationStatus;
    private TextView accelStatus;
    private TextView ecuStatus;
    
    private TextView elapsedLapTime;
    private TextView lapNumber;
    private TextView elapsedSessionTime;
    private TextView lastSplitTime;
    private TextView lastSplitTimeDelta;
    private TextView lastLapTime;
    private TextView lastLapTimeDelta;
    
    public LogActivityViewTest() {
        super(LogActivity.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        configuration = ConfigurationFactory.getInstance().getConfiguration();
        configuration.setRootLogLevel(Level.INFO);
        configuration.setLogToFile(false);
        
        DataProviderCoordinatorFactory.setInstance(
                new DataProviderCoordinatorFactory() {
                    
                    @Override
                    public DataProviderCoordinator createDataProviderCoordinator(
                            DataProviderCoordinatorService dataProviderCoordinatorService,
                            NotificationStrategy<DataProviderCoordinator.NotificationType> notificationStrategy,
                            Context context, BluetoothAdapter btAdapter) {
                        
                        LogActivityViewTest.this.notificationStrategy = notificationStrategy;
                        
                        return new DataProviderCoordinator() {

                            @Override
                            public void start() {
                                LogActivityViewTest.this.started = true;
                            }

                            @Override
                            public void stop() {
                                LogActivityViewTest.this.started = false;
                                
                            }

                            @Override
                            public boolean isRunning() {
                                return LogActivityViewTest.this.started;
                            }

                            @Override
                            public LocationData getCurrentLocationData() {
                                return LogActivityViewTest.this.locationData;
                            }

                            @Override
                            public double getLocationDataUpdateFrequency() {
                                return 0;
                            }

                            @Override
                            public AccelData getCurrentAccelData() {
                                return LogActivityViewTest.this.accelData;
                            }

                            @Override
                            public double getAccelDataUpdateFrequency() {
                                return 0;
                            }

                            @Override
                            public EcuData getCurrentEcuData() {
                                return LogActivityViewTest.this.ecuData;
                            }

                            @Override
                            public double getEcuDataUpdateFrequency() {
                                return 0;
                            }

                            @Override
                            public TimingData getCurrentTimingData() {
                                return LogActivityViewTest.this.timingData;
                            }

                            @Override
                            public boolean isLoggingStartTriggerFired() {
                                return false;
                            }

                            @Override
                            public boolean isReady() {
                                return false;
                            }

                            @Override
                            public void startAsynch() {
                                start();
                            } 
                        };
                    }
        });
    }
    
    public void testDefaultView() throws Throwable {
        
        configuration.setLogLayoutId(R.layout.log_default);
        
        final LogActivity logActivity = getActivity();
        initializeUiFields(logActivity);
        
        LocationDataBuilder locBuilder = new LocationDataBuilder();
        locBuilder.setAltitude(10d);
        locBuilder.setBearing(0f);
        locBuilder.setDataRecivedTime((new Date()).getTime());
        locBuilder.setLatitude(10d);
        locBuilder.setLongitude(-10d);
        locBuilder.setSpeed(10f);
        locBuilder.setTime(0);
        
        locationData = locBuilder.build();
        
        AccelDataBuilder accelBuilder = new AccelDataBuilder();
        accelBuilder.setDataRecivedTime((new Date()).getTime());
        accelBuilder.setLateral(1f);
        accelBuilder.setLongitudinal(-1f);
        accelBuilder.setVertical(0f);
        
        accelData = accelBuilder.build();
        
        TimingDataBuilder timingBuilder = new TimingDataBuilder();
        timingBuilder.setBestLapTime(null);
        timingBuilder.setBestSplitTimes(new ArrayList<Long>(Arrays.asList((Long) null, null)));
        timingBuilder.setDataRecivedTime((new Date()).getTime());
        timingBuilder.setLap(0);
        timingBuilder.setLapTime(null);
        timingBuilder.setSplitIndex(1);
        timingBuilder.setSplitTime(null);
        timingBuilder.setTime(0);
        
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.STARTING);
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.STARTED);
        
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.READY_PROGRESS,
                new Object[] {locationData, null, null});
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertEquals(logActivity.getText(R.string.log_wait_for_ready_text_ready), locationStatus.getText());
                assertEquals(logActivity.getText(R.string.log_wait_for_ready_text_waiting), accelStatus.getText());
                assertEquals(logActivity.getText(R.string.log_wait_for_ready_text_waiting), ecuStatus.getText());
                assertTrue(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
            }
        });
        
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.READY_PROGRESS,
                new Object[] {locationData, null, null});
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.READY_PROGRESS,
                new Object[] {locationData, accelData, null});
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertEquals(logActivity.getText(R.string.log_wait_for_ready_text_ready), locationStatus.getText());
                assertEquals(logActivity.getText(R.string.log_wait_for_ready_text_ready), accelStatus.getText());
                assertEquals(logActivity.getText(R.string.log_wait_for_ready_text_waiting), ecuStatus.getText());
                assertTrue(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
            }
        });
        
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.READY);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertTrue(getActivity().getWaitingForStartTriggerDialog().isShowing());
            }
        });
        
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.TIMING_START_TRIGGER_FIRED);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
            }
        });
        
        // Lap 1: start
        timingData = timingBuilder.build();
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.TIMING_DATA_UPDATE,
                timingData);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse("".equals(elapsedLapTime));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("", lastSplitTime.getText());
                assertEquals("", lastSplitTimeDelta.getText());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        // Lap 1.1
        timingBuilder.setLap(0);
        timingBuilder.setSplitIndex(0);
        timingBuilder.setSplitTime(30000l);
        timingBuilder.getBestSplitTimes().set(0, 30000l);
        timingData = timingBuilder.build();
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.TIMING_DATA_UPDATE,
                timingData);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse("".equals(elapsedLapTime));
                assertEquals("", lastLapTime.getText());
                assertEquals("", lastLapTimeDelta.getText());
                assertEquals("0:30.000", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                //assertEquals(Color.WHITE, lastLapTimeDelta.getCurrentTextColor());
                assertEquals("1", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        // Lap 1: end
        timingBuilder.setLapTime(60000l);
        timingBuilder.setLap(0);
        timingBuilder.setBestLapTime(60000l);
        timingBuilder.setSplitIndex(1);
        timingBuilder.setSplitTime(30000l);
        timingBuilder.getBestSplitTimes().set(1, 30000l);
        timingData = timingBuilder.build();
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.TIMING_DATA_UPDATE,
                timingData);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse("".equals(elapsedLapTime));
                assertEquals("1:00.000", lastLapTime.getText());
                assertEquals("0:00.000", lastLapTimeDelta.getText());
                assertEquals("0:30.000", lastSplitTime.getText());
                assertEquals("0:00.000", lastSplitTimeDelta.getText());
                //assertEquals(Color.WHITE, lastLapTimeDelta.getCurrentTextColor());
                assertEquals("2", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        // Lap 2.1
        timingBuilder.setLap(1);
        timingBuilder.setLapTime(null);
        timingBuilder.setSplitIndex(0);
        timingBuilder.setSplitTime(25045l);
        timingBuilder.getBestSplitTimes().set(0, 25045l);
        timingData = timingBuilder.build();
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.TIMING_DATA_UPDATE,
                timingData);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse("".equals(elapsedLapTime));
                assertEquals("1:00.000", lastLapTime.getText());
                assertEquals("0:00.000", lastLapTimeDelta.getText());
                assertEquals("0:25.045", lastSplitTime.getText());
                assertEquals("-0:04.955", lastSplitTimeDelta.getText());
                //assertEquals(Color.GREEN, lastLapTimeDelta.getTextColors().getDefaultColor());
                assertEquals("2", lapNumber.getText());
                assertFalse("".equals(elapsedSessionTime));
            }
        });
        
        // Lap 2: End
        timingBuilder.setLap(1);
        timingBuilder.setLapTime(55000l);
        timingBuilder.setBestLapTime(55000l);
        timingBuilder.setSplitIndex(1);
        timingBuilder.setSplitTime(31000l);
        timingData = timingBuilder.build();
        notificationStrategy.sendNotification(DataProviderCoordinator.NotificationType.TIMING_DATA_UPDATE,
                timingData);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse("".equals(elapsedLapTime));
                assertEquals("0:55.000", lastLapTime.getText());
                assertEquals("-0:05.000", lastLapTimeDelta.getText());
                assertEquals("0:31.000", lastSplitTime.getText());
                assertEquals("0:01.000", lastSplitTimeDelta.getText());
                assertEquals("3", lapNumber.getText());
            }
        });
    }
    
    public void testInitDataProviderCoordinatorError() {
        // TODO testInitDataProviderError
    }
    
    public void testCancelDuringInitDataProviderCoordinator() {
        // TODO testCancelDuringInitDataProviderCoordinator
    }
    
    public void testLoggingError() {
        // TODO testLoggingError
    }
    
    public void testUserStopWithBackArrow() {
        // TODO testUserStopWithBackArrow
    }
    
    protected void initializeUiFields(LogActivity logActivity) {
        elapsedLapTime = (TextView) logActivity.findViewById(R.id.log_elapsed_lap_time_value);
        lapNumber = (TextView) logActivity.findViewById(R.id.log_lap_number_value);
        elapsedSessionTime = (TextView) logActivity.findViewById(R.id.log_elapsed_session_time_value);
        lastSplitTime = (TextView) logActivity.findViewById(R.id.log_last_split_time_value);
        lastSplitTimeDelta = (TextView) logActivity.findViewById(R.id.log_last_split_time_delta_value);
        lastLapTime = (TextView) logActivity.findViewById(R.id.log_last_lap_time_value);
        lastLapTimeDelta = (TextView) logActivity.findViewById(R.id.log_last_lap_time_delta_value);
        
        Dialog initDataProviderCoordinatorDialog = logActivity.getInitDataProviderCoordinatorDialog();
        locationStatus = (TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_location_status);
        accelStatus = (TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_accel_status);
        ecuStatus = (TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_ecu_status);
    }
}
