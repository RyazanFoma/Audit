package com.example.eduardf.audit;

import android.support.v4.util.ArraySet;

import java.util.Collection;
import java.util.Set;

//Объект аудита:
public class AuditObject {

    public int id; //идентификатор
    public String name; //наименование
    public String desc; //описание
    public int pater; //родитель
    public boolean is_group; //признак группы
    public Set<Integer> analytics; //связанные с объектом аналитики

    public final static int NEW_TYPE_ID = -1;

    // конструктор
    public AuditObject(int id, String name, int pater, boolean is_group, String desc) {
        this.id = id;
        this.name = name;
        this.pater = pater;
        this.is_group =  is_group;
        this.desc = desc;
        analytics = new ArraySet<>();
    }

    // добавляет аналитику
    public void addAnalytic(int id) { analytics.add(id); }

    // удалает все аналитики
    public void clearAnalytics() { analytics.clear(); }

    // удаляет аналитику
    public void removeAnalytic(int id) { analytics.remove(id); }
}
