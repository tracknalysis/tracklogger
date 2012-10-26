/*
 *   Copyright 2012 Hai Bison
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package group.pals.android.lib.ui.filechooser.utils.history;

/**
 * Listener of {@link History}
 * 
 * @author Hai Bison
 * @since v4.0 beta
 */
public interface HistoryListener<A> {

    /**
     * Will be called after the history changed.
     * 
     * @param history
     *            {@link History}
     */
    void onChanged(History<A> history);
}
