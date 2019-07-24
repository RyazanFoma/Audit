package com.bit.eduardf.audit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;

import java.util.Locale;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 10.01.19 15:57
 *
 */

//Форма редактирования вида аудита
public class TypeActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<AType>
        /*TypeListEdit.OnTypeListEditInteractionListener */{

    private AType type; //Вид аудита
    private AuditOData oData; //Объект oData для доступа к базе 1С
//    private SimpleAdapter sAdapter; //адаптер спиннера с объектами для отбора аналитик
//    private ArrayList<Map<String, Object>> mObjects = new ArrayList<Map<String, Object>>(); //список объектов для спиннера, заполняется перед открытием закладки Аналитика
//    private int iObject = NOT_SELECTED; //ID текущего объекта

    //Режимы открытия формы
    private final static int CREATE_MODE = -1; //создание нового вида аудита
    private final static int EDIT_MODE = 0; //редактирвоание существующего вида аудита

    //Аргументы интент
    private final static String ARG_MODE = "mode"; //Режим
    private final static String ARG_ID = "id"; //Идентификатор вида аудита
    private final static String ARG_CODE = "code"; //Код вида аудита
    private final static String ARG_PATER = "pater"; //Родитель вида аудита

    //Аргументы для поворота экрана
    private final static String ARG_TAG = "tag"; //Текущая закладка
//    private final static String ARG_OBJECT = "object"; //Текущий объект для показа аналитики
//    private final static String ARG_OBJECTS = "objects"; //Объекты с аналитикой

    //Не указанное Id
//    private final static int NOT_SELECTED = -1;

    // Тэги закладок
    private final static String TAG1 = "tag1";
    private final static String TAG2 = "tag2";
    private final static String TAG3 = "tag3";

    //Запрашиваемые коды для редактирования списков
//    private final static int CODE_OBJECT = 1;
//    private final static int CODE_ANALYTIC = 2;

    /* возвращает Интент для создания нового вида аудита
    context - контекст формы списка видов аудита
    pater - идентификатор родителя
    */
    public static Intent intentActivityCreate(Context context, String pater) {
        Intent intent = new Intent(context, TypeActivity.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_PATER, pater);
        return intent;
    }

    /* возвращает Интент для изменения существующего вида аудита
    context - контекст формы списка видов аудита
    id - идентификатор вида аудита
    */
    public static Intent intentActivityEdit(Context context, String id) {
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
                //Заполняем значениями из полей
                type.name = ((TextInputEditText) findViewById(R.id.name)).getText().toString();
                type.criterion = (AType.Criterions) (((Spinner) findViewById(R.id.criterion_spinner)).getSelectedItem());
                type.value = Float.valueOf(((TextInputEditText) findViewById(R.id.input_value)).getText().toString());
                type.selection = (AType.Selections) (((Spinner) findViewById(R.id.select_spinner)).getSelectedItem());
                type.fillActualValue = ((Switch) findViewById(R.id.fillActualValue)).isChecked();
                type.openWithIndicators = ((Switch) findViewById(R.id.openWithIndicators)).isChecked();
                type.clearCopy = ((Switch) findViewById(R.id.clearCopy)).isChecked();
                type.showSubject = ((Switch) findViewById(R.id.showSubject)).isChecked();
                //Новый вид аудита?
                if (AuditOData.EMPTY_KEY.equals(type.id)) //Создаем
                    new Thread(new Runnable() {  public void run() { oData.createType(type); } }).start();
                else //Старый - обновляем
                    new Thread(new Runnable() {  public void run() { oData.updateType(type); } }).start();
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
            public void onTabChanged(String tabId) { /*update(tabId); */}
        });

        //Переключатель Изменить для объектов
//        Switch switchObject = (Switch) findViewById(R.id.switch_object);
//        switchObject.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { updateObject(isChecked); }
//        });

        //Переключатель Изменить для аналитики
