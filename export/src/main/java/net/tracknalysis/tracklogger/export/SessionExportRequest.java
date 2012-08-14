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

/**
 * @author David Valeri
 */
public class SessionExportRequest {
    
    private final int sessionId;
    private final String exportFormatIdentifier;
    private final Integer startLap;
    private final Integer stopLap;
    
    public SessionExportRequest(int sessionId, String exportFormatIdentifier,
            Integer startLap, Integer stopLap) {
        super();
        this.sessionId = sessionId;
        this.exportFormatIdentifier = exportFormatIdentifier;
        this.startLap = startLap;
        this.stopLap = stopLap;
    }

    public int getSessionId() {
        return sessionId;
    }

    public String getExportFormatIdentifier() {
        return exportFormatIdentifier;
    }

    public Integer getStartLap() {
        return startLap;
    }

    public Integer getStopLap() {
        return stopLap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SessionExportRequest [sessionId=");
        builder.append(sessionId);
        builder.append(", exportFormatIdentifier=");
        builder.append(exportFormatIdentifier);
        builder.append(", startLap=");
        builder.append(startLap);
        builder.append(", stopLap=");
        builder.append(stopLap);
        builder.append("]");
        return builder.toString();
    }
}
