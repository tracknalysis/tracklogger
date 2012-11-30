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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import net.tracknalysis.tracklogger.R;

/**
 * Handler for managing common dialogs and their lifecycles.
 * 
 * @author David Valeri
 */
class DialogManager {

    private Dialog errorDialog;

    /**
     * Displays the generic error dialog that on dismissal does not terminate
     * the activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    public void onNonTerminalError(Activity activity) {
        onNonTerminalError(activity, R.string.general_error);
    }

    /**
     * Displays an error dialog that on dismissal does not terminate the
     * activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    public void onNonTerminalError(Activity activity, int errorMessage) {
        onNonTerminalError(activity, errorMessage, (Object[]) null);
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
    public void onNonTerminalError(Activity activity, int errorMessage,
            Object... args) {
        if (errorDialog != null) {
            errorDialog.dismiss();
        }

        errorDialog = createErrorDialog(activity, false,
                R.string.error_alert_title, errorMessage, args);

        if (!activity.isFinishing()) {
            errorDialog.show();
        }
    }

    /**
     * Displays the generic error dialog that on dismissal terminates the
     * activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    public void onTerminalError(Activity activity) {
        onTerminalError(activity, R.string.general_error);
    }

    /**
     * Displays an error dialog that on dismissal terminates the activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     */
    public void onTerminalError(Activity activity, int errorMessage) {
        onTerminalError(activity, errorMessage, (Object[]) null);
    }

    /**
     * Displays an error dialog that on dismissal terminates the activity.
     * 
     * @param errorMessage
     *            the resource ID of the error message text
     * @param args
     *            optional arguments for the error message template text
     */
    public void onTerminalError(Activity activity, int errorMessage,
            Object... args) {
        if (errorDialog != null) {
            errorDialog.dismiss();
        }

        errorDialog = createErrorDialog(activity, true,
                R.string.error_alert_title, errorMessage, args);

        if (!activity.isFinishing()) {
            errorDialog.show();
        }
    }

    /**
     * Creates and returns an alert dialog with the title text containing the
     * string with ID {@code title} and an confirmation message containing the
     * string with ID {@code message} and optional arguments {@code args}. The
     * OK button on the dialog triggers the optional positive handler of this
     * dialog while the Cancel button and back button trigger the optional
     * negative handler.
     * 
     * @param context
     *            the context that owns this dialog
     * @param positiveRunnable
     *            a runnable executed on the click of the positive button
     * @param negativeRunnable
     *            a runnable executed on the click of the negative button
     * @param title
     *            the string ID for the dialog title
     * @param message
     *            the string ID for the dialog message
     * @param args
     *            the optional arguments for token replacement in the string
     *            with ID {@code errorMessage}
     */
    public AlertDialog createConfirmDialog(final Context context,
            final Runnable positiveRunnable, final Runnable negativeRunnable,
            int title, int message, Object... args) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(message, args))
                .setTitle(title)
                .setPositiveButton(R.string.general_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (positiveRunnable != null) {
                                    positiveRunnable.run();
                                }
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.general_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (negativeRunnable != null) {
                                    negativeRunnable.run();
                                }
                                dialog.dismiss();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        if (negativeRunnable != null) {
                            negativeRunnable.run();
                        }
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    /**
     * Creates and returns an alert dialog with the title text containing the
     * string with ID {@code title} and an error message containing the string
     * with ID {@code errorMessage} and optional arguments {@code args}. The OK
     * button on the dialog triggers the finishing of this activity if
     * {@code finish} is {@code true}. There is no cancel button.
     * 
     * @param avtivity
     *            the activity that owns this dialog
     * @param finish
     *            if true, clocking the OK button triggers the activity to
     *            finish
     * @param title
     *            the string ID for the dialog title
     * @param errorMessage
     *            the string ID for the dialog message
     * @param args
     *            the optional arguments for token replacement in the string
     *            with ID {@code errorMessage}
     */
    public AlertDialog createErrorDialog(final Activity activity,
            final boolean finish, int title, int errorMessage, Object... args) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(activity.getString(errorMessage, args))
                .setTitle(title)
                .setPositiveButton(R.string.general_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                if (finish) {
                                    activity.finish();
                                }
                            }
                        });

        return builder.create();
    }

    public void onDestroy() {
        if (errorDialog != null) {
            errorDialog.dismiss();
        }
    }
}
