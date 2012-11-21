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
package net.tracknalysis.tracklogger.view;

/**
 * Configuration structure for gauges.
 *
 * @author David Valeri
 */
public class GaugeConfiguration {

    private float maxValue;
    private float minValue;
    
    private float majorScaleMarkDelta;
    private int minorScaleMarkSegmentsPerMajorScaleMark;
    // The scale mark label value is divided by this value before rendering and
    // an indicator is placed in the gauge indicating that the value is X (times)
    // this value.
    private Float scaleMarkLabelScaleFactor;
    
    private Float maxWarningValue;
    private Float maxCriticalValue;
    
    private boolean useAlertColorGradient = false;
    
    private boolean showValue = true;
    
    private String title;
    
    protected GaugeConfiguration() {
    }

    public float getMaxValue() {
        return maxValue;
    }

    protected void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
    }

    public float getMinValue() {
        return minValue;
    }

    protected void setMinValue(float minValue) {
        this.minValue = minValue;
    }
    
    public float getMajorScaleMarkDelta() {
        return majorScaleMarkDelta;
    }

    protected void setMajorScaleMarkDelta(float majorScaleMarkDelta) {
        this.majorScaleMarkDelta = majorScaleMarkDelta;
    }

    public int getMinorScaleMarkSegmentsPerMajorScaleMark() {
        return minorScaleMarkSegmentsPerMajorScaleMark;
    }

    protected void setMinorScaleMarkSegmentsPerMajorScaleMark(
            int minorScaleMarkSegmentsPerMajorScaleMark) {
        this.minorScaleMarkSegmentsPerMajorScaleMark = minorScaleMarkSegmentsPerMajorScaleMark;
    }
    
    public Float getScaleMarkLabelScaleFactor() {
        return scaleMarkLabelScaleFactor;
    }

    protected void setScaleMarkLabelScaleFactor(Float scaleMarkLabelScaleFactor) {
        this.scaleMarkLabelScaleFactor = scaleMarkLabelScaleFactor;
    }

    public Float getMaxWarningValue() {
        return maxWarningValue;
    }

    protected void setMaxWarningValue(Float maxWarningValue) {
        this.maxWarningValue = maxWarningValue;
    }
    
    public boolean isMaxWarningEnabled() {
        return maxWarningValue != null;
    }

    public Float getMaxCriticalValue() {
        return maxCriticalValue;
    }

    protected void setMaxCriticalValue(Float maxCriticalValue) {
        this.maxCriticalValue = maxCriticalValue;
    }
    
    public boolean isMaxCriticalEnabled() {
        return maxCriticalValue != null;
    }
    
    public boolean isUseAlertColorGradient() {
        return useAlertColorGradient;
    }

    protected void setUseAlertColorGradient(boolean useColorGradient) {
        this.useAlertColorGradient = useColorGradient;
    }
    
    public boolean isShowValue() {
        return showValue;
    }

    protected void setShowValue(boolean showValue) {
        this.showValue = showValue;
    }

    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public static class GaugeConfigurationBuilder {
        private float maxValue;
        private float minValue;
        
        private float majorScaleMarkDelta;
        private int minorScaleMarkSegmentsPerMajorScaleMark;
        private Float scaleMarkLabelScaleFactor;
        
        private Float maxWarningValue;
        private Float maxCriticalValue;
        
        private boolean useAlertColorGradient = false;
        
        private boolean showValue = true;
        
        private String title;
        
        public float getMaxValue() {
            return maxValue;
        }
        
        public GaugeConfigurationBuilder setMaxValue(float maxValue) {
            this.maxValue = maxValue;
            return this;
        }
        
        public float getMinValue() {
            return minValue;
        }
        
        public GaugeConfigurationBuilder setMinValue(float minValue) {
            this.minValue = minValue;
            return this;
        }
        
        public float getMajorScaleMarkDelta() {
            return majorScaleMarkDelta;
        }

        public GaugeConfigurationBuilder setMajorScaleMarkDelta(float majorScaleMarkDelta) {
            this.majorScaleMarkDelta = majorScaleMarkDelta;
            return this;
        }

        public int getMinorScaleMarkSegmentsPerMajorScaleMark() {
            return minorScaleMarkSegmentsPerMajorScaleMark;
        }

        public GaugeConfigurationBuilder setMinorScaleMarkSegmentsPerMajorScaleMark(
                int minorScaleMarkSegmentsPerMajorScaleMark) {
            this.minorScaleMarkSegmentsPerMajorScaleMark = minorScaleMarkSegmentsPerMajorScaleMark;
            return this;
        }
        
        public Float getScaleMarkLabelScaleFactor() {
            return scaleMarkLabelScaleFactor;
        }

        public GaugeConfigurationBuilder setScaleMarkLabelScaleFactor(Float scaleMarkLabelScaleFactor) {
            this.scaleMarkLabelScaleFactor = scaleMarkLabelScaleFactor;
            return this;
        }

        public Float getMaxWarningValue() {
            return maxWarningValue;
        }
        
        public GaugeConfigurationBuilder setMaxWarningValue(Float maxWarningValue) {
            this.maxWarningValue = maxWarningValue;
            return this;
        }
        
        public Float getMaxCriticalValue() {
            return maxCriticalValue;
        }
        
        public GaugeConfigurationBuilder setMaxCriticalValue(Float maxCriticalValue) {
            this.maxCriticalValue = maxCriticalValue;
            return this;
        }
        
        public boolean isUseAlertColorGradient() {
            return useAlertColorGradient;
        }

        public GaugeConfigurationBuilder setUseAlertColorGradient(boolean useColorGradient) {
            this.useAlertColorGradient = useColorGradient;
            return this;
        }
        
        public boolean isShowValue() {
            return showValue;
        }

        public GaugeConfigurationBuilder setShowValue(boolean showValue) {
            this.showValue = showValue;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public GaugeConfigurationBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public GaugeConfiguration build() {
            GaugeConfiguration result = new GaugeConfiguration();
            
            if (minValue >= maxValue) {
                throw new IllegalArgumentException("minValue must be less than maxValue.");
            }
            
            if (minorScaleMarkSegmentsPerMajorScaleMark < 0) {
                throw new IllegalArgumentException(
                        "minorScaleMarksPerMajorScaleMark must be > 0.");
            }
            
            if (maxWarningValue != null && (maxWarningValue < minValue || maxWarningValue > maxValue)) {
                throw new IllegalArgumentException(
                        "maxWarningValue must be <= maxValue and >= minValue.");
            }
            
            if (maxCriticalValue != null) {
                if (maxCriticalValue < minValue || maxCriticalValue > maxValue) {
                    throw new IllegalArgumentException(
                            "maxCriticalValue must be <= maxValue and >= minValue.");
                }
                
                if (maxWarningValue != null && maxCriticalValue <= maxWarningValue) {
                    throw new IllegalArgumentException(
                            "maxCriticalValue must be > maxWarningValue.");
                }
            }
            
            result.setMaxValue(maxValue);
            result.setMinValue(minValue);
            result.setMajorScaleMarkDelta(majorScaleMarkDelta);
            result.setMinorScaleMarkSegmentsPerMajorScaleMark(minorScaleMarkSegmentsPerMajorScaleMark);
            result.setMaxWarningValue(maxWarningValue);
            result.setMaxCriticalValue(maxCriticalValue);
            result.setUseAlertColorGradient(useAlertColorGradient);
            result.setShowValue(showValue);
            result.setTitle(title);
            
            return result;
        }
    }
}
