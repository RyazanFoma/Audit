package com.example.eduardf.audit;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//Форма для редактирвоания задания на аудит
public class AuditTask extends AppCompatActivity implements
        View.OnClickListener,
        ObjectListEdit.OnObjectListEditInteractionListener,
        ReferenceManager.OnReferenceManagerInteractionListener,
        ReferenceManager.OnReferenceManagerInteractionChoose {

    AuditDB db; //База данных
    Task task; //Задание

    //Режимы открытия формы
    private final static int CREATE_MODE = -1; //создание нового задания аудита
    private final static int EDIT_MODE = 0; //редактирвоание существующего задания аудита

    //Аргументы интент
    private final static String ARG_MODE = "mode"; //Режим открытия формы
    private final static String ARG_DATE = "date"; //Даьта задания на аудит
    private final static String ARG_ID = "id"; //Идентификатор задания на аудит
    private final static String ARG_STATUS = "status"; //Статус задания на аудит
    private final static String ARG_TYPE = "type"; //Вид аудита
    private final static String ARG_OBJECT = "object"; //Объект аудита
    private final static String ARG_ANALYTICS = "analytics"; //Аналитика объекта аудита
    private final static String ARG_AUDITOR = "auditor"; //Аудитор задания на аудит
    private final static String ARG_TAG = "tag"; //Текущая закладка

    //Коды для выбора элементов справочников
    final static int SELECT_TYPE = 1;
    final static int SELECT_OBJECT = 2;

    //Не указанное Id
    private final static int NOT_SELECTED = -1;

    // Тэги закладок
    private final static String TAG1 = "tag1";
    private final static String TAG2 = "tag2";

    // возвращает Интент для создания нового задания на аудит
    public static Intent intentActivityCreate(Context context, int auditor, int status) {
        Intent intent = new Intent(context, AuditTask.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_STATUS, status);
        intent.putExtra(ARG_AUDITOR, auditor);
        return intent;
    }

    // возвращает Интент для изменения существующего задания на аудит
    public static Intent intentActivityEdit(Context context, int id) {
        Intent intent = new Intent(context, AuditTask.class);
        intent.putExtra(ARG_MODE, EDIT_MODE);
        intent.putExtra(ARG_ID, id);
        return intent;
    }

    // создает активность для редактирвания задания на аудит
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_task);

        //Все для текущей даты
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sDate = dateFormat.format(new Date());

        //Лента инструментов
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //ВСЕ ДЛЯ ВЫБОРА СТАТУСА
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.status_list));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner status = (Spinner) findViewById(R.id.status);
        status.setAdapter(adapter);
        status.setOnItemSelectedListener(new  AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { task.status = position; }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Кнопка Сохранить
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { saveTask(view); }
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
        tabSpec.setIndicator(getString(R.string.tab_ind));
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
                ArrayList<Integer> analytics = new ArrayList<>(task.analytics.size());
                analytics.addAll(task.analytics);
                startActivity(ReferenceManager.intentActivity(AuditTask.this, 0, AuditDB.TBL_ANALYTIC, "Аналитика объекта", analytics));
            }
        });

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Восстанавливаем основные параметры
        if (savedInstanceState==null) { // открываем форму впервые
            //Создаем объект Задание в зависимости от режима
            Intent intent = getIntent();
            switch (intent.getIntExtra(ARG_MODE, 0)){
                case CREATE_MODE:
                    task = new Task(Task.NEW_TASK_ID, sDate,
                            intent.getIntExtra(ARG_AUDITOR, 1),
                            -1, //Значения по умолчанию!!!
                            -1, //Значения по умолчанию!!!
                            intent.getIntExtra(ARG_STATUS, 1),
                            null);
                    break;
                case EDIT_MODE:
                    task = db.getTaskById(intent.getIntExtra(ARG_ID, Task.NEW_TASK_ID)); //Получаем задание через id
                    break;
            }
            if (task==null) Snackbar.make(findViewById(R.id.date), R.string.msg_not_find_id, Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
        else { // открываем после поворота. Восстанавливаем:
            task = new Task(savedInstanceState.getInt(ARG_ID),
                    savedInstanceState.getString(ARG_DATE, sDate),
                    savedInstanceState.getInt(ARG_AUDITOR, 1),
                    savedInstanceState.getInt(ARG_TYPE, 1),
                    savedInstanceState.getInt(ARG_OBJECT, 1),
                    savedInstanceState.getInt(ARG_STATUS, 1),
                    null);
            tabHost.setCurrentTabByTag(savedInstanceState.getString(ARG_TAG, TAG1)); //текущая вкладка
            getTaskAnalytics(savedInstanceState.getIntArray(ARG_ANALYTICS)); //текущий набор аналитик

        }

        //Заполняем View текущими значениями
        ((TextView) findViewById(R.id.date)).setText(task.date);
        status.setSelection(task.status); //Текущий статус
        ((TextView) findViewById(R.id.type)).setText(db.getNameById(AuditDB.TBL_TYPE, task.type));
        ((TextView) findViewById(R.id.object)).setText(db.getNameById(AuditDB.TBL_OBJECT, task.object));

        visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_DATE, task.date); //текущую закладку
        outState.putInt(ARG_ID, task.id); //идентификатор задания
        outState.putInt(ARG_STATUS, task.status); //статус задания
        outState.putInt(ARG_AUDITOR, task.auditor); //аудитор
        outState.putInt(ARG_TYPE, task.type); //вид аудита
        outState.putInt(ARG_OBJECT, task.object); //объект аудита
        outState.putString(ARG_TAG, ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag()); //текущую закладку
        outState.putIntArray(ARG_ANALYTICS, putTaskAnalytics()); //текущий набор аналитик
    }

    // восстанавливает список с аналитикой
    private void getTaskAnalytics(int[] in) { for (int id: in) task.addAnalytic(id); }

    // возвращает массив с аналитикой
    private int[] putTaskAnalytics() {
        int[] result = new int[task.analytics.size()];
        int i = 0;
        for (Integer id:  task.analytics) result[i++] = id;
        return result;
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
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
                saveTask((View) findViewById(R.id.fab));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // обновляет фрагмент с аналитикой
    @Override
    public void onResume() {
        super.onResume();
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.objectanalytics,
                        ObjectListEdit.newInstance(0, AuditDB.TBL_ANALYTIC, task.analytics)).
                commit();
    }

    // обработчик удаления аналитики из фрагмента
    @Override
    public void onObjectListEditInteractionListener(int requested, int id) { task.analytics.remove(id); }

    // обработчик добавления аналитик
    @Override
    public void onReferenceManagerInteractionChoose(Context context, int requestCode, List<Integer> ids) { task.analytics.addAll(ids); }

    //Обработчик кнопок сохранить и выбор из таблицы
    @Override
    public void onClick(View v) {
        final Intent intent;
        LinearLayout line;
        switch (v.getId()) {
            case R.id.type: //Выбрать вид
                line = animeView(this,R.id.line_type);
                intent = ReferenceManager.intentActivity(this, SELECT_TYPE, AuditDB.TBL_TYPE,
                        getString(R.string.txt_tp),task.type );
                line.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(intent);
                    }
                }, 1000L);
                break;
            case R.id.object: //Выбрать объект
                line = animeView(this,R.id.line_obj);
                intent = ReferenceManager.intentActivity(this, SELECT_OBJECT, AuditDB.TBL_OBJECT,
                        getString(R.string.txt_obj),task.object );
                line.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(intent);
                    }
                }, 1000L);
                break;
        }
    }

    @Override
    public void onReferenceManagerInteractionListener(Context referenceManager, int requestCode, Items.Item item) {
        switch (requestCode) {
            case SELECT_TYPE:
                task.type = item.id;
                ((TextView) findViewById(R.id.type)).setText(item.name);
                break;
            case SELECT_OBJECT:
                task.object = item.id;
                ((TextView) findViewById(R.id.object)).setText(item.name);
                break;
        }
        visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
    }

    // управляет доступностью выбора объекта и аналитики
    private void visibilityView() {
        int obj, anl;
        obj = anl = View.INVISIBLE;

        if (task.type!=-1) {
            obj = View.VISIBLE;
            if (task.object!=-1) anl = View.VISIBLE;
        }
        ((LinearLayout) findViewById(R.id.line_obj)).setVisibility(obj);
//        ((LinearLayout) findViewById(R.id.line_anl)).setVisibility(anl);
    }

    // сохраняет задание и закрывает активити
    private void saveTask(View v) {
        if (task.type!=AuditDB.NOT_SELECTED&&task.object!=AuditDB.NOT_SELECTED) {
            db.saveTask(task); //Сохраняем задание
            setResult(RESULT_OK, null);
            finish();
        }
        else Snackbar.make(v, R.string.msg_not_fill, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    // анимировать view перед выбором
    private LinearLayout animeView(Context context, int id) {
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.background);
        LinearLayout line = (LinearLayout) findViewById(id);
        set.setTarget((LinearLayout) findViewById(id));
        set.start();
        return line;
    }

}
