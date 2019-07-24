package com.bit.eduardf.audit;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 22.01.19 10:36
 *
 */

//Список пунктов - элементы справочников
class Items extends ArrayList<Items.Item> {

    //Пункт списка - элемент справочника
    static class Item {
        String id; //Идентификатор
        String pater; //Родитель или 0, если корень
        boolean folder; //Признак группы
        String name; //Наименование
        boolean deleted; //Пометка на удаление
        boolean predefined; //Предопределенный элемент
        String prenamed; //Предопределенное наименование
        boolean checked; //Отмеченные пункт
        boolean expand; //Развернутый пункт
    }

    //возвращает количество отмеченных пунтов
    int checkedCount() {
        int checked = 0;
        for(Item item: this) if (item.checked) checked++;
        return checked;
    }

    //Возвращает список идентификаторов отмеченных заданий
    ArrayList<String> getChecked() {
        final ArrayList<String> checked = new ArrayList<String>();
        for(Item item: this) if (item.checked) checked.add(item.id);
        return checked;
    }

    //Отмечает пункты по списку
    void setChecked(ArrayList<String> checked) {
        if (!(checked == null || checked.isEmpty()))
            for(Item item: this) item.checked = checked.contains(item.id);
    }

    //Возвращает список идентификаторов развернутых пунтов
    ArrayList<String> getExpand() {
        final ArrayList<String> expand = new ArrayList<String>();
        for(Item item: this) if (item.expand) expand.add(item.id);
        return expand;
    }

    //Разворачивает пунты по списку
    void setExpand(ArrayList<String> expand) {
        if (!(expand == null || expand.isEmpty()))
            for(Item item: this) item.expand = expand.contains(item.id);
    }

    //Помечает/отменяет все задания
    void setCheckedAll(boolean checked, boolean only_child) {
        if (only_child) {
            for(Item item: this) if (!item.folder) item.checked=checked;
        }
        else {
            for(Item item: this) item.checked=checked;
        }
    }

    //Возвращает первый попавшийся отчеченный пункт
    Items.Item checkedItemFirst() {
        for (final Items.Item item: this) if (item.checked) return item;
        return null;
    }

    //Возвращает позицию пункта по id, если не найден 0
    int getPosition(String id) {
        int i=0;
        for(final Items.Item item: this) if (id.equals(item.id)) return i; else i++;
        return 0;
    }

    /**
     * Сохраняет содержимое списка
     * @param outState - среда для хранения ParcelableArray с содержимым списка
     * @param argName - имя ParcelableArray
     */
    void onSaveInstanceState(@NonNull Bundle outState, String argName) {
        final Parcelable[] parcelables = new Parcelable[size()];
        int i = 0;
        for (Item item: this) parcelables[i++] = new ParcelableItem(item);
        outState.putParcelableArray(argName, parcelables);
    }

    /**
     * Восстанавливает содержимое списка. Список предварительно очищается
     * @param savedInstanceState - содержит ParcelableArray с содержимым списка
     * @param argName - имя ParcelableArray
     */
    void onRestoreInstanceState(Bundle savedInstanceState, String argName) {
        if (savedInstanceState.containsKey(argName)) {
            if (!isEmpty()) clear();
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(argName);
            if (parcelables != null)
                for (Parcelable row : parcelables) add(((ParcelableItem) row).item);
        }
    }

}
//Фома2018