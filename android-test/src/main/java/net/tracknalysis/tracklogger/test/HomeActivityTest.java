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
package net.tracknalysis.tracklogger.test;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.activity.HomeActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;

/**
 * @author David Valeri
 */
public class HomeActivityTest extends ActivityInstrumentationTestCase2<HomeActivity> {
    
    private Button logButton;
    private Button sessionButton;
    private Button configButton;

    public HomeActivityTest() {
        super(HomeActivity.class);
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        logButton = (Button) getActivity().findViewById(R.id.launchLog);
        sessionButton = (Button) getActivity().findViewById(R.id.launchSessionList);
        configButton = (Button) getActivity().findViewById(R.id.launchConfig);
    }

    public void testInitialLayout() {
        assertNotNull(logButton);
        assertNotNull(sessionButton);
        assertNotNull(configButton);
    }
}
