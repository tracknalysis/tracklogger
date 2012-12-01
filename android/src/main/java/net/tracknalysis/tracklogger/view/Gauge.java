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
 * Interface providing core gauge functionality.
 *
 * @author David Valeri
 */
public interface Gauge {

    /**
     * Initialize the gauge with its configuration settings and force an initial
     * draw or redraw of the gauge.
     */
    void init(GaugeConfiguration configuration);
    
    /**
     * Set the gauges current value and force a redraw to display the value.
     *
     * @param value the value to set
     */
    void setCurrentValue(float value);
}
