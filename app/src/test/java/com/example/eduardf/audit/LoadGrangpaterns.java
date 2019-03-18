package com.example.eduardf.audit;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 28.12.18 16:48
 *
 */

/**
 * Загрузчик предметов или папок показателей первого уровня
 * Входные параметры - [0] - guid вида аудита
 * Результат - список пунктов рециклервью
 */
class LoadGrangpaterns extends AsyncTask<String, Void, Items> {

    private boolean bySubject;
    private AuditOData oData;
    private OnLoadGrangpaternsExecute onExecute;

    /**
     * Конструктор
     * @param onExecute - интерфейс обработчиков до и после загрузки
     * @param oData - доступ к 1С
     * @param bySubject - true - если выводим по предметам, false - по показателям
     */
    LoadGrangpaterns(@NonNull OnLoadGrangpaternsExecute onExecute, AuditOData oData, boolean bySubject) {
        this.onExecute = onExecute;
        this.oData = oData;
        this.bySubject = bySubject;
    }
    @Override
    protected void onPreExecute() {
        onExecute.onLoadGrangpaternsPreExecute();
    }
    /**
     * Загрузка предметов или папок показателей первого уровня
     * @param strings - [0] - guid вида аудита
     * @return - список пунктов рециклервью
     */
    @Override
    protected Items doInBackground(String... strings) {
        if (strings.length > 0) {
            if (bySubject)
                return oData.getSubjectFirstLayer(strings[0]);
            else
                return oData.getIndicatorFoldersFirstLayer(strings[0]);
        }
        else throw new RuntimeException("No set argument");
    }
    @Override
    protected void onPostExecute(Items items) {
        onExecute.onLoadGrangpaternsPostExecute(items);
    }
    /**
     * Интерфейсы, вызываются до начала загрузки и после завершения
     */
    interface OnLoadGrangpaternsExecute {
        void onLoadGrangpaternsPreExecute();
        void onLoadGrangpaternsPostExecute(Items items);
    }
}
//Фома2018