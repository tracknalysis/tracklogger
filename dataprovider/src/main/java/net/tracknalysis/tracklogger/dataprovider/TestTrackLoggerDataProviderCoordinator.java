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
package net.tracknalysis.tracklogger.dataprovider;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.tracknalysis.tracklogger.model.LogEntry;
import net.tracknalysis.tracklogger.model.TimingEntry;

/**
 * An implementation used for unit testing of {@link TrackLoggerDataProviderCoordinator} using existing
 * {@link DataProvider} instances and recording "logged" data to collections for later examination.
 *
 * @author David Valeri
 */
public class TestTrackLoggerDataProviderCoordinator extends TrackLoggerDataProviderCoordinator {

    private AccelDataProvider accelDataProvider;
    private LocationDataProvider locationDataProvider;
    private EcuDataProvider ecuDataProvider;
    private TimingDataProvider timingDataProvider;
    
    private List<LogEntry> logEntries;
    private List<TimingEntry> timingEntries;
    
    private AtomicInteger sessionCounter = new AtomicInteger();
    
    public TestTrackLoggerDataProviderCoordinator(
            AccelDataProvider accelDataProvider,
            LocationDataProvider locationDataProvider,
            EcuDataProvider ecuDataProvider,
            TimingDataProvider timingDataProvider) {
        this.accelDataProvider = accelDataProvider;
        this.locationDataProvider = locationDataProvider;
        this.ecuDataProvider = ecuDataProvider;
        this.timingDataProvider = timingDataProvider;
        
        logEntries = new LinkedList<LogEntry>();
        timingEntries = new LinkedList<TimingEntry>();
    }
    
    /**
     * Returns the {@link LogEntry} instances "logged".
     */
    public List<LogEntry> getLogEntries() {
        return logEntries;
    }
    
    /**
     * Returns the {@link TimingEntry} instances "logged".
     */
    public List<TimingEntry> getTimingEntries() {
        return timingEntries;
    }
    
    public int getSessionCount() {
        return sessionCounter.get();
    }
    
    @Override
    protected AccelDataProvider getAccelDataProvider() {
        return accelDataProvider;
    }

    @Override
    protected LocationDataProvider getLocationDataProvider() {
        return locationDataProvider;
    }

    @Override
    protected EcuDataProvider getEcuDataProvider() {
        return ecuDataProvider;
    }

    @Override
    protected TimingDataProvider getTimingDataProvider() {
        return timingDataProvider;
    }

    @Override
    protected int createSession() {
        return sessionCounter.incrementAndGet();
    }

    @Override
    protected void openSession(int sessionId) {
    }

    @Override
    protected void storeLogEntry(LogEntry logEntry) {
        logEntries.add(logEntry);
    }

    @Override
    protected void storeTimingEntry(TimingEntry timingEntry) {
        timingEntries.add(timingEntry);            
    }
}