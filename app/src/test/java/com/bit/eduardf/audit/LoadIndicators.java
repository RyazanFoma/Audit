package com.bit.eduardf.audit;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 11.01.19 12:31
 *
 */

/**
 * Загрузка показателей по папкам или предметам
 * Вход: [0] - guid вида аудита, [1] - guid родителя
 * Выход: Список показателей {@link com.bit.eduardf.audit.LoadIndicators#doInBackground}
 */
class LoadIndicators extends AsyncTask<String, Void, Indicators> {
    private final OnLoadIndicatorsExecute onExecute;
    private final AuditOData oData;
    private final boolean bySubject;

    /**
     * Конструктор
     * @param onExecute - интерфейс для вызова обработчиков
     * @param oData - доступ в 1С
     * @param bySubject - признак По предметам
     */
    LoadIndicators(@NonNull OnLoadIndicatorsExecute onExecute, @NonNull AuditOData oData, boolean bySubject) {
        this.onExecute = onExecute;
        this.bySubject = bySubject;
        this.oData = oData;
    }
    @Override
    protected void onPreExecute() {
        onExecute.onLoadIndicatorsPreExecute();
    }
    @Override
    protected Indicators doInBackground(String... key) {
        final String type = key[0];
        final String pater = key[1];
        Indicators indicators;
        if (bySubject) {
            indicators = oData.getSubjects(type, pater);
            indicators.addAll(oData.getIndicators(type, null, pater));
        }
        else
            indicators = oData.getIndicators(type, pater, null);
        return indicators;
    }
    @Override
    protected void onPostExecute(Indicators indicators) {
        onExecute.onLoadIndicatorsPostExecute(indicators);
    }
    interface OnLoadIndicatorsExecute {
        void onLoadIndicatorsPreExecute();
        void onLoadIndicatorsPostExecute(Indicators indicators);
    }
}
//Фома2019
