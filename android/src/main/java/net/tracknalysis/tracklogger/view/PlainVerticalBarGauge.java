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

import android.content.Context;
import android.util.AttributeSet;

/**
 * A bar gauge that is oriented such that the bar moves from bottom to top with
 * the option to display the bar value in the middle of the bar. No adornments
 * such as scale marks are provided.
 * 
 * @author David Valeri
 */
public class PlainVerticalBarGauge extends AbstractBarGauge {
    
    public PlainVerticalBarGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected SweepDirection getSweepDirection() {
        return SweepDirection.BOTTOM_TO_TOP;
    }
    
    @Override
    protected boolean isDrawScale() {
        return false;
    }
}
