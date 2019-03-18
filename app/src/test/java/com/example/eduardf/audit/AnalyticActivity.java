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

//Форма для редактирования аналитики объекта аудита
public class AnalyticActivity extends AppCompatActivity implements
        /*Objects.OnObjectListEditInteractionListener,*/
        ReferenceChoice.OnReferenceManagerInteractionMultipleChoice {

    private AuditAnalytic analytic; //Аналитика объекта аудита
    private AuditDB db; //База данных

    //Режимы открытия формы
    private final static int CREATE_MODE = -1; //создание новой аналитики объекта аудита
    private final static int EDIT_MODE = 0; //редактирвоание существующей аналитики объекта аудита

    //Аргументы интент
    private final static String ARG_MODE = "mode"; //Режим
    private final static String ARG_ID = "id"; //Идентификатор аналитики объекта аудита
    private final static String ARG_PATER = "pater"; //Родитель аналитики объекта аудита

    //Аргументы для поворота экрана
    private final static String ARG_TAG = "tag"; //Текущая закладка
    private final static String ARG_OBJECTS = "objects"; //Связанные с аналитикой объекты

    //Не указанное Id
    private final static int NOT_SELECTED = -1;

    // Тэги закладок
    private final static String TAG1 = "tag1";
    private final static String TAG2 = "tag2";

    // возвращает Интент для создания новой аналитики объекта аудита
    public static Intent intentActivityCreate(Context context, int pater) {
        Intent intent = new Intent(context, AnalyticActivity.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_PATER, pater);
        return intent;
    }

    // возвращает Интент для изменения существующей аналитики объекта аудита
    public static Intent intentActivityEdit(Context context, int id) {
        Intent intent = new Intent(context, AnalyticActivity.class);
        intent.putExtra(ARG_MODE, EDIT_MODE);
        intent.putExtra(ARG_ID, id);
        return intent;
    }

    // создает активность для редактирования аналитики объекта аудита
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytic);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Кнопка Сохранить
        FloatingActionButton save = (FloatingActionButton) findViewById(R.id.saveanalytic);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                analytic.name = ((EditText) findViewById(R.id.analyticname)).getText().toString();
                analytic.desc = ((EditText) findViewById(R.id.analyticdesc)).getText().toString();
                db.saveAnalytic(analytic);
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
        tabSpec.setIndicator(getString(R.string.tab_obj));
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);
        //Обработчик закладок
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {/* update(tabId); */}
        } );

        //Кнопка Добавить объекты в список
        ImageButton addObjects = (ImageButton) findViewById(R.id.addobjects);
        addObjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Integer> objects = new ArrayList<>(analytic.objects.size());
                objects.addAll(analytic.objects);
//                startActivity(ReferenceChoice.intentActivity(AnalyticActivity.this, 0, AuditDB.TBL_OBJECT, "Объекты аналитики", objects));
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
                    analytic = new AuditAnalytic(AuditObject.NEW_TYPE_ID, "", intent.getIntExtra(ARG_PATER, NOT_SELECTED), false, "");
                    break;
                case EDIT_MODE: //режим редактирования существующего вида аудита
                    analytic = db.getAnalytic(intent.getIntExtra(ARG_ID, NOT_SELECTED));
//                    if (type == null)
//                        Snackbar.make(findViewById(R.id.date), R.string.msg_not_find_id, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    break;
            }
            tabHost.setCurrentTabByTag(TAG1); // Закладка по умолчанию
            //Заполняем вкладку Общее текущими значениями
            ((EditText) findViewById(R.id.analyticname)).setText(analytic.name); //наименование
            ((EditText) findViewById(R.id.analyticdesc)).setText(analytic.desc); //описание
        }
        else { // открываем после поворота. Восстанавливаем:
            analytic = new AuditAnalytic(savedInstanceState.getInt(ARG_ID), //аналитика
                    ((EditText) findViewById(R.id.analyticname)).getText().toString(),
                    savedInstanceState.getInt(ARG_PATER),
                    false, //только элементы, группы редактируются диалогом
                    ((EditText) findViewById(R.id.analyticdesc)).getText().toString());
            tabHost.setCurrentTabByTag(savedInstanceState.getString(ARG_TAG, TAG1)); //текущая вкладка
            getAnalyticObjects(savedInstanceState.getIntArray(ARG_OBJECTS)); //текущий набор объектов
        }
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_ID, analytic.id); //идентификатор аналитики
        outState.putInt(ARG_PATER, analytic.pater); //родитель аналитики
        outState.putString(ARG_TAG, ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag()); //текущая закладка
        outState.putIntArray(ARG_OBJECTS, putAnalyticObjects()); //текущий набор объектов
    }

    // восстанавливает список с объектами аналитики
    private void getAnalyticObjects(int[] in) { for (int id: in) analytic.addObject(id); }

    // возвращает массив с объектами аналитики
    private int[] putAnalyticObjects() {
        int[] result = new int[analytic.objects.size()];
        int i = 0;
        for (Integer id:  analytic.objects) result[i++] = id;
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

    // обработчик удаления объекта из фрагмента
//    @Override
//    public void onObjectListEditInteractionListener(int requested, int id) { analytic.objects.remove(id); }

    // обновляет фрагмент с объектами
    @Override
    public void onResume() {
        super.onResume();
//        getSupportFragmentManager().beginTransaction().replace(R.id.analyticobjects, Objects.newInstance(0, AuditDB.TBL_OBJECT, analytic.objects)).commit();
    }

    // обработчик добавления аналитик
//    @Override
//    public void onReferenceManagerInteractionChoose(Context context, int requestCode, List<Integer> ids) { analytic.objects.addAll(ids); }

    @Override
    public void onReferenceManagerInteractionMultipleChoice(int requestCode, Items items) {

    }
}
