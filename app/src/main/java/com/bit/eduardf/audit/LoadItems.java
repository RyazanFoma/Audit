package com.bit.eduardf.audit;

import android.support.annotation.NonNull;
import android.os.AsyncTask;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 28.12.18 16:48
 *
 */

/**
 * Загрузка пунктов списка по списку guid
 * Входные параметры: список guid
 * Результат возвращается через образный выхов OnLoadItems
 */
class LoadItems extends AsyncTask<String, Items.Item, Items> {

    final private OnLoadItems onExecute;
    final private AuditOData.Set set;
    final private AuditOData oData;

    /**
     * Конструктор
     * @param onExecute - контекст с обратным вызовом OnLoadItems
     * @param oData - доступ в 1С
     * @param set - таблица справочника
     */
    LoadItems(@NonNull OnLoadItems onExecute, AuditOData oData, AuditOData.Set set) {
        this.onExecute = onExecute;
        this.oData = oData;
        this.set = set;
    }
    @Override
    protected void onPreExecute() {
        onExecute.onPreLoadItems();
    }
    @Override
    protected final Items doInBackground(String[] lists) {
        final Items items = new Items();
        if ((lists.length > 0) && (lists[0] != null)) {
            for(String key: lists) {
                final Items.Item item = new Items.Item();
                item.id = key;
                item.name = oData.getName(set, key);
                publishProgress(item);
                if (isCancelled()) break;
            }
        }
        return items;
    }
    @Override
    protected void onProgressUpdate(Items.Item... items) {
        onExecute.onLoadedItem(items[0]);
    }
    @Override
    protected void onPostExecute(Items items) {
        onExecute.onPostLoadItems(items);
    }

    /**
     * Обратный вызов для возврата наименования
     */
    interface OnLoadItems{
        void onPreLoadItems(); //перед загрузкой
        void onLoadedItem(Items.Item item); //после загрузки каждого пункта
        void onPostLoadItems(Items items); //после загрузки всех пунктов
    }
}
//Фома2018