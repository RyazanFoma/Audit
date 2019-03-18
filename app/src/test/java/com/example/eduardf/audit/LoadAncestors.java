package com.example.eduardf.audit;

import android.os.AsyncTask;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 09.01.19 16:49
 *
 */

/**
 * Загрузка всех предков в стек
 * Вход: [0] - guid родителя
 * Выход: стек предков
 */
class LoadAncestors extends AsyncTask<String, Void, Stack> {
    private OnLoadAncestorsExecute onExecute;
    private AuditOData oData;
    private boolean bySubject;

    /**
     * Конструктор
     * @param onExecute - интерфейс для вызова обратотчиков
     * @param oData - доступ в 1С
     * @param bySubject - признак По предметам
     */
    LoadAncestors(OnLoadAncestorsExecute onExecute, AuditOData oData, boolean bySubject) {
        this.onExecute = onExecute;
        this.oData = oData;
        this.bySubject = bySubject;
    }
    @Override
    protected void onPreExecute() {
        onExecute.OnLoadAncestorsPreExecute();
    }
    @Override
    protected Stack doInBackground(String... strings) {
        final String pater = strings[0];
        final Stack stack = new Stack() {
            @Override
            Item getItem(String id) {
                return oData.getItem(
                        bySubject? AuditOData.Set.SUBJECT:
                                AuditOData.Set.INDICATOR, id);
            }
        };
        stack.loadStack(pater);
        return stack;
    }
    @Override
    protected void onPostExecute(Stack stack) {
        onExecute.OnLoadAncestorsPostExecute(stack);
    }
    interface OnLoadAncestorsExecute {
        void OnLoadAncestorsPreExecute();
        void OnLoadAncestorsPostExecute(Stack stack);
    }
}
//Фома2019