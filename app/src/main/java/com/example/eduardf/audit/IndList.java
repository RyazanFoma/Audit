package com.example.eduardf.audit;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;

/**
 * Список показателей
 * Используется для рециклервью
 */
public class IndList extends ArrayList<IndList.Ind> {

    /**
     * Показатель аудита
     */
    public class Ind {
        String id; //Giud показателя
        String name; //Наименование
        String pater; //Giud родителя
        boolean folder; //Папка
        String desc; //Описание
        Indicators.Types type; //Тип показателя
        Indicators.Criterions criterion; //Критерий достижения цели
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
        public void notifyAchived() {
            try {
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
    }

    /**
     * Сохраняет содержимое списка
     * @param outState - среда для хранения ParcelableArray с содержимым списка
     * @param argName - имя ParcelableArray
     */
    public void onSaveInstanceState(@NonNull Bundle outState, String argName) {
        final Parcelable[] parcelables = new Parcelable[this.size()];
        int i = 0;
        for (Ind ind: this) parcelables[i++] = new ParcelableInd(ind);
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
                for (Parcelable row : parcelables) this.add(((ParcelableInd) row).ind);
        }
    }
}
//Фома2018