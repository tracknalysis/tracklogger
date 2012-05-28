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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David Valeri
 */
public class TimingData extends AbstractData {

    long time;
    int splitIndex;
    int lap;
    private Long lapTime;
    private Long splitTime;
    private Long bestLapTime;
    private List<Long> bestSplitTimes;
    
    public long getTime() {
        return time;
    }
    
    protected void setTime(long time) {
        this.time = time;
    }
    
    /**
     * Returns the zero based index of the current split marker.
     */
    public int getSplitIndex() {
        return splitIndex;
    }
    
    protected void setSplitIndex(int splitIndex) {
        this.splitIndex = splitIndex;
    }

    /**
     * Returns the one based lap counter.
     */
    public int getLap() {
        return lap;
    }

    protected void setLap(int lap) {
        this.lap = lap;
    }

    /**
     * Returns the duration, in milliseconds of the completed lap or {@code null} if no
     * laps have been completed.
     */
    public Long getLapTime() {
        return lapTime;
    }
    
    protected void setLapTime(Long lapTime) {
        this.lapTime = lapTime;
    }

    /**
     * Returns the duration, in milliseconds of the completed segment or {@code null} if no
     * segments have been completed.
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

    public void setBestLapTime(Long bestLapTime) {
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

    public void setBestSplitTimes(List<Long> bestSplitTimes) {
        this.bestSplitTimes = Collections.unmodifiableList(new ArrayList<Long>(bestSplitTimes));
    }

    public static class TimingDataBuilder extends AbstractDataBuilder<TimingData> {
        
        long time;
        int splitIndex;
        int lap;
        private Long lapTime;
        private Long splitTime;
        private Long bestLapTime;
        private List<Long> bestSplitTimes;
        
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
            
            return newData;
        }
    }
}
