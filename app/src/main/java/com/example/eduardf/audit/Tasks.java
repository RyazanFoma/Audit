package com.example.eduardf.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//Класс - список заданий на аудит, предназначен для рециклервью
public class Tasks extends ArrayList<Tasks.Task> {

    //Класс - задание на аудит:
    public static class Task {
        String id; //Идентификатор
        Date date; //Дата
        String number; //Номер
        int status; //Статус: 0 - утвержден, 1 - в работе, 2 - проведен
        String auditor_key; //Идентификатор аудитора
        String type_key; //Идентификатор вида аудита
        String type_name; //Наименование вида аудита
        String organization_key; //Идентификатор организации
        String object_key; //Идентификатор объекта аудита
        String object_name; //Наименование объекта аудита
        String responsible_key; //Идентификатор ответственного за объект
        String comment; //Комментарий
        String analytic_names; //Наименования аналитик строкой
        boolean achieved; //Цель аудита достигнута
        boolean deleted; //Пометка на удаление
        boolean posted; //Проведен
        boolean checked; //Отмечен в списке
        boolean expand; //Карточка в списке развернута
        ArrayList<String> analytics; //Аналитика списком
        ArrayList<IndicatorRow> indicators; //Покаказели задания

        public Task() {
            analytics = new ArrayList<String>();
            indicators = new ArrayList<IndicatorRow>();
        }

        /**
         * Класс - строка таблицы показателей
         */
        public class IndicatorRow {
            String indicator; //guid показателя
            Indicators.Types type; //Тип индикатора
            Object goal; //Целевое значение
            Object minimum; //Минимальное значение
            Object maximum; //Максимальное значение
            float error; //Погрешность
            Object value; //Фактическое значение
            String comment; //Комментарий
            boolean achived; //Цель показателя достигнута
        }
    }

    //возвращает количество отмеченных заданий
    public int checkedCount() {
        int checked = 0;
        for(Task task: this) if (task.checked) checked++;
        return checked;
    }

    //Возвращает список идентификаторов отмеченных заданий
    public ArrayList<String> getChecked() {
        ArrayList<String> checked = new ArrayList<String>();
        for(Task task: this) if (task.checked) checked.add(task.id);
        return checked;
    }

    //Отмечает задания по списку
    public void setChecked(ArrayList<String> checked) {
        if (!(this == null || checked == null || checked.isEmpty()))
            for(Task task: this) task.checked = checked.contains(task.id);
    }

    //Возвращает список идентификаторов развернутых заданий
    public ArrayList<String> getExpand() {
        ArrayList<String> expand = new ArrayList<String>();
        for(Task task: this) if (task.expand) expand.add(task.id);
        return expand;
    }

    //Разворачивает задания по списку
    public void setExpand(ArrayList<String> expand) {
        if (!(this == null || expand == null || expand.isEmpty()))
            for(Task task: this) task.expand = expand.contains(task.id);
    }

    //Помечает/отменяет все задания
    public void setCheckedAll(boolean checked) {
        for(Task task: this) task.checked=checked;
    }
}
//Фома2018