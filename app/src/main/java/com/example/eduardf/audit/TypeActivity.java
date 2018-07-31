package com.example.eduardf.audit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;

import android.support.v4.util.ArraySet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

//Форма редактирования вида аудита
public class TypeActivity extends AppCompatActivity implements TypeListEdit.OnTypeListEditInteractionListener {

    private AuditType type; //Вид аудита
    private AuditDB db; //База данных
    private SimpleAdapter sAdapter; //адаптер спиннера с объектами для отбора аналитик
    private ArrayList<Map<String, Object>> mObjects = new ArrayList<Map<String, Object>>(); //список объектов для спиннера, заполняется перед открытием закладки Аналитика
    private int iObject = NOT_SELECTED; //ID текущего объекта

    //Режимы открытия формы
    private final static int CREATE_MODE = -1; //создание нового вида аудита
    private final static int EDIT_MODE = 0; //редактирвоание существующего вида аудита

    //Аргументы интент
    private final static String ARG_MODE = "mode"; //Режим
    private final static String ARG_ID = "id"; //Идентификатор вида аудита
    private final static String ARG_PATER = "pater"; //Родитель вида аудита

    //Аргументы для поворота экрана
    private final static String ARG_TAG = "tag"; //Текущая закладка
    private final static String ARG_OBJECT = "object"; //Текущий объект для показа аналитики
    private final static String ARG_OBJECTS = "objects"; //Объекты с аналитикой

    //Не указанное Id
    private final static int NOT_SELECTED = -1;

    // Тэги закладок
    private final static String TAG1 = "tag1";
    private final static String TAG2 = "tag2";
    private final static String TAG3 = "tag3";

    //Запрашиваемые коды для редактирования списков
    private final static int CODE_OBJECT = 1;
    private final static int CODE_ANALYTIC = 2;

    // возвращает Интент для создания нового вида аудита
    public static Intent intentActivityCreate(Context context, int pater) {
        Intent intent = new Intent(context, TypeActivity.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_PATER, pater);
        return intent;
    }

    // возвращает Интент для изменения существующего вида аудита
    public static Intent intentActivityEdit(Context context, int id) {
        Intent intent = new Intent(context, TypeActivity.class);
        intent.putExtra(ARG_MODE, EDIT_MODE);
        intent.putExtra(ARG_ID, id);
        return intent;
    }

