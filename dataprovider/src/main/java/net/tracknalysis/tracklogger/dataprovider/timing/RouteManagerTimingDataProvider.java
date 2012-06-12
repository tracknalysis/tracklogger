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
package net.tracknalysis.tracklogger.dataprovider.timing;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.util.TimeUtil;
import net.tracknalysis.location.Route;
import net.tracknalysis.location.RouteListener;
import net.tracknalysis.location.RouteManager;
import net.tracknalysis.tracklogger.dataprovider.AbstractDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TimingData;
import net.tracknalysis.tracklogger.dataprovider.TimingData.TimingDataBuilder;
import net.tracknalysis.tracklogger.dataprovider.TimingDataProvider;

/**
 * @author David Valeri
 */
public class RouteManagerTimingDataProvider extends AbstractDataProvider<TimingData>
        implements TimingDataProvider, RouteListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(RouteManagerTimingDataProvider.class);
    
    private final RouteManager routeManager;
    private final Route route;
    private volatile TimingData currentTimingData;
    private volatile int lap = 0;
    private volatile long lastLapStartTime;
    private volatile long lastSplitStartTime;
    private volatile Long bestLapTime;
    private volatile Long[] bestSplitTimes;
    
    public RouteManagerTimingDataProvider(RouteManager routeManager, Route segmentsRoute) {
        super();
        this.routeManager = routeManager;
        this.route = segmentsRoute;
    }

    @Override
    public void start() {
        routeManager.addRoute(route, 15f, this);
        bestSplitTimes = new Long[route.getWaypoints().size()];
    }

    @Override
    public void stop() {
        routeManager.removeRoute(route);
    }

    @Override
    public TimingData getCurrentData() {
        return currentTimingData;
    }

    @Override
    public void waypointEvent(int waypointIndex, Route route,
            long locationTime, long systemTime, WaypointEventType eventType,
            float distanceToWaypoint) {
        
        if (eventType == WaypointEventType.CLOSEST_TO_WAYPOINT) {
            
            TimingDataBuilder builder = new TimingDataBuilder();
            builder.setDataRecivedTime(systemTime);
            builder.setTime(locationTime);
            
            long deltaLap = getLocationBasedElapsedTime(lastLapStartTime, locationTime);
            long deltaSplit = getLocationBasedElapsedTime(lastSplitStartTime, locationTime);
            
            if (lap == 0 && waypointIndex == 0) {
                builder.setLap(lap++);
                builder.setSplitIndex(bestSplitTimes.length - 1);
            } else {
                
                int splitIndex;
                
                if (waypointIndex == 0) {
                    splitIndex = bestSplitTimes.length - 1;
                    
                    if (bestLapTime == null || deltaLap < bestLapTime) {
                        bestLapTime = deltaLap;
                    }
                    
                    builder.setLapTime(deltaLap);
                    builder.setBestLapTime(bestLapTime);
                } else {
                    splitIndex = waypointIndex - 1;
                }
                
                if (bestSplitTimes[splitIndex] == null || deltaSplit < bestSplitTimes[splitIndex]) {
                    bestSplitTimes[splitIndex] = deltaSplit;
                }
                
                
                builder.setLap(lap++);
                builder.setSplitIndex(splitIndex);
                
                builder.setSplitTime(deltaSplit);
                
                lastSplitStartTime = locationTime;
            }
            
            builder.setBestSplitTimes(Arrays.asList(bestSplitTimes));
            
            TimingData newTimingData = builder.build();
            currentTimingData = newTimingData;
            
            notifySynchronousListeners(newTimingData);
        }
    }
    
    protected long getLocationBasedElapsedTime(long lastTime, long currentTime) {
        
        if (currentTime < lastTime) {
            return (TimeUtil.MS_IN_DAY - lastTime) + currentTime;
        } else {
            return currentTime - lastTime;
        }
    }
    
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
