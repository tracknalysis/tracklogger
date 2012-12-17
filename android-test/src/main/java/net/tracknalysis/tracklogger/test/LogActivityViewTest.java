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

import net.tracknalysis.common.notification.NotificationListener;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.activity.LogActivity;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinatorNotificationType;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorFactory;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorManagerService;
import net.tracknalysis.tracklogger.model.AccelData;
import net.tracknalysis.tracklogger.model.EcuData;
import net.tracknalysis.tracklogger.model.LocationData;
import net.tracknalysis.tracklogger.model.TimingData;
import net.tracknalysis.tracklogger.model.AccelData.AccelDataBuilder;
import net.tracknalysis.tracklogger.model.LocationData.LocationDataBuilder;
import net.tracknalysis.tracklogger.model.TimingData.TimingDataBuilder;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.Uri;

/**
 * Tests the log activity views with a mock {@link DataProviderCoordinator} instance for 
 * fine grained unit testing.
 *
 * @author David Valeri
 */
public class LogActivityViewTest extends AbstractLogActivityTest {
    
    private NotificationListener<DataProviderCoordinatorNotificationType> notificationListener;
    private boolean started;
    private LocationData locationData;
    private AccelData accelData;
    private EcuData ecuData;
    private TimingData timingData;
    
    private DataProviderCoordinatorNotificationType lastNotificationType = 
            DataProviderCoordinatorNotificationType.STOPPED;
    private Object lastNotificationBody;
    
    @Override
    protected void init() throws Exception {
        
        DataProviderCoordinatorFactory.setInstance(
                new DataProviderCoordinatorFactory() {
                    
                    @Override
                    public DataProviderCoordinator createDataProviderCoordinator(
                            DataProviderCoordinatorManagerService dataProviderCoordinatorService,
                            Context context, BluetoothAdapter btAdapter,
                            Uri splitMarkerSetUri) {
                        
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
                            
                            @Override
                            public void addListener(
                            		NotificationListener<DataProviderCoordinatorNotificationType> listener) {
                            }
                            
                            @Override
                            public void removeListener(
                            		NotificationListener<DataProviderCoordinatorNotificationType> listener) {
                            }

                            @Override
                            public void addWeakReferenceListener(
                            		NotificationListener<DataProviderCoordinatorNotificationType> listener) {
                            	LogActivityViewTest.this.notificationListener = listener;
                                LogActivityViewTest.this.notificationListener
                                        .onNotification(lastNotificationType, lastNotificationBody);
                            }
                            
                            @Override
                            public void removeWeakReferenceListener(
                            		NotificationListener<DataProviderCoordinatorNotificationType> listener) {
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
        
        Long initialTimingReceivedTime = new Date().getTime();
        TimingDataBuilder timingBuilder = new TimingDataBuilder();
        timingBuilder.setBestLapTime(null);
        timingBuilder.setBestSplitTimes(new ArrayList<Long>(Arrays.asList((Long) null, null)));
        timingBuilder.setDataRecivedTime((new Date()).getTime());
        timingBuilder.setLap(0);
        timingBuilder.setLapTime(null);
        timingBuilder.setSplitIndex(1);
        timingBuilder.setSplitTime(null);
        timingBuilder.setTime(0);
        timingBuilder.setInitialLapStartDataReceivedTime(initialTimingReceivedTime);
        timingBuilder.setLastLapStartDataReceivedTime(initialTimingReceivedTime);
        timingBuilder.setLastSplitStartDataReceivedTime(initialTimingReceivedTime);

        triggerStart(logActivity);
        
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.STARTING);
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.STARTED);
        
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.READY_PROGRESS,
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
        
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.READY_PROGRESS,
                new Object[] {locationData, null, null});
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.READY_PROGRESS,
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
        
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.READY);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertTrue(getActivity().getWaitingForStartTriggerDialog().isShowing());
            }
        });
        
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.TIMING_START_TRIGGER_FIRED);
        
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                assertFalse(getActivity().getInitDataProviderCoordinatorDialog().isShowing());
                assertFalse(getActivity().getWaitingForStartTriggerDialog().isShowing());
            }
        });
        
        // Lap 1: start
        timingData = timingBuilder.build();
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.TIMING_DATA_UPDATE,
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
        timingBuilder.setLastSplitStartDataReceivedTime(initialTimingReceivedTime + 30000l);
        timingData = timingBuilder.build();
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.TIMING_DATA_UPDATE,
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
        timingBuilder.setLastLapStartDataReceivedTime(initialTimingReceivedTime + 60000l);
        timingBuilder.setLastSplitStartDataReceivedTime(initialTimingReceivedTime + 60000l);
        timingData = timingBuilder.build();
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.TIMING_DATA_UPDATE,
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
        timingBuilder.setLastSplitStartDataReceivedTime(initialTimingReceivedTime + 90000l);
        timingData = timingBuilder.build();
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.TIMING_DATA_UPDATE,
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
        timingBuilder.setLastLapStartDataReceivedTime(initialTimingReceivedTime + 120000l);
        timingBuilder.setLastSplitStartDataReceivedTime(initialTimingReceivedTime + 120000l);
        timingData = timingBuilder.build();
        notificationListener.onNotification(DataProviderCoordinatorNotificationType.TIMING_DATA_UPDATE,
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
    
//    public void testInitDataProviderCoordinatorError() {
//        // TODO testInitDataProviderError
//    }
//    
//    public void testCancelDuringInitDataProviderCoordinator() {
//        // TODO testCancelDuringInitDataProviderCoordinator
//    }
//    
//    public void testLoggingError() {
//        // TODO testLoggingError
//    }
//    
//    public void testUserStopWithBackArrow() {
//        // TODO testUserStopWithBackArrow
//    }
}
