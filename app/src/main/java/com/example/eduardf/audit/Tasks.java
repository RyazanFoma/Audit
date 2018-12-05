package com.example.eduardf.audit;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.text.DateFormat.getDateTimeInstance;

//Класс - список заданий на аудит, предназначен для рециклервью
public class Tasks extends ArrayList<Tasks.Task> {

    //Класс - задание на аудит:
    public static class Task {
        String id; //Идентификатор
        Date date; //Дата
        String number; //Номер
        Status status; //Статус
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

        //Конструктор
        public Task() {
            analytics = new ArrayList<String>();
            indicators = new ArrayList<IndicatorRow>();
        }

        //Идентификаторы статуса задания из 1С
        private static final String ENUM_STATUS_0 = "Утвержден";
        private static final String ENUM_STATUS_1 = "ВРаботе";
        private static final String ENUM_STATUS_2 = "Проведен";

        /**
         * Статус задания
         */
        enum Status {
            APPROVED(0, ENUM_STATUS_0, "Утвержден"),
            IN_WORK(1, ENUM_STATUS_1, "В работе"),
            POSTED(2, ENUM_STATUS_2, "Проведен");

            int number; //Номер закладки
            String id; //Идентификатор
            private String desc; //Описание

            private Status(int number, String id, String desc) {
                this.number = number;
                this.id = id;
                this.desc = desc;
            }

            /**
             * Представление статуса в виде строки
             * @return
             */
            @Override
            public String toString() {
                return desc;
            }

            static Status toValue(String id) {
                switch (id) {
                    case ENUM_STATUS_0: return APPROVED;
                    case ENUM_STATUS_1: return IN_WORK;
                    case ENUM_STATUS_2: return POSTED;
                    default:
                        throw new RuntimeException("Status.toValue('"+id+"') Not exist the status for this id.");
                }
            }

            static Status toValue(Integer number) {
                switch (number) {
                    case 0: return APPROVED;
                    case 1: return IN_WORK;
                    case 2: return POSTED;
                    default:
                        throw new RuntimeException("Status.toValue('"+number+"') Not exist the status for this number.");
                }
            }
        }
        /**
         * Класс - строка таблицы показателей
         */
        public class IndicatorRow {
            String indicator; //guid показателя
            Object goal; //Целевое значение
            Object minimum; //Минимальное значение
            Object maximum; //Максимальное значение
            float error; //Погрешность
            Object value; //Фактическое значение
            String comment; //Комментарий
            boolean achived; //Цель показателя достигнута
        }

        private final static String ARG_DATE = "date"; //Дата задания на аудит
        private final static String ARG_ID = "id"; //Идентификатор задания на аудит
        private final static String ARG_STATUS = "status"; //Статус задания на аудит
        private final static String ARG_AUDITOR = "auditor"; //Аудитор задания на аудит
        private final static String ARG_NUMBER = "number"; //Номер задания
        private final static String ARG_TYPE = "type"; //Вид аудита
        private final static String ARG_ORGANIZATION = "organization"; //Организация
        private final static String ARG_OBJECT = "object"; //Объект аудита
        private final static String ARG_RESPONSIBLE = "responsible"; //Ответственный за объект
        private final static String ARG_ANALYTICS = "analytics"; //Аналитика объекта аудита
        private final static String ARG_INDICATORS = "indicators"; //Показатели аудита
        private final static String ARG_DELETED = "deleted"; //Пометка на удаление
        private final static String ARG_POSTED = "posted"; //Документ проведен
        private final static String ARG_ACHIVED = "achieved"; //Цель достигнута

