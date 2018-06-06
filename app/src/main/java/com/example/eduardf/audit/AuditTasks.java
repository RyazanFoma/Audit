package com.example.eduardf.audit;

import android.support.annotation.NonNull;

import com.example.eduardf.audit.MediaList;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by eduardf on 16.03.2018.
 */

//Основной класс - список заданий
public class AuditTasks {

    //Список заданий на аудит
    private TreeSet<ATsk> list;

    //Конструктор списка заданий
    public AuditTasks() {
        class ATskComparator implements Comparator<ATsk> {
            public int compare(ATsk a, ATsk b) {
                return a.compareTo(b);
            }
        }
        list = new TreeSet<ATsk>(new ATskComparator());
    }

    //Добавляет в список задание
    public boolean add(ATsk e) {
        return list.add(e);
    }

    //Удаляет задание из списка
    public boolean remove(ATsk e) {
        return list.remove(e);
    }

    //Очищает список заданий
    public void clear() {
        list.clear();
    }

    //Возвращает список заданий в виде массива строк
    public String[] toArray() {
        String[] result = new String[list.size()];
        int i=0;
        for (ATsk e: list) {
            result[i++] = list.toString();
        }
        return result;
    }

    //Вложенный класс - задание на аудит
    public static class ATsk {

        //Дата аудита
        private Date date;
        //Аудитор
        private String auditor;
        //Список показателей
        private Inds list;

        //Конструктор заданий
        public ATsk(Date date, String auditor) {
            this.date = date;
            this.auditor = auditor;
            this.list = new Inds();
        }

        public ATsk(Date date, String auditor, Inds inds) {
            this.date = date;
            this.auditor = auditor;
            this.list = inds;
        }

        //Компаратор для сортировки заданий по датам
        public int compareTo(ATsk a) {
            return date.compareTo(a.date);
        }

        //Возвращает описание задачи строкой
        public String toString() {
            return date.toString() +" "+ auditor.toString();
        }

        //Вложенный класс - список показателей
        public class Inds {

            //Список показателей
            private LinkedHashSet<IndsGroup> list;

            //Конструктор
            public Inds() {
                list = new LinkedHashSet<IndsGroup>();
            }

            //Добавить группу
            public boolean add(IndsGroup e) {
                return list.add(e);
            }

            //Удалить группу
            public boolean remove(IndsGroup e) {
                return list.remove(e);
            }

            //Внутренний класс - группа показателей
            public class IndsGroup {

                //Наименование группы показателей
                private String Name;
                //Список показателей группы
                private LinkedHashSet<Ind> list;

                //Конструктор группы показателей
                public IndsGroup(String Name) {
                    this.Name = Name;
                    this.list = new LinkedHashSet<>();
                }

                //Добавить показатель в группу
                public boolean add(Ind o) {
                    return list.add(o);
                }

                //Удалить показатель из группы
                public boolean remove(Ind i) {
                    return list.remove(i);
                }

                //Внутренний класс - показатель
                public class Ind {

                    //Тип показателя
                    private int Type;
                    public static final int BOOLEAN_TYPE = 0;
                    public static final int INT_TYPE = 1;
                    public static final int FLOAT_TYPE = 2;

                    //Значения показателя для каждого типа
                    private boolean bValue;
                    private int iValue;
                    private float fValue;

                    //Нормативные значения показателя для каждого типа
                    private boolean bGoal;
                    private int iGoal;
                    private float fGoal;

                    private String Name; //Наименование показателя
                    private String Comment; //Комментарий к показателю
                    private boolean Reach; //Признак достижения цели
                    private MediaList mediaList; //Список медиафайлов

                    //Критерий достижения цели
                    private int Condition;
                    public static final int CONDITION_NOT_EQUAL = 0;
                    public static final int CONDITION_EQUAL = 1;
                    public static final int CONDITION_MORE = 10;
                    public static final int CONDITION_LESS = 20;

