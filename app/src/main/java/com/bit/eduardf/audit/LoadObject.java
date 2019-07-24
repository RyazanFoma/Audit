package com.bit.eduardf.audit;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 04.02.19 17:00
 *
 */

/**
 * Загрузка вида аудита
 * Вход: [0] guid вида аудита
 * Выход: вид аудита
 */
class LoadObject extends AsyncTask<String, Void, Object> {

    final private OnLoadObject onExecute;
    final private AuditOData oData;
    final private AuditOData.Set set;

    /**
     * Конструктор
     * @param onExecute - контекст с обратным вызовом
     * @param oData - доступ в 1С
     */
    LoadObject(@NonNull OnLoadObject onExecute, AuditOData oData, AuditOData.Set set) {
        this.onExecute = onExecute;
        this.oData = oData;
        this.set = set;
    }
    @Override
    protected void onPreExecute() {
        onExecute.onPreLoadObject();
    }
    @Override
    protected final Object doInBackground(String[] guids) {
        return oData.getObject(guids[0], set);
    }
    @Override
    protected void onPostExecute(Object object) {
        onExecute.onPostLoadObject(object);
    }

    /**
     * Обратный вызов для возврата загруженных данных
     */
    interface OnLoadObject{
        void onPreLoadObject(); //перед загрузкой
        void onPostLoadObject(Object object); //после загрузки всех пунктов
    }
}
//Фома2018