package com.example.eduardf.audit;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.eduardf.audit.AuditOData.DATE_FORMAT_1C;

/**
 * Список показателей аудита
 */
public class Indicators extends ArrayList<Indicators.Indicator> {

    public class Indicator {
        String id; //Giud показателя
        String code; //Код в 1С
        String name; //Наименование
        String pater; //Giud родителя
        String owner; //Giud владелеца - вид аудита
        boolean folder; //Папка
        String desc; //Описание
        Types type; //Тип показателя
        String subject; //предмет аудита
        Criterions criterion; //Критерий достижения цели
        String unit; //Единица измерения
        Object goal; //Целевое значение
        Object minimum; //Минимальное значение
        Object maximum; //Максимальное значение
        float error; //Погрешность
        boolean deleted; //Пометка на удаление
        boolean predefined; //Предопределенный
        String prenamed; //Предопределенное имя
    }

    //Критерии достижения цели по показателю
    enum Criterions {
        NOT_ACCOUNT("НеУчаствует", "Не участвует"),
        EQUALLY("Равно","Равно"),
        NOT_EQUAL("НеРавно", "Не равно"),
        MORE("Больше", "Больше"),
        MORE_OR_EQUAL("БольшеИлиРавно", "Больше или равно"),
        LESS("Меньше", "Меньше"),
        LESS_OR_EQUEL("МеньшеИлиРавно", "Меньше или равно"),
        IN_RANGE("ВДиапозоне", "В диапазоне"),
        IN_ERROR("ВПределахПогрешности", "В пределах погрешности");

        String id; //Наименование
        private String desc; //Описание

        private Criterions(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        @Override
        public String toString() {
            return this.desc;
        }

        /**
         * Определение значения перечесления по идентификатору
         * @param id - идентификатор
         * @return - значение перечисления
         */
        static public Criterions toValue(String id) {
            switch (id) {
                case "НеУчаствует":
                    return NOT_ACCOUNT;
                case "Равно":
                    return EQUALLY;
                case "НеРавно":
                    return NOT_EQUAL;
                case "Больше":
                    return MORE;
                case "БольшеИлиРавно":
                    return MORE_OR_EQUAL;
                case "Меньше":
                    return LESS;
                case "МеньшеИлиРавно":
                    return LESS_OR_EQUEL;
                case "ВДиапозоне":
                    return IN_RANGE;
                case "ВПределахПогрешности":
                    return IN_ERROR;
            }
            return null;
        }
    }

    //Тип значения показателя
    enum Types {
        IS_BOOLEAN("Булево"),
        IS_NUMERIC("Число"),
        IS_DATE("Дата");

        String id;

        private Types(String name) {
            this.id = name;
        }

        @Override
        public String toString() {
            return this.id;
        }

        public String toEdmType() {
            //Edm.Boolean Edm.Double Edm.Date
            switch (id) {
                case "Булево":
                    return AuditOData.BOOLEAN_TYPE;
                case "Число":
                    return AuditOData.DOUBLE_TYPE;
                case "Дата":
                    return AuditOData.DATE_TYPE;
            }
            return AuditOData.UNDEFINED_TYPE;
        }

        /**
         * Определение значения перечесления по идентификатору
         * @param key - идентификатор
         * @return - значение перечисления
         */
        static public Types toValue(String key) {
            switch (key) {
                case "Булево":
                    return IS_BOOLEAN;
                case "Число":
                    return IS_NUMERIC;
                case "Дата":
                    return IS_DATE;
            }
            return null;
        }

        /**
         * Преобразует значение объекта в строку в соответствии с типом
         * @param value - объект
         * @return - строку со значением, соответвтующим типу
         */
        public String valueToString(Object value) {
            switch (this) {
                case IS_DATE:
                    if (value != null)
                        return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).format(value);
                    else {
                        Date empty = new Date();
                        empty.setTime(0);
                        return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).format(empty);
                    }
                case IS_BOOLEAN:
                    return ((Boolean) value)? "true": "false";
                case IS_NUMERIC: {
                    final float a = (Float) value;
                    if (a == (long) a) return String.format(Locale.US, "%d", (long) a);
                    else return String.format(Locale.US, "%s", a);
                }
                default:
                    throw new RuntimeException("Indicators.valueToString('"+value.toString()+"') Error on binding of value from type '"+this.id+"'.");
            }
        }

        /**
         * Разбор строки со значением объекта в соответствии с типом
         * @param string - строка
         * @return - значение объекта
         */
        public Object stringToValue(String string) {
            Object result = null;
            switch (this) {
                case IS_DATE:
                    try {
                        result = (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).parse(string);
                    } catch (ParseException e) {
                        final Date empty = new Date();
                        empty.setTime(0);
                        result = (Date) empty;
                    }
                    break;
                case IS_BOOLEAN:
                    result = (Boolean) string.equals("true");
                    break;
                case IS_NUMERIC:
                    try {
                        result = Float.parseFloat(string);
                    }
                    catch (NumberFormatException e) {
                        result = Float.valueOf(0);
                    }
            }
            return result;
        }
    }
}
//Фома2018