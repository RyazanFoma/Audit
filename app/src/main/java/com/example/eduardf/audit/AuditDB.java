package com.example.eduardf.audit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.support.v4.util.ArraySet;
import android.util.Log;

import com.example.eduardf.audit.dummy.DummyContent;

/**
 * Created by eduardf on 04.04.2018.
 */

public class AuditDB {

    //База данных
    private static final String DB_NAME = "auditDB";
    private static final int DB_VERSION = 18;

    //Ключевые названия колонок
    static final String ID = "_id"; //Идентификатор
    static final String NAME = "name"; //Намиенование
    static final String DESC = "dsc"; //Описание
    static final String DATE = "date"; //Дата
    private static final String PATER = "pater"; //Родитель
    static final String IS_GROUP = "is_group"; //Призкан группы

    //Справочник Пользователи
    static final String TBL_USER = "user"; //Имя таблицы
    static final String USER_PASSWORD = "password"; //Пароль пользователя
    private static final String TBL_USERS_CREATE = "create table " + TBL_USER + "(" +
                    ID + " integer primary key autoincrement, " +
                    NAME + " char(100), " +
                    USER_PASSWORD + " char(21) );";

    //Справочник Виды аудита
    static final String TBL_TYPE = "types"; //Имя таблицы
    private static final String TBL_TYPE_CREATE = "create table " + TBL_TYPE + "(" +
            ID + " integer primary key autoincrement, " +
            IS_GROUP + " integer not null, " +
            PATER + " integer not null, " +
            NAME + " char(100), " +
            DESC + " text );";

    //Справочник Объекты
    static final String TBL_OBJECT = "object"; //Имя таблицы
    private static final String TBL_OBJECT_CREATE = "create table " + TBL_OBJECT + "(" +
            ID + " integer primary key autoincrement not null, " +
            IS_GROUP + " integer not null, " +
            PATER + " integer not null, " +
            NAME + " char(100), " +
            DESC + " text );";

    //Справочник Аналитики объекта
    static final String TBL_ANALYTIC = "analytic"; //Имя таблицы
    private static final String TBL_ANALYTIC_CREATE = "create table " + TBL_ANALYTIC + "(" +
            ID + " integer primary key autoincrement not null, " +
            IS_GROUP + " integer not null, " +
            PATER + " integer not null, " +
            NAME + " char(100), " +
            DESC + " text );";

    //Регистр соответствия Видов аудита и Групп объектов и Групп аналитик
    static final String TBL_REGISTER_TOA = "regtoa"; //Имя таблицы
    private static final String TBL_REGISTER_TOA_CREATE = "create table " + TBL_REGISTER_TOA + "(" +
            TBL_TYPE + " integer, " +
            TBL_OBJECT + " integer, " +
            TBL_ANALYTIC + " integer );";

    //Список заданий
    static final String TBL_TASKLIST = "tasklist"; //Имя таблицы
    static final String TASKLIST_STATUS = "status"; //Статус задания
    private static final String TBL_TASKLIST_CREATE = "create table " + TBL_TASKLIST + "(" +
            ID + " integer primary key autoincrement, " +
            DATE + " char(19) not null, " +
            TBL_USER + " integer not null, " +
            TBL_TYPE + " integer not null, " +
            TBL_OBJECT + " integer not null, " +
            TASKLIST_STATUS + " integer not null);";

    //Аналитики задания
    static final String TBL_ANLTASK = "anltask"; //Имя таблицы
    private static final String TBL_ANLTASK_CREATE = "create table " + TBL_ANLTASK + "(" +
            TBL_TASKLIST + " integer not null, " +
            TBL_ANALYTIC + " integer not null);";


    static final int NOT_SELECTED = -1; //Элемент не выбран
    static final int ALL_CHILD = 0; //Все записи
    static final int ONLY_GROUPS = 1; //Только группы

    private final Context mCtx;
    private DBHelper mDBHelper;
    private SQLiteDatabase mDB;

    //Конструктор
    AuditDB(Context ctx) {
        mCtx = ctx;
    }

