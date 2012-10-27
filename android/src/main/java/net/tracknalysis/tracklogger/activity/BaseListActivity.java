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

import android.app.ListActivity;

/**
 * Base activity providing common functionality in the application.
 * 
 * @author David Valeri
 */
public class BaseListActivity extends ListActivity {

    private DialogManager dialogManager = new DialogManager();

    /**
     * Displays the generic error dialog that on dismissal does not terminate
     * the activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    protected void onNonTerminalError() {
        dialogManager.onNonTerminalError(this);
    }

    /**
     * Displays an error dialog that on dismissal does not terminate the
     * activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    protected void onNonTerminalError(int errorMessage) {
        dialogManager.onNonTerminalError(this, errorMessage);
    }

    /**
     * Displays an error dialog that on dismissal does not terminate the
     * activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     * @param args
     *            optional arguments for the error message template text
     */
    protected void onNonTerminalError(int errorMessage, Object... args) {
        dialogManager.onNonTerminalError(this, errorMessage, args);
    }

    /**
     * Displays the generic error dialog that on dismissal terminates the
     * activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    protected void onTerminalError() {
        dialogManager.onTerminalError(this);
    }

    /**
     * Displays an error dialog that on dismissal terminates the activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    protected void onTerminalError(int errorMessage) {
        dialogManager.onTerminalError(this, errorMessage);
    }

    /**
     * Displays an error dialog that on dismissal terminates the activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     * @param args
     *            optional arguments for the error message template text
     */
    protected void onTerminalError(int errorMessage,
            Object... args) {
        dialogManager.onTerminalError(this, errorMessage, args);
    }
    
    protected DialogManager getDialogManager() {
        return dialogManager;
    }

    @Override
    protected void onDestroy() {
        dialogManager.onDestroy();
        super.onDestroy();
    }
}
