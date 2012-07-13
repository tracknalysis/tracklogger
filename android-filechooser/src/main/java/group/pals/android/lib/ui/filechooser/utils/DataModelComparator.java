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

package group.pals.android.lib.ui.filechooser.utils;

import group.pals.android.lib.ui.filechooser.DataModel;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.util.Comparator;

/**
 * {@link DataModel} comparator.
 * 
 * @author Hai Bison
 * @since v.2.0 alpha
 */
public class DataModelComparator implements Comparator<DataModel> {

    private final FileComparator mFileComparator;

    /**
     * Creates new {@link DataModelComparator}
     * 
     * @param sortType
     *            see {@link IFileProvider#SortType}
     * @param sortOrder
     *            see {@link IFileProvider#SortOrder}
     */
    public DataModelComparator(IFileProvider.SortType sortType, IFileProvider.SortOrder sortOrder) {
        mFileComparator = new FileComparator(sortType, sortOrder);
    }

    @Override
    public int compare(DataModel lhs, DataModel rhs) {
        return mFileComparator.compare(lhs.getFile(), rhs.getFile());
    }
}
