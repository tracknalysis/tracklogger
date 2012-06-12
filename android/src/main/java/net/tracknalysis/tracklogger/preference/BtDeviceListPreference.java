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
package net.tracknalysis.tracklogger.preference;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * @author Georg Lukas
 */
public class BtDeviceListPreference extends ListPreference {
    
    public BtDeviceListPreference(Context context) {
        super(context, null);
    }

    public BtDeviceListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        String[] entries = new String[pairedDevices.size()];
        String[] entryValues = new String[pairedDevices.size()];
        
        int i = 0;
        for (BluetoothDevice dev : pairedDevices) {
            entries[i] = dev.getName();
            entryValues[i] = dev.getAddress();
            i++;
        }
        
        setEntries(entries);
        setEntryValues(entryValues);
    }
}
