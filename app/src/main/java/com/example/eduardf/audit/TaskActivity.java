package com.example.eduardf.audit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

//Форма для редактирвоания задания на аудит
public class TaskActivity extends AppCompatActivity implements
        ReferenceField.OnFragmentChoiceListener,
        ObjectListEdit.OnObjectListEditInteractionListener,
        IndicatorFragment.OnListFragmentInteractionListener,
        IndicatorFragment.OnScrollUpListener,
        DateTime.OnDateTimeInteractionListener,
        LoaderManager.LoaderCallbacks<Tasks.Task>{

    AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    Tasks.Task task; //Задание

    //Режимы открытия формы
    private final static int CREATE_MODE = 0; //создание нового задания аудита
    private final static int EDIT_MODE = 1; //редактирвоание существующего задания аудита

    //Аргументы интент и поворота экрана
    private final static String ARG_MODE = "mode"; //Режим открытия формы
    private final static String ARG_AUDITOR = "auditor"; //Аудитор задания на аудит
    private final static String ARG_ID = "id"; //Идентификатор задания на аудит
    final static String ARG_STATUS = "status"; //Статус задания на аудит
    private final static String ARG_TASK = "task"; //Задание
    private final static String ARG_TAG = "tag"; //Текущая закладка
    private static final String ARG_FILLED = "filled"; //Признаки заполнения закладок

    //Коды для выбора элементов справочников
    final static int SELECT_TYPE = 1;
    final static int SELECT_ORGANIZATION = 2;
    final static int SELECT_OBJECT = 3;
    final static int SELECT_RESPONSIBLE = 4;
    final static int SELECT_ANALYTICS = 5;

    // Тэги закладок
    private final static String TAG0 = "tag0";
    private final static String TAG1 = "tag1";
    private final static String TAG2 = "tag2";
    private final static String TAG3 = "tag3";
    // Признаки заполнения закладок
    private boolean[] filledTag = {false, false, false};

    // возвращает Интент для создания нового задания на аудит
    public static Intent intentActivityCreate(Context context, String auditor, Tasks.Task.Status status) {
        Intent intent = new Intent(context, TaskActivity.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_STATUS, status.id);
        intent.putExtra(ARG_AUDITOR, auditor);
        return intent;
    }

    // возвращает Интент для изменения существующего задания на аудит
    public static Intent intentActivityEdit(Context context, String id) {
        Intent intent = new Intent(context, TaskActivity.class);
        intent.putExtra(ARG_MODE, EDIT_MODE);
        intent.putExtra(ARG_ID, id);
        return intent;
    }

    // создает активность для редактирвания задания на аудит
    @SuppressLint("ClickableViewAccessibility") //Чтобы для onTouch не возникало предупреждение о performClick
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        //Лента инструментов
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //ВСЕ ДЛЯ ВЫБОРА СТАТУСА
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.status_list));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner status = (Spinner) findViewById(R.id.status);
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
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { saveTask(); }
        });

        //ВСЕ ДЛЯ ЗАКЛАДОК
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
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
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (!getSupportLoaderManager().hasRunningLoaders())
                    fillViews(tabId);
            }
        });

        //Создает объект OData
        oData = new AuditOData(this);

        if (savedInstanceState==null) { // открываем форму впервые
            //Создаем объект Задание в зависимости от режима
            Intent intent = getIntent();
            switch (intent.getIntExtra(ARG_MODE, 0)){
                case CREATE_MODE:
                    task = new Tasks.Task();
                    task.date = new Date();
                    task.status = Tasks.Task.Status.toValue(intent.getStringExtra(ARG_STATUS));
                    task.auditor_key = intent.getStringExtra(ARG_AUDITOR);
                    task.number = "Новое";
                    task.comment = "";
                    task.deleted = false;
                    task.posted = false;
                    task.achieved = false;
                    //Восстановить из значений по умолчанию - индивидуальные настройки пользователя
//                    task.type_key = null;
//                    task.organization_key = null;
//                    task.object_key = null;
//                    task.responsible_key = null;
                    fillViews(TAG0); //Общие поля
                    fillViews(TAG1); //и первую закладку.
//                    tabHost.setCurrentTabByTag(TAG3);  // Или 3ю, в зависимости от настроек в виде аудита!!!
                    break;
                case EDIT_MODE:
                    //Запускаем загрузчик для чтения данных.
                    startLoader(intent.getExtras());
                    tabHost.setCurrentTabByTag(TAG1); //и первую закладку. Потом переделать на открытие показателей, в зависимости от настроек пользователя
                    break;
            }
        }
        else { // открываем после поворота.
            //Успели загрузить-сохранить задание?
            if (savedInstanceState.containsKey(ARG_TASK)) { //Восстанавливаем:
                task = new Tasks.Task();
                task.onRestoreInstanceState(savedInstanceState, ARG_TASK);
                //В заголовок активности добавим номер
                setTitle(getString(R.string.title_activity_audit_task)+" ("+task.number+")");
                filledTag = savedInstanceState.getBooleanArray(ARG_FILLED);
            }
            else { //Не успели - грузим по новой
                startLoader(getIntent().getExtras());
            }
            tabHost.setCurrentTabByTag(savedInstanceState.getString(ARG_TAG)); //текущая вкладка
        }

        //Обработчик очистки комментария
        ((TextInputEditText) findViewById(R.id.comment)).setOnTouchListener(new View.OnTouchListener() {
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

    }

    //Запускает одноразовый загрузчик задания
    private void startLoader(Bundle arg) {
        Loader loader = getSupportLoaderManager().getLoader(-1);
        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(-1, new Bundle(arg), this);
        else getSupportLoaderManager().initLoader(-1, new Bundle(arg), this);
        //Загрузчик cработает, только при первом открытии задания, потом будет убит в onLoadFinished
        //Там же будет вызван fillViews(""); - Общие поля и первая закладка
    }

    //Заполняет поля активности
    private void fillViews(String tabId) {
        switch (tabId) {
            case TAG0: //Поля вне закладок
                //В заголовок активности добавим номер
                setTitle(getString(R.string.title_activity_audit_task)+" ("+task.number+")");
                //Вставляем фрагменты
                getSupportFragmentManager().
                        beginTransaction().
                        //для выбора даты и времени
                                add(R.id.datetime, DateTime.newInstance(task.date)).
                        commit();
                ((Spinner) findViewById(R.id.status)).setSelection(task.status.number);
                ((EditText) findViewById(R.id.comment)).setText(task.comment);
                //Иконки
                ((ImageView) findViewById(R.id.deleted)).setVisibility(task.deleted?View.VISIBLE:View.GONE);
                ((ImageView) findViewById(R.id.posted)).setVisibility(task.posted?View.VISIBLE:View.GONE);
                if (!task.deleted && task.posted && task.status == Tasks.Task.Status.IN_WORK) {
                    ((ImageView) findViewById(R.id.thumb)).setVisibility(View.VISIBLE);
                    ((ImageView) findViewById(R.id.thumb)).setImageResource(task.achieved?
                            R.drawable.ic_black_thumb_up_alt_24px:
                            R.drawable.ic_black_thumb_down_alt_24px);
                }
                else ((ImageView) findViewById(R.id.thumb)).setVisibility(View.GONE);
                break;
            case TAG1: //поля закладки Общее
                if (!filledTag[0] ) {
                    filledTag[0] = true;
                    getSupportFragmentManager().
                            beginTransaction().
                            //для выбора вида аудита
                                    add(R.id.type,
                                    ReferenceField.newInstance(SELECT_TYPE,
                                            AuditOData.Set.TYPE,
                                            getString(R.string.txt_tp), null, task.type_key)).
                            //для выбора организации
                                    add(R.id.organization,
                                    ReferenceField.newInstance(SELECT_ORGANIZATION,
                                            AuditOData.Set.ORGANIZATION,
                                            getString(R.string.txt_org),null, task.organization_key)).
                            //для выбора объекта аудита
                                    add(R.id.object,
                                    ReferenceField.newInstance(SELECT_OBJECT,
                                            AuditOData.Set.OBJECT,
                                            getString(R.string.txt_obj), null, task.object_key)).
                            //для выбора ответственного за объект
                                    add(R.id.responsible,
                                    ReferenceField.newInstance(SELECT_RESPONSIBLE,
                                            AuditOData.Set.RESPONSIBLE,
                                            getString(R.string.txt_rsp),null, task.responsible_key)).
                            commit();
                }
                break;
            case TAG2: //поля закладки Аналитика
                if (!filledTag[1]) {
                    filledTag[1] = true;
                    getSupportFragmentManager().
                            beginTransaction().
                            add(R.id.analytics,
                                    ObjectListEdit.newInstance(SELECT_ANALYTICS,
                                            AuditOData.Set.ANALYTIC, getString(R.string.tab_anl), task.analytics)).
                            commit();
                }
                break;
            case TAG3: //поля закладки Показатели
                //Обработчик кнопки Заполнить
                ((ImageButton) findViewById(R.id.fill)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new FillIndicators().execute(task.type_key, task.object_key);
                    }
                });

                //Переключатель По предметам
//                final Switch aSwitch = findViewById(R.id.by_subject);
//        aSwitch.setChecked(false); //Здесь нужно установить значение по умолчанию из вида аудита
                //Обработчик переключателя По предметам
                ((Switch) findViewById(R.id.by_subject)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (buttonView.isPressed())
                            updateIndicators();
                    }
                });

                if (!filledTag[2]) {
                    filledTag[2] = true;
                    getSupportFragmentManager().
                            beginTransaction().
                            add(R.id.indicators,
                                    IndicatorFragment.newInstance(task.type_key, task.indicators,
                                            ((Switch) findViewById(R.id.by_subject)).isChecked())).
                            commit();
                }
                break;
        }
        visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
    }

    //Обновляет фрагмент с показателями
    private void updateIndicators() {
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.indicators,
                        IndicatorFragment.newInstance(task.type_key, task.indicators,
                                ((Switch) findViewById(R.id.by_subject)).isChecked())).
                commit();
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (task != null)
            task.onSaveInstanceState(outState, ARG_TASK);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.save:
                saveTask();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // управляет доступностью выбора объекта и аналитики
    private void visibilityView() {
//        int obj, anl;
//        obj = anl = View.INVISIBLE;
//
//        if (task.auditType!=-1) {
//            obj = View.VISIBLE;
//            if (task.object!=-1) anl = View.VISIBLE;
//        }
//        ((LinearLayout) findViewById(R.id.line_obj)).setVisibility(obj);
//        ((LinearLayout) findViewById(R.id.line_anl)).setVisibility(anl);
    }

    //Обработчик скроллинга показателей
    @Override
    public void onScrollUpListener(int visibility) {
        findViewById(R.id.head).setVisibility(visibility);
        findViewById(android.R.id.tabs).setVisibility(visibility);
    }

    /**
     * Класс для операций создания/сохранения задания в потоке
     * с последующим закрытием активности
     * Входные параметры: задание
     */
    private class SaveTask extends AsyncTask<Void, Void, Void> {
        private final Tasks.Task task;
        //Конструктор
        private SaveTask(Tasks.Task task) {
            this.task = task;
        }
        //Показывает прогрессбар
        protected void onPreExecute() {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }
        //Сохраняет новое или существующее задание
        protected Void doInBackground(Void... voids) {
            if (task.id != null) //Обновляем существующее задание
                new Thread(new Runnable() {  public void run() { oData.updateTask(task); } }).start();
            else //Создаем новое задание
                new Thread(new Runnable() {  public void run() { oData.createTask(task); } }).start();
            return null;
        }
        //Закрывает активность
        protected void onPostExecute(Void voids) {
            Intent intent = new Intent();
            intent.putExtra(ARG_STATUS, task.status.number);
            setResult(RESULT_OK, intent);
            finish(); //Закрываем активность
        }
    }

    /**
     * Сохраняет задание
     */
    private void saveTask() {
        //НУЖНО ДОБАВИТЬ ПРОВЕРКУ ЗАПОЛНЕНИЯ ОБЯЗАТЕЛЬНЫХ ПОЛЕЙ!!!
        task.comment = ((EditText) findViewById(R.id.comment)).getText().toString();
        new SaveTask(task).execute();
    }

    //вызывается при изменении даты во фрагменте
    @Override
    public void onDateTimeInteraction(Date date) {
        task.date = date;
    }

    //Загрузчик задания
    @NonNull
    @Override
    public Loader<Tasks.Task> onCreateLoader(int id, @Nullable Bundle args) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        return new FillTask(TaskActivity.this, oData, args.getString(ARG_ID));
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Tasks.Task> loader, Tasks.Task data) {
        task = data;
        fillViews(TAG0); //Заполним общие поля
        fillViews(((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag()); //и текущую закладку
        getSupportLoaderManager().destroyLoader(-1); //Убиваем загрузчик, т.к. он нужен только на один раз при первом отрытии задания
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Tasks.Task> loader) {
        task = null;
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    //вызывается при выборе элемента справочника или очистки значения поля
    @Override
    public void onFragmentChoiceListener(int requestCode, String id) {
        switch (requestCode) {
            case SELECT_TYPE:
                task.type_key = id;
                break;
            case SELECT_ORGANIZATION:
                task.organization_key = id;
                break;
            case SELECT_OBJECT:
                task.object_key = id;
                break;
            case SELECT_RESPONSIBLE:
                task.responsible_key = id;
                break;
        }
    }

    //вызывается из фрагмента для добавления аналитики
    @Override
    public void onObjectListEditAdd(int requestcode, Items items) {
        if (requestcode == SELECT_ANALYTICS)
            for (Items.Item item: items)
                task.analytics.add(item.id);
    }

    //вызывается из фрагмента для удаления аналитики
    @Override
    public void onObjectListEditDelete(int requestcode, String key) {
        if (requestcode == SELECT_ANALYTICS) {
            task.analytics.remove(key);
        }
    }

    @Override
    public void onListFragmentInteraction(String id, Object value, boolean achived, String comment) {
        for(Tasks.Task.IndicatorRow row: task.indicators)
            if (row.indicator.equals(id)) {
                row.value = value;
                row.achived = achived;
                row.comment = comment;
            }
    }

    //Асинхронный загрузчик задания
    private static class FillTask extends AsyncTaskLoader<Tasks.Task> {
        final AuditOData oData;
        final String key;
        private FillTask(@NonNull Context context, AuditOData oData, String key) {
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

    //Класс для перезаполнения показателей задания
    private class FillIndicators extends AsyncTask<String, Void, ArrayList<Tasks.Task.IndicatorRow>> {
        @Override
        protected void onPreExecute() {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            findViewById(R.id.indicators).setVisibility(View.INVISIBLE);
        }
        @Override
        protected ArrayList<Tasks.Task.IndicatorRow> doInBackground(String... strings) {
            return oData.getStandardIndicators(strings[0], strings[1]);
        }
        @Override
        protected void onPostExecute(ArrayList<Tasks.Task.IndicatorRow> rows) {
            task.indicators.clear();
            task.indicators.addAll(rows);
            updateIndicators(); //обновим Показатели
            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            findViewById(R.id.indicators).setVisibility(View.VISIBLE);
        }
    }

}
//Фома2018