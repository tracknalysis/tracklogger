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
package net.tracknalysis.tracklogger._import;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.notification.NotificationListener;

/**
 * @author David Valeri
 */
public abstract class AbstractSplitMarkerSetCsvImporter extends AbstractSplitMarkerSetImporter implements
        SplitMarkerSetImporter {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSplitMarkerSetCsvImporter.class);
    
    protected AbstractSplitMarkerSetCsvImporter(
            NotificationListener<SplitMarkerSetImporterNotificationType> notificationStrategy) {
        super(notificationStrategy);
    }

    @Override
    public final void doImport() {
        getNotificationStrategy().onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTING);
        
        InputStream is = null;
        int counter = 0;
        
        try {
            is = getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            
            getNotificationStrategy().onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTED);
            
            createSplitMarkerSet(getName());
            
            String line = br.readLine();
            while (line != null) {
                
                String[] tokens = line.split(",");
                
                if (tokens.length != 3) {
                    LOG.error("Import row [{}] does not contain 3 tokens.", line);
                    getNotificationStrategy()
                            .onNotification(
                                    SplitMarkerSetImporterNotificationType.IMPORT_FAILED);
                    return;
                }
                
                try {
                    createSplitMarker(tokens[0].trim(), Double.valueOf(tokens[1]), Double.valueOf(tokens[2]));
                    getNotificationStrategy().onNotification(SplitMarkerSetImporterNotificationType.IMPORT_PROGRESS,
                            new ImportProgress(counter++, null));
                } catch (NumberFormatException e) {
                    LOG.error(
                            "Number format error parsing latitude and longitude data from import row ["
                                    + line + "].", e);
                    getNotificationStrategy()
                            .onNotification(
                                    SplitMarkerSetImporterNotificationType.IMPORT_FAILED);
                    return;
                }
                
                line = br.readLine();
            }
            
            commitTx();
            
            getNotificationStrategy().onNotification(SplitMarkerSetImporterNotificationType.IMPORT_FINISHED, getId());
        } catch (Exception e) {
            LOG.error("Unknown error performing the import.", e);
            getNotificationStrategy().onNotification(SplitMarkerSetImporterNotificationType.IMPORT_FAILED, e);
            
            try {
                rollbackTx();
            } catch (Exception e2) {
                LOG.warn("Error trying to rollback import.", e);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    LOG.warn("Error cleaning up input stream.", e);
                }
            }
        }
    }

    /**
     * Creates and stores the split marker set based on the provided name.
     *
     * @param name the name to give the new set
     */
    protected abstract void createSplitMarkerSet(String name);
    
    /**
     * Create a new split marker as part of the previously created set.
     *
     * @param name the name of the split marker
     * @param lat the latitude of the marker's position in degrees
     * @param lon the longitude of the marker's position in degrees
     */
    protected abstract void createSplitMarker(String name, double lat, double lon);
    
    /**
     * Discard any previously stored information that was stored as part of the import.
     * Called if an exception is encountered while executing {@link #doImport()}.
     */
    protected void rollbackTx() throws Exception {
    }
    
    /**
     * Commit any previously stored information that was stored as part of the import.
     * Called if {@link #doImport()} completes successfully.
     */
    protected void commitTx() throws Exception {
    }

    /**
     * Returns an input stream for the CSV formatted content.
     *
     * @throws IOException if there is an error obtaining the stream
     */
    protected abstract InputStream getInputStream() throws IOException;
}