                    //Конструктор для показателя INT
                    public Ind(int Value, int Goal, int Condition, String Name, String Comment, MediaList mediaList) {
                        Type = INT_TYPE;
                        iValue = Value;
                        iGoal = Goal;
                        this.Condition = Condition;
                        this.Reach = reachGoal(Value);
                        this.Name = Name;
                        this.Comment = Comment;
                        this.mediaList = mediaList;
                    }
                    //Конструктор для показателя FLOAT
                    public Ind(float Value, float Goal, int Condition, String Name, String Comment, MediaList mediaList) {
                        Type = FLOAT_TYPE;
                        fValue = Value;
                        fGoal = Goal;
                        this.Condition = Condition;
                        this.Reach = reachGoal(Value);
                        this.Name = Name;
                        this.Comment = Comment;
                        this.mediaList = mediaList;
                    }
                    //Конструктор для показателя BOOLEAN
                    public Ind(boolean Value, boolean Goal, int Condition, String Name, String Comment, MediaList mediaList) {
                        Type = BOOLEAN_TYPE;
                        bValue = Value;
                        bGoal = Goal;
                        this.Condition = Condition;
                        this.Reach = reachGoal(Value);
                        this.Name = Name;
                        this.Comment = Comment;
                        this.mediaList = mediaList;
                    }

                    //Возвращает результат выполнения условия по BOOLEAN показателям
                    private boolean reachGoal(boolean Value) {
                        switch (Condition) {
                            case CONDITION_NOT_EQUAL: return Value!=bGoal;
                            case CONDITION_EQUAL: return Value==bGoal;
                        }
                        return false;
                    }

                    //Возвращает результат выполнения условия по INT показателям
                    private boolean reachGoal(int Value) {
                        switch (Condition) {
                            case CONDITION_NOT_EQUAL: return Value!=iGoal;
                            case CONDITION_EQUAL: return Value==iGoal;
                            case CONDITION_MORE: return Value>iGoal;
                            case CONDITION_MORE|CONDITION_EQUAL: return Value>=iGoal;
                            case CONDITION_LESS: return Value<iGoal;
                            case CONDITION_LESS|CONDITION_EQUAL: return Value<=iGoal;
                        }
                        return false;
                    }

                    //Возвращает результат выполнения условия по FLOAT показателям
                    private boolean reachGoal(float Value) {
                        switch (Condition) {
                            case CONDITION_NOT_EQUAL: return Value!=fGoal;
                            case CONDITION_EQUAL: return Value==fGoal;
                            case CONDITION_MORE: return Value>fGoal;
                            case CONDITION_MORE|CONDITION_EQUAL: return Value>=fGoal;
                            case CONDITION_LESS: return Value<fGoal;
                            case CONDITION_LESS|CONDITION_EQUAL: return Value<=fGoal;
                        }
                        return false;
                    }

                    //Возвращает текст условия
                    private String toStringCondition() {
                        switch (Condition) {
                            case CONDITION_NOT_EQUAL: return "не равно";
                            case CONDITION_EQUAL: return "равно";
                            case CONDITION_MORE: return "больше";
                            case CONDITION_MORE|CONDITION_EQUAL: return "больше или равно";
                            case CONDITION_LESS: return "меньше";
                            case CONDITION_LESS|CONDITION_EQUAL: return "меньше или равно";
                        }
                        return "";
                    }

                    //Возвращает текст выполнения условия
                    private String reachGoalWhy() {

                        String v="", g="";

                        switch (Type) {
                            case BOOLEAN_TYPE:
                                v = bValue?"Да":"Нет";
                                g = bGoal?"Да":"Нет";
                                break;
                            case INT_TYPE:
                                v = Integer.toString(iValue);
                                g = Integer.toString(iGoal);
                                break;
                            case FLOAT_TYPE:
                                v = Float.toString(fValue);
                                g = Float.toString(fGoal);
                                break;
                        }

                        return "Показатель ("+v+") "+toStringCondition()+" Норматив ("+g+")";
                    }
                }
            }
        }
    }
}
