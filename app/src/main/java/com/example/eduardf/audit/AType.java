package com.example.eduardf.audit;

public class AType {
    String id; //Идентификатор
    String name; //Наименование
    String code; //Код в 1С
    String pater; //Родитель
    boolean folder; //Папка
    boolean deleted; //Пометка на удаление
    boolean predefined; //Предопределенный
    String prenamed; //Предопределенное имя
    Criterions criterion; //Критерий достижения цели
    float value; //Целевое значение
    boolean fillActualValue; //Заполнять фактические значения по умолчанию
    boolean openWithIndicators; //Открывать задания с показателей
    boolean clearCopy; //Очищать задание при копировании
    boolean showSubject; //Показывать предметы показателей
    Selections selection; //Вид отбора аналитик по объекту

    //Виды отбора аналитик по объекту
    enum Selections {
        NOT_ANALYTICS("АналитикиНеИспользуется", "Аналитики не используются"),
        NOT_SELECTION("ОтборНеПрименяется", "Отбор не применяется"),
        BY_TYPES("ОтборПоТипам", "Отбор по типам"),
        BY_FEILDS("ОтборПоПолям", "Отбор по полям"),
        ON_QUERY("ОтборЗапросом", "Отбор запросом"),
        HAND_LINK("РучнаяСвязь", "Ручная связь");

        String id; //Наименование
        String desc; //Описание

        private Selections(String id, String desc) {
            this.id = id;
            this.desc = desc;
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

        private Criterions(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        @Override
        public String toString() {
            return desc;
        }
    };
}
