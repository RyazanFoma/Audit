/*
 * Created by Eduard Fomin on 18.09.19 9:29
 * Copyright (c) 2019 Eduard Fomin. All rights reserved.
 * Last modified 17.09.19 11:34
 */

package com.bit.eduardf.audit;

import android.arch.lifecycle.Lifecycle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class DialogThread implements Runnable {

    private final FragmentActivity activity;
    private final AuditOData oData;
    private final String message;
//    private final DialogODataError dialogODataError;

    /**
     * Конструктор
     * @param activity - текущая активность
     * @param oData - контекст с обработчиком кнопок диалога
     * @param message - сообщение об ошибке
     */
    DialogThread(@NonNull FragmentActivity activity, @NonNull AuditOData oData, @NonNull String message) {
        this.activity = activity;
        this.oData = oData;
        this.message = message;
//        dialogODataError = DialogODataError.newInstance(oData, message);
    }

    @Override
    public void run() {
        final FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        final Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("DialogODataError");
        if (prev != null) {
            if (prev.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED)
                return;
        }
//        if (prev != null) {
//            ((DialogODataError)prev).dismissAllowingStateLoss();
//            ft.remove(prev);
//        }
//        ft.addToBackStack(null);
        final DialogODataError dialogODataError = DialogODataError.newInstance(oData, message);
        dialogODataError.show(ft, "DialogODataError");
//        dialogODataError.show(activity.getSupportFragmentManager(), "DialogODataError");
    }

}
//Фома2019