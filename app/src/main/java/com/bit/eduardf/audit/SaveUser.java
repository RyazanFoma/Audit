package com.bit.eduardf.audit;

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
 * [0] - quid аудитора,
 * [1] - quid вида аудита,
 * [2] - quid организации,
 * [3] - quid объекта,
 * [4] - quid ответственного
 */
class SaveUser extends AsyncTask<String, Void, Void> {

    final private AuditOData oData;

    /**
     * Конструктор
     * @param oData - OData для доступа в 1С
     */
    SaveUser(AuditOData oData) {
            this.oData = oData;
    }
    protected Void doInBackground(String... guids) {
        oData.saveUser(guids[0], guids[1], guids[2], guids[3], guids[4]);
        return null;
    }
    protected void onPostExecute(Void v) {
    }
}
//Фома2018