    // открыть подключение
    void open() {
        mDBHelper = new DBHelper(mCtx, DB_NAME, null, DB_VERSION);
        mDB = mDBHelper.getWritableDatabase();
    }

    // закрыть подключение
    void close() {
        if (mDBHelper != null) mDBHelper.close();
    }

//    //Печатать все данные таблицы
//    public void printAllData(String table) {
//
//        String TAG = "TaskList";
//
//        Cursor c = getAllData(table);
//        String str;
//
//        Log.i(TAG, table);
//
//        if (c.moveToFirst()) {
//            str = "";
//            for (int i=0;i<c.getColumnCount();i++) str=str+" "+c.getColumnName(i);
//            Log.i(TAG, str);
//            do {
//                str = "";
//                for (int i=0;i<c.getColumnCount();i++) str=str+" "+c.getString(i);
//                Log.i(TAG, str);
//            } while (c.moveToNext());
//        } else
//            Log.d(TAG, "0 rows");
//        c.close();
//    }


    //УНИВЕРСАЛЬНЫЕ МЕТОДЫ ПОЛУЧЕНИЯ ДАННЫХ ИЗ ТАБЛИЦ
    //Получить все данные таблицы
    Cursor getAllData(String table) {
        return mDB.rawQuery("SELECT * FROM " + table + " ORDER BY " + NAME + ";", null );
    }

    //Возвращает наименование по id или null, если не найден
    String getNameById(String table, int id) {
        String name = null;
        Cursor c = mDB.rawQuery( "SELECT " + NAME + " FROM " + table + " WHERE " + ID + " = ?;", new String[] { String.valueOf(id) });
        if (c.moveToFirst()) name = c.getString(c.getColumnIndex(NAME));
        c.close();
        return name;
    }

