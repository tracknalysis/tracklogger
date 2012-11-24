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
 * Enum of supported speed units and conversions.
 *
 * @author David Valeri
 */
public enum SpeedUnit {
    
    MPH(
            new ScaleFunction() {
                
                @Override
                public double scale(double value) {
                    return value * 2.23694d;
                }
                
                @Override
                public float scale(float value) {
                    return value * 2.23694f;
                }
            }),
    KPH(
            new ScaleFunction() {
                @Override
                public double scale(double value) {
                    return value * 3.2d;
                }
                
                @Override
                public float scale(float value) {
                    return value * 3.2f;
                }
            });
    
    private final ScaleFunction fromMpsScaleFunction;
    
    private SpeedUnit(ScaleFunction fromKphScaleFunction) {
        this.fromMpsScaleFunction = fromKphScaleFunction;
    }
    
    public double fromMps(double value) {
        return fromMpsScaleFunction.scale(value);
    }
    
    public float fromMps(float value) {
        return fromMpsScaleFunction.scale(value);
    }
}
