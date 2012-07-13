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

import android.os.Parcelable;

/**
 * A history store of any object.
 * 
 * @author Hai Bison
 * 
 * @param <A>
 *            any type
 * @since v2.0 alpha
 */
public interface History<A> extends Parcelable {

    /**
     * Pushes new item to the history.
     * 
     * @param currentItem
     *            usage: assume we have history of: 1-2-3-4, if current item is
     *            3, and we push 5 to the history, then 4 will be truncated, and
     *            new history will be 1-2-3-5
     * @param newItem
     *            the new item
     */
    void push(A currentItem, A newItem);

    /**
     * Removes an item.
     * 
     * @param item
     *            {@link A}
     * @since v4.0 beta
     */
    void remove(A item);

    /**
     * Removes all items by a filter.
     * 
     * @param filter
     *            {@link HistoryFilter}
     * @since v4.0 beta
     */
    void removeAll(HistoryFilter<A> filter);

    /**
     * Gets size of the history
     * 
     * @return the size of the history
     */
    int size();

    /**
     * Gets index of item {@code a}
     * 
     * @param a
     *            an item
     * @return index of the {@code a}, or -1 if there is no one
     */
    int indexOf(A a);

    /**
     * Gets previous item of {@code a}
     * 
     * @param a
     *            current item
     * @return the previous item, can be {@code null}
     */
    A prevOf(A a);

    /**
     * Gets next item of {@code a}
     * 
     * @param a
     *            current item
     * @return the next item, can be {@code null}
     */
    A nextOf(A a);

    /**
     * Adds a {@link HistoryListener}
     * 
     * @param listener
     *            {@link HistoryListener}
     * @since v4.0 beta
     */
    void addListener(HistoryListener<A> listener);

    /**
     * Removes a {@link HistoryListener}
     * 
     * @param listener
     *            {@link HistoryListener}
     * @return the removed listener
     * @since v4.0 beta
     */
    void removeListener(HistoryListener<A> listener);

    /**
     * Notifies to all {@link HistoryListener}'s that the history changed.
     */
    void notifyHistoryChanged();
}
