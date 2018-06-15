package com.example.eduardf.audit;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabItem;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AuditTask extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener,
        View.OnClickListener,
        ReferenceManager.OnReferenceManagerInteractionListener,
        ReferenceManager.OnReferenceManagerInteractionChoose {

    AuditDB db; //База данных
    Task task; //Задание
    ArrayList<Map<String, Object>> dataAnalytics; // данные для адаптера аналитик
    SimpleAdapter anlAdapter; // адаптер аналитик
    ListView anlList; //Список аналитик

    //Режимы открытия формы
    public final static int CREATE_MODE = -1;
    public final static int OPEN_MODE = 0;
    public final static int COPY_MODE = 1;

    //Коды для выбора элементов справочников
    final static int SELECT_TYPE = 1;
    final static int SELECT_OBJECT = 2;
    final static int SELECT_ANALYTICS = 3;

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
        status.setOnItemSelectedListener(this); // устанавливаем обработчик нажатия

        //Кнопка Сохранить
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Создаем объект Задание в зависимости от режима
        Intent intent = getIntent();
        switch (intent.getIntExtra("mode", 0)){
            case CREATE_MODE:
                task = new Task(Task.NEW_TASK_ID, sDate,
                        intent.getIntExtra("auditor", 1),
                        -1, //Значения по умолчанию!!!
                        -1, //Значения по умолчанию!!!
                        intent.getIntExtra("status", 1),
                        null);
                break;
            case OPEN_MODE:
                task = db.getTaskById(intent.getIntExtra("id", Task.NEW_TASK_ID)); //Получаем задачу через id
                break;
            case COPY_MODE:
                task = db.getTaskById(intent.getIntExtra("id", Task.NEW_TASK_ID)); //Получаем задачу через id
                if (task!=null) task.id = Task.NEW_TASK_ID; //Устанавливаем id как у нового задания
                break;
        }
        if (task==null) Snackbar.make(findViewById(R.id.date), R.string.msg_not_find_id, Snackbar.LENGTH_LONG).setAction("Action", null).show();

        //Заполняем View текущими значениями
        ((TextView) findViewById(R.id.date)).setText(task.date);
        ((TextView) findViewById(R.id.type)).setText(db.getNameById(AuditDB.TBL_TYPE, task.type));
        ((TextView) findViewById(R.id.object)).setText(db.getNameById(AuditDB.TBL_OBJECT, task.object));
        status.setSelection(task.status); //Текущий статус

        dataAnalytics = db.getNamebyIds(AuditDB.TBL_ANALYTIC,task.analytics);
        anlAdapter = new SimpleAdapter(this,
                dataAnalytics,
                android.R.layout.simple_list_item_1, new String[] {"name"},
                new int[] {android.R.id.text1});
        anlList = (ListView) findViewById(R.id.analytics);
        anlList.setAdapter(anlAdapter);

        visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
        setResult(RESULT_CANCELED, null);
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

    //Обработчик кнопок сохранить и выбор из таблицы
    @Override
    public void onClick(View v) {
        final Intent intent;
        LinearLayout line;
        switch (v.getId()) {
            case R.id.fab: //Сохранить задание
                saveTask(v);
                break;
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
            case R.id.btn_analytic: //Выбрать аналитику
                line = animeView(this,R.id.line_anl);
                ArrayList<Integer> analytics = new ArrayList<>(task.analytics.size());
                analytics.addAll(task.analytics);
                intent = ReferenceManager.intentActivity(this, SELECT_ANALYTICS, AuditDB.TBL_ANALYTIC,
                        getString(R.string.txt_anobj), analytics , db.getAnalyticsByTO(task.type, task.object));
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
        ((Activity)referenceManager).finish();
        switch (requestCode) {
            case SELECT_TYPE:
                task.type = item.id;
                ((TextView) findViewById(R.id.type)).setText(item.name);
                break;
            case SELECT_OBJECT:
                task.object = item.id;
                ((TextView) findViewById(R.id.object)).setText(item.name);
                break;
            case SELECT_ANALYTICS:
        }
        visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
    }

    @Override
    public void onReferenceManagerInteractionChoose(Context referenceManager, int requestCode, List<Integer> ids) {
        ((Activity)referenceManager).finish();
        if (requestCode==SELECT_ANALYTICS) {
            task.addAnalytics(ids); //Добавляем аналитику в задачу
            dataAnalytics.clear();
            dataAnalytics.addAll(db.getNamebyIds(AuditDB.TBL_ANALYTIC,task.analytics));
            anlAdapter.notifyDataSetChanged();
        }
    }

//    // получает результат активити выбора из таблиц
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//        int id;
//        if (resultCode==RESULT_OK && intent != null)
//            switch (requestCode) {
//                case SELECT_TYPE:
//                    id = intent.getIntExtra("id",TreeTable.NO_SELECTED);
//                    ((TextView) findViewById(R.id.type)).setText(db.getNameById(AuditDB.TBL_TYPE, id));
//                    task.type = id;
//                    visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
//                    break;
//                case SELECT_OBJECT:
//                    id = intent.getIntExtra("id",TreeTable.NO_SELECTED);
//                    ((TextView) findViewById(R.id.object)).setText(db.getNameById(AuditDB.TBL_OBJECT, id));
//                    task.object = id;
//                    visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
//                    break;
//                case SELECT_ANALYTICS:
//                    task.setAnalytics(intent.getIntArrayExtra("id"));
//                    db.getNamebyIds(AuditDB.TBL_ANALYTIC,dataAnalytics,task.analytics);
//                    anlAdapter.notifyDataSetChanged();
//                    break;
//            }
//    }

    //Обработчик для выбора статуса
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        task.status = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
        ((LinearLayout) findViewById(R.id.line_anl)).setVisibility(anl);
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
