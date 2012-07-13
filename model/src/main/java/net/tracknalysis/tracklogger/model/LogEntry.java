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
package net.tracknalysis.tracklogger.model;

/**
 * @author David Valeri
 */
public final class LogEntry {
    
    private long synchTimestamp;
    private int sessionId;
    private AccelData accelData;
    private LocationData locationData;
    private EcuData ecuData;
    
    public LogEntry(long synchTimestamp, int sessionId, AccelData accelData,
            LocationData locationData, EcuData ecuData) {
        this.synchTimestamp = synchTimestamp;
        this.sessionId = sessionId;
        this.accelData = accelData;
        this.locationData = locationData;
        this.ecuData = ecuData;
    }
    
    public long getSynchTimestamp() {
        return synchTimestamp;
    }
    
    public int getSessionId() {
        return sessionId;
    }

    public AccelData getAccelData() {
        return accelData;
    }

    public LocationData getLocationData() {
        return locationData;
    }

    public EcuData getEcuData() {
        return ecuData;
    }
}