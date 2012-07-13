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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David Valeri
 */
public class TimingData extends AbstractData {

    private long time;
    private int splitIndex;
    private int lap;
    private Long lapTime;
    private Long splitTime;
    private Long bestLapTime;
    private List<Long> bestSplitTimes;
    private long initialLapStartDataReceivedTime;
    private long lastLapStartDataReceivedTime;
    private long lastSplitStartDataReceivedTime;
    
    /**
     * Returns the UTC time in the day when the lap time was captured.  Used for synchronization
     * with other data.
     */
    public long getTime() {
        return time;
    }
    
    protected void setTime(long time) {
        this.time = time;
    }
    
    /**
     * Returns the zero based index of the split marker that was just completed.
     */
    public int getSplitIndex() {
        return splitIndex;
    }
    
    protected void setSplitIndex(int splitIndex) {
        this.splitIndex = splitIndex;
    }

    /**
     * Returns the zero based lap counter.  That is, the first timing data
     * will be for lap 0 and will have no valid time information.  This number
     * indicates the lap that is in progress or just completed.
     */
    public int getLap() {
        return lap;
    }

    protected void setLap(int lap) {
        this.lap = lap;
    }

    /**
     * Returns the duration, in milliseconds of the completed lap or {@code null} if no
     * laps have been completed.  That is, the first timing event will not contain a lap time.
     */
    public Long getLapTime() {
        return lapTime;
    }
    
    protected void setLapTime(Long lapTime) {
        this.lapTime = lapTime;
    }

    /**
     * Returns the duration, in milliseconds of the completed segment or {@code null} if no
     * segments have been completed.  That is, the first timing event will not contain a split time.
     */
    public Long getSplitTime() {
        return splitTime;
    }

    protected void setSplitTime(Long splitTime) {
        this.splitTime = splitTime;
    }
    
    /**
     * Returns the duration, in milliseconds of the fasted completed lap in the
     * session or {@code null} if no laps have been completed.
     */
    public Long getBestLapTime() {
        return bestLapTime;
    }

    protected void setBestLapTime(Long bestLapTime) {
        this.bestLapTime = bestLapTime;
    }

    /**
     * Returns the duration, in milliseconds of the fasted completion time for
     * each segment in the session. Each element may contain {@code null} if
     * that segment has not yet been completed in the current session.
     */
    public List<Long> getBestSplitTimes() {
        return bestSplitTimes;
    }

    protected void setBestSplitTimes(List<Long> bestSplitTimes) {
        this.bestSplitTimes = Collections.unmodifiableList(new ArrayList<Long>(bestSplitTimes));
    }
    
    /**
     * Gets the time at which the first lap start was received from the source in milliseconds
     * since midnight January 1, 1970 UTC.
     */
    public long getInitialLapStartDataReceivedTime() {
        return initialLapStartDataReceivedTime;
    }

    protected void setInitialLapStartDataReceivedTime(long initialLapStartDataReceivedTime) {
        this.initialLapStartDataReceivedTime = initialLapStartDataReceivedTime;
    }

    /**
     * Gets the time at which the last lap start was received from the source in milliseconds
     * since midnight January 1, 1970 UTC.
     */
    public long getLastLapStartDataReceivedTime() {
        return lastLapStartDataReceivedTime;
    }

    protected void setLastLapStartDataReceivedTime(long lastLapStartDataReceivedTime) {
        this.lastLapStartDataReceivedTime = lastLapStartDataReceivedTime;
    }
    
    /**
     * Gets the time at which the last lap split start was received from the source in milliseconds
     * since midnight January 1, 1970 UTC.
     */
    public long getLastSplitStartDataReceivedTime() {
        return lastSplitStartDataReceivedTime;
    }

    protected void setLastSplitStartDataReceivedTime(
            long lastSplitStartDataReceivedTime) {
        this.lastSplitStartDataReceivedTime = lastSplitStartDataReceivedTime;
    }

    public static class TimingDataBuilder extends AbstractDataBuilder<TimingData> {
        
        long time;
        int splitIndex;
        int lap;
        private Long lapTime;
        private Long splitTime;
        private Long bestLapTime;
        private List<Long> bestSplitTimes;
        private long initialLapStartDataReceivedTime;
        private long lastLapStartDataReceivedTime;
        private long lastSplitStartDataReceivedTime;
        
        public long getTime() {
            return time;
        }
        public void setTime(long time) {
            this.time = time;
        }
        public int getSplitIndex() {
            return splitIndex;
        }
        public void setSplitIndex(int splitIndex) {
            this.splitIndex = splitIndex;
        }
        public int getLap() {
            return lap;
        }
        public void setLap(int lap) {
            this.lap = lap;
        }
        
        public Long getLapTime() {
            return lapTime;
        }
        
        public void setLapTime(Long lapTime) {
            this.lapTime = lapTime;
        }
        
        public Long getSplitTime() {
            return splitTime;
        }
        
        public void setSplitTime(Long splitTime) {
            this.splitTime = splitTime;
        }
        
        public Long getBestLapTime() {
            return bestLapTime;
        }
        
        public void setBestLapTime(Long bestLapTime) {
            this.bestLapTime = bestLapTime;
        }
        
        public List<Long> getBestSplitTimes() {
            return bestSplitTimes;
        }
        
        public void setBestSplitTimes(List<Long> bestSplitTimes) {
            this.bestSplitTimes = bestSplitTimes;
        }
        
        public long getInitialLapStartDataReceivedTime() {
            return initialLapStartDataReceivedTime;
        }
        public void setInitialLapStartDataReceivedTime(
                long initialLapStartDataReceivedTime) {
            this.initialLapStartDataReceivedTime = initialLapStartDataReceivedTime;
        }
        public long getLastLapStartDataReceivedTime() {
            return lastLapStartDataReceivedTime;
        }
        public void setLastLapStartDataReceivedTime(long lastLapStartDataReceivedTime) {
            this.lastLapStartDataReceivedTime = lastLapStartDataReceivedTime;
        }
        public long getLastSplitStartDataReceivedTime() {
            return lastSplitStartDataReceivedTime;
        }
        public void setLastSplitStartDataReceivedTime(
                long lastSplitStartDataReceivedTime) {
            this.lastSplitStartDataReceivedTime = lastSplitStartDataReceivedTime;
        }
        @Override
        protected TimingData doBuild() {
            TimingData newData = new TimingData();
            
            newData.setTime(getTime());
            newData.setSplitIndex(getSplitIndex());
            newData.setLap(getLap());
            newData.setLapTime(getLapTime());
            newData.setSplitTime(getSplitTime());
            newData.setBestLapTime(getBestLapTime());
            newData.setBestSplitTimes(getBestSplitTimes());
            newData.setInitialLapStartDataReceivedTime(getInitialLapStartDataReceivedTime());
            newData.setLastLapStartDataReceivedTime(getLastLapStartDataReceivedTime());
            newData.setLastSplitStartDataReceivedTime(getLastSplitStartDataReceivedTime());
            
            return newData;
        }
    }
}
