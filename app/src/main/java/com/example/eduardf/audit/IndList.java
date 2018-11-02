package com.example.eduardf.audit;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//Класс для пунктов показателей в рециклервью
public class IndList extends ArrayList<IndList.Ind> {
    public class Ind {
        String id; //Giud показателя
        String name; //Наименование
        String pater; //Giud родителя
        boolean folder; //Папка
        String desc; //Описание
        Indicators.Types type; //Тип показателя
        String subject; //Предмет аудита
        Indicators.Criterions criterion; //Критерий достижения цели
        String unit; //Единица измерения
        Object goal; //Целевое значение
        Object minimum; //Минимальное значение
        Object maximum; //Максимальное значение
        float error; //Погрешность
        Object value; //Фактическое значение
        String comment; //Комментаний
        boolean achived; //Цель достигнута
        boolean expand; //Пункт развернут

        public void notifyAchived() {
//            switch (type) {
//                case IS_BOOLEAN: {
//                    final boolean a = (Boolean) value;
//                    final boolean b = (Boolean) goal;
//                    switch (criterion) {
//                        case EQUALLY: achived =  (a == b); break;
//                        case NOT_EQUAL: achived =  (a != b); break;
//                    }
//                    break;
//                }
//                case IS_NUMERIC: {
//                    final float a = (Float)value;
//                    final float b = (Float)goal;
//                    final float min = (Float)minimum;
//                    final float max = (Float)maximum;
//                    switch (criterion) {
//                        case EQUALLY: achived =  (a == b); break;
//                        case NOT_EQUAL: achived =  (a != b); break;
//                        case MORE: achived =  (a > b); break;
//                        case MORE_OR_EQUAL: achived =  (a >= b); break;
//                        case LESS: achived =  (a < b); break;
//                        case LESS_OR_EQUEL: achived =  (a <= b); break;
//                        case IN_RANGE: achived =  (a >= min && a <= max); break;
//                        case IN_ERROR: achived =  (Math.abs(a-b)/b*100 <= error); break;
//                    }
//                    break;
//                }
//                case IS_DATE: {
//                    final long a = ((Date) value).getTime();
//                    final long b = ((Date) goal).getTime();
////                    final long min = ((Date) minimum).getTime();
////                    final long max = ((Date) maximum).getTime();
//                    switch (criterion) {
//                        case EQUALLY: achived =  (a == b); break;
//                        case NOT_EQUAL: achived =  (a != b); break;
////                        case MORE: achived =  (a > b); break;
////                        case MORE_OR_EQUAL: achived =  (a >= b); break;
////                        case LESS: achived =  (a < b); break;
////                        case LESS_OR_EQUEL: achived =  (a <= b); break;
////                        case IN_RANGE: achived =  (a >= min && a <= max); break;
//                    }
//                    break;
//                }
//            }
        }
    }
}
//Фома2018