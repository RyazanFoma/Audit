package com.example.eduardf.audit;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.ArraySet;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.Set;

//Форма редактирования вида аудита
public class ActivityType extends AppCompatActivity implements  View.OnClickListener,
        GroupTable.OnGroupTableInteractionListener /*, ReferenceManager.OnReferenceManagerInteractionListener*/ {

    private AuditType type; //Вид аудита
    private AuditDB db; //База данных
    private int iMode;
    private GroupTable paterChoose;

    //Режимы открытия формы
    public final static int GROUP_MODE = -2;
    public final static int CREATE_MODE = -1;
    public final static int OPEN_MODE = 0;
    public final static int COPY_MODE = +1;

    //Аргументы интент
    public final static String ARG_ID = "id";
    public final static String ARG_MODE = "mode";
    public final static String ARG_PATER = "pater";

    //Коды для выбора элементов справочников
    final static int SELECT_PATER = 1;
//    final static int SELECT_OBJECT = 2;
//    final static int SELECT_ANALYTICS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type);

        //Лента инструментов
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Кнопка Сохранить
        FloatingActionButton save = (FloatingActionButton) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //Все для закладок
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec tabSpec;
        //Общее
        tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setIndicator(getString(R.string.tab_com));
        tabSpec.setContent(R.id.tab1);
        tabHost.addTab(tabSpec);
        //Объекты
        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setIndicator(getString(R.string.tab_obj));
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);
        //Аналитика
        tabSpec = tabHost.newTabSpec("tag3");
        tabSpec.setIndicator(getString(R.string.tab_anl));
        tabSpec.setContent(R.id.tab3);
        tabHost.addTab(tabSpec);

        // Закладка по умолчанию
        tabHost.setCurrentTabByTag("tag1");

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Создаем объект Вид аудита в зависимости от режима
        Intent intent = getIntent();
        boolean is_group = false;
        switch (intent.getIntExtra(ARG_MODE, CREATE_MODE)){
            case GROUP_MODE:
                is_group = true;
            case CREATE_MODE:
                type = new AuditType(AuditType.NEW_TYPE_ID, "", intent.getIntExtra(ARG_PATER, 0), is_group,"");
                break;
            case OPEN_MODE:
                type = db.getTypeById(intent.getIntExtra(ARG_ID, AuditType.NEW_TYPE_ID)); //Получаем вид аудита через id
                break;
            case COPY_MODE:
                type = db.getTypeById(intent.getIntExtra(ARG_ID, AuditType.NEW_TYPE_ID)); //Получаем вид аудита через id
                if (type!=null) type.id = AuditType.NEW_TYPE_ID; //Устанавливаем id как у нового вида аудита
                break;
        }
        if (type==null) Snackbar.make(findViewById(R.id.date), R.string.msg_not_find_id, Snackbar.LENGTH_LONG).setAction("Action", null).show();

        //Заполняем вкладку Общее текущими значениями
        if (is_group) ((TextView) findViewById(R.id.title)).setText("Наименование группы*");
        ((TextView) findViewById(R.id.name)).setText(type.name);
        ((TextView) findViewById(R.id.pater)).setText(db.getNameById(AuditDB.TBL_TYPE, type.pater));
        ((TextView) findViewById(R.id.desc)).setText(type.desc);

        //Заполняем вкладку Объекты списком групп объектов
        RecyclerView objects = (RecyclerView) findViewById(R.id.objects);
        objects.setHasFixedSize(true);
        objects.setLayoutManager(new LinearLayoutManager(this));
        objects.setAdapter(new RecyclerAdapter(db.getDummyByIds(AuditDB.TBL_OBJECT,type.objects.keySet())));

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

    @Override
    public void onClick(View v) {
        final Intent intent;
        LinearLayout line;
        Set<Integer> s;
        switch (v.getId()) {
            case R.id.save: //Сохранить
//                saveTask(v);
                break;
            case R.id.pater: //Выбрать объект
                startActivity(ReferenceManager.intentActivity(this, 0, AuditDB.TBL_TYPE, "Вид аудита", type.id));
//                //Вставляем фрагмент
//                FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
////                paterChoose = GroupTable.newInstance(AuditDB.TBL_TYPE, type.pater);
//                s = new ArraySet<>(); s.add(type.pater);
//                paterChoose = GroupTable.newInstance(AuditDB.TBL_TYPE, s);
//                fTrans.add(R.id.pater_choose, paterChoose);
//                fTrans.commit();
//                //Прячем вью с текущей группой
//                ((TextView) findViewById(R.id.pater)).setVisibility(TextView.GONE);
                break;
//            case R.id.btn_object: //Выбрать объект
//                line = animeView(this,R.id.line_obj);
//                intent = newIntent(this, AuditDB.TBL_OBJECT, getString(R.string.txt_obj), task.object, db.getObjectsByType(task.type), null);
//                line.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        startActivityForResult(intent, SELECT_OBJECT);
//                    }
//                }, 1000L);
//                break;
//            case R.id.btn_analytic: //Выбрать аналитику
//                line = animeView(this,R.id.line_anl);
//                intent = newIntent(this, AuditDB.TBL_ANALYTIC, getString(R.string.txt_anobj), task.getAnalytics(), db.getAnalyticsByTO(task.type, task.object), null);
//                line.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        startActivityForResult(intent, SELECT_ANALYTICS);
//                    }
//                }, 1000L);
//                break;
        }
    }

