package com.example.eduardf.audit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.support.v4.util.ArraySet;

import static android.database.Cursor.FIELD_TYPE_FLOAT;
import static android.database.Cursor.FIELD_TYPE_INTEGER;
import static android.database.Cursor.FIELD_TYPE_STRING;


/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 10.01.19 15:57
 *
 */

/** УДАЛИТЬ - УСТАРЕЛО!!!
 * Created by eduardf on 04.04.2018.
 */

public class AuditDB {

    //База данных
    private static final String DB_NAME = "auditDB";
    private static final int DB_VERSION = 25;

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

    //Регистр для ручного связывания объектов и аналитик
    static final String TBL_REGISTER_OA = "regoa"; //Имя таблицы
    private static final String TBL_REGISTER_OA_CREATE = "create table " + TBL_REGISTER_OA + "(" +
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
    static private final String PATERN_DATE = "yyyy-MM-dd HH:mm:ss"; //Шаблон формата даты

    //Аналитики задания
    static final String TBL_ANLTASK = "anltask"; //Имя таблицы
    private static final String TBL_ANLTASK_CREATE = "create table " + TBL_ANLTASK + "(" +
            TBL_TASKLIST + " integer not null, " +
            TBL_ANALYTIC + " integer not null);";

    //Предметы аудита
    static final String TBL_SUBJECT = "subject"; //Имя таблицы
    static final String SUBJECT_CRITERION = "criterion"; //Критерий
    static final String SUBJECT_VALUE = "password"; //Значение критерия
    private static final String TBL_SUBJECT_CREATE = "create table " + TBL_SUBJECT + "(" +
            ID + " integer primary key autoincrement, " +
            TBL_TYPE + " integer not null, " +
            IS_GROUP + " integer not null, " +
            PATER + " integer not null, " +
            NAME + " char(100), " +
            DESC + " text, " +
            SUBJECT_CRITERION + " integer, " +
            SUBJECT_VALUE + " float );";

    //Показатели аудита
    static final String TBL_INDICATOR = "indicator"; //Имя таблицы
    static final String INDICATOR_TYPE = "type"; //Тип показателя
    static final String INDICATOR_CRITERION = "criterion"; //Критерий
    static final String INDICATOR_UNIT = "unit"; //Единица измерения
    private static final String TBL_INDICATOR_CREATE = "create table " + TBL_INDICATOR + "(" +
            ID + " integer primary key autoincrement, " +
            TBL_SUBJECT + " integer not null, " +
            IS_GROUP + " integer not null, " +
            PATER + " integer not null, " +
            NAME + " char(100), " +
            DESC + " text, " +
            INDICATOR_TYPE + " integer, " +
            INDICATOR_CRITERION + " integer, " +
            INDICATOR_UNIT + " integer );";

    static final int NOT_SELECTED = -1; //Элемент не выбран

    private final Context mCtx;
    private DBHelper mDBHelper;
    private SQLiteDatabase mDB;

    //Конструктор
    public AuditDB(Context ctx) {
        mCtx = ctx;
    }

    // открыть подключение
    public void open() {
        mDBHelper = new DBHelper(mCtx, DB_NAME, null, DB_VERSION);
        mDB = mDBHelper.getWritableDatabase();
    }

    // закрыть подключение
    public void close() {
        if (mDBHelper != null) mDBHelper.close();
    }

    //УНИВЕРСАЛЬНЫЕ МЕТОДЫ ПОЛУЧЕНИЯ ДАННЫХ ИЗ ТАБЛИЦ
    //Получить все данные таблицы
    public Cursor getAllData(String table) {
        return mDB.rawQuery("SELECT * FROM " + table + " ORDER BY " + NAME + ";", null );
    }

    //Возвращает наименование по id или null, если не найден
    public String getName(String table, int id) {
        String name = null;
        Cursor c = mDB.rawQuery( "SELECT " + NAME + " FROM " + table + " WHERE " + ID + " = ?;", new String[] { String.valueOf(id) });
        if (c.moveToFirst()) name = c.getString(c.getColumnIndex(NAME));
        c.close();
        return name;
    }

    //Возвращает пукнт по id
    public Items.Item getItem(String table, int id, String like) {
        Items.Item item = null;
        Cursor cursor = mDB.rawQuery("SELECT * FROM "+table+" WHERE "+ID+" = ?;", new String[]{String.valueOf(id)});
        Items items = new Items();
        if (cursor.moveToFirst()) {
            int folders = 0;
            int files = 0;
            String desc;
            boolean folder = cursor.getInt(cursor.getColumnIndex(IS_GROUP))==1;
            if (folder) {
                folders = child(table, true, id);
                files = child(table, id, null); //Файлы подсчитываем без учета фильтра
                desc = "Папок "+folders+", элементов "+child(table, id, like); //В описании указываем с учетом фильтра
            }
            else desc = cursor.getString(cursor.getColumnIndex(DESC));
//            item = new Items.Item(id, folder, folders, files, cursor.getInt(cursor.getColumnIndex(PATER)), cursor.getString(cursor.getColumnIndex(NAME)), desc);
        }
        cursor.close();
        return item;
    }

