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

package group.pals.android.lib.ui.filechooser.services;

import group.pals.android.lib.ui.filechooser.io.IFile;

import java.util.List;

/**
 * Interface for {@link IFile} providers.<br>
 * <br>
 * 
 * @author Hai Bison
 * @since v2.1 alpha
 */
public interface IFileProvider {

    /**
     * {@link IFile} sorting parameters.<br>
     * Includes:<br>
     * - {@link #SortByName}<br>
     * - {@link #SortBySize}<br>
     * - {@link #SortByDate}
     * 
     * @author Hai Bison
     * @since v2.1 alpha
     */
    public static enum SortType {
        /**
         * Sort by name, (directories first, case-insensitive)
         */
        SortByName,
        /**
         * Sort by size (directories first)
         */
        SortBySize,
        /**
         * Sort by date (directories first)
         */
        SortByDate
    }// _SortType

    /**
     * {@link IFile} sorting parameters.<br>
     * Includes:<br>
     * - {@link #Ascending}<br>
     * - {@link #Descending}
     * 
     * @author Hai Bison
     * @since v2.1 alpha
     */
    public static enum SortOrder {
        /**
         * Sort ascending.
         */
        Ascending,
        /**
         * Sort descending.
         */
        Descending
    }// _SortOrder

    /**
     * The filter of {@link IFile}.<br>
     * Includes:<br>
     * - {@link #FilesOnly}<br>
     * - {@link #DirectoriesOnly}<br>
     * - {@link #FilesAndDirectories}
     * 
     * @author Hai Bison
     * @since v2.1 alpha
     */
    public static enum FilterMode {
        /**
         * User can choose files only
         */
        FilesOnly,
        /**
         * User can choose directories only
         */
        DirectoriesOnly,
        /**
         * User can choose files or directories
         */
        FilesAndDirectories
    }// _FilterMode

    /**
     * Sets {@code true} if you want to display hidden files.
     * 
     * @param display
     */
    void setDisplayHiddenFiles(boolean display);

    /**
     * 
     * @return {@code true} if hidden files are displayed
     */
    boolean isDisplayHiddenFiles();

    /**
     * Sets regular expression for filter filename.
     * 
     * @param regex
     */
    void setRegexFilenameFilter(String regex);

    /**
     * 
     * @return the regular expression for file name filter
     */
    String getRegexFilenameFilter();

    /**
     * Sets filter mode.
     * 
     * @param fm
     *            {@link FilterMode}
     */
    void setFilterMode(FilterMode fm);

    /**
     * 
     * @return the {@link FilterMode}
     */
    FilterMode getFilterMode();

    /**
     * Sets sort type.
     * 
     * @param st
     *            {@link SortType}
     */
    void setSortType(SortType st);

    /**
     * 
     * @return the {@link SortType}
     */
    SortType getSortType();

    /**
     * Sets sort order.
     * 
     * @param so
     *            {@link SortOrder}
     */
    void setSortOrder(SortOrder so);

    /**
     * 
     * @return {@link SortOrder}
     */
    SortOrder getSortOrder();

    /**
     * Sets max file count allowed to be listed.
     * 
     * @param max
     */
    void setMaxFileCount(int max);

    /**
     * 
     * @return the max file count allowed to be listed
     */
    int getMaxFileCount();

    /**
     * Gets default path of file provider.
     * 
     * @return {@link IFile}
     */
    IFile defaultPath();

    /**
     * Gets path from pathname.
     * 
     * @param pathname
     *            a {@link String}
     * @return the path from {@code pathname}
     */
    IFile fromPath(String pathname);

    /**
     * Lists files inside {@code dir}, the result should be sorted with
     * {@link SortType} and {@link SortOrder}
     * 
     * @deprecated
     * 
     * @param dir
     *            the root directory which needs to list files
     * @param hasMoreFiles
     *            since Java does not allow variable parameters, so we use this
     *            trick. To use this parameter, set its size to {@code 1}. If
     *            the {@code dir} has more files than max file count allowed,
     *            the element returns {@code true}, otherwise it is
     *            {@code false}
     * @return an array of files, or {@code null} if an exception occurs.
     * @throws a
     *             {@link Exception}
     */
    IFile[] listFiles(IFile dir, boolean[] hasMoreFiles) throws Exception;

    /**
     * Lists files inside {@code dir}, the result should be sorted with
     * {@link SortType} and {@link SortOrder}
     * 
     * @param dir
     *            the root directory which needs to list files
     * @param hasMoreFiles
     *            since Java does not allow variable parameters, so we use this
     *            trick. To use this parameter, set its size to {@code 1}. If
     *            the {@code dir} has more files than max file count allowed,
     *            the element returns {@code true}, otherwise it is
     *            {@code false}
     * @return an array of files, or {@code null} if an exception occurs.
     * @throws a
     *             {@link Exception}
     * @since v4.0 beta
     */
    List<IFile> listAllFiles(IFile dir, boolean[] hasMoreFiles) throws Exception;

    /**
     * Lists all files inside {@code dir}, <b><i>no</b></i> filter.
     * 
     * @param dir
     *            the root directory which needs to list files
     * @return a list of files, or {@code null} if an exception occurs.
     * @throws a
     *             {@link Exception}
     * @since v4.0 beta
     */
    List<IFile> listAllFiles(IFile dir) throws Exception;
}
