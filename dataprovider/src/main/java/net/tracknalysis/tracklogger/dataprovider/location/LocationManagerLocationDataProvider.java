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
package net.tracknalysis.tracklogger.dataprovider.location;

import net.tracknalysis.tracklogger.dataprovider.AbstractDataProvider;
import net.tracknalysis.tracklogger.dataprovider.LocationData;
import net.tracknalysis.tracklogger.dataprovider.LocationDataProvider;
import net.tracknalysis.location.Location;
import net.tracknalysis.location.LocationListener;
import net.tracknalysis.location.LocationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Valeri
 */
public class LocationManagerLocationDataProvider extends
        AbstractDataProvider<LocationData> implements LocationDataProvider,
        LocationListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(LocationManagerLocationDataProvider.class);
    
    private final LocationManager locationManager;
    
    private volatile LocationData currentLocationData;
    
    public LocationManagerLocationDataProvider(LocationManager locationManager) {
        super();
        this.locationManager = locationManager;
    }
    
    @Override
    public synchronized void start() {
        locationManager.addSynchronousListener(this);
    }
    
    @Override
    public synchronized void stop() {
        locationManager.removeSynchronousListener(this);
    }
    
    @Override
    public LocationData getCurrentData() {
        return currentLocationData;
    }
    
    @Override
    protected Logger getLogger() {
        return LOG;
    }
    
    @Override
    public void receiveLocation(Location location) {
        LocationData.LocationDataBuilder builder = new LocationData.LocationDataBuilder();
        builder.setAltitude(location.getAltitude());
        builder.setBearing(location.getBearing());
        builder.setLatitude(location.getLatitude());
        builder.setLongitude(location.getLongitude());
        builder.setSpeed(location.getSpeed());
        builder.setTime(location.getTime());
        builder.setDataRecivedTime(location.getReceivedTime());
        
        LocationData newLocationData = builder.build();
        currentLocationData = newLocationData;
        
        notifySynchronousListeners(newLocationData);
    }
}
