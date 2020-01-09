/*
 * Created by Eduard Fomin on 20.09.19 11:57
 * Copyright (c) 2019 Eduard Fomin. All rights reserved.
 * Last modified 20.09.19 11:57
 */

package com.bit.eduardf.audit;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

class ODataErrorException extends RuntimeException {

    private String errorMessage;

    /**
     * Constructor
     * @param e - exception
     * @param errorMessage - message of error
     */
    ODataErrorException(@NonNull Exception e, @NonNull String errorMessage) {
        super(e);
        this.errorMessage = errorMessage;
    }

    /**
     * Show the error message to Snackbar
     * @param activity - current activity that has a view with viewId
     * @param viewId - identifier of any view that has a CoordinatorLayout at the root
     */
    void snackbarShow(@NonNull final Activity activity, final int viewId ) {
        if (!activity.isFinishing()) {
            final View view = activity.findViewById(viewId);
            if (view != null) {
                final Runnable thread = new Runnable() {
                    @Override
                    public void run() {
                        //R.string.msg_odata_error
                        Snackbar.make(view, "Ошибка доступа в 1С: "+errorMessage, Snackbar.LENGTH_LONG)
                        .setAction(R.string.btn_finish, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                activity.finish();
                            }
                        })
                        .show();
                    }
                };
                activity.runOnUiThread(thread);
            }
        }
    }

}
//Фома2019