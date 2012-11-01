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
     * Returns the throttle position % (0-100).
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
        
        private int rpm;
        private double manifoldAbsolutePressure;
        private double throttlePosition;
        private double airFuelRatio;
        private double manifoldAirTemperature;
        private double coolantTemperature;
        private double ignitionAdvance;
        private double batteryVoltage;
        
        /**
         * @see EcuData#getRpm()
         */
        public int getRpm() {
            return rpm;
        }

        /**
         * @see EcuData#getRpm()
         */
        public void setRpm(int rpm) {
            this.rpm = rpm;
        }

        /**
         * @see EcuData#getManifoldAbsolutePressure()
         */
        public double getManifoldAbsolutePressure() {
            return manifoldAbsolutePressure;
        }

        /**
         * @see EcuData#getManifoldAbsolutePressure()
         */
        public void setManifoldAbsolutePressure(double manifoldAbsolutePressure) {
            this.manifoldAbsolutePressure = manifoldAbsolutePressure;
        }

        /**
         * @see EcuData#getThrottlePosition()
         */
        public double getThrottlePosition() {
            return throttlePosition;
        }

        /**
         * @see EcuData#getThrottlePosition()
         */
        public void setThrottlePosition(double throttlePosition) {
            this.throttlePosition = throttlePosition;
        }

        /**
         * @see EcuData#getAirFuelRatio()
         */
        public double getAirFuelRatio() {
            return airFuelRatio;
        }

        /**
         * @see EcuData#getAirFuelRatio()
         */
        public void setAirFuelRatio(double airFuelRatio) {
            this.airFuelRatio = airFuelRatio;
        }

        /**
         * @see EcuData#getManifoldAirTemperature()
         */
        public double getManifoldAirTemperature() {
            return manifoldAirTemperature;
        }

        /**
         * @see EcuData#getManifoldAirTemperature()
         */
        public void setManifoldAirTemperature(double manifoldAirTemperature) {
            this.manifoldAirTemperature = manifoldAirTemperature;
        }

        /**
         * @see EcuData#getCoolantTemperature()
         */
        public double getCoolantTemperature() {
            return coolantTemperature;
        }

        /**
         * @see EcuData#getCoolantTemperature()
         */
        public void setCoolantTemperature(double coolantTemperature) {
            this.coolantTemperature = coolantTemperature;
        }

        /**
         * @see EcuData#getIgnitionAdvance()
         */
        public double getIgnitionAdvance() {
            return ignitionAdvance;
        }

        /**
         * @see EcuData#getIgnitionAdvance()
         */
        public void setIgnitionAdvance(double ignitionAdvance) {
            this.ignitionAdvance = ignitionAdvance;
        }

        /**
         * @see EcuData#getBatteryVoltage()
         */
        public double getBatteryVoltage() {
            return batteryVoltage;
        }

        /**
         * @see EcuData#getBatteryVoltage()
         */
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
