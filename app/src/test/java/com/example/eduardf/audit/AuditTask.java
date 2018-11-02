package com.example.eduardf.audit;

import android.support.v4.util.ArraySet;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

/** УДАЛИТЬ -  УСТАРЕЛО!!!
 * Created by eduardf on 13.04.2018.
 */

public class AuditTask {
    public int id; //Идентификатор задания
    public Date date; //Дата и время задания
    public int auditor; //Аудитор - пользователь
    public int object; //Объект аудита
    public int auditType; //Вид аудита
    public int status; //Статус задания
    public Set<Integer> analytics; //Аналитика объекта

    public final static int NEW_TASK_ID = -1; //Признак нового задания

    public AuditTask(int id, Date date, int auditor, int auditType, int object, int status, Set<Integer> analytics) {
        this.id = id;
        this.date = date;
        this.auditor = auditor;
        this.auditType = auditType;
        this.object = object;
        this.status = status;
        this.analytics = new ArraySet<>(analytics);
    }

    // добавить аналитику
    public boolean addAnalytic(int id) {
        return analytics.add(id);
    }

    // добавить коллекцию аналитики
    public boolean addAnalytics(Collection<Integer> ids) {
        return analytics.addAll(ids);
    }

    // удалить аналитику
    public boolean removeAnalytic(int id) {
        return analytics.remove(id);
    }

    // удалить всю аналитику
    public void clearAnalytic() {
        analytics.clear();
    }

    // возвращает массив с аналитикой
    public int[] getAnalytics() {
        int[] analytics = new int[this.analytics.size()];
        int i = 0;
        for (int id:this.analytics) analytics[i++]=id;
        return analytics;
    }

    // устанавливает новый список аналитики по коллекции
    public boolean setAnalytics(Collection<Integer> ids) {
        clearAnalytic();
        return addAnalytics(ids);
    }

    // устанавливает новый список аналитики по массиву
    public void setAnalytics(int[] ids) {
        clearAnalytic();
        if (ids!=null) for (int i:ids) addAnalytic(i);
    }
}