    //Возвращает список пунктов с наименованиями по списку идентификаторов
    public Items getItemsByIds(String table, ArrayList<Integer> in) {
        StringBuffer where = new StringBuffer();
        if ((in!=null)&&(in.size()>0)) for (int i: in) if (where.length()==0) where.append(i); else where.append(","+i);
        Cursor cursor = mDB.rawQuery("SELECT * FROM "+table+" WHERE "+ID+" IN ("+where.toString()+") ORDER BY 1-"+IS_GROUP+", "+NAME+" ASC;", null);
        Items items = new Items();
        if (cursor.moveToFirst()) {
            do {
//                items.addItem(new Items.Item(cursor.getInt(cursor.getColumnIndex(ID)),
//                        cursor.getInt(cursor.getColumnIndex(IS_GROUP))==1,
//                        NOT_SELECTED, NOT_SELECTED,
//                        cursor.getInt(cursor.getColumnIndex(PATER)),
//                        cursor.getString(cursor.getColumnIndex(NAME)),
//                        cursor.getString(cursor.getColumnIndex(DESC))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    //Возвращает список пунктов с наименованиями и полным путем по списку идентификаторов
    public Items getItemsPathByIds(String table, ArrayList<Integer> in) {
        StringBuffer where = new StringBuffer();
        if ((in!=null)&&(in.size()>0)) for (int i: in) if (where.length()==0) where.append(i); else where.append(","+i);
        Cursor cursor = mDB.rawQuery("SELECT * FROM "+table+" WHERE "+ID+" IN ("+where.toString()+") ORDER BY 1-"+IS_GROUP+", "+NAME+" ASC;", null);
        Items items = new Items();
        if (cursor.moveToFirst()) {
            do {
                int pater = cursor.getInt(cursor.getColumnIndex(PATER));
//                items.addItem(new Items.Item(cursor.getInt(cursor.getColumnIndex(ID)),
//                        cursor.getInt(cursor.getColumnIndex(IS_GROUP))==1,
//                        NOT_SELECTED, NOT_SELECTED,
//                        pater,
//                        cursor.getString(cursor.getColumnIndex(NAME)),
//                        getAncestors(table, pater)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    // возвращает строку с полным гинеологическим древом
    private String getAncestors(String table, int id) {
        String result = "";
        if (id > 0) {
            Cursor cursor = mDB.rawQuery("SELECT * FROM "+table+" WHERE "+ID+"=?;", new String[] {Integer.toString(id)});
            if (cursor.moveToFirst()) {
                int pater = cursor.getInt(cursor.getColumnIndex(PATER));
                result = getAncestors(table, pater)+" > "+cursor.getString(cursor.getColumnIndex(NAME));
            }
            cursor.close();
        }
        return result;
    }

    //Возвращает список пунктов родителя
    public Items getItemsByPater(String table, int pater, String keyName, int key, String like) {
        Items items = null;
        Cursor cursor;
        StringBuffer where = new StringBuffer();
        //Если указана подстрока
        if ((like!=null)&&(!like.isEmpty())) where.append(" AND ( "+NAME+" LIKE '%"+like+"%' OR "+IS_GROUP+" = 1 )");

        //Добавлено, после ошибки Attempt to reopen an already-closed object sqlitedatabase, которая случается после поворота экрана.
        if (!mDB.isOpen() & mDBHelper!=null) mDB = mDBHelper.getWritableDatabase();

        //Отбор по строке в наименовании
        cursor =  mDB.rawQuery("SELECT * FROM "+table+" WHERE "+keyName+" = ? AND "+PATER+" = ?"+where.toString()+" ORDER BY 1-"+IS_GROUP+", "+NAME+" ASC;",
                new String[] {Integer.toString(key), Integer.toString(pater)});
        try {
            items = new Items();
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(ID));
                    int folders = 0;
                    int files = 0;
                    String desc;
                    boolean folder = cursor.getInt(cursor.getColumnIndex(IS_GROUP))==1;
                    if (folder) {
                        folders = child(table, true, id);
                        files = child(table, id, null); //Файлы подсчитываем без учета фильтра
                        desc = "Папок "+folders+", элементов "+child(table, id, like); //В описании указываем с учетом фильтра
                    }
                    else desc = cursor.getString(cursor.getColumnIndex(DESC));
//                    items.addItem(new Items.Item(id, folder, folders, files, pater, cursor.getString(cursor.getColumnIndex(NAME)), desc));
                } while (cursor.moveToNext());
            }
        }
        finally {
            cursor.close();
        }

        return items;
    }

    //Возвращает список пунктов родителя
    public Items getItemsByPater(String table, int pater, int[] in, String like) {
        Items items = null;
        Cursor cursor;
        StringBuffer where = new StringBuffer();
        //Если верхний уровень, то возможен отбор по папкам
        if ((pater==0)&&(in!=null)&&(in.length>0)) {
            where.append(" AND " + ID + " IN (");
            for (int i=0; i<in.length; i++) if (i==0) where.append(in[i]); else where.append(","+in[i]);
            where.append(") AND " + IS_GROUP + " = 1");
        }
        //Если указана подстрока
        if ((like!=null)&&(!like.isEmpty())) where.append(" AND ( "+NAME+" LIKE '%"+like+"%' OR "+IS_GROUP+" = 1 )");

        //Добавлено, после ошибки Attempt to reopen an already-closed object sqlitedatabase, которая случается после поворота экрана.
        if (!mDB.isOpen() & mDBHelper!=null) mDB = mDBHelper.getWritableDatabase();

        //Отбор по строке в наименовании
        cursor =  mDB.rawQuery("SELECT * FROM "+table+" WHERE "+PATER+" = ?"+where.toString()+" ORDER BY 1-"+IS_GROUP+", "+NAME+" ASC;",
                new String[] {Integer.toString(pater)});
        try {
            items = new Items();
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(ID));
                    int folders = 0;
                    int files = 0;
                    String desc;
                    boolean folder = cursor.getInt(cursor.getColumnIndex(IS_GROUP))==1;
                    if (folder) {
                        folders = child(table, true, id);
                        files = child(table, id, null); //Файлы подсчитываем без учета фильтра
                        desc = "Папок "+folders+", элементов "+child(table, id, like); //В описании указываем с учетом фильтра
                    }
                    else desc = cursor.getString(cursor.getColumnIndex(DESC));
//                    items.addItem(new Items.Item(id, folder, folders, files, pater, cursor.getString(cursor.getColumnIndex(NAME)), desc));
                } while (cursor.moveToNext());
            }
        }
        finally {
            cursor.close();
        }
        return items;
    }

