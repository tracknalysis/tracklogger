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
public class EcuData extends AbstractData {
    
    private int rpm;
    private double manifoldAbsolutePressure;
    private double throttlePosition;
    private double airFuelRatio;
    private double manifoldAirTemperature;
    private double coolantTemperature;
    private double ignitionAdvance;
    private double batteryVoltage;
    
    protected EcuData() {
    }
    
    /**
     * Returns the engine RPM.
     */
    public int getRpm() {
        return rpm;
    }

    protected void setRpm(int rpm) {
        this.rpm = rpm;
    }

    /**
     * Returns the intake manifold absolute pressure in KPa.
     */
    public double getManifoldAbsolutePressure() {
        return manifoldAbsolutePressure;
    }

    protected void setManifoldAbsolutePressure(double manifoldAbsolutePressure) {
        this.manifoldAbsolutePressure = manifoldAbsolutePressure;
    }

    /**
     * Returns the throttle position % (0-1).
     */
    public double getThrottlePosition() {
        return throttlePosition;
    }

    protected void setThrottlePosition(double throttlePosition) {
        this.throttlePosition = throttlePosition;
    }

    /**
     * Returns the air fuel ratio.
     */
    public double getAirFuelRatio() {
        return airFuelRatio;
    }

    protected void setAirFuelRatio(double airFuelRatio) {
        this.airFuelRatio = airFuelRatio;
    }

    /**
     * Returns the intake manifold air temperature in degrees Celsius.
     */
    public double getManifoldAirTemperature() {
        return manifoldAirTemperature;
    }

    protected void setManifoldAirTemperature(double manifoldAirTemperature) {
        this.manifoldAirTemperature = manifoldAirTemperature;
    }

    /**
     * Returns the coolant temperature in degrees Celsius.
     */
    public double getCoolantTemperature() {
        return coolantTemperature;
    }

    protected void setCoolantTemperature(double coolantTemperature) {
        this.coolantTemperature = coolantTemperature;
    }

    /**
     * Returns the ignition advance in degrees BTDC.
     */
    public double getIgnitionAdvance() {
        return ignitionAdvance;
    }

    protected void setIgnitionAdvance(double ignitionAdvance) {
        this.ignitionAdvance = ignitionAdvance;
    }

    /**
     * Returns the battery voltage in Volts.
     */
    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    protected void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EcuData [rpm=");
        builder.append(rpm);
        builder.append(", manifoldAbsolutePressure=");
        builder.append(manifoldAbsolutePressure);
        builder.append(", throttlePosition=");
        builder.append(throttlePosition);
        builder.append(", airFuelRatio=");
        builder.append(airFuelRatio);
        builder.append(", manifoldAirTemperature=");
        builder.append(manifoldAirTemperature);
        builder.append(", coolantTemperature=");
        builder.append(coolantTemperature);
        builder.append(", ignitionAdvance=");
        builder.append(ignitionAdvance);
        builder.append(", batteryVoltage=");
        builder.append(batteryVoltage);
        builder.append(", getDataRecivedTime()=");
        builder.append(getDataRecivedTime());
        builder.append("]");
        return builder.toString();
    }
    
    public static class EcuDataBuilder extends AbstractDataBuilder<EcuData> {
        
        /**
         * Engine RPM.
         */
        private int rpm;
        
        /**
         * Manifold absolute pressure in KPa.
         */
        private double manifoldAbsolutePressure;
        
        /**
         * Throttle position % (0-1).
         */
        private double throttlePosition;
        
        /**
         * Air fuel ratio.
         */
        private double airFuelRatio;
        
        /**
         * Manifold air temperature in degrees Celsius.
         */
        private double manifoldAirTemperature;
        
        /**
         * Coolant temperature in degrees Celsius.
         */
        private double coolantTemperature;
        
        /**
         * Ignition advance in degrees BTDC.
         */
        private double ignitionAdvance;
        
        /**
         * Battery voltage in Volts.
         */
        private double batteryVoltage;
        
        public int getRpm() {
            return rpm;
        }

        public void setRpm(int rpm) {
            this.rpm = rpm;
        }

        public double getManifoldAbsolutePressure() {
            return manifoldAbsolutePressure;
        }

        public void setManifoldAbsolutePressure(double manifoldAbsolutePressure) {
            this.manifoldAbsolutePressure = manifoldAbsolutePressure;
        }

        public double getThrottlePosition() {
            return throttlePosition;
        }

        public void setThrottlePosition(double throttlePosition) {
            this.throttlePosition = throttlePosition;
        }

        public double getAirFuelRatio() {
            return airFuelRatio;
        }

        public void setAirFuelRatio(double airFuelRatio) {
            this.airFuelRatio = airFuelRatio;
        }

        public double getManifoldAirTemperature() {
            return manifoldAirTemperature;
        }

        public void setManifoldAirTemperature(double manifoldAirTemperature) {
            this.manifoldAirTemperature = manifoldAirTemperature;
        }

        public double getCoolantTemperature() {
            return coolantTemperature;
        }

        public void setCoolantTemperature(double coolantTemperature) {
            this.coolantTemperature = coolantTemperature;
        }

        public double getIgnitionAdvance() {
            return ignitionAdvance;
        }

        public void setIgnitionAdvance(double ignitionAdvance) {
            this.ignitionAdvance = ignitionAdvance;
        }

        public double getBatteryVoltage() {
            return batteryVoltage;
        }

        public void setBatteryVoltage(double batteryVoltage) {
            this.batteryVoltage = batteryVoltage;
        }

        @Override
        protected EcuData doBuild() {
            EcuData newData = new EcuData();
            
            newData.setRpm(getRpm());
            newData.setManifoldAbsolutePressure(getManifoldAbsolutePressure());
            newData.setThrottlePosition(getThrottlePosition());
            newData.setAirFuelRatio(getAirFuelRatio());
            newData.setManifoldAirTemperature(getManifoldAirTemperature());
            newData.setCoolantTemperature(getCoolantTemperature());
            newData.setIgnitionAdvance(getIgnitionAdvance());
            newData.setBatteryVoltage(getBatteryVoltage());
            
            return newData;
        }
    }
}
