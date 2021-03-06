package com.bit.eduardf.audit;

import android.support.v4.util.ArraySet;

import java.util.Set;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 24.07.18 10:50
 *
 */

//Объект аудита:
public class AuditAnalytic {

    public int id; //идентификатор
    public String name; //наименование
    public String desc; //описание
    public int pater; //родитель
    public boolean is_group; //признак группы
    public Set<Integer> objects; //связанные с аналитикой объекты

    public final static int NEW_TYPE_ID = -1;

    // конструктор
    public AuditAnalytic(int id, String name, int pater, boolean is_group, String desc) {
        this.id = id;
        this.name = name;
        this.pater = pater;
        this.is_group =  is_group;
        this.desc = desc;
        objects = new ArraySet<>();
    }

    // добавляет объект
    public void addObject(int id) { objects.add(id); }

    // удалает все объекты
    public void clearObjects() { objects.clear(); }

    // удаляет объект
    public void removeObject(int id) { objects.remove(id); }
}
