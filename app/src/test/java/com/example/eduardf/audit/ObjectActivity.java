package com.example.eduardf.audit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TabHost;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 14.12.18 14:22
 *
 */

//Форма для редактирования объекта аудита
public class ObjectActivity extends AppCompatActivity implements
        /*Analytics.OnObjectListEditInteractionListener,*/
        ReferenceChoice.OnReferenceManagerInteractionMultipleChoice {

    private AuditObject object; //Объект аудита
    private AuditDB db; //База данных

    //Режимы открытия формы
    private final static int CREATE_MODE = -1; //создание нового вида аудита
    private final static int EDIT_MODE = 0; //редактирвоание существующего вида аудита

    //Аргументы интент
    private final static String ARG_MODE = "mode"; //Режим
    private final static String ARG_ID = "id"; //Идентификатор объекта аудита
    private final static String ARG_PATER = "pater"; //Родитель объекта аудита

    //Аргументы для поворота экрана
    private final static String ARG_TAG = "tag"; //Текущая закладка
    private final static String ARG_ANALYTICS = "analytics"; //Связанная с объектом аналитика

    //Не указанное Id
    private final static int NOT_SELECTED = -1;

    // Тэги закладок
    private final static String TAG1 = "tag1";
    private final static String TAG2 = "tag2";

    // возвращает Интент для создания нового объекта аудита
    public static Intent intentActivityCreate(Context context, int pater) {
        Intent intent = new Intent(context, ObjectActivity.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_PATER, pater);
        return intent;
    }

    // возвращает Интент для изменения существующего объекта аудита
    public static Intent intentActivityEdit(Context context, int id) {
        Intent intent = new Intent(context, ObjectActivity.class);
        intent.putExtra(ARG_MODE, EDIT_MODE);
        intent.putExtra(ARG_ID, id);
        return intent;
    }

    // создает активность для редактирования объекта аудита
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Кнопка Сохранить
        FloatingActionButton save = (FloatingActionButton) findViewById(R.id.saveobject);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                object.name = ((EditText) findViewById(R.id.objectname)).getText().toString();
                object.desc = ((EditText) findViewById(R.id.objectdesc)).getText().toString();
                db.saveObject(object);
                finish();
            }
        });

        //ВСЕ ДЛЯ ЗАКЛАДОК
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec tabSpec;
        //Общее
        tabSpec = tabHost.newTabSpec(TAG1);
        tabSpec.setIndicator(getString(R.string.tab_com));
        tabSpec.setContent(R.id.tab1);
        tabHost.addTab(tabSpec);
        //Аналитика
        tabSpec = tabHost.newTabSpec(TAG2);
        tabSpec.setIndicator(getString(R.string.tab_anl));
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);
        //Обработчик закладок
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {/* update(tabId); */}
        } );

        //Кнопка Добавить аналитику в список
        ImageButton addAnalytic = (ImageButton) findViewById(R.id.addanalytics);
        addAnalytic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Integer> analytics = new ArrayList<>(object.analytics.size());
                analytics.addAll(object.analytics);
//                startActivity(ReferenceChoice.intentActivity(ObjectActivity.this, 0, AuditDB.TBL_ANALYTIC, "Аналитика объекта", analytics));
            }
        });

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Восстанавливаем основные параметры
        if (savedInstanceState==null) { // открываем форму впервые
            Intent intent = getIntent();
            switch (intent.getIntExtra(ARG_MODE, CREATE_MODE)) {
                case CREATE_MODE: //режим создания нового вида аудита
                    object = new AuditObject(AuditObject.NEW_TYPE_ID, "", intent.getIntExtra(ARG_PATER, NOT_SELECTED), false, "");
                    break;
                case EDIT_MODE: //режим редактирования существующего вида аудита
                    object = db.getObject(intent.getIntExtra(ARG_ID, NOT_SELECTED));
//                    if (type == null)
//                        Snackbar.make(findViewById(R.id.date), R.string.msg_not_find_id, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    break;
            }
            tabHost.setCurrentTabByTag(TAG1); // Закладка по умолчанию
            //Заполняем вкладку Общее текущими значениями
            ((EditText) findViewById(R.id.objectname)).setText(object.name); //наименование1
            ((EditText) findViewById(R.id.objectdesc)).setText(object.desc); //описание
        }
        else { // открываем после поворота. Восстанавливаем:
            object = new AuditObject(savedInstanceState.getInt(ARG_ID), //вид аудита
                    ((EditText) findViewById(R.id.objectname)).getText().toString(),
                    savedInstanceState.getInt(ARG_PATER),
                    false, //только элементы, группы редактируются диалогом
                    ((EditText) findViewById(R.id.objectdesc)).getText().toString());
            tabHost.setCurrentTabByTag(savedInstanceState.getString(ARG_TAG, TAG1)); //текущая вкладка
            getObjectAnalytics(savedInstanceState.getIntArray(ARG_ANALYTICS)); //текущий набор аналитик
        }
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_ID, object.id); //идентификатор вида аудита
        outState.putInt(ARG_PATER, object.pater); //родителя вида аудита
        outState.putString(ARG_TAG, ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag()); //текущую закладку
        outState.putIntArray(ARG_ANALYTICS, putObjectAnalytics()); //текущий набор аналитик
    }

    // восстанавливает список с аналитикой
    private void getObjectAnalytics(int[] in) { for (int id: in) object.addAnalytic(id); }

    // возвращает массив с аналитикой объекта
    private int[] putObjectAnalytics() {
        int[] result = new int[object.analytics.size()];
        int i = 0;
        for (Integer id:  object.analytics) result[i++] = id;
        return result;
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close(); // закрываем подключение при выходе
    }

    //Обработчик возврата назад
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // обработчик удаления аналитики из фрагмента
//    @Override
//    public void onObjectListEditInteractionListener(int requested, int id) { object.analytics.remove(id); }

    // обновляет фрагмент с аналитикой
    @Override
    public void onResume() {
        super.onResume();
        getSupportFragmentManager().
                beginTransaction().
//                replace(R.id.objectanalytics,
//                        Analytics.newInstance(0, AuditDB.TBL_ANALYTIC, object.analytics)).
                commit();
    }

    // обработчик добавления аналитик
//    @Override
//    public void onReferenceManagerInteractionChoose(Context context, int requestCode, List<Integer> ids) { object.analytics.addAll(ids); }

    @Override
    public void onReferenceManagerInteractionMultipleChoice(int requestCode, Items items) {

    }
}