    //Возвращает список наименований по коллеции идентификаторов
    List<DummyContent.DummyItem> getDummyByIds(String sTable, Collection<Integer> ids) {
        List<DummyContent.DummyItem> list = new ArrayList<DummyContent.DummyItem>(ids.size());
        if (!ids.isEmpty()) {
            StringBuffer in = new StringBuffer();
            for (int id:ids) if (in.length()==0) in.append(Integer.toString(id)); else in.append(","+Integer.toString(id));
            Cursor cursor = mDB.rawQuery("SELECT "+ID+","+NAME+" FROM " + sTable + " WHERE " + ID + " IN (" + in.toString() + ") ORDER BY " + NAME + ";", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(new DummyContent.DummyItem(cursor.getString(cursor.getColumnIndex(ID)), cursor.getString(cursor.getColumnIndex(NAME)), ""));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    //Возвращает список наименований по коллеции идентификаторов
    Items getItemsByPater(String table, int pater) {

        Cursor cursor = mDB.rawQuery("SELECT * FROM "+table+" WHERE "+PATER+" = ? ORDER BY 1-"+IS_GROUP+", "+NAME+" ASC;",
                new String[] {Integer.toString(pater)});

        Items items = new Items(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(ID));
                String desc;
                boolean folder = cursor.getInt(cursor.getColumnIndex(IS_GROUP))==1;
                if (folder) {
                    int folders = child(table, true, id);
                    int files = child(table, false, id)-folders;
                    desc = "Папок "+folders+", элементов "+files;
                }
                else desc = cursor.getString(cursor.getColumnIndex(DESC));
                items.add(new Items.Item(id, folder, pater, cursor.getString(cursor.getColumnIndex(NAME)), desc));

            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    //Возвращает список наименований по коллеции идентификаторов
    List<GroupTable.Item> getGroupsByPater(String table, int pater) {

        Cursor cursor = mDB.rawQuery("SELECT * FROM "+table+" WHERE "+IS_GROUP+" = 1 AND "+PATER+" = ? ORDER BY "+NAME+";", new String[] {Integer.toString(pater)});
        List<GroupTable.Item> list = new ArrayList<GroupTable.Item>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(ID));
                int child = child(table, true, id);
                list.add(new GroupTable.Item(id, pater, child, cursor.getString(cursor.getColumnIndex(NAME)), cursor.getString(cursor.getColumnIndex(DESC))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    //Возвращает число потомков
    private int child(String sTable, boolean only_group, int id) {
        Cursor cursor = mDB.rawQuery("SELECT * FROM "+sTable+" WHERE "+(only_group?IS_GROUP+" = 1 AND ":"")+PATER+" = ?;", new String[] {Integer.toString(id)});
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // возвращает мап-список наименований по коллекци идентификаторов
    public ArrayList<Map<String, Object>> getNamebyIds(String sTable, Collection<Integer> ids) {
        ArrayList<Map<String, Object>> analytics = new ArrayList<Map<String, Object>>(ids.size());
        Map<String, Object> m;
        if (!ids.isEmpty()) {
            StringBuffer in = new StringBuffer();
            for (int id:ids) if (in.length()==0) in.append(Integer.toString(id)); else in.append(","+Integer.toString(id));
            Cursor c = mDB.rawQuery("SELECT * FROM " + sTable + " WHERE " + ID + " IN (" + in.toString() + ") ORDER BY " + NAME + ";", null);
            if (c.moveToFirst())
                do {
                    m = new HashMap<String, Object>();
                    m.put("id", c.getInt(c.getColumnIndex(ID)));
                    m.put("name", c.getString(c.getColumnIndex(NAME)));
                    analytics.add(m);
                } while (c.moveToNext());
            c.close();
        }
        return analytics;
    }

    // обновляет мап-список по коллекции идентификаторов
    public void getNamebyIds(String sTable, ArrayList<Map<String, Object>> analytics, Collection<Integer> ids) {
        analytics.clear();
        Map<String, Object> m;
        if (ids.size()>0) {
            StringBuffer in = new StringBuffer();
            for (int id:ids) if (in.length()==0) in.append(id); else in.append(","+id);
            Cursor c = mDB.rawQuery("SELECT * FROM " + sTable + " WHERE " + ID + " IN (" + in.toString() + ") ORDER BY " + NAME + ";", null);
            if (c.moveToFirst())
                do {
                    m = new HashMap<String, Object>();
                    m.put("id", c.getInt(c.getColumnIndex(ID)));
                    m.put("name", c.getString(c.getColumnIndex(NAME)));
                    analytics.add(m);
                } while (c.moveToNext());
            c.close();
        }
    }

    //МЕТОДЫ РАБОТЫ СО СТРУКТУРИРОВАННЫМИ ТАБЛИЦАМИ
    //Возвращает родителя наследника или -1, если не найден (нарушена целостность базы)
    int getPaterById(String table, int id) {
        int pater = -1;
        Cursor c = mDB.rawQuery("SELECT " + PATER + " FROM " + table + " WHERE " + ID + " = " + id + ";", null );
        if (c.moveToFirst()) pater = c.getInt(c.getColumnIndex(PATER));
        c.close();
        return pater;
    }

    //Возвращает прародителя наследника или -1, если не найден (нарушена целостность базы)
    int getAncestorById(String table, int id) {
        int pater = -1;
        Cursor c = mDB.rawQuery("SELECT " + PATER + " FROM " + table + " WHERE " + ID + " = " + id + ";", null );
        if (c.moveToFirst()) {
            pater = c.getInt(c.getColumnIndex(PATER));
            c.close();
            if (pater==0) pater = id; //Мs достигли прародителя
            else pater = getAncestorById(table, pater); //Рекурсия!!!
        }
        else c.close();
        return pater;
    }


    // получить всех наследников с сортировкой по алфавиту
    Cursor getChildsByPater(String table, int pater, String like) {
        String where = "";
        if ((like!=null)&&(!like.isEmpty())) where = " AND ( " + NAME + " LIKE '%"+ like + "%' OR " + IS_GROUP + " = 1 )";
        String query = "SELECT " + ID + ", " + NAME + ", " + IS_GROUP + " FROM " + table + " WHERE " + PATER + " = " + pater + where + " ORDER BY " + NAME + ";";
        return mDB.rawQuery(query, null );
    }

    // получить всех наследников с отбором по массиву и с сортировкой по алфавиту
    Cursor getChildsInPater(String table, int pater, int[] in) {
        StringBuffer where = new StringBuffer();
        if ((in!=null)&&(in.length>0)) {
            for (int i:in) if (where.length()==0) where.append(i); else where.append(","+i);
            where.append(" AND " + ID + " IN ("+Integer.toString(in[0]) + where.toString() + ") AND " + IS_GROUP + " = 1");
        }
        String query = "SELECT " + ID + ", " + NAME + ", " + IS_GROUP + " FROM " + table + " WHERE " + PATER + " = " + pater + where.toString() + " ORDER BY " + NAME + ";";
        return mDB.rawQuery(query, null );
    }

    //МЕТОДЫ РАБОТЫ С РЕГИСТРОМ TOA
    //Возвращает массив групп объектов из регистра по виду аудита
    int[] getObjectsByType(int type) {
        int[] objects = null;
        Cursor c = mDB.rawQuery("SELECT * FROM " + TBL_REGISTER_TOA + " WHERE " + TBL_TYPE + " = " + type + ";", null);
        if (c.moveToFirst()) {
            int i = 0;
            objects = new int[c.getCount()];
            do objects[i++] =  c.getInt(c.getColumnIndex(TBL_OBJECT)); while (c.moveToNext());
        }
        c.close();
        return objects;
    }

    //Возвращает массив групп аналитик из регистра по виду аудита и прародителю объекта
    int[] getAnalyticsByTO(int type, int object) {
        int[] analytics = null;
        int ancestor = getAncestorById(TBL_OBJECT,object);
        Cursor c = mDB.rawQuery("SELECT * FROM " + TBL_REGISTER_TOA + " WHERE " + TBL_TYPE + " = " + type + " AND " + TBL_OBJECT + " = " + ancestor + ";", null);
        if (c.moveToFirst()) {
            int i = 0;
            analytics = new int[c.getCount()];
            do analytics[i++] =  c.getInt(c.getColumnIndex(TBL_ANALYTIC)); while (c.moveToNext());
        }
        c.close();
        return analytics;
    }

    //МЕТОДЫ ДЛЯ РАБОТЫ С ЗАДАНИЯМИ
    //Получить все задания по аудитору и статусу в порядке убывания дат с отбором по наименованию объекта
    Cursor getTasksByAuditor(int auditor, int status, String like) {
        String query = "SELECT TL." + ID +
                ", TL." + DATE +
                ", TP." + NAME + " AS " + TBL_TYPE +
                ", OB." + NAME + " AS " + TBL_OBJECT +
                ", TL." + TASKLIST_STATUS +
                " FROM " + TBL_TASKLIST + " AS TL" +
                " LEFT JOIN " + TBL_OBJECT + " AS OB ON TL." + TBL_OBJECT + " = OB." + ID +
                " LEFT JOIN " + TBL_TYPE + " AS TP ON TL." + TBL_TYPE + " = TP." + ID +
                " WHERE TL." + TBL_USER + " = ? AND TL." + TASKLIST_STATUS + " = ?" + ((like!=null)||(!like.isEmpty())?" AND OB." + NAME + " LIKE '%" + like + "%' ":"") +
                " ORDER BY TL." + DATE + " DESC;";
        return mDB.rawQuery(query, new String[] {String.valueOf(auditor), String.valueOf(status)});
    }

//    //Добавить задание
//    public void addTask(String date, int auditor, int status) {
//        ContentValues cv = new ContentValues();
//        cv.put(TASKLIST_DATE, date);
//        cv.put(TASKLIST_AUDITOR, auditor);
//        cv.put(TASKLIST_STATUS, status);
//        mDB.beginTransaction();
//        mDB.insert(TBL_TASKLIST, null, cv);
//        mDB.endTransaction();
//    }

    // возвращает множество идентификаторов аналитики задания
    Set<Integer> getAnalytics(int id) {
        Set<Integer> analitics = new ArraySet<Integer>();
        Cursor c = mDB.rawQuery("SELECT * FROM " + TBL_ANLTASK + " WHERE " + TBL_TASKLIST + "=" + id + ";", null);
        if (c.moveToFirst()) do analitics.add(c.getInt(c.getColumnIndex(TBL_ANALYTIC))); while (c.moveToNext());
        c.close();
        return  analitics;
    }

    // возвращает задание по id или null, если не нашлось
    Task getTaskById(int id) {
        Task task = null;
        String query = "SELECT * FROM " + TBL_TASKLIST + " WHERE " + ID + " = ?;";
        Cursor c = mDB.rawQuery( query, new String[] { String.valueOf(id) });
        if (c.moveToFirst()) {
            task = new Task( id,
                    c.getString(c.getColumnIndex(DATE)),
                    c.getInt(c.getColumnIndex(TBL_USER)),
                    c.getInt(c.getColumnIndex(TBL_TYPE)),
                    c.getInt(c.getColumnIndex(TBL_OBJECT)),
                    c.getInt(c.getColumnIndex(TASKLIST_STATUS)),
                    getAnalytics(id));
        }
        c.close();
        return task;
    }

    // сохранить задание
    void saveTask(Task task) {
        ContentValues cv = new ContentValues();
        cv.put(DATE, task.date);
        cv.put(TBL_USER, task.auditor);
        cv.put(TBL_TYPE, task.type);
        cv.put(TBL_OBJECT, task.object);
        cv.put(TASKLIST_STATUS, task.status);
        mDB.beginTransaction();
        try {
            //Добавляем задание или обновляем
            if (task.id==Task.NEW_TASK_ID) mDB.insert(TBL_TASKLIST, null, cv);
            else mDB.update(TBL_TASKLIST, cv, ID + " = " + task.id, null);
            mDB.delete(TBL_ANLTASK, TBL_TASKLIST + "=" + task.id, null); //Удаляем всю аналитику задания
            //Добавляем аналитику задания
            cv.clear();
            for(int a:task.analytics) {
                cv.put(TBL_TASKLIST,task.id);
                cv.put(TBL_ANALYTIC,a);
                mDB.insert(TBL_ANLTASK, null, cv);
            }
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    // установить статус задания
    void changeStatusById(int id, int status) {
        ContentValues cv = new ContentValues();
        cv.put(TASKLIST_STATUS, status);
        mDB.beginTransaction();
        try {
            mDB.update(TBL_TASKLIST, cv, ID + " = " + id, null);
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    // удалить задание
    void delTask(int id) {
        mDB.beginTransaction();
        try {
            mDB.delete(TBL_TASKLIST, ID + " = " + id, null);
            mDB.delete(TBL_ANLTASK, TBL_TASKLIST + "=" + id, null); //Удаляем всю аналитику задания
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    //ВСЕ ДЛЯ ВИДОВ АУДИТА
    // возвращает вид аудита по id
    AuditType getTypeById(int id) {
        AuditType type = null;

        //Ищем вид аудита в справочнике
        Cursor c = mDB.rawQuery( "SELECT * FROM " + TBL_TYPE + " WHERE " + ID + " = ?;", new String[] { String.valueOf(id) });
        if (c.moveToFirst()) { //Создаем объект Вид аудита
            type = new AuditType( id,
                    c.getString(c.getColumnIndex(NAME)),
                    c.getInt(c.getColumnIndex(PATER)),
                    c.getInt(c.getColumnIndex(IS_GROUP))!=0,
                    c.getString(c.getColumnIndex(DESC)));
            //Ищем группы объектов и аналитик в регистре
            c = mDB.rawQuery( "SELECT * FROM "+TBL_REGISTER_TOA+" WHERE "+TBL_TYPE+" = ?;", new String[] {String.valueOf(id)});
            if (c.moveToFirst()) {
                do { //Добавляем группы объектов и аналитик
                    type.addAnalytic(c.getInt(c.getColumnIndex(TBL_OBJECT)),c.getInt(c.getColumnIndex(TBL_ANALYTIC)));
                } while (c.moveToNext());
            }
        }
        c.close();
        return type;
    }

    // класс по созданию и управлению БД
    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // создаем и заполняем БД
        @Override
        public void onCreate(SQLiteDatabase db) {

            int id;
            int[] type = new int[2];
            int[] object = new int[5];
            int[] analytic = new int[5];

            //Создаем таблицы
            db.execSQL(TBL_USERS_CREATE); //Пользователи
            db.execSQL(TBL_TYPE_CREATE); //Виды
            db.execSQL(TBL_OBJECT_CREATE); //Объекты
            db.execSQL(TBL_ANALYTIC_CREATE); //Аналитика
            db.execSQL(TBL_REGISTER_TOA_CREATE); //Регистр Вид+Объект+Аналитика
            db.execSQL(TBL_TASKLIST_CREATE); //Задания
            db.execSQL(TBL_ANLTASK_CREATE); //Аналитика задания
            //В методе onUpgrade предусмотреть удаление таблиц!!!

            //ЗАПОЛНЯЕМ ТАБЛИЦЫ ДАННЫМИ ДЛЯ ТЕСТИРОВАНИЯ
            //Заполняем таблицу пользователей
            ContentValues cv = new ContentValues();
            cv.put(NAME, "Иванов");
            cv.put(USER_PASSWORD, "1234");
            db.insert(TBL_USER, null, cv);
            cv.put(NAME, "Петров");
            cv.put(USER_PASSWORD, "2345");
            db.insert(TBL_USER, null, cv);
            cv.put(NAME, "Сидоров");
            cv.put(USER_PASSWORD, "3456");
            db.insert(TBL_USER, null, cv);

            //Заполняем таблицу видов аудита
            cv.clear();
            id = 0;
            for (int i = 1; i <= 2; i++) {
                ++id;
                cv.put(NAME, "Вид " + 0 + "-" + i );
                cv.put(IS_GROUP, false);
                cv.put(PATER, 0);
                db.insert(TBL_TYPE, null, cv);
                type[i-1]=id;
            }
            for (int j = 1; j <= 2; j++) {
                int gr = ++id;
                cv.put(NAME, "Группа " + j);
                cv.put(IS_GROUP, 1);
                cv.put(PATER, 0);
                db.insert(TBL_TYPE, null, cv);
                for (int i2 = 1; i2 <= 3; i2++) {
                    ++id;
                    cv.put(NAME, "Вид " + j + "-" + i2 );
                    cv.put(IS_GROUP, 0);
                    cv.put(PATER, gr);
                    db.insert(TBL_TYPE, null, cv);
                }
                for (int j1 = 1; j1 <= 2; j1++) {
                    int gr1 = ++id;
                    cv.put(NAME, "Группа " + j + j1);
                    cv.put(IS_GROUP, 1);
                    cv.put(PATER, gr);
                    db.insert(TBL_TYPE, null, cv);
                    for (int i1 = 1; i1 <= 3; i1++) {
                        ++id;
                        cv.put(NAME, "Вид " + j + j1 + "-" + i1 );
                        cv.put(IS_GROUP, 0);
                        cv.put(PATER, gr1);
                        db.insert(TBL_TYPE, null, cv);
                    }
                }
            }

            //Заполняем таблицу объектов
            cv.clear();
            id = 0;
            for (int i = 1; i <= 5; i++) {
                ++id;
                cv.put(NAME, "Объект " + 0 + "-" + i );
                cv.put(IS_GROUP, false);
                cv.put(PATER, 0);
                db.insert(TBL_OBJECT, null, cv);
            }
            for (int j = 1; j <= 5; j++) {
                int gr = ++id;
                cv.put(NAME, "Группа " + j);
                cv.put(IS_GROUP, 1);
                cv.put(PATER, 0);
                db.insert(TBL_OBJECT, null, cv);
                object[j-1]=id;
                for (int i = 1; i <= 10; i++) {
                    ++id;
                    cv.put(NAME, "Объект " + j + "-" + i );
                    cv.put(IS_GROUP, 0);
                    cv.put(PATER, gr);
                    db.insert(TBL_OBJECT, null, cv);
                }
                for (int j1 = 1; j1 <= 2; j1++) {
                    int gr1 = ++id;
                    cv.put(NAME, "Группа " + j + " " + j1);
                    cv.put(IS_GROUP, 1);
                    cv.put(PATER, gr);
                    db.insert(TBL_OBJECT, null, cv);
                    for (int i1 = 1; i1 <= 2; i1++) {
                        ++id;
                        cv.put(NAME, "Объект " + j + " " + j1 + "-" + i1 );
                        cv.put(IS_GROUP, 0);
                        cv.put(PATER, gr1);
                        db.insert(TBL_OBJECT, null, cv);
                    }
                }
            }

            //Заполняем таблицу аналитик объектов
            cv.clear();
            id = 0;
            for (int i = 1; i <= 5; i++) {
                ++id;
                cv.put(NAME, "Аналитика " + 0 + "-" + i );
                cv.put(IS_GROUP, false);
                cv.put(PATER, 0);
                db.insert(TBL_ANALYTIC, null, cv);
            }
            for (int j = 1; j <= 5; j++) {
                int gr = ++id;
                cv.put(NAME, "Группа " + j);
                cv.put(IS_GROUP, 1);
                cv.put(PATER, 0);
                db.insert(TBL_ANALYTIC, null, cv);
                analytic[j-1]=id;
                for (int i = 1; i <= 10; i++) {
                    ++id;
                    cv.put(NAME, "Аналитика " + j + "-" + i );
                    cv.put(IS_GROUP, 0);
                    cv.put(PATER, gr);
                    db.insert(TBL_ANALYTIC, null, cv);
                }
                for (int j1 = 1; j1 <= 2; j1++) {
                    int gr1 = ++id;
                    cv.put(NAME, "Группа " + j + " " + j1);
                    cv.put(IS_GROUP, 1);
                    cv.put(PATER, gr);
                    db.insert(TBL_ANALYTIC, null, cv);
                    for (int i1 = 1; i1 <= 2; i1++) {
                        ++id;
                        cv.put(NAME, "Аналитика " + j + " " + j1 + "-" + i1 );
                        cv.put(IS_GROUP, 0);
                        cv.put(PATER, gr1);
                        db.insert(TBL_ANALYTIC, null, cv);
                    }
                }
            }

            //Заполняем регистр Вид+Объект+Аналитика
            cv.clear();
            for(int i=0;i<5;i++)
                for(int j=0;j<5;j++) {
                    int t = analytic[j]+object[i];
                    cv.put(TBL_TYPE, type[t-t/2*2] );
                    cv.put(TBL_OBJECT, object[t-t/5*5]);
                    cv.put(TBL_ANALYTIC, analytic[j]);
                    db.insert(TBL_REGISTER_TOA, null, cv);
                }

            //Создает таблицу заданий
            cv.clear();
            int o = 1;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = dateFormat.format(new Date());
            for (int j = 1; j <= 3; j++) { //По аудиторам
                for (int i = 0; i < 18; i++) {
                    cv.put(DATE, date);
                    cv.put(TBL_USER, j);
                    cv.put(TBL_TYPE, type[i-i/2*2]);
                    cv.put(TBL_OBJECT, o++);
                    int s=o+i+j;
                    cv.put(TASKLIST_STATUS, s-s/3*3);
                    db.insert(TBL_TASKLIST, null, cv);
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion!=newVersion) {
                db.beginTransaction();
                try {
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_USER);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_TYPE);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_OBJECT);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_ANALYTIC);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_REGISTER_TOA);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_TASKLIST);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_ANLTASK);
                    this.onCreate(db);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        }
    }
}
