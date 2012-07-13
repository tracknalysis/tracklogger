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
 * An immutable container for location data.
 *
 * @author David Valeri
 */
public final class LocationData extends AbstractData {

    private long time;
    private double latitude;
    private double longitude;
    private double altitude;
    private float speed;
    private float bearing;
    
    protected LocationData() {
    }
    
    /**
     * Returns the UTC time of the location fix as millisecond offset into the day
     * on which the capture occurred.
     */
    public long getTime() {
        return time;
    }

    protected void setTime(long time) {
        this.time = time;
    }
    
    /**
     * Returns the latitude of the fix in degrees.
     */
    public double getLatitude() {
        return latitude;
    }

    protected void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the longitude of the fix in degrees.
     */
    public double getLongitude() {
        return longitude;
    }

    protected void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    /**
     * Returns the altitude of the fix above MSL in meters.
     */
    public double getAltitude() {
        return altitude;
    }

    protected void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    /**
     * Returns the speed over ground at the time of the fix in meters per second.
     */
    public float getSpeed() {
        return speed;
    }

    protected void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Returns the heading at the time of the fix in degrees.
     */
    public float getBearing() {
        return bearing;
    }

    protected void setBearing(float bearing) {
        this.bearing = bearing;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GpsData [time=");
        builder.append(time);
        builder.append(", latitude=");
        builder.append(latitude);
        builder.append(", longitude=");
        builder.append(longitude);
        builder.append(", altitude=");
        builder.append(altitude);
        builder.append(", speed=");
        builder.append(speed);
        builder.append(", bearing=");
        builder.append(bearing);
        builder.append(", getDataRecivedTime()=");
        builder.append(getDataRecivedTime());
        builder.append("]");
        return builder.toString();
    }
    
    public final static class LocationDataBuilder extends AbstractDataBuilder<LocationData> {
        private long time = 0l;
        private double latitude = 0.0d;
        private double longitude = 0.0d;
        private boolean hasAltitude;
        private double altitude = 0.0f;
        private float speed = 0.0f;
        private float bearing = 0.0f;
        
        /**
         * See {@link LocationData#getTime()}.
         */
        public long getTime() {
            return time;
        }

        /**
         * See {@link LocationData#getTime()}.
         */
        public void setTime(long time) {
            this.time = time;
        }
        
        /**
         * See {@link LocationData#getLatitude()}.
         */
        public double getLatitude() {
            return latitude;
        }

        /**
         * See {@link LocationData#getLatitude()}.
         */
        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        /**
         * See {@link LocationData#getLongitude()}.
         */
        public double getLongitude() {
            return longitude;
        }

        /**
         * See {@link LocationData#getLongitude()}.
         */
        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
        
        /**
         * See {@link LocationData#getAltitude()}.
         */
        public double getAltitude() {
            return altitude;
        }

        /**
         * See {@link LocationData#getAltitude()}.
         */
        public void setAltitude(double altitude) {
            this.altitude = altitude;
        }

        /**
         * See {@link LocationData#getSpeed()}.
         */
        public float getSpeed() {
            return speed;
        }

        /**
         * See {@link LocationData#getSpeed()}.
         */
        public void setSpeed(float speed) {
            this.speed = speed;
        }

        /**
         * See {@link LocationData#getBearing()}.
         */
        public float getBearing() {
            return bearing;
        }

        /**
         * See {@link LocationData#getBearing()}.
         */
        public void setBearing(float bearing) {
            this.bearing = bearing;
        }

        public boolean isHasAltitude() {
            return hasAltitude;
        }

        protected LocationData doBuild() {
            LocationData gpsData = new LocationData();
            
            gpsData.setTime(getTime());
            gpsData.setLatitude(getLatitude());
            gpsData.setLongitude(getLongitude());
            gpsData.setSpeed(getSpeed());
            gpsData.setBearing(getBearing());
            gpsData.setAltitude(getAltitude());
            
            return gpsData;
        }
    }
}