//        Switch switchAnalytic = (Switch) findViewById(R.id.switch_analytic);
//        switchAnalytic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { updateAnalytic(isChecked); }
//        });

        // открываем подключение к БД
        oData = new AuditOData(this);

//        //Спиннер для выбора объекта. Список объектов заполняем каждый раз перед открытием вкладки Аналитика
//        sAdapter = new SimpleAdapter(this, mObjects, android.R.layout.simple_spinner_item, new String[] {AuditDB.NAME}, new int[] {android.R.id.text1});
//        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        Spinner object = (Spinner) findViewById(R.id.object);
//        object.setAdapter(sAdapter);
//        //Обработчик спиннера для выбора объекта и отбора аналитик по выбранному объекту
//        object.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                iObject = (int) mObjects.getItem(position).getItem("_id");;
//                updateAnalytic(((Switch) findViewById(R.id.switch_analytic)).isChecked());
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {}
//        });

        //Восстанавливаем основные параметры
        if (savedInstanceState == null) { // открываем форму впервые
            Intent intent = getIntent();
            switch (intent.getIntExtra(ARG_MODE, CREATE_MODE)) {
                case CREATE_MODE: //режим создания нового вида аудита
                    type = new AType();
                    type.id = AuditOData.EMPTY_KEY;
                    type.code = "Новый"; //Код будет выводиться в заголовок активности
                    type.pater = intent.getStringExtra(ARG_PATER);
                    fillViews();
                    break;
                case EDIT_MODE: //режим редактирования существующего вида аудита
                    //Запускаем загрузчик для чтения данных.
                    Loader loader = getSupportLoaderManager().getLoader(-1);
                    if (loader != null && !loader.isReset())
                        getSupportLoaderManager().restartLoader(-1, new Bundle(intent.getExtras()), this);
                    else
                        getSupportLoaderManager().initLoader(-1, new Bundle(intent.getExtras()), this);
                    //Загрузчик cработает, только при первом открытии задания, потом будет убит в onLoadFinished
                    break;
            }
            tabHost.setCurrentTabByTag(TAG1); // Закладка по умолчанию
        } else { // открываем после поворота. Восстанавливаем:
            type = new AType();
            type.id = savedInstanceState.getString(ARG_ID);
            type.code = savedInstanceState.getString(ARG_CODE);
            type.pater = savedInstanceState.getString(ARG_PATER);
            tabHost.setCurrentTabByTag(savedInstanceState.getString(ARG_TAG, TAG1)); //текущая вкладка
            setTitle( getString(R.string.txt_tp)+" ("+type.code+")");
            if (((Spinner) findViewById(R.id.criterion_spinner)).getSelectedItem() == AType.Criterions.PERCENT)
                ((TextInputLayout) findViewById(R.id.input_layout_value)).setHint(getString(R.string.txt_val_float));
            else
                ((TextInputLayout) findViewById(R.id.input_layout_value)).setHint(getString(R.string.txt_val_int));

//            iObject = savedInstanceState.getInt(ARG_OBJECT, NOT_SELECTED); //текущий объект для отбора аналитик
//            getTypeObjects(savedInstanceState.getParcelableArray(ARG_OBJECTS)); //текущий набор объектов с аналитиками
//            update(tabHost.getCurrentTabTag()); //обновляем содержимое вкладки
        }

