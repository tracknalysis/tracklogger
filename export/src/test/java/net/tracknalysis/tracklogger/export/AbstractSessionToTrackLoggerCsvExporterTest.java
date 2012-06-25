package net.tracknalysis.tracklogger.export;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

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
        assertEquals(4, lines.size());
        
        assertEquals("Session 1 - Wed Dec 31 19:00:00 EST 1969", lines.get(0));
        assertEquals(
                "synch_timestamp,accel_capture_timestamp,longitudinal_accel,lateral_accel,vertical_accel,location_capture_timestamp,latitude,longitude,altitude,speed,bearing,ecu_capture_timestamp,rpm,map,throttle_position,afr,mat,clt,ignition_advance,battery_voltage,lap_capture_timestamp,lap,split",
                lines.get(1));
        assertEquals(
                "1,2,3.00000000000000000000,4.00000000000000000000,5.00000000000000000000,6,7.00000000000000000000,8.00000000000000000000,9.00000000000000000000,10.00000000000000000000,11.00000000000000000000,12,13,14.00000000000000000000,15.00000000000000000000,16.00000000000000000000,17.00000000000000000000,18.00000000000000000000,19.00000000000000000000,20.00000000000000000000,21,22,23",
                lines.get(2));
        assertEquals(
                "11,12,13.00000000000000000000,14.00000000000000000000,15.00000000000000000000,16,17.00000000000000000000,18.00000000000000000000,19.00000000000000000000,110.00000000000000000000,111.00000000000000000000,112,113,114.00000000000000000000,115.00000000000000000000,116.00000000000000000000,117.00000000000000000000,118.00000000000000000000,119.00000000000000000000,120.00000000000000000000,121,122,123",
                lines.get(3));
    }
    
    private final class AbstractSessionToTrackLoggerCsvExporterHarness extends AbstractSessionToTrackLoggerCsvExporter {

        public AbstractSessionToTrackLoggerCsvExporterHarness(File exportDir) {
            super(exportDir);
        }

        @Override
        protected void exportEntries(Writer writer, int sessionId,
                Integer startLap, Integer endLap) throws IOException {
            
            writeEntry(writer, 1l, 2l, 3f, 4f, 5f, 6l, 7d, 8d, 9d, 10f, 11f, 12l, 13, 14d, 15d, 16d, 17d, 18d, 19d, 20d, 21l, 22, 23);
            writeEntry(writer, 11l, 12l, 13f, 14f, 15f, 16l, 17d, 18d, 19d, 110f, 111f, 112l, 113, 114d, 115d, 116d, 117d, 118d, 119d, 120d, 121l, 122, 123);
        }

        @Override
        protected Date getSessionStartTime(int sessionId) {
            return startTime;
        }
    }
}
