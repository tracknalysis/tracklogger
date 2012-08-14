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

/**
 * @author David Valeri
 */
public interface SessionExporter {
    
    public enum SessionExporterNotificationType implements net.tracknalysis.common.notification.NotificationType {
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
    
    void export(int sessionId);
    
    void export(int sessionId, Integer startLap, Integer endLap);
    
    String getMimeType();
}