//        Button subject = (Button) findViewById(R.id.subject);
//        if (type.id == NOT_SELECTED) //Если новый вид аудита, то предметы не доступны
//            subject.setEnabled(false);
//        else //Иначе, по кнопке вызываем активность с редактированием предметов
//            subject.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    startActivity(ReferenceEdit.intentActivity(TypeActivity.this, AuditDB.TBL_SUBJECT, "Предметы аудита", AuditDB.TBL_TYPE, type.id ));
//                }
//            });

        setupCriterion(); //Настройка поля Критерий
        setupSelect(); //Настройка поля Отбор
    }

    //Настраивает поле Критерий
    private void setupCriterion() {
        final Spinner spinnerCriterion = (Spinner) findViewById(R.id.criterion_spinner);
        final EditText criterion_input = (EditText) findViewById(R.id.input_criterion);
        ArrayAdapter<AType.Criterions> dataAdapterForSpinner1 = new ArrayAdapter<AType.Criterions>(this, android.R.layout.simple_spinner_item, AType.Criterions.values());
        dataAdapterForSpinner1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCriterion.setAdapter(dataAdapterForSpinner1);
        criterion_input.setKeyListener(null);
        criterion_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spinnerCriterion.setVisibility(View.VISIBLE);
                spinnerCriterion.performClick();
            }
        });
//        Не удалось использовать. При открытии после поворота вызывает критическую ошибку на performClick
//        criterion_input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean b) {
//                if (!b) {
//                    spinnerCriterion.setVisibility(View.VISIBLE);
//                    spinnerCriterion.performClick();
//                } else {
//                    spinnerCriterion.setVisibility(View.GONE);
//                }
//            }
//        });
        spinnerCriterion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                criterion_input.setText(spinnerCriterion.getSelectedItem().toString());
                final TextInputEditText inputValue = (TextInputEditText) findViewById(R.id.input_value);
                final float value = Float.valueOf(inputValue.getText().toString());
                if (spinnerCriterion.getSelectedItem() == AType.Criterions.PERCENT) {
                    inputValue.setText(String.format(Locale.US, "%1.3f", value)); //значение критерия
                    inputValue.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    ((TextInputLayout) findViewById(R.id.input_layout_value)).setHint(getString(R.string.txt_val_float));
                }
                else {
                    inputValue.setText(String.format(Locale.US, "%1d", (int) value)); //значение критерия
                    inputValue.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
                    ((TextInputLayout) findViewById(R.id.input_layout_value)).setHint(getString(R.string.txt_val_int));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                criterion_input.setText("");
            }
        });
    }

    //Настраивает поле Отбор
    private void setupSelect() {
        final Spinner spinnerSelect = (Spinner) findViewById(R.id.select_spinner);
        final EditText select_input = (EditText) findViewById(R.id.input_select);
        ArrayAdapter<AType.Selections> dataAdapterForSpinner = new ArrayAdapter<AType.Selections>(this, android.R.layout.simple_spinner_item, AType.Selections.values());
        dataAdapterForSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelect.setAdapter(dataAdapterForSpinner);
        select_input.setKeyListener(null);
        select_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spinnerSelect.setVisibility(View.VISIBLE);
                spinnerSelect.performClick();
            }
        });
//        Не удалось использовать. При открытии после поворота вызывает критическую ошибку на performClick
//        select_input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean b) {
//                if (!b) {
//                    spinnerSelect.setVisibility(View.VISIBLE);
//                    spinnerSelect.performClick();
//                } else {
//                    spinnerSelect.setVisibility(View.GONE);
//                }
//            }
//        });
        spinnerSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                select_input.setText(spinnerSelect.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                select_input.setText("");
            }
        });
    }

    //Заполнение полей активности значенями из объекта type
    private void fillViews() {
        setTitle( getString(R.string.txt_tp)+" ("+type.code+")");
        ((TextInputEditText) findViewById(R.id.name)).setText(type.name); //наименование
        if (type.criterion != null) {
            ((EditText) findViewById(R.id.input_criterion)).setText(type.criterion.toString()); //наименование критерия
            ((Spinner) findViewById(R.id.criterion_spinner)).setSelection(type.criterion.ordinal());
            if (type.criterion == AType.Criterions.PERCENT) {
                ((EditText) findViewById(R.id.input_value)).setText(String.format(Locale.US, "%1.3f", type.value)); //значение критерия
                ((EditText) findViewById(R.id.input_value)).setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                ((TextInputLayout) findViewById(R.id.input_layout_value)).setHint(getString(R.string.txt_val_float));
            }
            else {
                ((EditText) findViewById(R.id.input_value)).setText(String.format(Locale.US, "%1d", (int) type.value)); //значение критерия
                ((EditText) findViewById(R.id.input_value)).setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
                ((TextInputLayout) findViewById(R.id.input_layout_value)).setHint(getString(R.string.txt_val_int));
            }
        }
        if (type.selection != null) {
            ((EditText) findViewById(R.id.input_select)).setText(type.selection.toString()); //наименование вида отбора
            ((Spinner) findViewById(R.id.select_spinner)).setSelection(type.selection.ordinal());
        }
        ((Switch) findViewById(R.id.fillActualValue)).setChecked(type.fillActualValue);
        ((Switch) findViewById(R.id.openWithIndicators)).setChecked(type.openWithIndicators);
        ((Switch) findViewById(R.id.clearCopy)).setChecked(type.clearCopy);
        ((Switch) findViewById(R.id.showSubject)).setChecked(type.showSubject);
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_ID, type.id);
        outState.putString(ARG_CODE, type.code);
        outState.putString(ARG_PATER, type.pater);
        outState.putString(ARG_TAG, ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag());
