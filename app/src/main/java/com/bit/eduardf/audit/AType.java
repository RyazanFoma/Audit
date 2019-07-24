package com.bit.eduardf.audit;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 05.02.19 9:42
 *
 */

/**
 * Вид аудита
 */
class AType {
    String id; //Идентификатор
    String name; //Наименование
//    String code; //Код в 1С
//    String pater; //Родитель
//    boolean folder; //Папка
//    boolean deleted; //Пометка на удаление
//    boolean predefined; //Предопределенный
//    String prenamed; //Предопределенное имя
//    Criteria criterion; //Критерий достижения цели
//    float value; //Целевое значение
    boolean fillActualValue; //Заполнять фактические значения по умолчанию
    boolean openWithIndicators; //Открывать задания с показателей
    boolean clearCopy; //Очищать задание при копировании
    boolean showSubject; //Показывать предметы показателей
    Selections selection; //Вид отбора аналитик по объекту
    ArrayList<String> objectTypes; //Типы объектов аудита

    //Виды отбора аналитик по объекту
    enum Selections {
        NOT_ANALYTICS("АналитикиНеИспользуется", "Аналитики не используются"),
        NOT_SELECTION("ОтборНеПрименяется", "Отбор не применяется"),
        BY_TYPES("ОтборПоТипам", "Отбор по типам"),
//        BY_FEILDS("ОтборПоПолям", "Отбор по полям"),
//        ON_QUERY("ОтборЗапросом", "Отбор запросом"),
        HAND_LINK("РучнаяСвязь", "Ручная связь");

        final String id; //Наименование
        private String desc; //Описание

        Selections(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        static Selections toValue(String id) {
            switch (id) {
                case "ОтборНеПрименяется":
                    return  NOT_SELECTION;
                case "ОтборПоТипам":
                    return BY_TYPES;
                case "РучнаяСвязь":
                    return HAND_LINK;
                case "АналитикиНеИспользуется": default:
                    return NOT_ANALYTICS;
            }
        }

        @Override
        public String toString() {
            return this.desc;
        }
    }

    //Критерии достижения цели по виду адутиа
    enum Criterions {
        NOT_LESS("МинимальноеКоличествоПредметовСДостигнутымиЦелями", "Достигнуто не менее"),
        NOT_MORE("МаксимальноеКоличествоПредметовСНеДостигнутымиЦелями", "Не достигнуто менее или равно"),
        PERCENT("МинимальныйПроцентПунктовГруппыСДостигнутымиЦелями", "Достигнуто не менее, %");

        String id; //Наименование
        String desc; //Описание

        Criterions(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        static Criterions toValue(String id) {
            switch (id) {
                case "МинимальноеКоличествоПредметовСДостигнутымиЦелями":
                    return NOT_LESS;
                case "МаксимальноеКоличествоПредметовСНеДостигнутымиЦелями":
                    return  NOT_MORE;
                case "МинимальныйПроцентПунктовГруппыСДостигнутымиЦелями":
                    return PERCENT;
                default:
                    throw new RuntimeException("Criteria.toValue('"+id+
                            "') Not exist the criterion for this id.");
            }
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    void onSaveInstanceState(@NonNull Bundle outState, String argName) {
        outState.putParcelable(argName, new ParcelableType(this));
    }

    void onRestoreInstanceState(Bundle savedInstanceState, String argName) {
        if (savedInstanceState.containsKey(argName)) {
            final ParcelableType parcelable = savedInstanceState.getParcelable(argName);
            if (parcelable != null) {
                final AType type = parcelable.type;
                id = type.id;
                name = type.name;
                fillActualValue = type.fillActualValue;
                openWithIndicators = type.openWithIndicators;
                clearCopy = type.clearCopy;
                showSubject = type.showSubject;
                selection = type.selection;
                objectTypes = type.objectTypes;
            }
        }
    }

}
//Фома2019