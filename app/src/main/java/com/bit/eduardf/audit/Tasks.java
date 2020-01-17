package com.bit.eduardf.audit;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import static java.text.DateFormat.getDateTimeInstance;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 30.01.19 10:35
 *
 */

//Класс - список заданий на аудит, предназначен для рециклервью
class Tasks extends ArrayList<Tasks.Task> {

    //Класс - задание на аудит:
    static class Task {
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
        boolean group; //Карточка в списке занимает все колонки
        ArrayList<String> analytics; //Аналитика списком
        ArrayList<IndicatorRow> indicators; //Покаказели задания
        MediaFiles mediaFiles; //Медиафайлы

        //Конструктор
        Task() {
            analytics = new ArrayList<>();
            indicators = new ArrayList<>();
            mediaFiles = new MediaFiles();
        }

        //Конструктор разделителя заданий на группы
        Task(Date date, Status status) {
            analytics = new ArrayList<>();
            indicators = new ArrayList<>();
            mediaFiles = new MediaFiles();
            group = true;
// this is filling in for success a сom.bit.eduardf.audit.ParcelableTask.writeToParcel
            this.date = date;
            this.status = status;
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

            final int number; //Номер закладки
            final String id; //Идентификатор
            private String desc; //Наименование

            Status(int number, String id, String desc) {
                this.number = number;
                this.id = id;
                this.desc = desc;
            }

            /**
             * Представление статуса в виде строки
             * @return наименование статуса
             */
            @Override
            public String toString() {
                return desc;
            }

            static Status toValue(@NonNull String id) {
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
        class IndicatorRow {
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
        private final static String ARG_MEDIAFILES = "mediafiles"; //Медиафайлы

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
            final ArrayList<ParcelableMedia> parcelableMedia = new ArrayList<>();
            for (MediaFiles.MediaFile mediaFile: this.mediaFiles)
                parcelableMedia.add(new ParcelableMedia(mediaFile));
            bundle.putParcelableArrayList(ARG_MEDIAFILES, parcelableMedia);

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
                if (bundle == null) throw new RuntimeException("Error on parsing of task.");
                try {
                    this.date = getDateTimeInstance().parse(bundle.getString(ARG_DATE));
                }
                catch (ParseException e) {
                    e.printStackTrace(); //Здесь нужно сообщить о неправильной дате
                    throw new RuntimeException("Error on parsing of date '"+bundle.getString(ARG_DATE)+"'.");
                }
                this.id = bundle.getString(ARG_ID);
                final String status_id = bundle.getString(ARG_STATUS);
                if (status_id != null)
                    this.status = Status.toValue(status_id);
                else
                    throw new RuntimeException("Can't restore task status");
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
                final ArrayList<ParcelableMedia> parcelableMedia = bundle.getParcelableArrayList(ARG_MEDIAFILES);
                if (parcelableMedia != null)
                    for (ParcelableMedia media: parcelableMedia)
                        this.mediaFiles.add(media.mediaFile);
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
        if (savedInstanceState.containsKey(argName)) {
            if (!isEmpty()) clear();
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(argName);
            if (parcelables != null)
                for (Parcelable row : parcelables) this.add(((ParcelableTask) row).task);
        }
    }

    //возвращает количество отмеченных заданий
    int checkedCount() {
        int checked = 0;
        for(Task task: this) if (task.checked) checked++;
        return checked;
    }

    //Возвращает список идентификаторов отмеченных заданий
    public ArrayList<String> getChecked() {
        ArrayList<String> checked = new ArrayList<>();
        for(Task task: this) if (task.checked) checked.add(task.id);
        return checked;
    }

    public int getCount() {
        int i = 0;
        for (Task task: this) if (!task.group) ++i;
        return i;
    }

    //Отмечает задания по списку
    public void setChecked(ArrayList<String> checked) {
        if (!(checked == null || checked.isEmpty()))
            for(Task task: this) task.checked = checked.contains(task.id);
    }

    //Возвращает список идентификаторов развернутых заданий
    public ArrayList<String> getExpand() {
        ArrayList<String> expand = new ArrayList<>();
        for(Task task: this) if (task.expand) expand.add(task.id);
        return expand;
    }

    //Разворачивает задания по списку
    public void setExpand(ArrayList<String> expand) {
        if (!(expand == null || expand.isEmpty()))
            for(Task task: this) task.expand = expand.contains(task.id);
    }

    //Помечает/отменяет все задания
    void setCheckedAll(boolean checked) {
        for(Task task: this) {
            if (!task.group) {
                task.checked=checked;
            }
        }
    }
}
//Фома2018