package com.bit.eduardf.audit;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 04.04.19 15:18
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 05.02.19 9:47
 *
 */

/**
 * Класс для получения списка типов аналитик для отбора по виду аудита и типу объекта
 * Вход: [0] - guid вида аудита, [1] - тип объекта аудита
 * Выход: список показателей для заполнения задания
 */
class LoadAnalyticTypes extends AsyncTask<String, Void, ArrayList<String>> {
    private final OnLoadAnalyticTypes onExecute;
    private final AuditOData oData;

    /**
     * Конструктор
     * @param onExecute - обработчики загрузчика
     * @param oData - доступ в 1С
     */
    LoadAnalyticTypes(@NonNull OnLoadAnalyticTypes onExecute, @NonNull AuditOData oData) {
            this.onExecute = onExecute;
            this.oData = oData;
    }

    @Override
    protected void onPreExecute() {
        onExecute.onLoadAnalyticTypesPreExecute();
    }
    @Override
    protected ArrayList<String> doInBackground(String... strings) {
        final String type = strings[0];
        final String objectType = strings[1];
        return oData.getAnalyticTypes(type, objectType);
    }
    @Override
    protected void onPostExecute(ArrayList<String> parentTypes) {
        onExecute.onLoadAnalyticTypesPostExecute(parentTypes);
    }
    /**
     * Интерфейс обработчика событий до и после
     */
    interface OnLoadAnalyticTypes {
        void onLoadAnalyticTypesPreExecute();
        void onLoadAnalyticTypesPostExecute(ArrayList<String> parentTypes);
    }
}
//Фома2018
