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
package net.tracknalysis.tracklogger.export;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import net.tracknalysis.common.notification.NoOpNotificationStrategy;
import net.tracknalysis.common.util.TimeUtil;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David Valeri
 */
public class AbstractSessionToTrackLoggerCsvExporterTest {
    
    private Date startTime;
    private AbstractSessionToTrackLoggerCsvExporter exporter;
    
    @Before
    public void setup() {
        startTime = new Date(0);
        exporter = new AbstractSessionToTrackLoggerCsvExporterHarness(new File("target"));
    }
    
    @Test
    public void testExport() throws Exception {
        exporter.export(1);
        
        List<String> lines = IOUtils.readLines(new FileInputStream(new File("target", "1-1969.12.31-19.00.00.csv")));
        assertEquals(9, lines.size());
        
        assertTrue(lines.get(0).startsWith("Session Title: 1 - "));
        assertEquals("Split Marker Set: Split Marker Name!", lines.get(1));
        assertTrue(lines.get(2).startsWith("Export Time: "));
        assertEquals("Format: TrackLogger CSV 1.0b", lines.get(3));
        assertEquals(
                "running_time,synch_timestamp,accel_capture_timestamp,longitudinal_accel,lateral_accel,vertical_accel,location_capture_timestamp,latitude,longitude,altitude,speed,bearing,ecu_capture_timestamp,rpm,map,throttle_position,afr,mat,clt,ignition_advance,battery_voltage,lap_capture_timestamp,lap,split",
                lines.get(5));
        assertEquals(
                "0,1,2,3.00000000000000000000,4.00000000000000000000,5.00000000000000000000,6,7.00000000000000000000,8.00000000000000000000,9.00000000000000000000,10.00000000000000000000,11.00000000000000000000,12,13,14.00000000000000000000,15.00000000000000000000,16.00000000000000000000,17.00000000000000000000,18.00000000000000000000,19.00000000000000000000,20.00000000000000000000,21,22,23",
                lines.get(6));
        assertEquals(
                "86399998,86399999,12,13.00000000000000000000,14.00000000000000000000,15.00000000000000000000,16,17.00000000000000000000,18.00000000000000000000,19.00000000000000000000,110.00000000000000000000,111.00000000000000000000,112,113,114.00000000000000000000,115.00000000000000000000,116.00000000000000000000,117.00000000000000000000,118.00000000000000000000,119.00000000000000000000,120.00000000000000000000,121,122,123",
                lines.get(7));
        assertEquals(
                "86400000,1,12,13.00000000000000000000,14.00000000000000000000,15.00000000000000000000,16,17.00000000000000000000,18.00000000000000000000,19.00000000000000000000,110.00000000000000000000,111.00000000000000000000,112,113,114.00000000000000000000,115.00000000000000000000,116.00000000000000000000,117.00000000000000000000,118.00000000000000000000,119.00000000000000000000,120.00000000000000000000,121,122,123",
                lines.get(8));
    }
    
    private final class AbstractSessionToTrackLoggerCsvExporterHarness extends AbstractSessionToTrackLoggerCsvExporter {

        public AbstractSessionToTrackLoggerCsvExporterHarness(File exportDir) {
            super(exportDir, new NoOpNotificationStrategy<SessionExporterNotificationType>());
        }

        @Override
        protected void exportEntries(Writer writer, int sessionId,
                Integer startLap, Integer endLap) throws IOException {
            
            writeEntry(writer, 1l, 2l, 3f, 4f, 5f, 6l, 7d, 8d, 9d, 10f, 11f, 12l, 13, 14d, 15d, 16d, 17d, 18d, 19d, 20d, 21l, 22, 23);
            writeEntry(writer, TimeUtil.MS_IN_DAY - 1, 12l, 13f, 14f, 15f, 16l, 17d, 18d, 19d, 110f, 111f, 112l, 113, 114d, 115d, 116d, 117d, 118d, 119d, 120d, 121l, 122, 123);
            writeEntry(writer, 1, 12l, 13f, 14f, 15f, 16l, 17d, 18d, 19d, 110f, 111f, 112l, 113, 114d, 115d, 116d, 117d, 118d, 119d, 120d, 121l, 122, 123);
        }
        
        @Override
        protected String getSplitMarkerSetName() {
            return "Split Marker Name!";
        }

        @Override
        protected Date getSessionStartTime() {
            return startTime;
        }
    }
}
