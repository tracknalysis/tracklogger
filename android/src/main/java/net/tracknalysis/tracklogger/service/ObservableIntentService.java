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
package net.tracknalysis.tracklogger.service;

import android.annotation.SuppressLint;
import android.app.Activity;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.notification.NotificationType;

/**
 * Interface for handling the queuing and processing of requests.
 * <p/>
 * Allows rebinding to the state of a previous request and also to the state of the entire service for
 * enhanced integration with the {@link Activity} lifecycle.
 * 
 * @author David Valeri
 */
public interface ObservableIntentService {
    
    public static enum RequestLifecycleStatus {
        /**
         * The request is not in the queue or another state in the service.
         */
        NOT_QUEUED(0),
        /**
         * The request is queued in the service.
         */
        QUEUED(10),
        /**
         * The request is beginning.
         */
        STARTING(20),
        /**
         * The request initialization is complete and the request is running.
         */
        STARTED(30),
        /**
         * The request is executing and progress is being made.
         */
        RUNNING(40),
        /**
         * The request finished successfully.
         */
        FINISHED(50),
        /**
         * The request failed to complete normally.
         */
        FAILED(60);
        
        private int weight;
        
        private RequestLifecycleStatus(int weight) {
            this.weight = weight;
        }
        
        /**
         * Returns the numerical weight of the state such that later states in the lifecycle
         * will always have a higher weight than earlier states in the lifecycle.  The weight
         * value is arbitrary and may change between program version.
         * <p/>
         * The weight is useful for detecting duplicate events and/or ensuring that
         * a transition to a later lifecycle state is only processed once.
         */
        public int getWeight() {
            return weight;
        }
    }
    
    /**
     * Notification types emitted by {@link ObservableIntentService}
     * implementations regarding a single request's details.
     */
    public enum RequestNotificationType implements NotificationType {
        /**
         * A general update about the state of a request.  Contains a {@link RequestLifecycleNotification}.
         */
        REQUEST_LIFECYCLE_NOTIFICATION;

        @Override
        public int getNotificationTypeId() {
            return ordinal();
        }
        
        @SuppressLint("UseSparseArrays")
        private static final Map<Integer, RequestNotificationType> intToTypeMap = 
                new HashMap<Integer, RequestNotificationType>();
        
        static {
            for (RequestNotificationType type : RequestNotificationType.values()) {
                intToTypeMap.put(type.ordinal(), type);
            }
        }

        public static RequestNotificationType fromInt(int i) {
            RequestNotificationType type = intToTypeMap.get(Integer.valueOf(i));
            if (type == null) {
                throw new IllegalArgumentException(
                        "No enum const " + i);
            }
            return type;
        }
    }
    
    /**
     * A simple structure for holding status and progress information emitted with notifications
     * of type {@link RequestNotificationType#REQUEST_LIFECYCLE_NOTIFICATION}.
     */
    public static final class RequestLifecycleNotification<U> {
        private final RequestLifecycleStatus status;
        private final U progress;
        
        /**
         * Initialize the notification fields.
         * 
         * @param status
         *            the required status of request represented by the
         *            notification
         * @param progress
         *            the optional progress information for the notification.
         *            The progress is expected to be non-{@code null} when the
         *            status is {@link RequestLifecycleStatus#RUNNING} and
         *            {@code null} for all other statuses.
         */
        public RequestLifecycleNotification(RequestLifecycleStatus status,
                U progress) {
            super();
            this.status = status;
            this.progress = progress;
        }

        /**
         * Returns the lifecycle status associated with the notification.
         */
        public RequestLifecycleStatus getStatus() {
            return status;
        }

        /**
         * Returns the progress information associated with the notification.
         * The progress is expected to be non-{@code null} when the status is
         * {@link RequestLifecycleStatus#RUNNING} and {@code null} for all other
         * statuses.
         */
        public U getProgress() {
            return progress;
        }
    }
    
    /**
     * Notification types emitted by {@link ObservableIntentService}
     * implementations regarding the entire service's details.
     */
    public enum ObservableIntentServiceNotificationType implements NotificationType {
        /**
         * A general update about the state of the service. Contains an
         * {@link ObservableIntentServiceRequestStatusNotification}.
         */
        REQUEST_STATUS_NOTIFICATION;

        @Override
        public int getNotificationTypeId() {
            return ordinal();
        }
        
        @SuppressLint("UseSparseArrays")
        private static final Map<Integer, ObservableIntentServiceNotificationType> intToTypeMap = 
                new HashMap<Integer, ObservableIntentServiceNotificationType>();
        
        static {
            for (ObservableIntentServiceNotificationType type : ObservableIntentServiceNotificationType.values()) {
                intToTypeMap.put(type.ordinal(), type);
            }
        }

        public static ObservableIntentServiceNotificationType fromInt(int i) {
            ObservableIntentServiceNotificationType type = intToTypeMap.get(Integer.valueOf(i));
            if (type == null) {
                throw new IllegalArgumentException(
                        "No enum const " + i);
            }
            return type;
        }
    }
    
    /**
     * General notification about the state of the service as a whole.  Included in notifications
     * of type {@link ObservableIntentServiceNotificationType#REQUEST_STATUS_NOTIFICATION}.
     */
    public static class ObservableIntentServiceRequestStatusNotification {
        Map<Integer, RequestLifecycleStatus> requestStatusMap;

        public ObservableIntentServiceRequestStatusNotification(Map<Integer, RequestLifecycleStatus> requestMap) {
            this.requestStatusMap = Collections
                    .unmodifiableMap(new LinkedHashMap<Integer, ObservableIntentService.RequestLifecycleStatus>(
                            requestMap));
        }
        
        /**
         * Returns an ordered map of request IDs to the request's current status in the service.
         * Requests are ordered in the order that the service received them.
         */
        public Map<Integer, RequestLifecycleStatus> getRequestStatusMap() {
            return requestStatusMap;
        }
    }

    /**
     * Register a listener for notifications regarding the overall state of the service.
     */
    void register(NotificationStrategy<ObservableIntentServiceNotificationType> notificationStrategy);
    
    /**
     * Unregister a listener for notifications regarding the overall state of the service.
     */
    void unRegister(NotificationStrategy<ObservableIntentServiceNotificationType> notificationStrategy);
    
    /**
     * Register the listener for notifications regarding the request with ID {@code requestId}.
     * Upon registration a notification of type {@link RequestNotificationType#REQUEST_LIFECYCLE_NOTIFICATION} 
     * will be sent indicating the current state of the request in the service.
     */
    void register(
            int requestId,
            NotificationStrategy<RequestNotificationType> notificationStrategy);

    /**
     * Unregister the listener for notifications regarding the session with ID {@code requestId}.
     */
    void unRegister(
            int requestId,
            NotificationStrategy<RequestNotificationType> notificationStrategy);
}
