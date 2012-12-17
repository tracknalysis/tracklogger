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
package net.tracknalysis.tracklogger.config;

import java.util.HashMap;
import java.util.Map;

import net.tracknalysis.tracklogger.model.PressureUnit;
import net.tracknalysis.tracklogger.model.SpeedUnit;
import net.tracknalysis.tracklogger.model.TemperatureUnit;
import net.tracknalysis.tracklogger.view.GaugeConfiguration;

import org.apache.log4j.Level;

/**
 * Interface for getting and setting core application configuration options.
 *
 * @author David Valeri
 */
public interface Configuration {
    
    public static enum DisplayGauge {
        SPEED("display.gauges.speed"),
        RPM("display.gauges.rpm"),
        MAP("display.gauges.map"),
        MGP("display.gauges.mgp"),
        TP("display.gauges.tp"),
        AFR("display.gauges.afr"),
        MAT("display.gauges.mat"),
        CLT("display.gauges.clt"),
        IGN_ADV("display.gauges.ignAdv"),
        BAT_V("display.gauges.batV");
        
        private static final Map<String, DisplayGauge> ROOT_KEY_MAP = 
                new HashMap<String, Configuration.DisplayGauge>();
        
        static {
            for (DisplayGauge displayGauge : DisplayGauge.values()) {
                ROOT_KEY_MAP.put(displayGauge.getRootKey(), displayGauge);
            }
        }
        
        public static DisplayGauge fromRootKey(String rootKey) {
            DisplayGauge displayGauge = ROOT_KEY_MAP.get(rootKey);
            if (displayGauge == null) {
                throw new IllegalArgumentException("No instance exists for root key [" + rootKey + "].");
            } else {
                return displayGauge;
            }
        }
        
        private final String rootKey;
        
        private DisplayGauge(String rootKey) {
            this.rootKey = rootKey;
        }
        
        public String getRootKey() {
            return rootKey;
        }
    }
    
    /**
     * Returns the log level for the root logger in the application.
     */
    Level getRootLogLevel();
    
    /**
     * @see #getRootLogLevel()
     */
    void setRootLogLevel(Level level);
    
    /**
     * Returns true if the application should output log data to a file.
     */
    boolean isLogToFile();
    
    /**
     * @see #isLogToFile()
     */
    void setLogToFile(boolean enabled);
    
    /**
     * Returns true if ECU data should be captured and logged when logging session data.
     */
    boolean isEcuEnabled();
    
    /**
     * @see #isEcuEnabled()
     */
    void setEcuEnabled(boolean enabled);
    
    /**
     * Returns the address of the BT device representing the ECU.
     */
    String getEcuBtAddress();
    
    /**
     * Returns true if the system should log IO operations to the ECU.
     */
    boolean isEcuIoLogEnabled();
    
    /**
     * @see #isEcuIoLogEnabled()
     */
    public void setEcuIoLogEnabled(boolean enabled);

    /**
     * Returns the address of the BT device representing an NMEA location source.
     */
    String getLocationBtAddress();
    
    /**
     * Returns the name/id of the view to use when performing logging.
     */
    int getLogLayoutId();
    
    /**
     * @see #getLogLayoutId()
     */
    void setLogLayoutId(int id);
    
    /**
     * Returns the unit in which speed should be displayed to the user in the application.
     */
    SpeedUnit getDisplaySpeedUnit();
    
    /**
     * @see #setDisplaySpeedUnit(SpeedUnit)
     */
    void setDisplaySpeedUnit(SpeedUnit unit);
    
    /**
     * Returns the unit in which temperature should be displayed to the user in the application.
     */
    TemperatureUnit getDisplayTemperatureUnit();
    
    /**
     * @see #getDisplayTemperatureUnit()
     */
    void setDisplayTemperatureUnit(TemperatureUnit unit);
    
    /**
     * Returns the unit in which pressure should be displayed to the user in the application.
     */
    PressureUnit getDisplayPressureUnit();
    
    /**
     * @see #getDisplayPressureUnit()
     */
    void setDisplayPressureUnit(PressureUnit unit);
    
    /**
     * Returns the gauge configuration for the given {@link DisplayGauge}.
     *
     * @param displayGauge the gauge to retreive the configuration for
     */
    GaugeConfiguration getGaugeConfiguration(DisplayGauge displayGauge);
    
    /**
     * Sets the gauge configuration for the given gauge.
     *
     * @param displayGauge the gauge to save the configuration for
     * @param gaugeConfiguration the configuration to save
     */
    void setGaugeConfiguration(DisplayGauge displayGauge, GaugeConfiguration gaugeConfiguration);
    
    /**
     * Returns the path to the folder, relative to external storage root, where user accessible data is written.
     */
    String getDataDirectory();
    
    /**
     * Returns true if operating in test mode.  This mode is used to disable checks for
     * resources that are unavailable in an emulator.
     */
    boolean isTestMode();
    
    /**
     * @see #isTestMode()
     */
    void setTestMode(boolean testMode);
    
    /**
     * Adds a listener for configuration change events if the listener is not already registered.
     *
     * @param listener the listener to add
     */
    void addConfigurationChangeListenerListener(ConfigurationChangeListener listener);
    
    /**
     * Removes the listener for configuration change events if the listener is registered.
     * 
     * @param listener the listener to remove
     */
    void removeConfigurationChangeListener(ConfigurationChangeListener listener);
}
