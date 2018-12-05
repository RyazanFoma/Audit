package com.example.eduardf.audit;

import android.nfc.FormatException;

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
class Indicators extends ArrayList<Indicators.Indicator> {

    class Indicator {
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

        Types(String name) {
            this.id = name;
        }

        @Override
        public String toString() {
            return this.id;
        }

        /**
         * Определение значения перечесления по идентификатору
         * @param key - идентификатор
         * @return - значение перечисления
         */
        static public Types toValue(String key) {
            switch (key) {
                case "Булево": return IS_BOOLEAN;
                case "Число": return IS_NUMERIC;
                case "Дата": return IS_DATE;
                default: return null;
            }
        }

    }
}
//Фома2018