//        outState.putInt(ARG_OBJECT, iObject); //значение объекта для отбора аналитики
//        outState.putParcelableArray(ARG_OBJECTS, putTypeObjects()); //текущий набор объектов с аналитиками
    }

//    // восстанавливает список объектов с аналитикой
//    private void getTypeObjects(Parcelable[] in) {
//        for (Parcelable parcelable: in)
//            type.addObject(((ParcelableObject) parcelable).key, ((ParcelableObject) parcelable).analytics);
//    }
//
//    // возвращает массив парселейбл с объектами и аналитикой
//    private Parcelable[] putTypeObjects() {
//        Parcelable[] result = new Parcelable[type.objects.size()];
//        int i = 0;
//        for (Integer key:  type.objects.keySet()) result[i++] = new ParcelableObject(key, type.objects.getItem(key));
//        return result;
//    }
//
//    //Класс для сохранения списка объектов с аналитикой при повороте экрана
//    private static class ParcelableObject implements Parcelable {
//
//        int key; //Объект - ключь
//        Set<Integer> analytics; //Список аналитики объекта
//
//        //Конструктор обычный
//        private ParcelableObject(int key, Set<Integer> analytics) {
//            this.key = key;
//            this.analytics = analytics;
//        }
//
//        //Конструктор из парселя
//        protected ParcelableObject(Parcel in) {
//            key = in.readInt(); //Считываем объект
//            int size_analytic = in.readInt(); //Считываем количество аналитик
//            analytics = new ArraySet<Integer>(in.readInt());
//            for(int i=0;i<size_analytic;i++) analytics.addItem(in.readInt()); //Добавляем все аналитики
//        }
//
//        //Пишем объект в парсель
//        @Override
//        public void writeToParcel(Parcel dest, int flags) {
//            dest.writeInt(key); //Пишем объект
//            dest.writeInt(analytics.size()); //Пишем количество аналитик
//            for(Integer a: analytics) dest.writeInt(a); //Пишем все аналитики
//        }
//
//        //по умолчанию
//        @Override
//        public int describeContents() {
//            return 0;
//        }
//
//        //Создатель парсель объекта
//        public static final Creator<ParcelableObject> CREATOR = new Creator<ParcelableObject>() {
//            @Override
//            public ParcelableObject createFromParcel(Parcel in) { return new ParcelableObject(in); }
//            @Override
//            public ParcelableObject[] newArray(int size) { return new ParcelableObject[size]; }
//        };
//    }

    //при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        oData = null;
    }

    //Обработчик возврата назад
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @NonNull
    @Override
    public Loader<AType> onCreateLoader(int id, @Nullable Bundle args) {
        return new FillType(TypeActivity.this, oData, args.getString(ARG_ID));
    }

    @Override
    public void onLoadFinished(@NonNull Loader<AType> loader, AType data) {
        type = data;
        fillViews();
        getSupportLoaderManager().destroyLoader(-1); //Убиваем загрузчик, т.к. он нужен только на один раз при первом отрытии задания
    }

    @Override
    public void onLoaderReset(@NonNull Loader<AType> loader) {
        type = null;
    }

    // обновляет закладки
