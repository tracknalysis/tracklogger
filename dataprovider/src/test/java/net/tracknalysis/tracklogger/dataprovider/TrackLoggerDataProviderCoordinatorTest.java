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
package net.tracknalysis.tracklogger.dataprovider;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger.dataprovider.AccelData.AccelDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.EcuData.EcuDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.LocationData.LocationDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.TimingData.TimingDataBuilder;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David Valeri
 */
public class TrackLoggerDataProviderCoordinatorTest {

    TrackLoggerDataProviderCoordinator dpc;
    private AtomicInteger sessionCounter = new AtomicInteger();
    private List<TrackLoggerDataProviderCoordinator.LogEntry> logEntries;
    private List<TimingData> timingDatas;
    
    private NotificationStrategy mockNotificationStrategy;
    private LocationDataProvider mockLocationDataProvider;
    private EcuDataProvider mockEcuDataProvider;
    private AccelDataProvider mockAccelDataProvider;
    private TimingDataProvider mockTimingDataProvider;
    
    @Before
    public void setup() throws Exception {
        logEntries = new LinkedList<TrackLoggerDataProviderCoordinator.LogEntry>();
        timingDatas = new LinkedList<TimingData>();
        
        mockNotificationStrategy = createMock(NotificationStrategy.class);
        mockLocationDataProvider = createMock(LocationDataProvider.class);
        mockEcuDataProvider = createMock(EcuDataProvider.class);
        mockAccelDataProvider = createMock(AccelDataProvider.class);
        mockTimingDataProvider = createMock(TimingDataProvider.class);
        
        
        dpc = new TrackLoggerDataProviderCoordinatorTestHarness(
                mockNotificationStrategy,
                mockAccelDataProvider,
                mockLocationDataProvider,
                mockEcuDataProvider,
                mockTimingDataProvider);   
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testFullLifecycle() throws Exception {
        
        LocationDataBuilder locationDataBuilder = new LocationDataBuilder();
        locationDataBuilder.setLatitude(0d);
        locationDataBuilder.setLongitude(0d);
        locationDataBuilder.setTime(1l);
        locationDataBuilder.setDataRecivedTime(1l);
        locationDataBuilder.setAltitude(0d);
        locationDataBuilder.setBearing(0f);
        
        AccelDataBuilder accelDataBuilder = new AccelDataBuilder();
        accelDataBuilder.setDataRecivedTime(1l);
        accelDataBuilder.setLateral(0);
        accelDataBuilder.setLongitudinal(0);
        accelDataBuilder.setVertical(0);
        
        EcuDataBuilder ecuDataBuilder = new EcuDataBuilder();
        ecuDataBuilder.setDataRecivedTime(1l);
        ecuDataBuilder.setAirFuelRatio(14.7d);
        ecuDataBuilder.setBatteryVoltage(14.0d);
        ecuDataBuilder.setCoolantTemperature(80d);
        ecuDataBuilder.setIgnitionAdvance(22d);
        ecuDataBuilder.setManifoldAbsolutePressure(60d);
        ecuDataBuilder.setManifoldAirTemperature(50d);
        ecuDataBuilder.setRpm(3500);
        ecuDataBuilder.setThrottlePosition(0.15d);
        
        TimingDataBuilder timingDataBuilder = new TimingDataBuilder();
        timingDataBuilder.setDataRecivedTime(1l);
        timingDataBuilder.setLap(0);
        timingDataBuilder.setBestSplitTimes(Arrays.asList((Long) null, (Long) null, (Long) null));
        timingDataBuilder.setSplitIndex(1);
        timingDataBuilder.setTime(1l);
        
        Capture<DataListener<LocationData>> locationListenerCapture = new Capture<DataListener<LocationData>>();
        Capture<DataListener<TimingData>> timingListenerCapture = new Capture<DataListener<TimingData>>();
        
        // start()
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.STARTING));
        expectLastCall();
        mockLocationDataProvider.start();
        expectLastCall();
        mockAccelDataProvider.start();
        expectLastCall();
        mockEcuDataProvider.start();
        expectLastCall();
        mockTimingDataProvider.start();
        expectLastCall();
        