    // возвращает список групп родителя
    public Items getGroupsByPater(String table, int pater) {
        Items items = new Items();
        Cursor cursor = mDB.rawQuery("SELECT * FROM " + table + " WHERE " + IS_GROUP + " = 1 AND " + PATER + " = ? ORDER BY " + NAME + ";",
                new String[]{Integer.toString(pater)});
        if (cursor.moveToFirst())
            if (cursor.moveToFirst())
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(ID));
                    int folders = child(table, true, id);
                    int files = child(table, id, null); //Файлы подсчитываем без учета фильтра
//                    items.addItem(new Items.Item(id, true, folders, files, pater,
//                            cursor.getString(cursor.getColumnIndex(NAME)),
//                            "Папок " + folders + ", элементов " + files));
                } while (cursor.moveToNext());
        cursor.close();
        return items;
    }

    //Возвращает число потомков
    private int child(String sTable, boolean only_group, int id) {
        Cursor cursor = mDB.rawQuery("SELECT * FROM "+sTable+" WHERE "+(only_group?IS_GROUP+" = 1 AND ":"")+PATER+" = ?;", new String[] {Integer.toString(id)});
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    //Возвращает число элементов-потомков с отбором по наименованию
    private int child(String sTable, int id, String like) {
        String where = "";
        if (!(like==null||like.isEmpty())) where = " AND "+NAME+" LIKE '%"+like+"%'";
        Cursor cursor = mDB.rawQuery("SELECT * FROM "+sTable+" WHERE "+IS_GROUP+" = 0 AND "+PATER+" = ?"+where+";",
                new String[] {Integer.toString(id)});
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // возвращает мап-список наименований по коллекци идентификаторов
    public ArrayList<Map<String, Object>> getNamebyIds(String sTable, Collection<Integer> ids) {
        ArrayList<Map<String, Object>> analytics = new ArrayList<Map<String, Object>>(ids.size());
        if (!ids.isEmpty()) {
            StringBuffer in = new StringBuffer();
            for (int id:ids) if (in.length()==0) in.append(Integer.toString(id)); else in.append(","+Integer.toString(id));
            Cursor c = mDB.rawQuery("SELECT * FROM " + sTable + " WHERE " + ID + " IN (" + in.toString() + ") ORDER BY " + NAME + ";", null);
            if (c.moveToFirst())
                do {
                    Map<String, Object> m;
                    m = new HashMap<String, Object>();
                    m.put("id", c.getInt(c.getColumnIndex(ID)));
                    m.put("name", c.getString(c.getColumnIndex(NAME)));
                    analytics.add(m);
                } while (c.moveToNext());
            c.close();
        }
        return analytics;
    }

    // обновляет мап-список для spinner по коллекции идентификаторов
    public ArrayList<Map<String, Object>> getMapByIds(String sTable, ArrayList<Map<String, Object>> list, Collection<Integer> ids) {
        list.clear();
        Map<String, Object> m;
        if (ids.size()>0) {
            StringBuffer in = new StringBuffer();
            for (int id:ids) if (in.length()==0) in.append(id); else in.append(","+id);
            Cursor c = mDB.rawQuery("SELECT * FROM " + sTable + " WHERE " + ID + " IN (" + in.toString() + ") ORDER BY " + NAME + ";", null);
            if (c.moveToFirst())
                do {
                    m = new HashMap<String, Object>();
                    m.put("_id", c.getInt(c.getColumnIndex(ID)));
                    m.put("name", c.getString(c.getColumnIndex(NAME)));
                    list.add(m);
                } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    //МЕТОДЫ РАБОТЫ СО СТРУКТУРИРОВАННЫМИ ТАБЛИЦАМИ
    //Возвращает родителя наследника или -1, если не найден (нарушена целостность базы)
    public int getPater(String table, int id) {
        int pater = -1;
        Cursor c = mDB.rawQuery("SELECT " + PATER + " FROM " + table + " WHERE " + ID + " = ?;", new String[]{Integer.toString(id)} );
        if (c.moveToFirst()) pater = c.getInt(c.getColumnIndex(PATER));
        c.close();
        return pater;
    }

    //Возвращает прародителя наследника или -1, если не найден (нарушена целостность базы)
    private int getAncestor(String table, int id) {
        int pater = -1;
        Cursor c = mDB.rawQuery("SELECT " + PATER + " FROM " + table + " WHERE " + ID + " = ?;", new String[]{Integer.toString(id)} );
        if (c.moveToFirst()) {
            pater = c.getInt(c.getColumnIndex(PATER));
            c.close();
            if (pater==0) pater = id; //Мs достигли прародителя
            else pater = getAncestor(table, pater); //Рекурсия!!!
        }
        else c.close();
        return pater;
    }

    // удаляет записи по id. NB: Необходимо добавить проверку целостности!!!
    public void deleteRecord(String table, int id) {
            mDB.beginTransaction();
            try {
                mDB.delete(table, ID+" = ?;", new String[]{Integer.toString(id)});
                mDB.setTransactionSuccessful();
            } finally { mDB.endTransaction(); }
    }

    // изменяет поля записи
    public void updateRecord(String table, Items.Item item) {
        ContentValues cv = new ContentValues();
        cv.put(NAME, item.name);
//        cv.put(DESC, item.desc);
        cv.put(PATER, item.pater);
        mDB.beginTransaction();
        try {
//            mDB.update(table, cv,ID+" = ?;", new String[]{Integer.toString(item.id)});
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    // копирует запись
    public int copyRecord(String table, int id, int pater) {
        int newId = NOT_SELECTED;
        Cursor c = mDB.rawQuery("SELECT * FROM " + table + " WHERE " + ID + " = ?;", new String[]{Integer.toString(id)} );
        if (c.moveToFirst()) {
            ContentValues cv = new ContentValues();
            for (int column = 0; column < c.getColumnCount(); column++) {
                switch (c.getType(column)) {
                    case FIELD_TYPE_INTEGER:
                        cv.put(c.getColumnName(column), c.getInt(column));
                        break;
                    case FIELD_TYPE_STRING:
                        cv.put(c.getColumnName(column), c.getString(column));
                        break;
                    case FIELD_TYPE_FLOAT:
                        cv.put(c.getColumnName(column), c.getFloat(column));
                        break;
                }
            }
            cv.put(PATER, pater);
            mDB.beginTransaction();
            try {
                newId = (int) mDB.insert(table, null, cv);
                mDB.setTransactionSuccessful();
            } finally { mDB.endTransaction(); }

        }
        c.close();
        return newId;
    }

    // перемещает запись
    public void moveRecord(String table, int id, int pater) {
        ContentValues cv = new ContentValues();
        cv.put(PATER, pater);
        mDB.beginTransaction();
        try {
            mDB.update(table, cv,ID+" = ?;", new String[]{Integer.toString(id)});
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    // добавляет новую запись
    public int insertRecord(String table, Items.Item item) {
        return insertRecord(table, item, null, 0);
    }

    // добавляет новую запись c ключем
    public int insertRecord(String table, Items.Item item, String keyName, int key) {
        int id = NOT_SELECTED;
        ContentValues cv = new ContentValues();
        cv.put(NAME, item.name);
        cv.put(IS_GROUP, item.folder?1:0);
//        cv.put(DESC, item.desc);
        cv.put(PATER, item.pater);
        if (keyName!=null) cv.put(keyName, key);
        mDB.beginTransaction();
        try {
            id = (int) mDB.insert(table, null, cv);
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
        return id;
    }

    //МЕТОДЫ РАБОТЫ С РЕГИСТРОМ TOA
    //Возвращает массив групп аналитик из регистра по виду аудита и прародителю объекта
    public int[] getAnalyticsByTO(int type, int object) {
        int[] analytics = null;
        int ancestor = getAncestor(TBL_OBJECT,object);
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
    public Cursor getTasksByAuditor(int auditor, int status, String like) {
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

    // возвращает множество идентификаторов аналитики задания
    private Set<Integer> getAnalytics(int id) {
        Set<Integer> analitics = new ArraySet<>();
        Cursor c = mDB.rawQuery("SELECT * FROM " + TBL_ANLTASK + " WHERE " + TBL_TASKLIST + "=" + id + ";", null);
        if (c.moveToFirst()) do analitics.add(c.getInt(c.getColumnIndex(TBL_ANALYTIC))); while (c.moveToNext());
        c.close();
        return  analitics;
    }

    // возвращает задание по id или null, если не нашлось
    public AuditTask getTask(int id) {
        AuditTask task = null;
        String query = "SELECT * FROM " + TBL_TASKLIST + " WHERE " + ID + " = ?;";
        Cursor c = mDB.rawQuery( query, new String[] { String.valueOf(id) });
        if (c.moveToFirst()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(PATERN_DATE);
            Date date;
            try {
                date = dateFormat.parse(c.getString(c.getColumnIndex(DATE)));
            } catch (ParseException e) {
                e.printStackTrace();
                date = new Date();
            }

            task = new AuditTask( id, date,
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
    public void saveTask(AuditTask task) {
        ContentValues cv = new ContentValues();
        SimpleDateFormat dateFormat = new SimpleDateFormat(PATERN_DATE);
        cv.put(DATE, dateFormat.format(task.date));
        cv.put(TBL_USER, task.auditor);
        cv.put(TBL_TYPE, task.auditType);
        cv.put(TBL_OBJECT, task.object);
        cv.put(TASKLIST_STATUS, task.status);
        mDB.beginTransaction();
        try {
            //Добавляем задание или обновляем
            if (task.id== AuditTask.NEW_TASK_ID) task.id = (int) mDB.insert(TBL_TASKLIST, null, cv);
            else {
                mDB.update(TBL_TASKLIST, cv, ID + " = " + task.id, null);
                mDB.delete(TBL_ANLTASK, TBL_TASKLIST + "=" + task.id, null); //Удаляем всю аналитику задания
            }
            //Добавляем аналитику задания
            cv.clear();
            cv.put(TBL_TASKLIST,task.id);
            for(int a:task.analytics) {
                cv.put(TBL_ANALYTIC,a);
                mDB.insert(TBL_ANLTASK, null, cv);
            }
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    // установить статус задания
    public void changeStatusById(int id, int status) {
        ContentValues cv = new ContentValues();
        cv.put(TASKLIST_STATUS, status);
        mDB.beginTransaction();
        try {
            mDB.update(TBL_TASKLIST, cv, ID + " = " + id, null);
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    // удалить задание
    public void delTask(int id) {
        mDB.beginTransaction();
        try {
            mDB.delete(TBL_TASKLIST, ID + " = " + id, null);
            mDB.delete(TBL_ANLTASK, TBL_TASKLIST + "=" + id, null); //Удаляем всю аналитику задания
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    //ВСЕ ДЛЯ ОБЪЕКТОВ АУДИТА
    // возвращает объект аудита
    public AuditObject getObject(int id) {
        AuditObject object = null;

        //Ищем объект аудита в справочнике
        Cursor c = mDB.rawQuery( "SELECT * FROM " + TBL_OBJECT + " WHERE " + ID + " = ?;", new String[] {String.valueOf(id)});
        if (c.moveToFirst()) { //Создаем объект Вид аудита
            object = new AuditObject( id,
                    c.getString(c.getColumnIndex(NAME)),
                    c.getInt(c.getColumnIndex(PATER)),
                    c.getInt(c.getColumnIndex(IS_GROUP))!=0,
                    c.getString(c.getColumnIndex(DESC)));
            //Ищем связанные аналитики в регистре
            c = mDB.rawQuery( "SELECT * FROM "+TBL_REGISTER_OA+" WHERE "+TBL_OBJECT+" = ?;", new String[] {String.valueOf(id)});
            if (c.moveToFirst()) {
                do { //Добавляем связанные с объектом аналитики
                        object.addAnalytic(c.getInt(c.getColumnIndex(TBL_ANALYTIC))); //Аналитика объекта
                } while (c.moveToNext());
            }
        }
        c.close();
        return object;
    }

    // сохраняет объект аудита. Возвращает id
    public int saveObject(AuditObject object) {
        ContentValues cv = new ContentValues();
        cv.put(NAME, object.name);
        cv.put(DESC, object.desc);
        cv.put(PATER, object.pater);
        cv.put(IS_GROUP, object.is_group?1:0);
        mDB.beginTransaction();
        try {
            //Добавляем или обновляем
            if (object.id==AuditObject.NEW_TYPE_ID)
                object.id = (int) mDB.insert(TBL_OBJECT, null, cv);
            else {
                mDB.update(TBL_OBJECT, cv, ID + " = " + object.id, null);
                // чистим регистр по объекту аудита
                mDB.delete(TBL_REGISTER_OA, TBL_OBJECT + "=" + object.id, null);
            }

            cv.clear();
            cv.put(TBL_OBJECT, object.id);
            // заполняет регистр по объекту аудита связанными аналитиками
            for(Integer analytic: object.analytics) {
                cv.put(TBL_ANALYTIC, analytic);
                mDB.insert(TBL_REGISTER_OA, null, cv);
            }
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }

        return object.id;
    }

    // копирует объект аудита. Возвращает id
    public int copyObject(int id, int pater) {
        AuditObject object = getObject(id);
        object.id = AuditObject.NEW_TYPE_ID;
        object.pater = pater;
        return saveObject(object);
    }

    // удаляет объект аудита
    public void delObject(int id) {
        mDB.beginTransaction();
        try {
            mDB.delete(TBL_OBJECT, ID + " = ?;", new String[] { String.valueOf(id)});
            mDB.delete(TBL_REGISTER_OA, TBL_OBJECT + " = ?;", new String[] { String.valueOf(id)}); //Удаляем всю аналитику, связанную с объектом
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    //ВСЕ ДЛЯ АНАЛИТИКИ ОБЪЕКТОВ АУДИТА
    // возвращает аналитику объекта аудита
    public AuditAnalytic getAnalytic(int id) {
        AuditAnalytic analytic = null;

        //Ищем аналитику объекта аудита в справочнике
        Cursor c = mDB.rawQuery( "SELECT * FROM " + TBL_ANALYTIC + " WHERE " + ID + " = ?;", new String[] { String.valueOf(id) });
        if (c.moveToFirst()) { //Создаем объект Вид аудита
            analytic = new AuditAnalytic( id,
                    c.getString(c.getColumnIndex(NAME)),
                    c.getInt(c.getColumnIndex(PATER)),
                    c.getInt(c.getColumnIndex(IS_GROUP))!=0,
                    c.getString(c.getColumnIndex(DESC)));
            //Ищем связанные с аналитикой объекты в регистре
            c = mDB.rawQuery( "SELECT * FROM "+TBL_REGISTER_OA+" WHERE "+TBL_ANALYTIC+" = ?;", new String[] {String.valueOf(id)});
            if (c.moveToFirst()) {
                do { //Добавляем связанные с объектом аналитики
                    analytic.addObject(c.getInt(c.getColumnIndex(TBL_OBJECT))); //Объект аналитики
                } while (c.moveToNext());
            }
        }
        c.close();
        return analytic;
    }

    // сохраняет аналитику адита. Возвращает id
    public int saveAnalytic(AuditAnalytic analytic) {
        ContentValues cv = new ContentValues();
        cv.put(NAME, analytic.name);
        cv.put(DESC, analytic.desc);
        cv.put(PATER, analytic.pater);
        cv.put(IS_GROUP, analytic.is_group?1:0);
        mDB.beginTransaction();
        try {
            //Добавляем или обновляем
            if (analytic.id==AuditAnalytic.NEW_TYPE_ID)
                analytic.id = (int) mDB.insert(TBL_ANALYTIC, null, cv);
            else {
                mDB.update(TBL_ANALYTIC, cv, ID + " = " + analytic.id, null);
                // чистим регистр по аналитике объекта аудита
                mDB.delete(TBL_REGISTER_OA, TBL_ANALYTIC + "=" + analytic.id, null);
            }

            cv.clear();
            cv.put(TBL_ANALYTIC, analytic.id);
            // заполняет регистр по аналитике объекта аудита связанными объектами
            for(Integer object: analytic.objects) {
                cv.put(TBL_OBJECT, object);
                mDB.insert(TBL_REGISTER_OA, null, cv);
            }
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }

        return analytic.id;
    }

    // копирует аналитику объекта аудита. Возвращает id
    public int copyAnalytic(int id, int pater) {
        AuditAnalytic analytic = getAnalytic(id);
        analytic.id = AuditAnalytic.NEW_TYPE_ID;
        analytic.pater = pater;
        return saveAnalytic(analytic);
    }

    // удаляет аналитику объекта аудита
    public void delAnalytic(int id) {
        mDB.beginTransaction();
        try {
            mDB.delete(TBL_ANALYTIC, ID + " = " + id, null);
            mDB.delete(TBL_REGISTER_OA, TBL_ANALYTIC + "=" + id, null); //Удаляем все объекты, связанные с аналитикой
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    //ВСЕ ДЛЯ ВИДОВ АУДИТА
    // возвращает вид аудита по id
    public AuditType getType(int id) {
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
                    int a = c.getInt(c.getColumnIndex(TBL_ANALYTIC)); //Аналитика объекта
                    if (a == NOT_SELECTED) //У объекта нет аналитики
                        type.addObject(c.getInt(c.getColumnIndex(TBL_OBJECT)), null);
                    else
                        type.addAnalytic(c.getInt(c.getColumnIndex(TBL_OBJECT)), c.getInt(c.getColumnIndex(TBL_ANALYTIC)));
                } while (c.moveToNext());
            }
        }
        c.close();
        return type;
    }

    // сохраняет вид адита. Возвращает id
    public int saveType(AuditType type) {
        ContentValues cv = new ContentValues();
        cv.put(NAME, type.name);
        cv.put(DESC, type.desc);
        cv.put(PATER, type.pater);
        cv.put(IS_GROUP, type.is_group?1:0);
        mDB.beginTransaction();
        try {
            //Добавляем или обновляем
            if (type.id==AuditType.NEW_TYPE_ID)
                type.id = (int) mDB.insert(TBL_TYPE, null, cv);
            else {
                mDB.update(TBL_TYPE, cv, ID + " = " + type.id, null);
                // чистим регистр по виду аудита
                mDB.delete(TBL_REGISTER_TOA, TBL_TYPE + "=" + type.id, null);
            }

            cv.clear();
            cv.put(TBL_TYPE, type.id);
            // заполняет регистр по виду адита
            for(Integer key: type.objects.keySet()) {
                cv.put(TBL_OBJECT, key);
                Set<Integer> set = type.objects.get(key);
                if (set.isEmpty()) {
                    cv.put(TBL_ANALYTIC, NOT_SELECTED); //Объект без аналитики
                    mDB.insert(TBL_REGISTER_TOA, null, cv);
                } else
                    for (Integer a: set) {
                        cv.put(TBL_ANALYTIC, a);
                        mDB.insert(TBL_REGISTER_TOA, null, cv);
                    }
            }
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }

        return type.id;
    }

    // копирует вид аудита. Возвращает id
    public int copyType(int id, int pater) {
        AuditType type = getType(id);
        type.id = AuditType.NEW_TYPE_ID;
        type.pater = pater;
        return saveType(type);
    }

    // удаляет вид аудита
    public void delType(int id) {
        mDB.beginTransaction();
        try {
            mDB.delete(TBL_TYPE, ID + " = " + id, null);
            mDB.delete(TBL_REGISTER_TOA, TBL_TYPE + "=" + id, null); //Удаляем всю аналитику задания
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
    }

    //ВСЕ ДЛЯ ПРЕДМЕТОВ АУДИТА
    // возвращает предмет аудита по id
    public AuditSubject getSubject(int id) {
        AuditSubject subject = null;
        //Ищем вид аудита в справочнике
        Cursor c = mDB.rawQuery( "SELECT * FROM " + TBL_SUBJECT + " WHERE " + ID + " = ?;", new String[] { String.valueOf(id) });
        if (c.moveToFirst()) { //Создаем объект Вид аудита
            subject = new AuditSubject( id,
                    c.getInt(c.getColumnIndex(TBL_TYPE)),
                    c.getInt(c.getColumnIndex(PATER)),
                    c.getInt(c.getColumnIndex(IS_GROUP))!=0,
                    c.getString(c.getColumnIndex(NAME)),
                    c.getString(c.getColumnIndex(DESC)),
                    c.getInt(c.getColumnIndex(SUBJECT_CRITERION)),
                    c.getFloat(c.getColumnIndex(SUBJECT_VALUE)));
        }
        c.close();
        return subject;
    }

    // сохраняет предмет адита. Возвращает id
    public int saveSubject(AuditSubject subject) {
        ContentValues cv = new ContentValues();
        cv.put(NAME, subject.name);
        cv.put(DESC, subject.desc);
        cv.put(PATER, subject.pater);
        cv.put(IS_GROUP, (int) (subject.is_group?1:0));
        cv.put(SUBJECT_CRITERION, subject.criterion);
        cv.put(SUBJECT_VALUE, subject.value);
        mDB.beginTransaction();
        try {
            //Добавляем или обновляем
            if (subject.id==AuditSubject.NEW_SUBJECT_ID) subject.id = (int) mDB.insert(TBL_SUBJECT, null, cv);
            else mDB.update(TBL_SUBJECT, cv, ID + " = " + subject.id, null);
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }

        return subject.id;
    }

    // копирует предмет аудита. Возвращает id
    public int copySubject(int id, int pater) {
        AuditSubject subject = getSubject(id);
        subject.id = AuditSubject.NEW_SUBJECT_ID;
        subject.pater = pater;
        return saveSubject(subject);
    }

    // удаляет предмет аудита
    public void delSubject(int id) {
        mDB.beginTransaction();
        try {
            mDB.delete(TBL_SUBJECT, ID + " = " + id, null);
            mDB.setTransactionSuccessful();
        } finally { mDB.endTransaction(); }
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
            db.execSQL(TBL_REGISTER_OA_CREATE); //Регистр для связывания Объект+Аналитика
            db.execSQL(TBL_TASKLIST_CREATE); //Задания
            db.execSQL(TBL_ANLTASK_CREATE); //Аналитика задания
            db.execSQL(TBL_SUBJECT_CREATE); //Предметы аудита
            db.execSQL(TBL_INDICATOR_CREATE); //Показатели аудита
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
                cv.put(NAME, "Вид "+id);
                cv.put(IS_GROUP, false);
                cv.put(PATER, 0);
                db.insert(TBL_TYPE, null, cv);
                type[i-1]=id;
            }
            for (int j = 1; j <= 2; j++) {
                int gr = ++id;
                cv.put(NAME, "Группа видов "+id);
                cv.put(IS_GROUP, 1);
                cv.put(PATER, 0);
                db.insert(TBL_TYPE, null, cv);
                for (int i2 = 1; i2 <= 3; i2++) {
                    ++id;
                    cv.put(NAME, "Вид "+id);
                    cv.put(IS_GROUP, 0);
                    cv.put(PATER, gr);
                    db.insert(TBL_TYPE, null, cv);
                }
                for (int j1 = 1; j1 <= 2; j1++) {
                    int gr1 = ++id;
                    cv.put(NAME, "Группа видов "+id);
                    cv.put(IS_GROUP, 1);
                    cv.put(PATER, gr);
                    db.insert(TBL_TYPE, null, cv);
                    for (int i1 = 1; i1 <= 3; i1++) {
                        ++id;
                        cv.put(NAME, "Вид "+id);
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
                cv.put(NAME, "Объект "+id);
                cv.put(IS_GROUP, false);
                cv.put(PATER, 0);
                db.insert(TBL_OBJECT, null, cv);
            }
            for (int j = 1; j <= 5; j++) {
                int gr = ++id;
                cv.put(NAME, "Группа объектов "+id);
                cv.put(IS_GROUP, 1);
                cv.put(PATER, 0);
                db.insert(TBL_OBJECT, null, cv);
                object[j-1]=id;
                for (int i = 1; i <= 10; i++) {
                    ++id;
                    cv.put(NAME, "Объект "+id);
                    cv.put(IS_GROUP, 0);
                    cv.put(PATER, gr);
                    db.insert(TBL_OBJECT, null, cv);
                }
                for (int j1 = 1; j1 <= 2; j1++) {
                    int gr1 = ++id;
                    cv.put(NAME, "Группа объектов "+id);
                    cv.put(IS_GROUP, 1);
                    cv.put(PATER, gr);
                    db.insert(TBL_OBJECT, null, cv);
                    for (int i1 = 1; i1 <= 2; i1++) {
                        ++id;
                        cv.put(NAME, "Объект "+id);
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
                cv.put(NAME, "Аналитика "+id );
                cv.put(IS_GROUP, false);
                cv.put(PATER, 0);
                db.insert(TBL_ANALYTIC, null, cv);
            }
            for (int j = 1; j <= 5; j++) {
                int gr = ++id;
                cv.put(NAME, "Группа аналитики "+id);
                cv.put(IS_GROUP, 1);
                cv.put(PATER, 0);
                db.insert(TBL_ANALYTIC, null, cv);
                analytic[j-1]=id;
                for (int i = 1; i <= 10; i++) {
                    ++id;
                    cv.put(NAME, "Аналитика "+id);
                    cv.put(IS_GROUP, 0);
                    cv.put(PATER, gr);
                    db.insert(TBL_ANALYTIC, null, cv);
                }
                for (int j1 = 1; j1 <= 2; j1++) {
                    int gr1 = ++id;
                    cv.put(NAME, "Группа аналитики "+id);
                    cv.put(IS_GROUP, 1);
                    cv.put(PATER, gr);
                    db.insert(TBL_ANALYTIC, null, cv);
                    for (int i1 = 1; i1 <= 2; i1++) {
                        ++id;
                        cv.put(NAME, "Аналитика "+id);
                        cv.put(IS_GROUP, 0);
                        cv.put(PATER, gr1);
                        db.insert(TBL_ANALYTIC, null, cv);
                    }
                }
            }

            //Заполняем регистр Вид+Объект+Аналитика
//            cv.clear();
//            for(int i=0;i<5;i++)
//                for(int j=0;j<5;j++) {
//                    int t = analytic[j]+object[i];
//                    cv.put(TBL_TYPE, type[t-t/2*2] );
//                    cv.put(TBL_OBJECT, object[t-t/5*5]);
//                    cv.put(TBL_ANALYTIC, analytic[j]);
//                    db.insert(TBL_REGISTER_TOA, null, cv);
//                }

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
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_REGISTER_OA);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_TASKLIST);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_ANLTASK);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_SUBJECT);
                    db.execSQL("DROP TABLE IF EXISTS " + TBL_INDICATOR);
                    this.onCreate(db);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        }
    }
}