//    private void update(String tag) {
//        switch (tag) {
//            case TAG2:
//                updateObject(((Switch) findViewById(R.id.switch_object)).isChecked());
//                break;
//            case TAG3:
//                updateAnalytic(((Switch) findViewById(R.id.switch_analytic)).isChecked());
//                break;
//            default:
//                break;
//        }
//    }

    // обновляет закладку с объектами
//    private void updateObject(boolean isChecked) {
//
//        //Фрагмент с типами объектов вида аудита на редактирование или только на просмотр в зависимости от состояния флага Изменить (переключатель Изменить)
//        android.support.v4.app.Fragment typeObject;
//        if (isChecked) typeObject = TypeListEdit.newInstance(CODE_OBJECT, AuditDB.TBL_OBJECT, type.objects.keySet());
//        else typeObject = TypeListView.newInstance(AuditDB.TBL_OBJECT, type.objects.keySet());
//        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
//        fTrans.replace(R.id.typeobject, typeObject);
//        fTrans.commit();
//    }

    // обновляет закладку с аналитикой
//    private void updateAnalytic(boolean isChecked) {
//
//        // Заполняем объектами спиннер
//        db.getMapByIds(AuditDB.TBL_OBJECT, mObjects, type.objects.keySet());
//        sAdapter.notifyDataSetChanged();
//
//        if (mObjects.isEmpty()) { //Если список объектов пустой, то аналитика не нужна
//            isChecked = false;
//            iObject = NOT_SELECTED;
//        }
//
//        // Устанавливаем текущий объект в спиннере
//        int position = 0;
//        if (iObject != NOT_SELECTED) for (Map<String, Object> map: mObjects) if ((int) map.getItem("_id") == iObject) break; else position++;
//        ((Spinner) findViewById(R.id.object)).setSelection(position,true);
//
//        //Фрагмент с типами аналитик объекта аудита на редактирование или только на просмотр в зависимости от состояния флага Изменить (переключатель Изменить)
//        android.support.v4.app.Fragment typeAnalytic;
//        if (isChecked)
//            typeAnalytic = TypeListEdit.newInstance(CODE_ANALYTIC, AuditDB.TBL_ANALYTIC, type.objects.getItem(iObject));
//        else
//            typeAnalytic = TypeListView.newInstance(AuditDB.TBL_ANALYTIC, type.objects.getItem(iObject));
//        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
//        fTrans.replace(R.id.typeanalytic, typeAnalytic);
//        fTrans.commit();
//    }

    // интерэкшион для TypeListEdit
//    @Override
//    public void onTypeListEditInteraction(int requested, Items.Item item) {
//        switch (requested) {
//            case CODE_OBJECT: //Редактируем список объектов
//                if (item.checked) type.addObject(item.id, null);
//                else type.removeObject(item.id);
//                break;
//            case CODE_ANALYTIC: //Редактируем список аналитик
//                if (item.checked) type.addAnalytic(iObject, item.id);
//                else type.removeAnalytic(iObject, item.id);
//                break;
//        }
//    }

    //Асинхронный загрузчик заполнения задания. Можно использовать для заполнения задания при развертывании пункта
    private static class FillType extends AsyncTaskLoader<AType> {

        AuditOData oData;
        String key;

        public FillType(@NonNull Context context, AuditOData oData, String key) {
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
        public AType loadInBackground() {
            return oData.getAType(key);
        }
    }
}

