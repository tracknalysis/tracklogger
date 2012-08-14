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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.tracknalysis.common.notification.NoOpNotificationStrategy;
import net.tracknalysis.common.notification.NotificationStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Valeri
 */
public abstract class AbstractSessionToFileExporter implements SessionToFileExporter {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionToFileExporter.class);
    
    private DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
    
    private File exportFile;
    private OutputStream out;
    private File exportDir;
    private NotificationStrategy<SessionExporterNotificationType> notificationStrategy;
    
    public AbstractSessionToFileExporter(File exportDir, NotificationStrategy<SessionExporterNotificationType> notificationStrategy) {
        super();
        this.exportDir = exportDir;
        if (notificationStrategy == null) {
            notificationStrategy = new NoOpNotificationStrategy<SessionExporterNotificationType>();
        } else {
            this.notificationStrategy = notificationStrategy;
        }
    }
    
    @Override
    public final void setExportDir(File exportDir) {
        this.exportDir = exportDir;
    }
    
    @Override
    public final void export(int sessionId) {
        runExport(sessionId, null, null);
    }
    
    @Override
    public final void export(int sessionId, Integer startLap, Integer endLap) {
        runExport(sessionId, startLap, endLap);
    }
    
    @Override
    public final String getExportFileAbsolutePath() {
        return exportFile.getAbsolutePath();
    }
    
    /**
     * Writes the output to the output stream.  Implementations should flush the stream or any writers
     * that they instantiate on the stream; however, they should not close the stream as that is handled
     * by this class.
     *
     * @param out a buffered stream for writing the exported data
     * @param sessionId
     * @param startLap
     * @param endLap
     * @throws IOException 
     */
    protected abstract void export(OutputStream out, int sessionId,
            Integer startLap, Integer endLap) throws IOException;
    
    protected abstract Date getSessionStartTime(int sessionId);
    
    /**
     * Returns the file extension to append to the default file name.
     */
    protected abstract String getFileExtension();
    
    protected final NotificationStrategy<SessionExporterNotificationType> getNotificationStrategy() {
        return notificationStrategy;
    }
    
    protected final void sendExportProgressNotification(int currentRecord, int totalRecords) {
        getNotificationStrategy().sendNotification(
                SessionExporterNotificationType.EXPORT_PROGRESS,
                new ExportProgress(currentRecord, totalRecords));
    }
    
    private void runExport(int sessionId, Integer startLap, Integer endLap) {
        try {
            LOG.debug("Starting export.");
            notificationStrategy.sendNotification(SessionExporterNotificationType.EXPORT_STARTING);
            
            try {
                createLogFile(sessionId, startLap, endLap);
            } catch (IOException e) {
                out = null;
                exportFile = null;
                throw e;
            }
            
            LOG.debug("Started export.");
            notificationStrategy.sendNotification(SessionExporterNotificationType.EXPORT_STARTED);
            
            export(out, sessionId, startLap, endLap);
            
        } catch (Exception e) {
            LOG.error("Export failed due to an exception.");
            getNotificationStrategy().sendNotification(
                    SessionExporter.SessionExporterNotificationType.EXPORT_FAILED, e);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                    
                    getNotificationStrategy().sendNotification(
                            SessionExporter.SessionExporterNotificationType.EXPORT_FINISHED);
                } catch (Exception e) {
                    notificationStrategy.sendNotification(SessionExporterNotificationType.EXPORT_FAILED, e);
                } finally {
                    out = null;
                }
            }
        }
    }
    
    private void createLogFile(int sessionId, Integer startLap, Integer endLap) throws FileNotFoundException {
        String fileName = sessionId + "-" + dateFormat.format(getSessionStartTime(sessionId));
        
        fileName = fileName + "." + getFileExtension();
        
        exportFile = new File(exportDir, fileName);
        
        LOG.debug("Creating output stream to export file '{}'.",
                exportFile.getAbsolutePath());
        
        out = new BufferedOutputStream(new FileOutputStream(exportFile));
    }
}
