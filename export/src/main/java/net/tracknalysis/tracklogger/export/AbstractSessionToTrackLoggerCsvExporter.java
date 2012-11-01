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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.util.TimeUtil;

/**
 * @author David Valeri
 */
public abstract class AbstractSessionToTrackLoggerCsvExporter extends
        AbstractSessionToFileExporter {
    
    protected static final String TRACKLOGGER_CSV_1_0_b = "TrackLogger CSV 1.0b";
    
    private long lastSyncTimestamp = -1;
    private long runningTime = 0;

    public AbstractSessionToTrackLoggerCsvExporter(File exportDir,
            NotificationStrategy<SessionExporterNotificationType> notificationStrategy) {
        super(exportDir, notificationStrategy);
    }
    
    @Override
    public String getMimeType() {
        return "text/csv";
    }

    @Override
    protected final void export(OutputStream out, int sessionId, Integer startLap,
            Integer endLap) throws IOException {
        
        Writer writer;
        try {
            writer = new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            writer = new OutputStreamWriter(out);
        }
        
        writeHeader(writer, sessionId);
        
        exportEntries(writer, sessionId, startLap, endLap);
        
        writer.flush();
    }
    
    protected abstract void exportEntries(Writer writer, int sessionId,
            Integer startLap, Integer endLap) throws IOException;
    
    protected abstract String getSplitMarkerSetName();

    protected final void writeEntry(Writer writer,
            long logSynchTimestamp,
            Long accelCaptureTimestamp, Float longitudalAccel, Float lateralAccel, Float verticalAccel,
            Long locationCaptureTimestamp, Double latitude, Double longitude, Double altitude, Float speed, Float bearing,
            Long ecuCaptureTimestamp, Integer rpm, Double map, Double tp, Double afr, Double mat, Double clt, Double ignAdv, Double batV,
            Long timingCaptureTimestamp, int lap, int splitIndex) throws IOException {
        
        if (lastSyncTimestamp == -1) {
            lastSyncTimestamp = logSynchTimestamp;
        }
        
        if (logSynchTimestamp < lastSyncTimestamp) {
            // We rolled over days so we need to account for that in the maths.
            runningTime += (TimeUtil.MS_IN_DAY + logSynchTimestamp) - lastSyncTimestamp;
        } else {
            runningTime += logSynchTimestamp - lastSyncTimestamp;
        }
        
        lastSyncTimestamp = logSynchTimestamp;
        
        writer.write(String.valueOf(runningTime));
        writer.write(",");
        writer.write(String.valueOf(logSynchTimestamp));
        writer.write(",");
        writer.write(accelCaptureTimestamp == null ? "" : accelCaptureTimestamp.toString());
        writer.write(",");
        writer.write(longitudalAccel == null ? "" : formatFloat(longitudalAccel));
        writer.write(",");
        writer.write(lateralAccel == null ? "" : formatFloat(lateralAccel));
        writer.write(",");
        writer.write(verticalAccel == null ? "" : formatFloat(verticalAccel));
        writer.write(",");
        
        writer.write(locationCaptureTimestamp == null ? "" : locationCaptureTimestamp.toString());
        writer.write(",");
        writer.write(latitude == null ? "" : formatDouble(latitude));
        writer.write(",");
        writer.write(longitude == null ? "" : formatDouble(longitude));
        writer.write(",");
        writer.write(altitude == null ? "" : formatDouble(altitude));
        writer.write(",");
        writer.write(speed == null ? "" : formatFloat(speed));
        writer.write(",");
        writer.write(bearing == null ? "" : formatFloat(bearing));
        writer.write(",");
        
        writer.write(ecuCaptureTimestamp == null ? "" : ecuCaptureTimestamp.toString());
        writer.write(",");
        writer.write(rpm == null ? "" : rpm.toString());
        writer.write(",");
        writer.write(map == null ? "" : formatDouble(map));
        writer.write(",");
        writer.write(tp == null ? "" : formatDouble(tp));
        writer.write(",");
        writer.write(afr == null ? "" : formatDouble(afr));
        writer.write(",");
        writer.write(mat == null ? "" : formatDouble(mat));
        writer.write(",");
        writer.write(clt == null ? "" : formatDouble(clt));
        writer.write(",");
        writer.write(ignAdv == null ? "" : formatDouble(ignAdv));
        writer.write(",");
        writer.write(batV == null ? "" : formatDouble(batV));
        writer.write(",");
        
        writer.write(timingCaptureTimestamp.toString());
        writer.write(",");
        writer.write(Integer.toString(lap));
        writer.write(",");
        writer.write(Integer.toString(splitIndex));
        writer.write("\r\n");
    }
    
    private String formatFloat(Float value) {
        return String.format("%.20f", value);
    }
    
    private String formatDouble(Double value) {
        return String.format("%.20f", value);
    }
    
    private void writeHeader(Writer writer, int sessionId) throws IOException {
        writer.write("Session Title: " + sessionId + " - " + getSessionStartTime() + "\r\n");
        writer.write("Split Marker Set: " + getSplitMarkerSetName() + "\r\n");
        writer.write("Export Time: " + new Date() + "\r\n");
        writer.write("Format: " + TRACKLOGGER_CSV_1_0_b + "\r\n\r\n");
        
        writer.write("running_time,"
                + "synch_timestamp,accel_capture_timestamp,longitudinal_accel,lateral_accel,vertical_accel," 
                + "location_capture_timestamp,latitude,longitude,altitude,speed,bearing,"
                + "ecu_capture_timestamp,rpm,map,throttle_position,afr,mat,clt,ignition_advance,battery_voltage,"
                + "lap_capture_timestamp,lap,split\r\n");
    }

    @Override
    protected String getFileExtension() {
        return "csv";
    }
}
