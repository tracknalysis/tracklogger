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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;

import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.common.io.StreamSocketManager;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.util.TimeUtil;
import net.tracknalysis.location.LocationManager;
import net.tracknalysis.location.Route;
import net.tracknalysis.location.Waypoint;
import net.tracknalysis.location.nmea.NmeaLocationManager;
import net.tracknalysis.tracklogger.dataprovider.AccelData.AccelDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.location.LocationManagerLocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.timing.RouteManagerTimingDataProvider;

import org.junit.Before;
import org.junit.Test;

/**
 * @author David Valeri
 */
public class TrackLoggerDataProviderCoordinatorIntegrationTest {
    
    private TestTrackLoggerDataProviderCoordinator dpc;
    private LocationManager locationManager;
    
    private NotificationStrategy mockNotificationStrategy;
    private AccelDataProvider mockAccelDataProvider;
    
    @Before
    public void setup() throws Exception {
      
        // Data to test with
        InputStream is = this.getClass().getResourceAsStream("/RouteManagerTestData.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(baos);
        
        for (int i = 0; i < 20; i++) {
            writer.write("$GPGGA,000445.102,3859.0335,N,07731.9688,W,1,6,1.37,113.3,M,-33.4,M,,*6E\r\n");
            writer.write("$GPRMC,000445.102,A,3859.0335,N,07731.9688,W,0.09,229.39,130512,,,A*78\r\n");
        }
        
        String line = reader.readLine();
        while (line != null) {
            String[] tokens = line.split(",[ ]*");
            String[] timeTokens = tokens[1].split("\\.");
            
            // ms offset into day
            long time = 0;
            if (timeTokens.length == 2) {
                time += Long.valueOf(timeTokens[1]);
            }
            time += Long.valueOf(timeTokens[0]) * 1000;
            time = time - (86400 * 1000);
            long hours = time / TimeUtil.MS_IN_HOUR;
            long minutes = (time % TimeUtil.MS_IN_HOUR) / TimeUtil.MS_IN_MINUTE;
            long seconds = (time % TimeUtil.MS_IN_HOUR % TimeUtil.MS_IN_MINUTE) / TimeUtil.MS_IN_SECOND;
            long ms = (time % TimeUtil.MS_IN_HOUR % TimeUtil.MS_IN_MINUTE % TimeUtil.MS_IN_SECOND);
            
            writer.write("$GPGGA," + String.format("%02d%02d%02d.%03d", hours, minutes, seconds, ms) + "," + tokens[4] + ",N," + tokens[5] + ",W,1,6,1.37,113.3,M,-33.4,M,,*6E\r\n");
            writer.write("$GPRMC," + String.format("%02d%02d%02d.%03d", hours, minutes, seconds, ms) + ",A," + tokens[4] + ",N," + tokens[5] + ",W,0.09,229.39,130512,,,A*78\r\n");
            
            line = reader.readLine();
        }
        
        writer.close();
        baos.close();
        
        is = new ByteArrayInputStream(baos.toByteArray());
        
        
        SocketManager socketManager = new StreamSocketManager(is, null);
        
        locationManager = new NmeaLocationManager(socketManager);
        
        mockNotificationStrategy = createMock(NotificationStrategy.class);
        mockAccelDataProvider = createMock(AccelDataProvider.class);
        LocationDataProvider locationDataProvider = new LocationManagerLocationDataProvider(locationManager);
        TimingDataProvider timingDataProvider = new RouteManagerTimingDataProvider(locationManager.getRouteManager(),
                new Route("My Route", Arrays.asList(
                        new Waypoint("1", 38.979896545410156d, -77.54102325439453d),
                        new Waypoint("2", 38.98295974731445d, -77.53973388671875d),
                        new Waypoint("3", 38.982906341552734d, -77.54007720947266d),
                        new Waypoint("4", 38.972618103027344d, -77.54145050048828d),
                        new Waypoint("5", 38.97257995605469d, -77.5412826538086d))));
        
        dpc = new TestTrackLoggerDataProviderCoordinator(mockNotificationStrategy,
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
        
        mockNotificationStrategy.sendNotification(
                eq(DataProviderCoordinator.NotificationType.STARTING));
        expectLastCall();
        
        mockAccelDataProvider.addSynchronousListener(anyObject(DataListener.class));
        expectLastCall();
        mockAccelDataProvider.start();
        expectLastCall();
                
        mockNotificationStrategy.sendNotification(
                eq(DataProviderCoordinator.NotificationType.STARTED));
        expectLastCall();
        
        mockNotificationStrategy.sendNotification(
                eq(DataProviderCoordinator.NotificationType.READY_PROGRESS),
                anyObject(Object[].class));
        expectLastCall().anyTimes();
        
        mockNotificationStrategy.sendNotification(
                eq(DataProviderCoordinator.NotificationType.READY));
        expectLastCall();
        
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build());
        
        mockNotificationStrategy.sendNotification(
                eq(DataProviderCoordinator.NotificationType.TIMING_START_TRIGGER_FIRED));
        expectLastCall();
        
        // While logging we will get a bunch of calls to this method
        expect(mockAccelDataProvider.getCurrentData()).andReturn(accelDataBuilder.build()).anyTimes();
        
        mockNotificationStrategy.sendNotification(
                eq(DataProviderCoordinator.NotificationType.STOPPING));
        expectLastCall();
        mockAccelDataProvider.removeSynchronousListener(anyObject(DataListener.class));
        expectLastCall();
        mockAccelDataProvider.stop();
        expectLastCall();
        mockNotificationStrategy.sendNotification(
                eq(DataProviderCoordinator.NotificationType.STOPPED));
        expectLastCall();
        
        replay(mockNotificationStrategy, mockAccelDataProvider);
        
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
        
        assertEquals(6, dpc.getTimingDatas().size());
        assertEquals(Long.valueOf(16359), dpc.getTimingDatas().get(1).getSplitTime());
        assertEquals(Long.valueOf(12822), dpc.getTimingDatas().get(2).getSplitTime());
        assertEquals(Long.valueOf(45077), dpc.getTimingDatas().get(3).getSplitTime());
        assertEquals(Long.valueOf(11296), dpc.getTimingDatas().get(4).getSplitTime());
        assertEquals(Long.valueOf(36016), dpc.getTimingDatas().get(5).getSplitTime());
    }
}