//    // получает результат активити выбора из таблиц
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//        int id;
//        if (resultCode==RESULT_OK && intent != null)
//            switch (requestCode) {
//                case SELECT_PATER:
//                    id = intent.getIntExtra("id",GroupTable.NO_SELECTED);
//                    ((TextView) findViewById(R.id.pater)).setText(db.getNameById(AuditDB.TBL_TYPE, id));
//                    type.pater = id;
//                    break;
////                case SELECT_OBJECT:
////                    id = intent.getIntExtra("id",TreeTable.NO_SELECTED);
////                    ((TextView) findViewById(R.id.object)).setText(db.getNameById(AuditDB.TBL_OBJECT, id));
////                    task.object = id;
////                    visibilityView(); //Устанвилаем доступность в зависимости от реквизитов задания
////                    break;
////                case SELECT_ANALYTICS:
////                    task.setAnalytics(intent.getIntArrayExtra("id"));
////                    db.getNamebyIds(AuditDB.TBL_ANALYTIC,dataAnalytics,task.analytics);
////                    anlAdapter.notifyDataSetChanged();
////                    break;
//            }
//    }

    // анимировать view перед выбором
    private LinearLayout animeView(Context context, int id) {
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.background);
        LinearLayout line = (LinearLayout) findViewById(id);
        set.setTarget((LinearLayout) findViewById(id));
        set.start();
        return line;
    }

    // возвращает интент для вызова активити одиночного выбора из таблицы
    private Intent newIntent(Context context, String table, String title, int id, int[] in, String like) {
        Intent intent = new Intent(context, TreeTable.class);
        intent.putExtra(TreeTable.ARG_TABLE, table); //Имя таблицы
        intent.putExtra(TreeTable.ARG_TITLE, title); //Заголовок
        intent.putExtra(TreeTable.ARG_MODE, TreeTable.MODE_GROUP_CHOICE); //Режим выбора
        intent.putExtra(TreeTable.ARG_ID, id); //Выбранный элемент
        if (in!=null) intent.putExtra(TreeTable.ARG_ID, in); //Отбор по группам
        if ((like!=null)&&(!like.isEmpty())) intent.putExtra(TreeTable.ARG_LIKE, like); //Отбор по наименованию
        return intent;
    }

    @Override
    public void OnGroupTableInteractionListener(GroupTable.Item item) {
        type.pater = item.id;
        TextView pater = (TextView) findViewById(R.id.pater);
        pater.setText(item.name);
        pater.setVisibility(TextView.VISIBLE);
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        fTrans.remove(paterChoose);
        fTrans.commit();
    }

//    @Override
//    public void OnReferenceManagerInteractionListener(Items.Item item) {
//        type.pater = item.id;
//        TextView pater = (TextView) findViewById(R.id.pater);
//        pater.setText(item.name);
////        pater.setVisibility(TextView.VISIBLE);
////        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
////        fTrans.remove(paterChoose);
////        fTrans.commit();
//    }
}