    // создает активность для ввода вида аудита
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type);

        //Лента инструментов - для поддержки возврата назад
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Кнопка Сохранить
        FloatingActionButton save = (FloatingActionButton) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type.name = ((EditText) findViewById(R.id.typename)).getText().toString();
                type.desc = ((EditText) findViewById(R.id.typedesc)).getText().toString();
                db.saveType(type);
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
        //Объекты
        tabSpec = tabHost.newTabSpec(TAG2);
        tabSpec.setIndicator(getString(R.string.tab_obj));
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);
        //Аналитика
        tabSpec = tabHost.newTabSpec(TAG3);
        tabSpec.setIndicator(getString(R.string.tab_anl));
        tabSpec.setContent(R.id.tab3);
        tabHost.addTab(tabSpec);
        //Обработчик закладок
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) { update(tabId); }
        } );

        //Переключатель Изменить для объектов
        Switch switchObject = (Switch) findViewById(R.id.switch_object);
        switchObject.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { updateObject(isChecked); }
        });

        //Переключатель Изменить для аналитики
        Switch switchAnalytic = (Switch) findViewById(R.id.switch_analytic);
        switchAnalytic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { updateAnalytic(isChecked); }
        });

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Спиннер для выбора объекта. Список объектов заполняем каждый раз перед открытием вкладки Аналитика
        sAdapter = new SimpleAdapter(this, mObjects, android.R.layout.simple_spinner_item, new String[] {AuditDB.NAME}, new int[] {android.R.id.text1});
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner object = (Spinner) findViewById(R.id.object);
        object.setAdapter(sAdapter);
        //Обработчик спиннера для выбора объекта и отбора аналитик по выбранному объекту
        object.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                iObject = (int) mObjects.get(position).get("_id");;
                updateAnalytic(((Switch) findViewById(R.id.switch_analytic)).isChecked());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Восстанавливаем основные параметры
        if (savedInstanceState==null) { // открываем форму впервые
            Intent intent = getIntent();
            switch (intent.getIntExtra(ARG_MODE, CREATE_MODE)) {
                case CREATE_MODE: //режим создания нового вида аудита
                    type = new AuditType(AuditType.NEW_TYPE_ID, "", intent.getIntExtra(ARG_PATER, NOT_SELECTED), false, "");
                    break;
                case EDIT_MODE: //режим редактирования существующего вида аудита
                    type = db.getTypeById(intent.getIntExtra(ARG_ID, NOT_SELECTED));
//                    if (type == null)
//                        Snackbar.make(findViewById(R.id.date), R.string.msg_not_find_id, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    break;
            }
            tabHost.setCurrentTabByTag(TAG1); // Закладка по умолчанию
            //Заполняем вкладку Общее текущими значениями
            ((EditText) findViewById(R.id.typename)).setText(type.name); //наименование1
            ((EditText) findViewById(R.id.typedesc)).setText(type.desc); //описание
        }
        else { // открываем после поворота. Восстанавливаем:
            type = new AuditType(savedInstanceState.getInt(ARG_ID), //вид аудита
                    ((EditText) findViewById(R.id.typename)).getText().toString(),
                    savedInstanceState.getInt(ARG_PATER),
                    false, //только элементы, группы редактируются диалогом
                    ((EditText) findViewById(R.id.typedesc)).getText().toString());
            tabHost.setCurrentTabByTag(savedInstanceState.getString(ARG_TAG, TAG1)); //текущая вкладка
            iObject = savedInstanceState.getInt(ARG_OBJECT, NOT_SELECTED); //текущий объект для отбора аналитик
            getTypeObjects(savedInstanceState.getParcelableArray(ARG_OBJECTS)); //текущий набор объектов с аналитиками
            update(tabHost.getCurrentTabTag()); //обновляем содержимое вкладки
        }
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_ID, type.id); //идентификатор вида аудита
        outState.putInt(ARG_PATER, type.pater); //родителя вида аудита
        outState.putString(ARG_TAG, ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag()); //текущую закладку
        outState.putInt(ARG_OBJECT, iObject); //значение объекта для отбора аналитики
        outState.putParcelableArray(ARG_OBJECTS, putTypeObjects()); //текущий набор объектов с аналитиками
    }

    // восстанавливает список объектов с аналитикой
    private void getTypeObjects(Parcelable[] in) {
        for (Parcelable parcelable: in)
            type.addObject(((ParcelableObject) parcelable).key, ((ParcelableObject) parcelable).analytics);
    }

    // возвращает массив парселейбл с объектами и аналитикой
    private Parcelable[] putTypeObjects() {
        Parcelable[] result = new Parcelable[type.objects.size()];
        int i = 0;
        for (Integer key:  type.objects.keySet()) result[i++] = new ParcelableObject(key, type.objects.get(key));
        return result;
    }

    //Класс для сохранения списка объектов с аналитикой при повороте экрана
    private static class ParcelableObject implements Parcelable {

        int key; //Объект - ключь
        Set<Integer> analytics; //Список аналитики объекта

        //Конструктор обычный
        private ParcelableObject(int key, Set<Integer> analytics) {
            this.key = key;
            this.analytics = analytics;
        }

        //Конструктор из парселя
        protected ParcelableObject(Parcel in) {
            key = in.readInt(); //Считываем объект
            int size_analytic = in.readInt(); //Считываем количество аналитик
            analytics = new ArraySet<Integer>(in.readInt());
            for(int i=0;i<size_analytic;i++) analytics.add(in.readInt()); //Добавляем все аналитики
        }

        //Пишем объект в парсель
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(key); //Пишем объект
            dest.writeInt(analytics.size()); //Пишем количество аналитик
            for(Integer a: analytics) dest.writeInt(a); //Пишем все аналитики
        }

        //по умолчанию
        @Override
        public int describeContents() {
            return 0;
        }

        //Создатель парсель объекта
        public static final Creator<ParcelableObject> CREATOR = new Creator<ParcelableObject>() {
            @Override
            public ParcelableObject createFromParcel(Parcel in) { return new ParcelableObject(in); }
            @Override
            public ParcelableObject[] newArray(int size) { return new ParcelableObject[size]; }
        };
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

    // обновляет закладки
    private void update(String tag) {
        switch (tag) {
            case TAG2:
                updateObject(((Switch) findViewById(R.id.switch_object)).isChecked());
                break;
            case TAG3:
                updateAnalytic(((Switch) findViewById(R.id.switch_analytic)).isChecked());
                break;
            default:
                break;
        }
    }

    // обновляет закладку с объектами
    private void updateObject(boolean isChecked) {

        //Фрагмент с типами объектов вида аудита на редактирование или только на просмотр в зависимости от состояния флага Изменить (переключатель Изменить)
        android.support.v4.app.Fragment typeObject;
        if (isChecked) typeObject = TypeListEdit.newInstance(CODE_OBJECT, AuditDB.TBL_OBJECT, type.objects.keySet());
        else typeObject = TypeListView.newInstance(AuditDB.TBL_OBJECT, type.objects.keySet());
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        fTrans.replace(R.id.typeobject, typeObject);
        fTrans.commit();
    }

    // обновляет закладку с аналитикой
    private void updateAnalytic(boolean isChecked) {

        // Заполняем объектами спиннер
        db.getMapByIds(AuditDB.TBL_OBJECT, mObjects, type.objects.keySet());
        sAdapter.notifyDataSetChanged();

        if (mObjects.isEmpty()) { //Если список объектов пустой, то аналитика не нужна
            isChecked = false;
            iObject = NOT_SELECTED;
        }

        // Устанавливаем текущий объект в спиннере
        int position = 0;
        if (iObject != NOT_SELECTED) for (Map<String, Object> map: mObjects) if ((int) map.get("_id") == iObject) break; else position++;
        ((Spinner) findViewById(R.id.object)).setSelection(position,true);

        //Фрагмент с типами аналитик объекта аудита на редактирование или только на просмотр в зависимости от состояния флага Изменить (переключатель Изменить)
        android.support.v4.app.Fragment typeAnalytic;
        if (isChecked)
            typeAnalytic = TypeListEdit.newInstance(CODE_ANALYTIC, AuditDB.TBL_ANALYTIC, type.objects.get(iObject));
        else
            typeAnalytic = TypeListView.newInstance(AuditDB.TBL_ANALYTIC, type.objects.get(iObject));
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        fTrans.replace(R.id.typeanalytic, typeAnalytic);
        fTrans.commit();
    }

    // интерэкшион для TypeListEdit
    @Override
    public void onTypeListEditInteraction(int requested, Items.Item item) {
        switch (requested) {
            case CODE_OBJECT: //Редактируем список объектов
                if (item.checked) type.addObject(item.id, null);
                else type.removeObject(item.id);
                break;
            case CODE_ANALYTIC: //Редактируем список аналитик
                if (item.checked) type.addAnalytic(iObject, item.id);
                else type.removeAnalytic(iObject, item.id);
                break;
        }
    }
}

