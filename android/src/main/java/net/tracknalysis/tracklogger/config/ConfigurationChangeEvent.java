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

/**
 * @author David Valeri
 */
public class ConfigurationChangeEvent {
    private Configuration configuration;
    
    private boolean isRootLogLevelChanged;
    private boolean isLogToFileChanged;
    
    private boolean isEcuEnabledChanged;
    private boolean isEcuBtAddressChanged;
    
    private boolean isLocationbtAddressChanged;
    
    private ConfigurationChangeEvent() {
    }

    /**
     * Returns the current configuration with the latest state.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    protected void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns true if {@link Configuration#getRootLogLevel()} has changed.
     */
    public boolean isRootLogLevelChanged() {
        return isRootLogLevelChanged;
    }

    protected void setRootLogLevelChanged(boolean isRootLogLevelChanged) {
        this.isRootLogLevelChanged = isRootLogLevelChanged;
    }

    /**
     * Returns true if {@link Configuration#isLogToFile()} has changed.
     */
    public boolean isLogToFileChanged() {
        return isLogToFileChanged;
    }

    protected void setLogToFileChanged(boolean isLogToFileChanged) {
        this.isLogToFileChanged = isLogToFileChanged;
    }

    /**
     * Returns true if {@link Configuration#isEcuEnabled()} has changed.
     */
    public boolean isEcuLoggingEnabledChanged() {
        return isEcuEnabledChanged;
    }

    protected void setEcuLoggingEnabledChanged(boolean isEcuLoggingEnabledChanged) {
        this.isEcuEnabledChanged = isEcuLoggingEnabledChanged;
    }

    /**
     * Returns true if {@link Configuration#getEcuBtAddress()} has changed.
     */
    public boolean isEcuBtAddressChanged() {
        return isEcuBtAddressChanged;
    }

    protected void setEcuBtAddressChanged(boolean isEcuBtAddressChanged) {
        this.isEcuBtAddressChanged = isEcuBtAddressChanged;
    }

    /**
     * Returns true if {@link Configuration#getLocationBtAddress()} has changed.
     */
    public boolean isLocationbtAddressChanged() {
        return isLocationbtAddressChanged;
    }

    protected void setLocationbtAddressChanged(boolean isLocationbtAddressChanged) {
        this.isLocationbtAddressChanged = isLocationbtAddressChanged;
    }

    /**
     * A builder for {@link ConfigurationChangeEvent}s.
     *
     * @author David Valeri
     */
    public static class ConfigurationChangeEventBuilder {
        private Configuration configuration;
        
        private boolean isRootLogLevelChanged;
        private boolean isLogToFileChanged;
        
        private boolean isEcuEnabledChanged;
        private boolean isEcuBtAddressChanged;
        
        private boolean isLocationBtAddressChanged;

        /**
         * See {@link ConfigurationChangeEvent#getConfiguration()}.
         */
        public Configuration getConfiguration() {
            return configuration;
        }

        /**
         * See {@link ConfigurationChangeEvent#getConfiguration()}.
         */
        public void setConfiguration(Configuration configuration) {
            this.configuration = configuration;
        }

        /**
         * See {@link ConfigurationChangeEvent#isRootLogLevelChanged()}.
         */
        public boolean isRootLogLevelChanged() {
            return isRootLogLevelChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isRootLogLevelChanged()}.
         */
        public void setRootLogLevelChanged(boolean isDefaultLogLevelChanged) {
            this.isRootLogLevelChanged = isDefaultLogLevelChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isLogToFileChanged}.
         */
        public boolean isLogToFileChanged() {
            return isLogToFileChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isLogToFileChanged}.
         */
        public void setLogToFileChanged(boolean isLogToFileChanged) {
            this.isLogToFileChanged = isLogToFileChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isLogToFileChanged}.
         */
        public boolean isEcuEnabledChanged() {
            return isEcuEnabledChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isEcuEnabledChanged}.
         */
        public void setEcuEnabledChanged(boolean isEcuLoggingEnabledChanged) {
            this.isEcuEnabledChanged = isEcuLoggingEnabledChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isEcuBtAddressChanged.
         */
        public boolean isEcuBtAddressChanged() {
            return isEcuBtAddressChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isEcuBtAddressChanged.
         */
        public void setEcuBtAddressChanged(boolean isEcuBtAddressChanged) {
            this.isEcuBtAddressChanged = isEcuBtAddressChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isLocationBtAddressChanged.
         */
        public boolean isLocationBtAddressChanged() {
            return isLocationBtAddressChanged;
        }

        /**
         * See {@link ConfigurationChangeEvent#isLocationBtAddressChanged.
         */
        public void setLocationBtAddressChanged(boolean isLocationbtAddressChanged) {
            this.isLocationBtAddressChanged = isLocationbtAddressChanged;
        }
        
        public ConfigurationChangeEvent build() {
            ConfigurationChangeEvent event = new ConfigurationChangeEvent();
            event.setConfiguration(getConfiguration());
            
            event.setRootLogLevelChanged(isRootLogLevelChanged());
            event.setLogToFileChanged(isLogToFileChanged());
            
            event.setEcuLoggingEnabledChanged(isEcuEnabledChanged());
            event.setEcuBtAddressChanged(isEcuBtAddressChanged());
            
            event.setLocationbtAddressChanged(isLocationBtAddressChanged());
            
            return event;
        }
    }
}