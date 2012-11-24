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
    private int minorScaleMarkSegmentsPerMajorScaleMark = 1;
    private Float scaleMarkLabelScaleFactor;
    
    private Float minCriticalValue;
    private Float minWarningValue;
    
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

    /**
     * Returns the number of segments, sub divisions, between major scale marks.
     * Defaults to 1, that is no minor scale marks. For example, if the minimum
     * value is 0, the major mark delta is 10 and you desire a minor scale mark
     * on 5, 15, 25, you would specify 2 here. 10 / 2 = 5. So each
     * segment/sub-division would represent 5 units and there would be 2
     * segments per major scale mark with one minor scale mark separating the
     * two segments.
     */
    public int getMinorScaleMarkSegmentsPerMajorScaleMark() {
        return minorScaleMarkSegmentsPerMajorScaleMark;
    }

    protected void setMinorScaleMarkSegmentsPerMajorScaleMark(
            int minorScaleMarkSegmentsPerMajorScaleMark) {
        this.minorScaleMarkSegmentsPerMajorScaleMark = minorScaleMarkSegmentsPerMajorScaleMark;
    }
    
    /**
     * Returns the optional value by which scale mark label value is divided by
     * before rendering.  An indicator is placed in the gauge indicating that
     * the value is X (times) this value. Useful if the scale is large, such as
     * RPMs x 1000.
     */
    public Float getScaleMarkLabelScaleFactor() {
        return scaleMarkLabelScaleFactor;
    }

    protected void setScaleMarkLabelScaleFactor(Float scaleMarkLabelScaleFactor) {
        this.scaleMarkLabelScaleFactor = scaleMarkLabelScaleFactor;
    }
    
    public Float getMinCriticalValue() {
        return minCriticalValue;
    }

    protected void setMinCriticalValue(Float minCriticalValue) {
        this.minCriticalValue = minCriticalValue;
    }
    
    public boolean isMinCriticalEnabled() {
        return minCriticalValue != null;
    }

    public Float getMinWarningValue() {
        return minWarningValue;
    }

    protected void setMinWarningValue(Float minWarningValue) {
        this.minWarningValue = minWarningValue;
    }
    
    public boolean isMinWarningEnabled() {
        return minWarningValue != null;
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
    
    public boolean isAlertEnabled() {
        return isMinCriticalEnabled() || isMinWarningEnabled()
                || isMaxWarningEnabled() || isMaxCriticalEnabled();
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
        private int minorScaleMarkSegmentsPerMajorScaleMark = 1;
        private Float scaleMarkLabelScaleFactor;
        
        private Float minCriticalValue;
        private Float minWarningValue;
        
        private Float maxWarningValue;
        private Float maxCriticalValue;
        
        private boolean useAlertColorGradient = false;
        
        private boolean showValue = true;
        
        private String title;
        
        public float getMaxValue() {
            return maxValue;
        }
        
        /**
         * See {@link GaugeConfiguration#getMaxValue()}.
         */
        public GaugeConfigurationBuilder setMaxValue(float maxValue) {
            this.maxValue = maxValue;
            return this;
        }
        
        public float getMinValue() {
            return minValue;
        }
        
        /**
         * See {@link GaugeConfiguration#getMinValue()}.
         */
        public GaugeConfigurationBuilder setMinValue(float minValue) {
            this.minValue = minValue;
            return this;
        }
        
        public float getMajorScaleMarkDelta() {
            return majorScaleMarkDelta;
        }

        /**
         * See {@link GaugeConfiguration#getMajorScaleMarkDelta()}.
         */
        public GaugeConfigurationBuilder setMajorScaleMarkDelta(float majorScaleMarkDelta) {
            this.majorScaleMarkDelta = majorScaleMarkDelta;
            return this;
        }

        public int getMinorScaleMarkSegmentsPerMajorScaleMark() {
            return minorScaleMarkSegmentsPerMajorScaleMark;
        }

        /**
         * See {@link GaugeConfiguration#getMinorScaleMarkSegmentsPerMajorScaleMark()}.
         */
        public GaugeConfigurationBuilder setMinorScaleMarkSegmentsPerMajorScaleMark(
                int minorScaleMarkSegmentsPerMajorScaleMark) {
            this.minorScaleMarkSegmentsPerMajorScaleMark = minorScaleMarkSegmentsPerMajorScaleMark;
            return this;
        }
        
        public Float getScaleMarkLabelScaleFactor() {
            return scaleMarkLabelScaleFactor;
        }

        /**
         * See {@link GaugeConfiguration#getScaleMarkLabelScaleFactor()}.
         */
        public GaugeConfigurationBuilder setScaleMarkLabelScaleFactor(Float scaleMarkLabelScaleFactor) {
            this.scaleMarkLabelScaleFactor = scaleMarkLabelScaleFactor;
            return this;
        }
        
        public Float getMinCriticalValue() {
            return minCriticalValue;
        }

        public GaugeConfigurationBuilder setMinCriticalValue(Float minCriticalValue) {
            this.minCriticalValue = minCriticalValue;
            return this;
        }

        public Float getMinWarningValue() {
            return minWarningValue;
        }

        public GaugeConfigurationBuilder setMinWarningValue(Float minWarningValue) {
            this.minWarningValue = minWarningValue;
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
            
            // Min and Max
            
            if (minValue >= maxValue) {
                throw new IllegalArgumentException("minValue must be less than maxValue.");
            }
            
            if (minorScaleMarkSegmentsPerMajorScaleMark < 0) {
                throw new IllegalArgumentException(
                        "minorScaleMarksPerMajorScaleMark must be > 0.");
            }
            
            // Warning/Critical Alert Values
            
            if (minCriticalValue != null && (minCriticalValue < minValue || minCriticalValue > maxValue)) {
                throw new IllegalArgumentException(
                        "minCriticalValue must be <= maxValue and >= minValue.");
            }
            
            if (minWarningValue != null) {
                if (minWarningValue < minValue || minWarningValue > maxValue) {
                    throw new IllegalArgumentException(
                            "minWarningValue must be <= maxValue and >= minValue.");
                }
                
                if (minCriticalValue != null && minWarningValue < minCriticalValue) {
                    throw new IllegalArgumentException(
                            "minCriticalValue must be > minWarningValue.");
                }
            }
            
            if (maxWarningValue != null) {
                if (maxWarningValue < minValue || maxWarningValue > maxValue) {
                    throw new IllegalArgumentException(
                            "maxWarningValue must be <= maxValue and >= minValue.");
                }
                
                if (minWarningValue != null && maxWarningValue <= minWarningValue) {
                    throw new IllegalArgumentException(
                            "maxWarningValue must be > minWarningValue."); 
                }
                
                if (minCriticalValue != null && maxWarningValue <= minCriticalValue) {
                    throw new IllegalArgumentException(
                            "maxWarningValue must be > minCriticalValue."); 
                }
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
                
                if (minWarningValue != null && maxCriticalValue <= minWarningValue) {
                    throw new IllegalArgumentException(
                            "maxCriticalValue must be > minWarningValue."); 
                }
                
                if (minCriticalValue != null && maxCriticalValue <= minCriticalValue) {
                    throw new IllegalArgumentException(
                            "maxCriticalValue must be > minCriticalValue."); 
                }
            }
            
            ///////
            
            result.setMaxValue(maxValue);
            result.setMinValue(minValue);
            result.setMajorScaleMarkDelta(majorScaleMarkDelta);
            result.setMinorScaleMarkSegmentsPerMajorScaleMark(minorScaleMarkSegmentsPerMajorScaleMark);
            result.setMinCriticalValue(minCriticalValue);
            result.setMinWarningValue(minWarningValue);
            result.setMaxWarningValue(maxWarningValue);
            result.setMaxCriticalValue(maxCriticalValue);
            result.setUseAlertColorGradient(useAlertColorGradient);
            result.setShowValue(showValue);
            result.setTitle(title);
            
            return result;
        }
    }
}
