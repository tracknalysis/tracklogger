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

import org.apache.log4j.Level;

/**
 * @author David Valeri
 */
public interface Configuration {
    
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