        mockLocationDataProvider.addSynchronousListener(capture(locationListenerCapture));
        expectLastCall();
        mockTimingDataProvider.addSynchronousListener(capture(timingListenerCapture));
        
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.STARTED));
        expectLastCall();

        // receiveData(LocationData) - 1
        expect(mockAccelDataProvider.getCurrentData()).andReturn(null);
        expect(mockEcuDataProvider.getCurrentData()).andReturn(null);
        
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.READY_PROGRESS),
                anyObject(Object[].class));
        expectLastCall();
        
        // receiveData(LocationData) - 2
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build());
        expect(mockEcuDataProvider.getCurrentData()).andReturn(null);
        
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.READY_PROGRESS),
                anyObject(Object[].class));
        expectLastCall();
        
        // receiveData(LocationData) - 3
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build());
        expect(mockEcuDataProvider.getCurrentData()).andReturn(ecuDataBuilder.build());
        
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.READY_PROGRESS),
                anyObject(Object[].class));
        expectLastCall();
        
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.READY));
        expectLastCall();
        
        // startLogging()
        
        // receiveData(LocationData) - 4
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build());
        expect(mockEcuDataProvider.getCurrentData()).andReturn(ecuDataBuilder.build());
        
        // receiveData(TimingData) - 1
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.TIMING_START_TRIGGER_FIRED));
        expectLastCall();
        
        // receiveData(LocationData) - 5
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build());
        expect(mockEcuDataProvider.getCurrentData()).andReturn(ecuDataBuilder.build());
        
        // receiveData(TimingData) - 2
        
        // stop()
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.STOPPING));
        expectLastCall();
        mockLocationDataProvider.removeSynchronousListener(notNull(DataListener.class));
        expectLastCall();
        mockTimingDataProvider.removeSynchronousListener(notNull(DataListener.class));
        expectLastCall();
        mockLocationDataProvider.stop();
        expectLastCall();
        mockTimingDataProvider.stop();
        expectLastCall();
        mockAccelDataProvider.stop();
        expectLastCall();
        mockEcuDataProvider.stop();
        expectLastCall();
        mockNotificationStrategy.sendNotification(
                eq(TrackLoggerDataProviderCoordinator.NotificationType.STOPPED));
        expectLastCall();
        
        
        replay(mockNotificationStrategy, mockAccelDataProvider,
                mockLocationDataProvider, mockEcuDataProvider,
                mockTimingDataProvider);
        
        // start()
        dpc.start();
        
        // receiveData(LocationData) - 1
        locationListenerCapture.getValue().receiveData(locationDataBuilder.build());
        
        // receiveData(LocationData) - 2
        locationListenerCapture.getValue().receiveData(locationDataBuilder.build());
        
        // receiveData(LocationData) - 3
        locationListenerCapture.getValue().receiveData(locationDataBuilder.build());
        
        // startLogging()
        dpc.startLogging();
        assertEquals(1, sessionCounter.get());
        
        // receiveData(LocationData) - 4
        locationListenerCapture.getValue().receiveData(locationDataBuilder.build());
        
        // receiveData(TimingData) - 1
        timingListenerCapture.getValue().receiveData(timingDataBuilder.build());
        Thread.sleep(500l);  // Wait for asynchronous logging
        assertEquals(1, timingDatas.size());
        
        // receiveData(LocationData) - 5
        locationListenerCapture.getValue().receiveData(locationDataBuilder.build());
        Thread.sleep(500l);  // Wait for asynchronous logging
        assertEquals(2, logEntries.size());
        
        // receiveData(TimingData) - 2
        timingDataBuilder.setLap(1);
        timingDataBuilder.setSplitIndex(0);
        timingDataBuilder.setSplitTime(1l);
        timingDataBuilder.getBestSplitTimes().set(0, 1l);
        
        timingListenerCapture.getValue().receiveData(timingDataBuilder.build());
        Thread.sleep(500l);  // Wait for asynchronous logging
        assertEquals(2, timingDatas.size());
        
        // stop()
        dpc.stop();
        
        verify(mockNotificationStrategy, mockAccelDataProvider,
                mockLocationDataProvider, mockEcuDataProvider,
                mockTimingDataProvider);
    }
    
    @Test
    public void testFailedStart() {
        // TODO test failed start
    }
    
    @Test
    public void testFailedStop() {
        // TODO test failed stop
    }
    
    private final class TrackLoggerDataProviderCoordinatorTestHarness extends TrackLoggerDataProviderCoordinator {

        public TrackLoggerDataProviderCoordinatorTestHarness(
                NotificationStrategy notificationStrategy,
                AccelDataProvider accelDataProvider,
                LocationDataProvider gpsDataProvider,
                EcuDataProvider ecuDataProvider,
                TimingDataProvider timingDataProvider) {
            super(notificationStrategy, accelDataProvider, gpsDataProvider,
                    ecuDataProvider, timingDataProvider);
        }

        @Override
        protected int createSession() {
            return sessionCounter.incrementAndGet();
        }

        @Override
        protected void openSession(int sessionId) {
        }

        @Override
        protected void storeLogEntry(int sessionId, LogEntry logEntry) {
            logEntries.add(logEntry);
        }

        @Override
        protected void storeTimingEntry(int sessionId, TimingData timingData) {
            timingDatas.add(timingData);            
        }
    }
}