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

import java.util.HashMap;
import java.util.Map;

import net.tracknalysis.common.notification.NotificationType;

/**
 * @author David Valeri
 */
public interface SessionExporter {
    
    public static enum SessionExporterNotificationType implements NotificationType {
        EXPORT_STARTING,
        EXPORT_STARTED,
        /**
         * Triggered when progress is made in the export. The notification body
         * is a {@link ExportProgress} instance.
         */
        EXPORT_PROGRESS,
        EXPORT_FINISHED,
        EXPORT_FAILED;

        @Override
        public int getNotificationTypeId() {
            return ordinal();
        }
        
        private static final Map<Integer, SessionExporterNotificationType> intToTypeMap = 
                new HashMap<Integer, SessionExporterNotificationType>();
        
        static {
            for (SessionExporterNotificationType type : SessionExporterNotificationType.values()) {
                intToTypeMap.put(type.ordinal(), type);
            }
        }

        public static SessionExporterNotificationType fromInt(int i) {
            SessionExporterNotificationType type = intToTypeMap.get(Integer.valueOf(i));
            if (type == null) {
                throw new IllegalArgumentException(
                        "No enum const " + i);
            }
            return type;
        }
    }
    
    public static final class ExportProgress {
        private int currentRecordIndex;
        private int totalRecords;
        
        public ExportProgress(int currentRecordIndex, int totalRecords) {
            super();
            this.currentRecordIndex = currentRecordIndex;
            this.totalRecords = totalRecords;
        }

        public int getCurrentRecordIndex() {
            return currentRecordIndex;
        }

        public void setCurrentRecordIndex(int currentRecordIndex) {
            this.currentRecordIndex = currentRecordIndex;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ExportProgress [currentRecordIndex=");
            builder.append(currentRecordIndex);
            builder.append(", totalRecords=");
            builder.append(totalRecords);
            builder.append("]");
            return builder.toString();
        }
    }
    
    /**
     * Exports the session with the given ID, emitting {@link SessionExporterNotificationType} notifications.
     *
     * @param sessionId the ID of the session to export
     */
    void export(int sessionId);
    
    /**
     * Exports the session with the given ID, emitting
     * {@link SessionExporterNotificationType}s.
     * 
     * @param sessionId
     *            the ID of the session to export
     * @param startLap
     *            the optional lap to begin exporting on. If {@code null} the
     *            export will start on the first lap.
     * @param endLap
     *            the optional lap to stop exporting on. If {@code null} the
     *            export runs through the end of the session
     */
    void export(int sessionId, Integer startLap, Integer endLap);
    
    /**
     * Returns the mime type of the output that the exporter produces.
     */
    String getMimeType();
}
