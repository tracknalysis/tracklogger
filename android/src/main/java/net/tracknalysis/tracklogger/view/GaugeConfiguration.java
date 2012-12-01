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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.tracklogger.model.validation.BasicValidationErrorType;
import net.tracknalysis.tracklogger.model.validation.ValidationError;
import net.tracknalysis.tracklogger.model.validation.ValidationErrorException;
import net.tracknalysis.tracklogger.model.validation.ValidationResults;

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
    private int scaleMarkLabelPrecision = 0;
    
    private Float minCriticalValue;
    private Float minWarningValue;
    
    private Float maxWarningValue;
    private Float maxCriticalValue;
    
    private boolean useAlertColorGradient = false;
    
    private boolean showValue = true;
    private int valuePrecision = 0;
    
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
    
    /**
     * Returns the number of decimal places displayed on a scale mark label.
     * Defaults to 0.
     */
    public int getScaleMarkLabelPrecision() {
        return scaleMarkLabelPrecision;
    }

    protected void setScaleMarkLabelPrecision(int scaleMarkLabelPrecision) {
        this.scaleMarkLabelPrecision = scaleMarkLabelPrecision;
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
    
    /**
     * Returns the number of decimal places displayed when showing the value.
     * Defaults to 0.
     */
    public int getValuePrecision() {
        return valuePrecision;
    }

    protected void setValuePrecision(int valuePrecision) {
        this.valuePrecision = valuePrecision;
    }

    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public static class GaugeConfigurationBuilder {
        
        private static final Logger LOG = LoggerFactory.getLogger(GaugeConfigurationBuilder.class);
        
        private Float maxValue;
        private Float minValue;
        
        private Float majorScaleMarkDelta;
        private Integer minorScaleMarkSegmentsPerMajorScaleMark = 1;
        private Float scaleMarkLabelScaleFactor;
        private Integer scaleMarkLabelPrecision = 0;
        
        private Float minCriticalValue;
        private Float minWarningValue;
        
        private Float maxWarningValue;
        private Float maxCriticalValue;
        
        private Boolean useAlertColorGradient = false;
        
        private Boolean showValue = true;
        private Integer valuePrecision = 0;
        
        private String title;
        
        /**
         * See {@link GaugeConfiguration#getMaxValue()}.
         */
        public Float getMaxValue() {
            return maxValue;
        }
        
        /**
         * See {@link GaugeConfiguration#getMaxValue()}.
         */
        public GaugeConfigurationBuilder setMaxValue(Float maxValue) {
            this.maxValue = maxValue;
            return this;
        }
        
        /**
         * See {@link GaugeConfiguration#getMinValue()}.
         */
        public Float getMinValue() {
            return minValue;
        }
        
        /**
         * See {@link GaugeConfiguration#getMinValue()}.
         */
        public GaugeConfigurationBuilder setMinValue(Float minValue) {
            this.minValue = minValue;
            return this;
        }
        
        /**
         * See {@link GaugeConfiguration#getMajorScaleMarkDelta()}.
         */
        public Float getMajorScaleMarkDelta() {
            return majorScaleMarkDelta;
        }

        /**
         * See {@link GaugeConfiguration#getMajorScaleMarkDelta()}.
         */
        public GaugeConfigurationBuilder setMajorScaleMarkDelta(Float majorScaleMarkDelta) {
            this.majorScaleMarkDelta = majorScaleMarkDelta;
            return this;
        }

        /**
         * See {@link GaugeConfiguration#getMinorScaleMarkSegmentsPerMajorScaleMark()}.
         */
        public Integer getMinorScaleMarkSegmentsPerMajorScaleMark() {
            return minorScaleMarkSegmentsPerMajorScaleMark;
        }

        /**
         * See {@link GaugeConfiguration#getMinorScaleMarkSegmentsPerMajorScaleMark()}.
         */
        public GaugeConfigurationBuilder setMinorScaleMarkSegmentsPerMajorScaleMark(
                Integer minorScaleMarkSegmentsPerMajorScaleMark) {
            this.minorScaleMarkSegmentsPerMajorScaleMark = minorScaleMarkSegmentsPerMajorScaleMark;
            return this;
        }
        
        /**
         * See {@link GaugeConfiguration#getScaleMarkLabelScaleFactor()}.
         */
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
        
        /**
         * See {@link GaugeConfiguration#getScaleMarkLabelPrecision(int)}.
         */
        public Integer getScaleMarkLabelPrecision() {
            return scaleMarkLabelPrecision;
        }

        /**
         * See {@link GaugeConfiguration#getScaleMarkLabelPrecision(int)}.
         */
        public GaugeConfigurationBuilder setScaleMarkLabelPrecision(Integer scaleMarkLabelPrecision) {
            this.scaleMarkLabelPrecision = scaleMarkLabelPrecision;
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
        
        public Boolean isUseAlertColorGradient() {
            return useAlertColorGradient;
        }

        public GaugeConfigurationBuilder setUseAlertColorGradient(Boolean useColorGradient) {
            this.useAlertColorGradient = useColorGradient;
            return this;
        }
        
        public Boolean isShowValue() {
            return showValue;
        }

        public GaugeConfigurationBuilder setShowValue(Boolean showValue) {
            this.showValue = showValue;
            return this;
        }
        
        public Integer getValuePrecision() {
            return valuePrecision;
        }

        public GaugeConfigurationBuilder setValuePrecision(Integer valuePrecision) {
            this.valuePrecision = valuePrecision;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public GaugeConfigurationBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public GaugeConfiguration build() throws ValidationErrorException {
            GaugeConfiguration result = new GaugeConfiguration();
            ValidationResults validationResults = new ValidationResults();
            
            // Min and Max
            
            if (minValue == null) {
                LOG.error("minValue cannot be null.");
                validationResults.addValidationError(new ValidationError(
                        "minValue",
                        BasicValidationErrorType.NOT_NULL));
            } else if (maxValue == null) {
                LOG.error("maxValue cannot be null.");
                validationResults.addValidationError(new ValidationError(
                        "maxValue",
                        BasicValidationErrorType.NOT_NULL));
            }
            
            if ((minValue == null || maxValue == null) || minValue >= maxValue) {
                LOG.error("minValue must be less than maxValue.");
                validationResults.addValidationError(new ValidationError(
                        "minValue",
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
            }
            
            // Warning/Critical Alert Values
            
            if (minCriticalValue != null && minValue != null && maxValue != null 
                    && (minCriticalValue < minValue || minCriticalValue > maxValue)) {;
                LOG.error("minCriticalValue must be <= maxValue and >= minValue.");
                validationResults.addValidationError(new ValidationError(
                        "minCriticalValue",
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
            }
            
            if (minWarningValue != null) {
                if ((minValue != null && maxValue != null) 
                        && (minWarningValue < minValue || minWarningValue > maxValue)) {
                    LOG.error("minWarningValue must be <= maxValue and >= minValue.");
                    validationResults.addValidationError(new ValidationError(
                            "minWarningValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
                
                if (minCriticalValue != null && minWarningValue < minCriticalValue) {
                    LOG.error("minCriticalValue must be < minWarningValue.");
                    validationResults.addValidationError(new ValidationError(
                            "minCriticalValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
            }
            
            if (maxWarningValue != null) {
                if ((minValue != null && maxValue != null) 
                        && (maxWarningValue < minValue || maxWarningValue > maxValue)) {
                    LOG.error("maxWarningValue must be <= maxValue and >= minValue.");
                    validationResults.addValidationError(new ValidationError(
                            "maxWarningValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
                
                if (minWarningValue != null && maxWarningValue <= minWarningValue) {
                    LOG.error("maxWarningValue must be > minWarningValue.");
                    validationResults.addValidationError(new ValidationError(
                            "minWarningValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
                
                if (minCriticalValue != null && maxWarningValue <= minCriticalValue) {
                    LOG.error("maxWarningValue must be > minCriticalValue.");
                    validationResults.addValidationError(new ValidationError(
                            "minCriticalValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
            }
            
            if (maxCriticalValue != null) {
                if ((minValue != null && maxValue != null) 
                        && (maxCriticalValue < minValue || maxCriticalValue > maxValue)) {
                    LOG.error("maxCriticalValue must be <= maxValue and >= minValue.");
                    validationResults.addValidationError(new ValidationError(
                            "maxCriticalValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
                
                if (maxWarningValue != null && maxCriticalValue <= maxWarningValue) {
                    LOG.error("maxCriticalValue must be > maxWarningValue.");
                    validationResults.addValidationError(new ValidationError(
                            "maxWarningValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
                
                if (minWarningValue != null && maxCriticalValue <= minWarningValue) {
                    LOG.error("maxCriticalValue must be > minWarningValue.");
                    validationResults.addValidationError(new ValidationError(
                            "minWarningValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
                
                if (minCriticalValue != null && maxCriticalValue <= minCriticalValue) {
                    LOG.error("maxCriticalValue must be > minCriticalValue.");
                    validationResults.addValidationError(new ValidationError(
                            "minCriticalValue",
                            BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT));
                }
            }
            
            // Scale
            
            if (majorScaleMarkDelta == null) {
                LOG.error("majorScaleMarkDelta cannot be null.");
                validationResults.addValidationError(new ValidationError(
                        "majorScaleMarkDelta",
                        BasicValidationErrorType.NOT_NULL));
            }
            
            if (majorScaleMarkDelta == null || majorScaleMarkDelta <= 0) {
                LOG.error("majorScaleMarkDelta must be > 0.");
                validationResults.addValidationError(new ValidationError(
                        "majorScaleMarkDelta",
                        BasicValidationErrorType.RANGE_CONSTRAINT));
            }
            
            if (minorScaleMarkSegmentsPerMajorScaleMark == null) {
                LOG.error("minorScaleMarksPerMajorScaleMark cannot be null.");
                validationResults.addValidationError(new ValidationError(
                        "minorScaleMarkSegmentsPerMajorScaleMark",
                        BasicValidationErrorType.NOT_NULL));
            }
            
            if (minorScaleMarkSegmentsPerMajorScaleMark == null || minorScaleMarkSegmentsPerMajorScaleMark < 1) {
                LOG.error("minorScaleMarksPerMajorScaleMark must be >= 1.");
                validationResults.addValidationError(new ValidationError(
                        "minorScaleMarkSegmentsPerMajorScaleMark",
                        BasicValidationErrorType.RANGE_CONSTRAINT));
            }
            
            if (scaleMarkLabelScaleFactor != null && scaleMarkLabelScaleFactor < 1) {
                LOG.error("scaleMarkLabelScaleFactor must be >= 1.");
                validationResults.addValidationError(new ValidationError(
                        "scaleMarkLabelScaleFactor",
                        BasicValidationErrorType.RANGE_CONSTRAINT));
            }
            
            if (scaleMarkLabelPrecision == null) {
                LOG.error("scaleMarkLabelPrecision must be >= 0.");
                validationResults.addValidationError(new ValidationError(
                        "scaleMarkLabelPrecision",
                        BasicValidationErrorType.NOT_NULL));
            }
            
            if (scaleMarkLabelPrecision == null || scaleMarkLabelPrecision < 0) {
                LOG.error("scaleMarkLabelPrecision must be >= 0.");
                validationResults.addValidationError(new ValidationError(
                        "scaleMarkLabelPrecision",
                        BasicValidationErrorType.RANGE_CONSTRAINT));
            }
            
            // Value
            
            if (valuePrecision == null) {
                LOG.error("valuePrecision must be >= 0.");
                validationResults.addValidationError(new ValidationError(
                        "valuePrecision",
                        BasicValidationErrorType.NOT_NULL));
            }
            
            if (valuePrecision == null || valuePrecision < 0) {
                LOG.error("valuePrecision must be >= 0.");
                validationResults.addValidationError(new ValidationError(
                        "valuePrecision",
                        BasicValidationErrorType.RANGE_CONSTRAINT));
            }
            
            ///////
            
            if (validationResults.hasValidationErrors()) {
                throw new ValidationErrorException(validationResults);
            }
            
            result.setMaxValue(maxValue);
            result.setMinValue(minValue);
            result.setMajorScaleMarkDelta(majorScaleMarkDelta);
            result.setMinorScaleMarkSegmentsPerMajorScaleMark(minorScaleMarkSegmentsPerMajorScaleMark);
            result.setScaleMarkLabelScaleFactor(scaleMarkLabelScaleFactor);
            result.setScaleMarkLabelPrecision(scaleMarkLabelPrecision);
            result.setMinCriticalValue(minCriticalValue);
            result.setMinWarningValue(minWarningValue);
            result.setMaxWarningValue(maxWarningValue);
            result.setMaxCriticalValue(maxCriticalValue);
            result.setUseAlertColorGradient(useAlertColorGradient);
            result.setShowValue(showValue);
            result.setValuePrecision(valuePrecision);
            result.setTitle(title);
            
            return result;
        }
    }
}
