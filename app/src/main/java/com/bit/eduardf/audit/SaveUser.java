package com.bit.eduardf.audit;

import android.content.Context;
import android.os.AsyncTask;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 11.12.18 11:20
 *
 */

/**
 * Сохранение индивидуальных настроек пользователя
 * Входные параметры:
 * [0] - user giud
 * [1] - audit type giud
 * [2] - organization giud
 * [3] - object giud
 * [4] - responsible giud
 * [5] - DataVersion
 */
class SaveUser extends AsyncTask<String, Void, String> {

    final private AuditOData oData;
    private final OnSaveUserExecute onExecute;

    /**
     * Конструктор
     * @param oData - OData для доступа в 1С
     */
    SaveUser(Context context, AuditOData oData) {
        if (context instanceof SaveUser.OnSaveUserExecute) {
            this.onExecute = (SaveUser.OnSaveUserExecute) context;
            this.oData = oData;
        }
        else
            throw new RuntimeException(context.toString()+" must implement OnSaveUserExecute");
    }
    protected String doInBackground(String... guids) {
        return oData.saveUser(guids[0], guids[1], guids[2], guids[3], guids[4], guids[5]);
    }
    protected void onPostExecute(String version) { onExecute.onSaveUserExecute(version); }

    interface OnSaveUserExecute {
        void onSaveUserExecute(String version);
    }
}
//Фома2018