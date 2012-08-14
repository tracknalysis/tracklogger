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

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.notification.NotificationType;

/**
 * @author David Valeri
 */
public interface SessionExporterService {

    public static enum SessionExportStatus {
        /**
         * The session is not in the queue or another state in the exporter service.
         */
        NOT_QUEUED(0),
        /**
         * The session is queued for export in the service.
         */
        QUEUED(10),
        /**
         * The session export is beginning.
         */
        STARTING(20),
        /**
         * The session export initialization is complete and the export is running.
         */
        STARTED(30),
        /**
         * The session export executing and progress is being made.
         */
        EXPORTING(40),
        /**
         * The session export finished successfully.
         */
        FINISHED(50),
        /**
         * The session export failed to complete normally.
         */
        FAILED(60);
        
        private int weight;
        
        private SessionExportStatus(int weight) {
            this.weight = weight;
        }
        
        /**
         * Returns the numerical weight of the state such that later states in the lifecycle
         * will always have a higher weight than earlier states in the lifecycle.  The weight
         * value is arbitrary and may change between program version.
         * <p/>
         * The weight is useful for detecting duplicate events and/or ensuring that only
         * a transition to a later lifecycle state is only processed once.
         */
        public int getWeight() {
            return weight;
        }
    }
    
    /**
     * Notification types emitted by {@link SessionExporterService} implementations.
     */
    public enum SessionExporterServiceNotificationType implements NotificationType {
        /**
         * A general update about the state of an export.  Contains a {@link ExportNotification}.
         */
        SESSION_EXPORT_LIFECYCLE_NOTIFICATION;

        @Override
        public int getNotificationTypeId() {
            return ordinal();
        }
        
        private static final Map<Integer, SessionExporterServiceNotificationType> intToTypeMap = 
                new HashMap<Integer, SessionExporterServiceNotificationType>();
        
        static {
            for (SessionExporterServiceNotificationType type : SessionExporterServiceNotificationType.values()) {
                intToTypeMap.put(type.ordinal(), type);
            }
        }

        public static SessionExporterServiceNotificationType fromInt(int i) {
            SessionExporterServiceNotificationType type = intToTypeMap.get(Integer.valueOf(i));
            if (type == null) {
                throw new IllegalArgumentException(
                        "No enum const " + i);
            }
            return type;
        }
    }
    
    /**
     * A simple structure for holding status and progress information emitted with notifications
     * of type {@link SessionExporterServiceNotificationType#SESSION_EXPORT_LIFECYCLE_NOTIFICATION}.
     */
    public static final class SessionExportLifecycleNotification {
        private final SessionExportStatus status;
        private final ExportProgress progress;
        
        /**
         * Initialize the notification fields.
         * 
         * @param status
         *            the required status of session export represented by the
         *            notification
         * @param progress
         *            the optional progress information for the notification.
         *            The progress is expected to be non-{@code null} when the
         *            status is {@link SessionExportStatus#EXPORTING} and
         *            {@code null} for all other statuses.
         */
        public SessionExportLifecycleNotification(SessionExportStatus status,
                ExportProgress progress) {
            super();
            this.status = status;
            this.progress = progress;
        }

        /**
         * Returns the lifecycle status associated with the notification.
         */
        public SessionExportStatus getStatus() {
            return status;
        }

        /**
         * Returns the progress information associated with the notification.
         * The progress is expected to be non-{@code null} when the status is
         * {@link SessionExportStatus#EXPORTING} and {@code null} for all other
         * statuses.
         */
        public ExportProgress getProgress() {
            return progress;
        }
    }
    
    /**
     * Enqueue a request to export a session at some time in the future.
     */
    void enqueue(SessionExportRequest request);
    
    /**
     * Register the listener for notifications regarding the session with ID {@code sessionId}.
     */
    void register(
            int sessionId,
            NotificationStrategy<SessionExporterServiceNotificationType> notificationStrategy);

    /**
     * Unregister the listener for notifications regarding the session with ID {@code sessionId}.
     */
    void unRegister(
            int sessionId,
            NotificationStrategy<SessionExporterServiceNotificationType> notificationStrategy);
}