        /**
         * Сохраняет задание
         * @param outState - среда для хранения
         * @param argName - имя Bundle с содержимым задания
         */
        public void onSaveInstanceState(@NonNull Bundle outState, String argName) {
            final Bundle bundle = new Bundle();

            bundle.putString(ARG_DATE, getDateTimeInstance().format(this.date));
            bundle.putString(ARG_ID, this.id);
            bundle.putString(ARG_STATUS, this.status.id);
            bundle.putString(ARG_AUDITOR, this.auditor_key);
            bundle.putString(ARG_NUMBER, this.number);
            bundle.putString(ARG_TYPE, this.type_key);
            bundle.putString(ARG_ORGANIZATION, this.organization_key);
            bundle.putString(ARG_OBJECT, this.object_key);
            bundle.putString(ARG_RESPONSIBLE, this.responsible_key);
            bundle.putStringArrayList(ARG_ANALYTICS, this.analytics);
            final Parcelable[] parcelables = new Parcelable[this.indicators.size()];
            int i = 0;
            for (Task.IndicatorRow row: this.indicators) parcelables[i++] = new ParcelableRow(row);
            bundle.putParcelableArray(ARG_INDICATORS, parcelables);
            bundle.putBoolean(ARG_DELETED, this.deleted);
            bundle.putBoolean(ARG_POSTED, this.posted);
            bundle.putBoolean(ARG_ACHIVED, this.achieved);

            outState.putBundle(argName, bundle);
        }

        /**
         * Восстанавливает задание
         * @param savedInstanceState - среда для хранения
         * @param argName - имя Bundle
         */
        public void onRestoreInstanceState(Bundle savedInstanceState, String argName) {
            if (savedInstanceState.containsKey(argName)) {
                final Bundle bundle = savedInstanceState.getBundle(argName);
                try {
                    this.date = getDateTimeInstance().parse(bundle.getString(ARG_DATE));
                }
                catch (NullPointerException e) {
                    e.printStackTrace(); //Здесь нужно сообщить о неправильной дате
                    throw new RuntimeException("Error on parsing of date 'null'.");
                }
                catch (ParseException e) {
                    e.printStackTrace(); //Здесь нужно сообщить о неправильной дате
                    throw new RuntimeException("Error on parsing of date '"+bundle.getString(ARG_DATE)+"'.");
                }
                this.id = bundle.getString(ARG_ID);
                this.status = Status.toValue(bundle.getString(ARG_STATUS));
                this.auditor_key = bundle.getString(ARG_AUDITOR);
                this.number = bundle.getString(ARG_NUMBER);
                this.type_key = bundle.getString(ARG_TYPE);
                this.organization_key = bundle.getString(ARG_ORGANIZATION);
                this.object_key = bundle.getString(ARG_OBJECT);
                this.responsible_key = bundle.getString(ARG_RESPONSIBLE);
                if (!this.analytics.isEmpty()) this.analytics.clear();
                this.analytics.addAll(bundle.getStringArrayList(ARG_ANALYTICS));
                if (!this.indicators.isEmpty()) this.indicators.clear();
                final Parcelable[] parcelables = bundle.getParcelableArray(ARG_INDICATORS);
                if (parcelables != null)
                    for (Parcelable row : parcelables)
                        this.indicators.add(((ParcelableRow) row).row);
                this.deleted = bundle.getBoolean(ARG_DELETED, false);
                this.posted = bundle.getBoolean(ARG_POSTED, false);
                this.achieved = bundle.getBoolean(ARG_ACHIVED, false);
            }
        }
    }

    /**
     * Сохраняет содержимое списка
     * @param outState - среда для хранения ParcelableArray с содержимым списка
     * @param argName - имя ParcelableArray
     */
    public void onSaveInstanceState(@NonNull Bundle outState, String argName) {
        final Parcelable[] parcelables = new Parcelable[this.size()];
        int i = 0;
        for (Task task: this) parcelables[i++] = new ParcelableTask(task);
        outState.putParcelableArray(argName, parcelables);
    }

    /**
     * Восстанавливает содержимое списка. Список предварительно очищается
     * @param savedInstanceState - содержит ParcelableArray с содержимым списка
     * @param argName - имя ParcelableArray
     */
    public void onRestoreInstanceState(Bundle savedInstanceState, String argName) {
        if (this != null && savedInstanceState.containsKey(argName)) {
            if (!isEmpty()) clear();
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(argName);
            if (parcelables != null)
                for (Parcelable row : parcelables) this.add(((ParcelableTask) row).task);
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