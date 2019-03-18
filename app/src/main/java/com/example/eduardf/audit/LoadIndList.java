package com.example.eduardf.audit;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 23.01.19 15:34
 *
 */

class LoadIndList extends AsyncTask<Tasks.Task.IndicatorRow,Void,IndList> {

    private final OnLoadIndListExecute onExecute;
    private final AuditOData oData;
    private final boolean bySubject;
    private final String type;
    private ArrayList<String> required; //Список guid нужных показателей и папок

    /**
     * Конструктор
     * @param onExecute - интерфейс обработчика до и после загрузки
     * @param oData - доступ в 1С
     * @param bySubject - признак По предметам
     */
    LoadIndList(OnLoadIndListExecute onExecute, @NonNull AuditOData oData, @NonNull String type, boolean bySubject) {
        this.onExecute = onExecute;
        this.oData = oData;
        this.type = type;
        this.bySubject = bySubject;
        required = new ArrayList<>();
    }

    /**
     * Интерфейс обработчиков до и после загрузки
     */
    interface OnLoadIndListExecute {
        void onLoadIndListPreExecute();
        void onLoadIndListPostExecute(IndList indicators);
    }

    /**
     * Отметить всех нужных предков - expand = true
     * @param indicators - список показателей
     * @param children - guid потомока
     */
    private void rehabilitation(final IndList indicators, final String children) {
        //Если это не корень и еще неизвестный родитель
        if (!(children == null || required.contains(children))) {
            final IndList.Ind ind = indicators.get(children);
            //Признак что данного родителя нужно оставить в результатах загрузки
            ind.expand = true;
            //Признак того, что данный показатель нужно оставить в результатах загрузки
            required.add(children);
            //К следующему предку
            rehabilitation(indicators, ind.pater);
        }
    }

    @Override
    protected void onPreExecute() {
        if (onExecute != null) onExecute.onLoadIndListPreExecute();
    }
    @Override
    protected IndList doInBackground(Tasks.Task.IndicatorRow... indicatorRows) {
        //Результаты загрузки - показатели вместе с папками
        final IndList indicators = new IndList();
        if (bySubject) { //По предметам
            //Добавляем все предметы по виду аудита в качестве папок
            oData.addSubjects(indicators, type);
            //Добавляем все показатели по виду аудита в качестве элементов
            oData.addInd(indicators, type, true);
        }
        else { //По показателям
            //Добавляем все показатели вместе с папками по виду аудита
            oData.addInd(indicators, type, false);
        }
        //По показателям задания
        for (Tasks.Task.IndicatorRow row: indicatorRows) {
            final IndList.Ind ind = indicators.get(row.indicator);
            if (ind != null) {
                if (bySubject) ind.pater = ind.subject;
                ind.goal = row.goal;
                ind.minimum = row.minimum;
                ind.maximum = row.maximum;
                ind.error = row.error;
                ind.value = row.value;
                ind.comment = row.comment;
                ind.achieved = row.achived;
                //Признак того, что данный показатель нужно оставить в результатах загрузки
                required.add(ind.id);
                //Отмечаем нужных предков
                rehabilitation(indicators, ind.pater);
            }
        }
        //Переносим нужные показатели и папки в результаты загрузки
        final IndList indList = new IndList();
        for (String key: required) {
            indList.add(indicators.get(key));
        }
        required = null;
        return indList;
    }
    @Override
    protected void onPostExecute(IndList indicators) {
        if (onExecute != null) onExecute.onLoadIndListPostExecute(indicators);
    }
}
//Фома2019