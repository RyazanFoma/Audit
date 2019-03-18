package com.example.eduardf.audit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 04.02.19 15:26
 *
 */

//Форма для редактирвоания задания на аудит
public class TaskActivity extends AppCompatActivity implements
        IndicatorFragment.OnScrollUpListener,
        LoadIndicatorRows.OnLoadIndicatorRowsExecute,
        SaveTask.OnSaveTaskExecute,
        LoaderManager.LoaderCallbacks<Tasks.Task> {

    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private Tasks.Task task = new Tasks.Task(); //Задание

    //Поля для выбора
    private Reference typeReference;
    private Reference organizationReference;
    private Reference objectReference;
    private Reference responsibleReference;
    private Objects analyticsObjects;
    private IndicatorFragment indicatorFragment;
    private DateTime dateTime;

    //Режимы открытия формы
    private final static int CREATE_MODE = 0; //создание нового задания аудита
    private final static int EDIT_MODE = 1; //редактирвоание существующего задания аудита

    //Аргументы интент и поворота экрана
    private final static String ARG_MODE = "mode"; //Режим открытия формы
    private final static String ARG_AUDITOR = "auditor"; //Аудитор задания на аудит
    private final static String ARG_ID = "id"; //Идентификатор задания на аудит
    private final static String ARG_NUMBER = "number"; //Номер задания на аудит
    private final static String ARG_DATE = "date"; //Дата задания на аудит
    private final static String ARG_STATUS = "status"; //Статус задания на аудит
    private final static String ARG_TYPE = "type"; //Наименование вида аудита
    private final static String ARG_OBJECT = "object"; //Наименование объекта аудита
    private final static String ARG_TASK = "task"; //Задание
    private final static String ARG_TAG = "tag"; //Текущая закладка
    private static final String ARG_FILLED = "filled"; //Признаки заполнения закладок

    // Тэги закладок
    private final static String TAG0 = "tag0";
    private final static String TAG1 = "tag1";
    private final static String TAG2 = "tag2";
    private final static String TAG3 = "tag3";
    private boolean[] filledTag = {false, false, false}; // Признаки заполнения закладок

    // возвращает Интент для создания нового задания на аудит
    public static Intent intentActivityCreate(Context context, String auditor, Tasks.Task.Status status) {
        Intent intent = new Intent(context, TaskActivity.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_STATUS, status.number);
        intent.putExtra(ARG_AUDITOR, auditor);
        return intent;
    }

    // возвращает Интент для изменения существующего задания на аудит
    public static Intent intentActivityEdit(Context context, Tasks.Task task) {
        Intent intent = new Intent(context, TaskActivity.class);
        intent.putExtra(ARG_MODE, EDIT_MODE);
        intent.putExtra(ARG_ID, task.id);
        intent.putExtra(ARG_NUMBER, task.number);
        intent.putExtra(ARG_DATE, task.date.getTime());
        intent.putExtra(ARG_STATUS, task.status.number);
        intent.putExtra(ARG_TYPE, task.type_name);
        intent.putExtra(ARG_OBJECT, task.object_name);
        return intent;
    }

    // создает активность для редактирвания задания на аудит
    @SuppressLint("ClickableViewAccessibility") //Чтобы для onTouch не возникало предупреждение о performClick
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        //Лента инструментов
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp
        }

        //ВСЕ ДЛЯ ВЫБОРА СТАТУСА
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.status_list));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner status = findViewById(R.id.status);
        status.setAdapter(adapter);
        status.setOnItemSelectedListener(new  AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (task!=null) task.status = Tasks.Task.Status.toValue(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //Кнопка Сохранить
        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { saveTask(); }
        });

        //ВСЕ ДЛЯ ВКЛАДОК
        final String tab; //Вкладка для открытия
        final TabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec tabSpec;
        tabSpec = tabHost.newTabSpec(TAG1);
        tabSpec.setIndicator(getString(R.string.tab_com));
        tabSpec.setContent(R.id.tab1);
        tabHost.addTab(tabSpec);
        tabSpec = tabHost.newTabSpec(TAG2);
        tabSpec.setIndicator(getString(R.string.tab_anl));
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);
        tabSpec = tabHost.newTabSpec(TAG3);
        tabSpec.setIndicator(getString(R.string.tab_ind));
        tabSpec.setContent(R.id.tab3);
        tabHost.addTab(tabSpec);

        //Находим ссылки на поля настроек
        typeReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.type);
        typeReference.setOnChangedReferenceKey(new Reference.OnChangedReferenceKey() {
            @Override
            public void onChangedKey(int id, String key, Object object) {
                if (key == null || !key.equals(task.type_key)) {
                    //Возможно, потребуется очистить объект, аналитику и показатели
                    //((AType) objectReference.object).objectTypes.contains()
                    //Нужно предупредить юзера об ощищении полей
                    task.type_key = key;
                    objectReference.setKey(null);
                    task.analytics.clear();
                    analyticsObjects.setObjects(null);
                    task.indicators.clear();
                    indicatorFragment.setIndicators(task.indicators, task.type_key,
                            ((Switch) findViewById(R.id.by_subject)).isChecked());
                    for(int i=0; i<3; i++) filledTag[i] = false;
                    visibilityView();
                }
            }
        });
        organizationReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.organization);
        objectReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.object);
        objectReference.setOnChangedReferenceKey(new Reference.OnChangedReferenceKey() {
            @Override
            public void onChangedKey(int id, String key, Object object) {
                if (key == null || !key.equals(task.object_key)) {
                    //Возможно, потребуется очистить аналитику и показатели
                    task.object_key = key;
                    task.analytics.clear();
                    analyticsObjects.setObjects(null);
                    task.indicators.clear();
                    indicatorFragment.setIndicators(task.indicators, task.type_key,
                            ((Switch) findViewById(R.id.by_subject)).isChecked());
                    for(int i=0; i<3; i++) filledTag[i] = false;
                    visibilityView();
                }
            }
        });
        responsibleReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.responsible);
        analyticsObjects = (Objects) getSupportFragmentManager().findFragmentById(R.id.analytics);
        indicatorFragment = (IndicatorFragment) getSupportFragmentManager().findFragmentById(R.id.indicators);
        dateTime = (DateTime) getSupportFragmentManager().findFragmentById(R.id.datetime);

        //Создает объект OData
        oData = new AuditOData(this);

        if (savedInstanceState==null) { // открываем форму впервые
            //Создаем объект Задание в зависимости от режима
            final Intent intent = getIntent();
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            //Нужно проверять еще и вид аудита!!!
            tab = preferences.getString(getString(R.string.pref_key_task_tab), TAG1);
            switch (intent.getIntExtra(ARG_MODE, 0)){
                case CREATE_MODE:
                    task.date = new Date();
                    task.status = Tasks.Task.Status.toValue(intent.getIntExtra(ARG_STATUS, Tasks.Task.Status.APPROVED.number));
                    task.auditor_key = intent.getStringExtra(ARG_AUDITOR);
                    task.number = "Новое";
                    task.comment = "";
                    //Восстановить из значений по умолчанию - индивидуальные настройки пользователя
                    task.type_key = preferences.getString(SettingTask.DEFAULT_TYPE, AuditOData.EMPTY_KEY);
                    task.organization_key = preferences.getString(SettingTask.DEFAULT_ORGANIZATION, AuditOData.EMPTY_KEY);
                    task.object_key = preferences.getString(SettingTask.DEFAULT_OBJECT, AuditOData.EMPTY_KEY);
                    task.responsible_key = preferences.getString(SettingTask.DEFAULT_RESPONSIBLE, AuditOData.EMPTY_KEY);
                    fillViews(TAG0); //Заполняем общие поля
                    fillViews(tab); //и текущую вкладку
                    break;
                case EDIT_MODE:
                    //Перед загрузкой заполним общие поля данными из списка заданий
                    task.number = intent.getStringExtra(ARG_NUMBER);
                    task.date = new Date();
                    task.date.setTime(intent.getLongExtra(ARG_DATE, 0L));
                    task.status = Tasks.Task.Status.toValue(intent.getIntExtra(ARG_STATUS, Tasks.Task.Status.APPROVED.number));
                    task.type_name = intent.getStringExtra(ARG_TYPE);
                    task.object_name = intent.getStringExtra(ARG_OBJECT);
                    task.comment = "";
                    fillViews(TAG0); //Заполняем общие поля
                    //Запускаем загрузчик для чтения данных.
                    startLoader(intent.getExtras());
                    break;
            }
        }
        else { // открываем после поворота.
            tab = savedInstanceState.getString(ARG_TAG); //Текущая влкадка до поворота
            //Успели загрузить-сохранить задание?
            if (savedInstanceState.containsKey(ARG_TASK)) { //Восстанавливаем:
                task.onRestoreInstanceState(savedInstanceState, ARG_TASK);
                //В заголовок активности добавим номер
                setTitle(getString(R.string.title_activity_audit_task)+" ("+task.number+")");
                filledTag = savedInstanceState.getBooleanArray(ARG_FILLED);
            }
            else { //Не успели - грузим по новой
                startLoader(getIntent().getExtras());
            }
        }

        //Устанавливаем текущую вкладку
        tabHost.setCurrentTabByTag(tab);
        //Назначаем обработчик на выбор вкладки
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (!getSupportLoaderManager().hasRunningLoaders())
                    fillViews(tabId);
            }
        });

        //Обработчик очистки комментария
        findViewById(R.id.comment).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    final TextView textView = (TextView)v;
                    if(event.getX() >= textView.getWidth() - textView.getCompoundPaddingEnd()) {
                        textView.setText("");
                        return true;
                    }
                }
                return false;
            }
        });

        //Обработчик кнопки Заполнить
        findViewById(R.id.fill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LoadIndicatorRows(TaskActivity.this, oData)
                        .execute(task.type_key, task.object_key);
            }
        });

        //Обработчик переключателя По предметам
        ((Switch) findViewById(R.id.by_subject)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed())
                    updateIndicators();
            }
        });

    }

    //Запускает одноразовый загрузчик задания
    private void startLoader(Bundle arg) {
        Loader loader = getSupportLoaderManager().getLoader(-1);
        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(-1, new Bundle(arg), this);
        else getSupportLoaderManager().initLoader(-1, new Bundle(arg), this);
        //Загрузчик отработает, только при первом открытии задания, потом будет убит в onLoadFinished
        //Там же будет вызван fillViews(""); - Общие поля и первая закладка
    }

    //Заполняет поля активности
    private void fillViews(String tabId) {
        switch (tabId) {
            case TAG0: //Поля вне закладок + часть полей с Общее
                setTitle(getString(R.string.title_activity_audit_task)+" ("+task.number+")"); //В заголовок добавим номер
                dateTime.setDate(task.date);
                ((Spinner) findViewById(R.id.status)).setSelection(task.status.number);
                ((EditText) findViewById(R.id.comment)).setText(task.comment);
                //Иконки
                findViewById(R.id.deleted).setVisibility(task.deleted?View.VISIBLE:View.GONE);
                findViewById(R.id.posted).setVisibility(task.posted?View.VISIBLE:View.GONE);
                if (!task.deleted && task.posted && task.status == Tasks.Task.Status.IN_WORK) {
                    findViewById(R.id.thumb).setVisibility(View.VISIBLE);
                    ((ImageView) findViewById(R.id.thumb)).setImageResource(task.achieved?
                            R.drawable.ic_black_thumb_up_alt_24px:
                            R.drawable.ic_black_thumb_down_alt_24px);
                }
                else findViewById(R.id.thumb).setVisibility(View.GONE);
                //Подставим в поля наименования вида и объекта аудита до загрузки
                typeReference.setText(task.type_name);
                objectReference.setText(task.object_name);
                return; //Чтобы не выполнять com.example.eduardf.audit.TaskActivity.visibilityView
            case TAG1: //поля закладки Общее
                if (!filledTag[0] ) {
                    filledTag[0] = true;
                    typeReference.setKey(task.type_key);
                    organizationReference.setKey(task.organization_key);
                    objectReference.setKey(task.object_key);
                    responsibleReference.setKey(task.responsible_key);
                    visibilityView();
                }
                break;
            case TAG2: //поля закладки Аналитика
                if (!filledTag[1]) {
                    filledTag[1] = true;
                    analyticsObjects.setObjects(task.analytics);
                    visibilityView();
                }
                break;
            case TAG3: //поля закладки Показатели
                if (!filledTag[2]) {
                    filledTag[2] = true;
                    indicatorFragment.setIndicators(task.indicators, task.type_key,
                            ((Switch) findViewById(R.id.by_subject)).isChecked());
                    visibilityView();
                }
                break;
        }
    }

    // управляет доступностью выбора объекта и аналитики
    private void visibilityView() {
        final boolean enabled = !(typeReference == null || typeReference.getReferenceKey() == null);
        objectReference.setEnabled(enabled);
        analyticsObjects.setEnabled(enabled);
        findViewById(R.id.fill).setEnabled(enabled);
        findViewById(R.id.by_subject).setEnabled(enabled);
        indicatorFragment.setEnabled(enabled);
    }

    //Обновляет фрагмент с показателями
    private void updateIndicators() {
        indicatorFragment.setIndicators(task.indicators, task.type_key,
                ((Switch) findViewById(R.id.by_subject)).isChecked());
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (task != null) task.onSaveInstanceState(outState, ARG_TASK);
        outState.putBooleanArray(ARG_FILLED, filledTag);
        outState.putString(ARG_TAG, ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag()); //текущую закладку восстанавливаем в Create()
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        oData = null;
        task = null;
    }

    //Обработчик возврата назад
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //ВСЕ ДЛЯ ИНСТРУМЕНТАЛЬНОГО МЕНЮ:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_audit_task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                saveTask();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Обработчик скроллинга показателей
    @Override
    public void onScrollUpListener(int visibility) {
        findViewById(R.id.head).setVisibility(visibility);
        findViewById(android.R.id.tabs).setVisibility(visibility);
    }

    /**
     * Сохраняет задание
     */
    private void saveTask() {
        //НУЖНО ДОБАВИТЬ ПРОВЕРКУ ЗАПОЛНЕНИЯ ОБЯЗАТЕЛЬНЫХ ПОЛЕЙ!!!
        task.date = dateTime.getDate();
        if (filledTag[0]) {
            task.type_key = typeReference.getReferenceKey();
            task.organization_key = organizationReference.getReferenceKey();
            task.object_key = objectReference.getReferenceKey();
            task.responsible_key = responsibleReference.getReferenceKey();
            task.comment = ((EditText) findViewById(R.id.comment)).getText().toString();
        }
        if (filledTag[1]) task.analytics = analyticsObjects.getObjectKeys();
        if (filledTag[2]) task.indicators = indicatorFragment.getIndicators();
        new SaveTask(this, oData).execute(task);
    }

    /**
     * Вызывается до начала сохранения задания
     */
    @Override
    public void onSaveTaskPreExecute() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    /**
     * Вызывается после окончания сохранения задания
     */
    @Override
    public void onSaveTaskPostExecute() {
        Intent intent = new Intent();
        intent.putExtra(ARG_STATUS, task.status.number);
        setResult(RESULT_OK, intent);
        finish(); //Закрываем активность
    }

    //Загрузчик задания
    @NonNull
    @Override
    public Loader<Tasks.Task> onCreateLoader(int id, @Nullable Bundle args) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        if (args != null)
            return new LoadTask(TaskActivity.this, oData, args.getString(ARG_ID));
        else
            throw new RuntimeException("Is no arguments for loader");
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Tasks.Task> loader, Tasks.Task data) {
        task = data;
        fillViews(TAG0); //Заполним общие поля
        //и текущую закладку
        String tab = ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag();
        if (tab == null) tab = TAG1; //Или первую, если все плохо
        fillViews(tab);
        //Убиваем загрузчик, т.к. он нужен только на один раз при первом отрытии задания
        getSupportLoaderManager().destroyLoader(-1);
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Tasks.Task> loader) {
        task = null;
    }

    /**
     * Вызывается до заполнения показателей
     */
    @Override
    public void onLoadIndicatorRowsPreExecute() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    /**
     * Вызывается после загрузки показателей
     * @param rows - загруженный список показателей задания
     */
    @Override
    public void onLoadIndicatorRowsPostExecute(ArrayList<Tasks.Task.IndicatorRow> rows) {
        task.indicators.clear();
        task.indicators.addAll(rows);
        updateIndicators(); //обновим Показатели
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    //Асинхронный загрузчик задания
    private static class LoadTask extends AsyncTaskLoader<Tasks.Task> {
        final AuditOData oData;
        final String key;
        private LoadTask(@NonNull Context context, AuditOData oData, String key) {
            super(context);
            this.oData = oData;
            this.key = key;
        }
        @Override
        protected void onStartLoading() {
            forceLoad();
        }
        @Override
        protected void onStopLoading() {
            cancelLoad();
        }
        @Nullable
        @Override
        public Tasks.Task loadInBackground() {
            return oData.getTask(key);
        }
    }
}
//Фома2018