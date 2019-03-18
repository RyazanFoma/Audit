package com.example.eduardf.audit;

import android.content.Context;
import android.os.AsyncTask;
import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 23.01.19 16:47
 *
 */

/**
 * Класс для получения списка показателей из регистра нормативов
 * Вход: [0] - guid вида аудита, [1] - guid объекта аудита
 * Выход: список показателей для заполнения задания
 */
class LoadIndicatorRows extends AsyncTask<String, Void, ArrayList<Tasks.Task.IndicatorRow>> {
    private final OnLoadIndicatorRowsExecute onExecute;
    private final AuditOData oData;

    /**
     * Конструктор
     * @param context - текущий констекст
     * @param oData - доступ в 1С
     */
    LoadIndicatorRows(Context context, AuditOData oData) {
        if (context instanceof OnLoadIndicatorRowsExecute) {
            this.onExecute = (OnLoadIndicatorRowsExecute) context;
            this.oData = oData;
        }
        else
            throw new RuntimeException(context.toString()+" must implement OnLoadIndicatorRowsExecute");
    }
    @Override
    protected void onPreExecute() {
        onExecute.onLoadIndicatorRowsPreExecute();
    }
    @Override
    protected ArrayList<Tasks.Task.IndicatorRow> doInBackground(String... strings) {
        final String type = strings[0];
        final String object = strings[1];
        ArrayList<Tasks.Task.IndicatorRow> rows = oData.getStandardIndicatorRows(type, object);
        if (rows.isEmpty()) rows = oData.getIndicatorRows(type);
        return rows;
    }
    @Override
    protected void onPostExecute(ArrayList<Tasks.Task.IndicatorRow> rows) {
        onExecute.onLoadIndicatorRowsPostExecute(rows);
    }
    /**
     * Интерфейс обработчика событий до и после
     */
    interface OnLoadIndicatorRowsExecute {
        void onLoadIndicatorRowsPreExecute();
        void onLoadIndicatorRowsPostExecute(ArrayList<Tasks.Task.IndicatorRow> rows);
    }
}
//Фома2018
