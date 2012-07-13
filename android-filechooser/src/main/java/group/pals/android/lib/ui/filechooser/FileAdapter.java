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

package group.pals.android.lib.ui.filechooser;

import group.pals.android.lib.ui.filechooser.io.IFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;
import group.pals.android.lib.ui.filechooser.services.IFileProvider.FilterMode;
import group.pals.android.lib.ui.filechooser.utils.Converter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The adapter to be used in {@link android.widget.ListView}
 * 
 * @author Hai Bison
 * 
 */
public class FileAdapter extends ArrayAdapter<DataModel> {

    /**
     * Default short format for file time. Value = {@code "yyyy.MM.dd hh:mm a"}<br>
     * See <a href=
     * "http://developer.android.com/reference/java/text/SimpleDateFormat.html"
     * >API docs</a>.
     */
    public static final String _DefFileTimeShortFormat = "yyyy.MM.dd hh:mm a";

    /**
     * You can set your own short format for file time by this variable. If the
     * value is in wrong format, {@link #_DefFileTimeShortFormat} will be used.<br>
     * See <a href=
     * "http://developer.android.com/reference/java/text/SimpleDateFormat.html"
     * >API docs</a>.
     */
    public static String fileTimeShortFormat = _DefFileTimeShortFormat;

    private final boolean mIsMultiSelection;
    private final IFileProvider.FilterMode mSelectionMode;

    /**
     * Creates new {@link FileAdapter}
     * 
     * @param context
     *            {@link Context}
     * @param objects
     *            the data
     * @param filterMode
     *            see {@link IFileProvider.FilterMode}
     * @param multiSelection
     *            see {@link FileChooserActivity#_MultiSelection}
     */
    public FileAdapter(Context context, List<DataModel> objects, IFileProvider.FilterMode filterMode,
            boolean multiSelection) {
        super(context, R.layout.afc_file_item, objects);
        this.mSelectionMode = filterMode;
        this.mIsMultiSelection = multiSelection;
    }

    /**
     * The "view holder"
     * 
     * @author Hai Bison
     * 
     */
    private static final class Bag {

        TextView txtFileName;
        TextView txtFileInfo;
        CheckBox checkboxSelection;
        ImageView imageIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DataModel data = getItem(position);
        Bag bag;

        if (convertView == null) {
            LayoutInflater layoutInflater = ((Activity) getContext()).getLayoutInflater();
            convertView = layoutInflater.inflate(R.layout.afc_file_item, null);

            bag = new Bag();
            bag.txtFileName = (TextView) convertView.findViewById(R.id.afc_text_view_filename);
            bag.txtFileInfo = (TextView) convertView.findViewById(R.id.afc_text_view_file_info);
            bag.checkboxSelection = (CheckBox) convertView.findViewById(R.id.afc_checkbox_selection);
            bag.imageIcon = (ImageView) convertView.findViewById(R.id.afc_image_view_icon);

            convertView.setTag(bag);
        } else {
            bag = (Bag) convertView.getTag();
        }

        // update view
        updateView(parent, bag, data, data.getFile());

        return convertView;
    }

    /**
     * Updates the view.
     * 
     * @param parent
     *            the parent view
     * @param bag
     *            the "view holder", see {@link Bag}
     * @param fData
     *            {@link DataModel}
     * @param file
     *            {@link IFile}
     * @since v2.0 alpha
     */
    private void updateView(ViewGroup parent, Bag bag, final DataModel fData, IFile file) {
        // if parent is list view, enable multiple lines
        boolean useSingleLine = parent instanceof GridView;
        for (TextView tv : new TextView[] { bag.txtFileName, bag.txtFileInfo }) {
            tv.setSingleLine(useSingleLine);
            if (useSingleLine)
                tv.setEllipsize(TextUtils.TruncateAt.END);
        }

        // image icon
        if (file.isDirectory())
            bag.imageIcon.setImageResource(R.drawable.afc_folder);
        else
            bag.imageIcon.setImageResource(R.drawable.afc_file);

        // filename
        bag.txtFileName.setText(file.getName());

        // file info
        String time = null;
        try {
            time = new SimpleDateFormat(fileTimeShortFormat).format(file.lastModified());
        } catch (Exception e) {
            try {
                time = new SimpleDateFormat(_DefFileTimeShortFormat).format(file.lastModified());
            } catch (Exception ex) {
                time = new Date(file.lastModified()).toLocaleString();
            }
        }
        if (file.isDirectory())
            bag.txtFileInfo.setText(time);
        else {
            bag.txtFileInfo.setText(String.format("%s, %s", Converter.sizeToStr(file.length()), time));
        }

        // checkbox
        if (mIsMultiSelection) {
            if (mSelectionMode == FilterMode.FilesOnly && file.isDirectory()) {
                bag.checkboxSelection.setVisibility(View.GONE);
            } else {
                bag.checkboxSelection.setVisibility(View.VISIBLE);
                bag.checkboxSelection.setFocusable(false);
                bag.checkboxSelection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        fData.setSelected(isChecked);
                    }
                });
                bag.checkboxSelection.setChecked(fData.isSelected());
            }
        } else
            bag.checkboxSelection.setVisibility(View.GONE);
    }// updateView
}
