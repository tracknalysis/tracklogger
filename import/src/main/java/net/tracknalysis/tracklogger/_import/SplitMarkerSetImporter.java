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

import java.util.HashMap;
import java.util.Map;

import net.tracknalysis.common.notification.NotificationType;

/**
 * @author David Valeri
 */
public interface SplitMarkerSetImporter {
    
    public static enum SplitMarkerSetImporterNotificationType implements NotificationType {
        IMPORT_STARTING,
        IMPORT_STARTED,
        /**
         * Triggered when progress is made in the import. The notification body
         * is a {@link ImportProgress} instance.
         */
        IMPORT_PROGRESS,
        /**
         * The notification body contains the ID of the imported split marker set as an {@link Integer}.
         */
        IMPORT_FINISHED,
        /**
         * The notification body may contain an exception if an exception caused the failure. 
         */
        IMPORT_FAILED;

        @Override
        public int getNotificationTypeId() {
            return ordinal();
        }
        
        private static final Map<Integer, SplitMarkerSetImporterNotificationType> intToTypeMap = 
                new HashMap<Integer, SplitMarkerSetImporterNotificationType>();
        
        static {
            for (SplitMarkerSetImporterNotificationType type : SplitMarkerSetImporterNotificationType.values()) {
                intToTypeMap.put(type.ordinal(), type);
            }
        }

        public static SplitMarkerSetImporterNotificationType fromInt(int i) {
            SplitMarkerSetImporterNotificationType type = intToTypeMap.get(Integer.valueOf(i));
            if (type == null) {
                throw new IllegalArgumentException(
                        "No enum const " + i);
            }
            return type;
        }
    }
    
    /**
     * Simple structure for import progress information.
     */
    public static final class ImportProgress {
        private final int currentRecord;
        private final Integer totalRecords;
        
        /**
         * Creates a new instance with the specified progress information.
         * 
         * @param currentRecord
         *            the index of the current record imported
         * @param totalRecords
         *            the optional number representing the total number of
         *            records to import. {@code null} if the number is unknown
         *            at this point in the import.
         */
        public ImportProgress(int currentRecord, Integer totalRecords) {
            super();
            this.currentRecord = currentRecord;
            this.totalRecords = totalRecords;
        }
        
        /**
         * Returns the current index into the total number of records.
         */
        public int getCurrentRecord() {
            return currentRecord;
        }
        
        /**
         * Returns the optional total number of records.  {@code null} if the total number
         * is not known at this point in the import.
         */
        public Integer getTotalRecords() {
            return totalRecords;
        }
    }
    
    /**
     * Executes the import, emitting {@link SplitMarkerSetImporterNotificationType} notifications.
     */
    void doImport();
    
    /**
     * Returns a short description of the import for display to a user.  May be
     * called before, during, and after {@link #doImport()}.
     */
    String getImportDescription();

    /**
     * Returns the name of the resulting split marker set.  May be
     * called before, during, and after {@link #doImport()}.
     */
    String getName();
    
    /**
     * Returns the ID that was assigned to the split marker set that was imported.  Only returns
     * a valid result after {@link #doImport()} completes without error.  Otherwise returns -1.
     */
    int getId();
}
