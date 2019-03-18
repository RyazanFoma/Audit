package com.example.eduardf.audit;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 31.01.19 14:31
 *
 */

/**
 * Получение наименования элемента справочника по его коду
 * Входные параметры: quid элемента
 * Результат возвращается через обратный вызов OnSetName
 */
class SetName extends AsyncTask<String, Void, String> {

    final private OnSetName onSetName;
    final private AuditOData.Set set;
    final private AuditOData oData;

    /**
     * Конструктор
     * @param fragment - контекст с обратным вызовом OnSetName
     * @param oData - OData для доступа в 1С
     * @param set - таблица справочника
     */
    SetName(Fragment fragment, AuditOData oData, AuditOData.Set set) {
        if (fragment instanceof OnSetName) {
            this.onSetName = (OnSetName) fragment;
            this.oData = oData;
            this.set = set;
        }
        else
            throw new RuntimeException(fragment.toString()+" must implement OnSetName");
    }
    @Override
    protected String doInBackground(String... id) {
        return oData.getName(set, id[0]);
    }
    @Override
    protected void onPostExecute(String name) {
        if (onSetName != null) onSetName.onSetName(name);
    }

    /**
     * Обратный вызов для возврата наименования
     */
    interface OnSetName{
        void onSetName(String name);
    }
}
//Фома2018