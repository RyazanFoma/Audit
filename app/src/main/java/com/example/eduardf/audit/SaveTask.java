package com.example.eduardf.audit;

import android.content.Context;
import android.os.AsyncTask;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 13.12.18 13:56
 *
 */

/**
 * Класс для сохранения нового или существующего задания
 * Входные параметры: [0] - задание
 */
class SaveTask extends AsyncTask<Tasks.Task, Void, Void> {
    private final AuditOData oData;
    private final OnSaveTaskExecute onExecute;
    //Конструктор
    SaveTask(Context context, AuditOData oData) {
        if (context instanceof OnSaveTaskExecute) {
            this.onExecute = (OnSaveTaskExecute) context;
            this.oData = oData;
        }
        else
            throw new RuntimeException(context.toString()+" must implement OnSaveTaskExecute");
    }
    @Override
    protected void onPreExecute() {
        onExecute.onSaveTaskPreExecute();
    }
    @Override
    protected Void doInBackground(Tasks.Task... tasks) {
        if (tasks[0].id != null) //Обновляем существующее задание
            oData.updateTask(tasks[0]);
        else //Создаем новое задание
            oData.createTask(tasks[0]);
        return null;
    }
    @Override
    protected void onPostExecute(Void voids) {
        onExecute.onSaveTaskPostExecute();
    }

    /**
     * Интерфейс обработчика событий до и после
     */
    interface OnSaveTaskExecute {
        void onSaveTaskPreExecute();
        void onSaveTaskPostExecute();
    }
}
//Фома2018