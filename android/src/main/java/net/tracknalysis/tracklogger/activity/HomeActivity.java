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
package net.tracknalysis.tracklogger.activity;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * @author David Valeri
 */
public class HomeActivity extends Activity implements OnClickListener {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.home);
        
        Button sessionsButton = (Button) findViewById(R.id.launchSessionList);
        sessionsButton.setOnClickListener(this);
        
        Button logButton = (Button) findViewById(R.id.launchLog);
        logButton.setOnClickListener(this);
        
        Button splitMarkerSetManagementButton = (Button) findViewById(R.id.launchSplitMarkerSetManagement);
        splitMarkerSetManagementButton.setOnClickListener(this);
        
        Button configButton = (Button) findViewById(R.id.launchConfig);
        configButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.launchSessionList:
                startActivity(new Intent(Intent.ACTION_VIEW, TrackLoggerData.Session.CONTENT_URI));
                break;
            case R.id.launchLog:
                startActivity(new Intent(this, LogActivity.class));
                break;
            case R.id.launchSplitMarkerSetManagement:
                startActivity(new Intent(Intent.ACTION_VIEW, TrackLoggerData.SplitMarkerSet.CONTENT_URI));
                break;
            case R.id.launchConfig:
                startActivity(new Intent(this, ConfigActivity.class));
                break;
        }
    }
}
