package com.example.eduardf.audit;

import android.support.v4.util.ArraySet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 31.10.18 10:21
 *
 */

//Вид аудита:
//УСТАРЕЛО!!! нужно удалить
public class AuditType {

    public int id; //идентификатор
    public String name; //наименование
    public String desc; //описание
    public int pater; //родитель
    public boolean is_group; //признак группы
    public Map<Integer,Set<Integer>> objects; //группы объектов c группами аналитики

    public final static int NEW_TYPE_ID = -1;

    // конструктор
    public AuditType(int id, String name, int pater, boolean is_group, String desc) {
        this.id = id;
        this.name = name;
        this.pater = pater;
        this.is_group =  is_group;
        this.desc = desc;
        objects = new HashMap<>();
    }

    // добавляет группу объекта с группами аналитик
    public void addObject(int object, Collection<Integer> analytics) { objects.put(object, new ArraySet(analytics)); }

    // удалает все группы объектов
    public void clearObjects() { objects.clear(); }

    // удаляет группу объектов
    public void removeObject(int id) { objects.remove(id); }

    // добавляет группу аналитик в группу объектов
    public boolean addAnalytic(int object, int analytic) {
        if (!objects.containsKey(object))
            objects.put(object, new ArraySet()); //Если объекта еще нет
        return objects.get(object).add(analytic);
    }

    // добавляет группы аналитик к группу объектов
    public boolean addAnalytics(int object, Collection<Integer> analytics) { return objects.get(object).addAll(analytics); }

    // удаляет группу аналитик из группы объектов
    public boolean removeAnalytic(int object, int analytic) { return objects.get(object).remove(analytic);}

    // удаляет все группы аналитик из группы объектов
    public void clearAnalytic(int object) { objects.get(object).clear();}

//    public int getObject(int position) {
//        int i = 0;
//        for (int o: objects.keySet()) if (i==position) return o; else i++;
//        return AuditDB.NOT_SELECTED;
//    }
}
