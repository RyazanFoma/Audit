package com.example.eduardf.audit;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import java.util.Date;
import java.util.HashMap;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 30.01.19 16:25
 *
 */

/**
 * Список показателей
 * Используется для рециклервью
 */
class IndList extends HashMap<String, IndList.Ind> {

    /**
     * Показатель аудита
     */
    static class Ind {
        String id; //Giud показателя
        String name; //Наименование
        String pater; //Giud родителя
        boolean folder; //Папка
        String desc; //Описание
        Indicators.Types type; //Тип показателя
        boolean not_involved; //Не участвует
        Indicators.Criteria criterion; //Критерий достижения цели
        String subject; //Предмет аудита
        String unit; //Единица измерения
        Object goal; //Целевое значение
        Object minimum; //Минимальное значение
        Object maximum; //Максимальное значение
        float error; //Погрешность
        Object value; //Фактическое значение
        String comment; //Комментаний
        boolean achieved; //Цель достигнута
        boolean expand; //Пункт развернут

        /**
         * Проверяет достижение цели по показателю и изменяет реквизит achieved
         */
        void notifyAchived() {
            try {
                if (not_involved) { achieved = false; return; }
                switch (type) {
                    case IS_BOOLEAN: {
                        final boolean a = (Boolean) value;
                        final boolean b = (Boolean) goal;
                        switch (criterion) {
                            case EQUALLY:
                                achieved = (a == b);
                                break;
                            case NOT_EQUAL:
                                achieved = (a != b);
                                break;
                            default:
                                achieved = false;
                                break;
                        }
                        break;
                    }
                    case IS_NUMERIC: {
                        final float a = (Float) value;
                        final float b = (Float) goal;
                        switch (criterion) {
                            case EQUALLY:
                                achieved = (a == b);
                                break;
                            case NOT_EQUAL:
                                achieved = (a != b);
                                break;
                            case MORE:
                                achieved = (a > b);
                                break;
                            case MORE_OR_EQUAL:
                                achieved = (a >= b);
                                break;
                            case LESS:
                                achieved = (a < b);
                                break;
                            case LESS_OR_EQUEL:
                                achieved = (a <= b);
                                break;
                            case IN_RANGE: {
                                final float min = (Float) minimum;
                                final float max = (Float) maximum;
                                achieved = (a >= min && a <= max);
                                break;
                            }
                            case IN_ERROR:
                                achieved = (Math.abs(a - b) / b * 100 <= error);
                                break;
                            default:
                                achieved = false;
                                break;
                        }
                        break;
                    }
                    case IS_DATE: {
                        final long a = ((Date) value).getTime();
                        final long b = ((Date) goal).getTime();
                        switch (criterion) {
                            case EQUALLY:
                                achieved = (a == b);
                                break;
                            case NOT_EQUAL:
                                achieved = (a != b);
                                break;
                            case MORE:
                                achieved = (a > b);
                                break;
                            case MORE_OR_EQUAL:
                                achieved = (a >= b);
                                break;
                            case LESS:
                                achieved = (a < b);
                                break;
                            case LESS_OR_EQUEL:
                                achieved = (a <= b);
                                break;
                            case IN_RANGE: {
                                final long min = ((Date) minimum).getTime();
                                final long max = ((Date) maximum).getTime();
                                achieved = (a >= min && a <= max);
                                break;
                            }
                            default:
                                achieved = false;
                                break;
                        }
                        break;
                    }
                    default:
                        achieved = false;
                        break;
                }
            } catch (ClassCastException e) {
                achieved = false;
            }
        }

        /**
         * Формирование текстового описания условия достижения цели
         * @return - описание условия
         */
        String russianCriterion() {
            String russian = "Ошибка: Неизвестный критерий или тип!!!";
            switch (type) {
                case IS_BOOLEAN:
                    switch (criterion) {
                        case EQUALLY:
                            russian = String.format("Утверждение должно иметь %s ответ.", (Boolean) goal? "положительный": "отрицательный");
                            break;
                        case NOT_EQUAL:
                            russian = String.format("Утверждение должно иметь %s ответ.", (Boolean) goal? "отрицательный": "положительный");
                            break;
                        case NOT_INVOLVED:
                            russian = "Показатель не участвует в определении результатов";
                            break;
                    }
                    break;
                case IS_NUMERIC:
                    final String myUnit;
                    if (unit.isEmpty()) myUnit = "";
                    else myUnit = ", "+unit;
                    switch (criterion) {
                        case EQUALLY:
                        case MORE:
                        case MORE_OR_EQUAL:
                        case LESS:
                        case LESS_OR_EQUEL:
                            russian = String.format("Значение должно быть %s %s%s", criterion.toString(), goal, myUnit);
                            break;
                        case NOT_EQUAL:
                            russian = String.format("Значение не должно быть равно %s%s", goal, myUnit);
                            break;
                        case IN_RANGE:
                            russian = String.format("Значение должно быть равно от %s до %s%s", minimum, maximum, myUnit);
                            break;
                        case IN_ERROR:
                            russian = String.format("Значение должно быть равно %s%s, с погрешностью %s%%", goal, myUnit, error);
                            break;
                        case NOT_INVOLVED:
                            russian = "Показатель не участвует в определении результатов";
                            break;
                    }
                    break;
                case IS_DATE:
                    switch (criterion) {
                        case EQUALLY:
                            russian = String.format("Дата должна быть равна %s", goal);
                            break;
                        case NOT_EQUAL:
                            russian = String.format("Дата не должна быть равна %s", goal);
                            break;
                        case MORE: case LESS:
                            russian = String.format("Дата должна быть %s %s", criterion.toString(), goal);
                            break;
                        case MORE_OR_EQUAL:
                            russian = String.format("Дата должна быть больше или равна %s", goal);
                            break;
                        case LESS_OR_EQUEL:
                            russian = String.format("Дата должна быть меньше или равна %s", goal);
                            break;
                        case IN_RANGE:
                            russian = String.format("Дата должна быть от %s до %s", minimum, maximum);
                            break;
                        case NOT_INVOLVED:
                            russian = "Показатель не участвует в определении результатов";
                            break;
                    }
                    break;
            }
            return russian;
        }
    }

    /**
     * Добавление показателя в дерево показателей
     * @param ind - показетель
     */
    void add(Ind ind) {
        this.put(ind.id, ind);
    }

    /**
     * Сохранить содержимое списка
     * @param outState - среда для хранения ParcelableArray с содержимым списка
     * @param argName - имя ParcelableArray
     */
    void onSaveInstanceState(@NonNull Bundle outState, String argName) {
        final Parcelable[] parcelables = new Parcelable[this.size()];
        int i = 0;
        for (Entry<String, Ind> entry: this.entrySet()) parcelables[i++] = new ParcelableInd(entry.getValue());
        outState.putParcelableArray(argName, parcelables);
    }

    /**
     * Восстановить содержимое списка. Список предварительно очищается
     * @param savedInstanceState - содержит ParcelableArray с содержимым списка
     * @param argName - имя ParcelableArray
     */
    void onRestoreInstanceState(Bundle savedInstanceState, String argName) {
        if (savedInstanceState.containsKey(argName)) {
            if (!isEmpty()) clear();
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(argName);
            if (parcelables != null)
                for (Parcelable row : parcelables) add(((ParcelableInd) row).ind);
        }
    }
}
//Фома2018