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

package group.pals.android.lib.ui.filechooser.utils.ui;

import group.pals.android.lib.ui.filechooser.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Utilities for message boxes.
 * 
 * @author Hai Bison
 * @since v2.1 alpha
 */
public class Dlg {

    public static final int LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT;
    public static final int LENGTH_LONG = android.widget.Toast.LENGTH_LONG;

    private static android.widget.Toast mToast;

    public static void toast(Context context, CharSequence msg, int duration) {
        if (mToast != null)
            mToast.cancel();
        mToast = android.widget.Toast.makeText(context, msg, duration);
        mToast.show();
    }// mToast()

    public static void toast(Context context, int msgId, int duration) {
        toast(context, context.getString(msgId), duration);
    }// mToast()

    public static void showInfo(Context context, CharSequence msg) {
        new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_info).setTitle(R.string.afc_title_info)
                .setMessage(msg).show();
    }// showInfo()

    public static void showInfo(Context context, int msgId) {
        showInfo(context, context.getString(msgId));
    }// showInfo()

    public static void showError(Context context, CharSequence msg, DialogInterface.OnCancelListener listener) {
        new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.afc_title_error)
                .setMessage(msg).setOnCancelListener(listener).show();
    }// showError()

    public static void showError(Context context, int msgId, DialogInterface.OnCancelListener listener) {
        showError(context, context.getString(msgId), listener);
    }// showError()

    public static void showUnknownError(Context context, Throwable t, DialogInterface.OnCancelListener listener) {
        showError(context, String.format(context.getString(R.string.afc_pmsg_unknown_error), t), listener);
    }// showUnknownError()

    public static void confirmYesno(Context context, CharSequence msg, DialogInterface.OnClickListener onYes) {
        new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.afc_title_confirmation).setMessage(msg).setPositiveButton(android.R.string.yes, onYes)
                .setNegativeButton(android.R.string.no, null).show();
    }// confirmYesno()
}
