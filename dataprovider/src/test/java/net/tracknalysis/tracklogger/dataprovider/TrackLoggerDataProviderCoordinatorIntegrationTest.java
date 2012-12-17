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

import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.common.io.StreamSocketManager;
import net.tracknalysis.common.notification.NotificationListener;
import net.tracknalysis.location.LocationManager;
import net.tracknalysis.location.Route;
import net.tracknalysis.location.Waypoint;
import net.tracknalysis.location.nmea.NmeaLocationManager;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator.DataProviderCoordinatorNotificationType;
import net.tracknalysis.tracklogger.dataprovider.location.LocationManagerLocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.timing.RouteManagerTimingDataProvider;
import net.tracknalysis.tracklogger.model.TimingData;
import net.tracknalysis.tracklogger.model.AccelData.AccelDataBuilder;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the core data provider coordinator implementation logic against real input from
 * an {@link NmeaLocationManager}.
 *
 * @author David Valeri
 */
public class TrackLoggerDataProviderCoordinatorIntegrationTest {
    
    private TestTrackLoggerDataProviderCoordinator dpc;
    private LocationManager locationManager;
    
    private NotificationListener<DataProviderCoordinatorNotificationType> mockNotificationStrategy;
    private AccelDataProvider mockAccelDataProvider;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
      
        // Setup with canned NMEA data for a known "lap".  There is no output from
        // the location manager implementation so it is safe to leave the output stream
        // null.
        SocketManager socketManager = new StreamSocketManager(this.getClass()
                .getResourceAsStream("/NMEA-Test-Data.txt"), null);
        
        locationManager = new NmeaLocationManager(socketManager);
        
        mockNotificationStrategy = createMock(NotificationListener.class);
        mockAccelDataProvider = createMock(AccelDataProvider.class);
        LocationDataProvider locationDataProvider = new LocationManagerLocationDataProvider(locationManager);
        TimingDataProvider timingDataProvider = new RouteManagerTimingDataProvider(locationManager.getRouteManager(),
                new Route("My Route", Arrays.asList(
                        new Waypoint("1", 38.979896545410156d, -77.54102325439453d),
                        new Waypoint("2", 38.98295974731445d, -77.53973388671875d),
                        new Waypoint("3", 38.982906341552734d, -77.54007720947266d),
                        new Waypoint("4", 38.972618103027344d, -77.54145050048828d),
                        new Waypoint("5", 38.97257995605469d, -77.5412826538086d))));
        
        dpc = new TestTrackLoggerDataProviderCoordinator(
                mockAccelDataProvider, locationDataProvider, null,
                timingDataProvider);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testFullLifecycleWithouEcuDataProvider() throws Exception {
        AccelDataBuilder accelDataBuilder = new AccelDataBuilder();
        accelDataBuilder.setDataRecivedTime(1l);
        accelDataBuilder.setLateral(0);
        accelDataBuilder.setLongitudinal(0);
        accelDataBuilder.setVertical(0);
        
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.STOPPED));
        expectLastCall();
        
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.STARTING));
        expectLastCall();
        
        mockAccelDataProvider.addSynchronousListener(anyObject(DataListener.class));
        expectLastCall();
        mockAccelDataProvider.start();
        expectLastCall();
                
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.STARTED));
        expectLastCall();
        
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.READY_PROGRESS),
                anyObject(Object[].class));
        expectLastCall().anyTimes();
        
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.READY));
        expectLastCall();
        
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build());
        
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.TIMING_START_TRIGGER_FIRED));
        expectLastCall();
        
        // While logging we will get a bunch of calls to this method
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build()).anyTimes();
        mockNotificationStrategy
                .onNotification(
                        eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.TIMING_DATA_UPDATE),
                        anyObject(TimingData.class));
        expectLastCall().times(6);
        
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.STOPPING));
        expectLastCall();
        mockAccelDataProvider.removeSynchronousListener(anyObject(DataListener.class));
        expectLastCall();
        mockAccelDataProvider.stop();
        expectLastCall();
        mockNotificationStrategy.onNotification(
                eq(DataProviderCoordinator.DataProviderCoordinatorNotificationType.STOPPED));
        expectLastCall();
        
        replay(mockNotificationStrategy, mockAccelDataProvider);
        
        dpc.register(mockNotificationStrategy);
        dpc.start();
        locationManager.start();
        
        for (int i = 0; ; i++) {
            if (dpc.getLogEntries().size() == 1270) {
                dpc.stop();
                break;
            } else if (i > 10) {
                fail();
            } else {
                Thread.sleep(1000);
            }
        }
        
        verify(mockNotificationStrategy, mockAccelDataProvider);
        
        // Verify the timing data accuracy
        assertEquals(6, dpc.getTimingEntries().size());
        assertEquals(Long.valueOf(16359), dpc.getTimingEntries().get(1).getTimingData().getSplitTime());
        assertEquals(Long.valueOf(12822), dpc.getTimingEntries().get(2).getTimingData().getSplitTime());
        assertEquals(Long.valueOf(45077), dpc.getTimingEntries().get(3).getTimingData().getSplitTime());
        assertEquals(Long.valueOf(11296), dpc.getTimingEntries().get(4).getTimingData().getSplitTime());
        assertEquals(Long.valueOf(36016), dpc.getTimingEntries().get(5).getTimingData().getSplitTime());
    }
}
