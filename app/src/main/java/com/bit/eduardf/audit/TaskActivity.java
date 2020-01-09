package com.bit.eduardf.audit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

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
        LoaderManager.LoaderCallbacks<Tasks.Task>,
        IndicatorFragment.OnCallMediaApp,
        MediaGallery.OnChangeMediaFiles{

    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private Tasks.Task task = new Tasks.Task(); //Задание

    //Поля для выбора
    private Reference typeReference;
    private Reference organizationReference;
    private Reference objectReference;
    private Reference responsibleReference;
    private Analytics analyticsObjects;
    private IndicatorFragment indicatorFragment;
    private DateTime dateTime;
    private static GetAuditor getAuditor;
    private MediaFiles.MediaFile mediaFile; //Текущий медиафайл

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
    private final static String ARG_FILLED = "filled"; //Признаки заполнения закладок
    private final static String ARG_MEDIA = "media"; //текущий медиафайл

    private final static int RC_CAMERA = 1; //Код возврата для камеры
    private final static int RC_GALLERY = 2; //Код возврата для галлереи

    // Тэги закладок
    private enum Tags {
        HEAD(-1, "head"),
        COMMON(0, "common"),
        ANALYTICS(1, "analytics"),
        INDICATORS(2, "indicators");

        int index;
        String id;

        Tags(int index, String id) {
            this.index = index;
            this.id = id;
        }

        static Tags toValue(String id) {
            switch (id) {
                case "head": return HEAD;
                case "analytics": return ANALYTICS;
                case "indicators":  return INDICATORS;
                case "common": default: return COMMON;
            }
        }
    }

    private boolean[] filledTag = {false, false, false}; // Признаки заполнения закладок

    //Обработчик выбора вида аудита
    private Reference.OnChangedReferenceKey typeListener = new Reference.OnChangedReferenceKey() {
        @Override
        public void onChangedKey(int id, String key, Object obj) {
            //Возможно, потребуется очистить объект, аналитику и показатели
            if (key == null || !key.equals(task.type_key)) {
                //Нужно предупредить юзера об ощищении полей
                task.type_key = key;
                setEnabled(key != null);
                objectReference.setKey(null);
                task.analytics.clear();
                analyticsObjects.setKeys(null);
                task.indicators.clear();
                clearFilledTag(Tags.ANALYTICS, Tags.INDICATORS);
            }
            if(obj!=null) {
                final AType type = (AType)obj;
                //Установка отбора объектов по типам
                objectReference.setParentTypes(type.objectTypes);
                //Установка видимости аналитики
                switch (type.selection) {
                    case NOT_ANALYTICS:
                        visibilityAnalytics(View.GONE);
                        break;
                    case NOT_SELECTION:
                        visibilityAnalytics(View.VISIBLE);
                        analyticsObjects.setEnabled(true);
                        analyticsObjects.setTypeObject();
                        analyticsObjects.setParentTypes();
                        break;
                    default:
                        visibilityAnalytics(View.VISIBLE);
                        analyticsObjects.setEnabled(task.object_key != null);
                }
            }
        }
    };

    //Обработчик выбора объекта аудита
    private Reference.OnChangedReferenceKey objectListener = new Reference.OnChangedReferenceKey() {
        @Override
        public void onChangedKey(int id, String key, Object obj) {
            final AType type = (AType) typeReference.getObject();
            if (type != null) {
                //Возможно, потребуется очистить аналитику
                if (key == null || !key.equals(task.object_key)) {
                    //Нужно предупредить юзера об ощищении полей
                    task.object_key = key;
                    if (type.selection != AType.Selections.NOT_SELECTION) {
                        task.analytics.clear();
                        analyticsObjects.setKeys(null);
                        clearFilledTag(Tags.ANALYTICS);
                    }
                }
                if (obj != null) {
                    final AObject object = (AObject)obj;
                    //Установка видимости и отбора аналитик
                    switch (type.selection) {
                        case HAND_LINK:
                            analyticsObjects.setTypeObject(type.id, object.id);
                            analyticsObjects.setParentTypes();
                            break;
                        case BY_TYPES:
                            analyticsObjects.setParentTypes(task.type_key, object.objectType);
                            analyticsObjects.setTypeObject();
                            break;
                        default:
                            analyticsObjects.setTypeObject();
                            analyticsObjects.setParentTypes();
                            break;
                    }
                }
            }
        }
    };

    //Обработчик закладок
    private TabHost.OnTabChangeListener tabListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String id) {
            if (!getSupportLoaderManager().hasRunningLoaders())
                fillViews(Tags.toValue(id));
        }
    };

    //Обработчик кнопки очистки комментария
    private View.OnTouchListener commentListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility") //Чтобы для onTouch не возникало предупреждение о performClick
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
    };

    //Обработчик галлереи
    private ImageView.OnClickListener galleryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(MediaGallery.newInstance(TaskActivity.this, getTitle().toString(),
                    task.id, task.mediaFiles));
        }
    };

     //Обработчик переключателя По предметам
    private CompoundButton.OnCheckedChangeListener bySubjectListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView.isPressed())
                indicatorFragment.setIndicators(task.indicators, task.type_key,
                        ((Switch) findViewById(R.id.by_subject)).isChecked());
        }
    };

    //Обработчик кнопки Заполнить
    private View.OnClickListener fillListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new LoadIndicatorRows(TaskActivity.this, oData)
                    .execute(task.type_key, task.object_key);
        }
    };

    // возвращает Интент для создания нового задания на аудит
    public static Intent intentActivityCreate(Context context, String auditor, Tasks.Task.Status status) {
        Intent intent = new Intent(context, TaskActivity.class);
        intent.putExtra(ARG_MODE, CREATE_MODE);
        intent.putExtra(ARG_STATUS, status.number);
        intent.putExtra(ARG_AUDITOR, auditor);
        instanceOf(context);
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
        instanceOf(context);
        return intent;
    }

    private static void instanceOf(Context context) {
        if (context instanceof GetAuditor) {
            getAuditor = (GetAuditor) context;
        }
    }

    // создает активность для редактирвания задания на аудит
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final String tab; //Текущая вкладка для открытия
        TaskSet taskSet = new TaskSet(this); //Настройки - предпочтения

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        setupToolBar(); //Лента инструментов
        setupStatus(); //Поле статус
        setupSaveFAB(); //Кнопка Сохранить
        final TabHost tabHost = setupTabHost(); //Вкладки
        findViewById(R.id.comment).setOnTouchListener(commentListener);
        findViewById(R.id.gallery).setOnClickListener(galleryListener);

        findViewById(R.id.fill).setOnClickListener(fillListener);
        ((Switch) findViewById(R.id.by_subject)).setOnCheckedChangeListener(bySubjectListener);

        //Находим ссылки на поля настроек
        dateTime = (DateTime) getSupportFragmentManager().findFragmentById(R.id.datetime);
        typeReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.type);
        typeReference.setOnChangedReferenceKey(typeListener);
        organizationReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.organization);
        objectReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.object);
        objectReference.setOnChangedReferenceKey(objectListener);
        responsibleReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.responsible);
        analyticsObjects = (Analytics) getSupportFragmentManager().findFragmentById(R.id.analytics);
        indicatorFragment = (IndicatorFragment) getSupportFragmentManager().findFragmentById(R.id.indicators);

        //Создает объект OData
        oData = new AuditOData(this);

        if (savedInstanceState==null) { // открываем форму впервые
            //Создаем объект Задание в зависимости от режима
            final Intent intent = getIntent();
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            tab = taskSet.openWithTab;
            ((Switch) findViewById(R.id.by_subject)).setChecked(taskSet.showSubject);
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
                    //Заполняем общие поля и текущую вкладку
                    fillViews(Tags.HEAD, Tags.COMMON, Tags.toValue(tab));
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
                    fillViews(Tags.HEAD); //Заполняем общие поля
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
                typeReference.performChangedReferenceKey();
                setEnablesImageView(R.id.gallery, task.mediaFiles.size()>0);
            }
            else { //Не успели - грузим по новой
                startLoader(getIntent().getExtras());
            }
            if (savedInstanceState.containsKey(ARG_MEDIA)) {
                final Parcelable parcelable = savedInstanceState.getParcelable(ARG_MEDIA);
                if (parcelable != null) {
                    mediaFile = ((ParcelableMedia)parcelable).mediaFile;
                }
            }
        }

        //Устанавливаем текущую вкладку
        tabHost.setCurrentTabByTag(tab);

        //Назначаем обработчики в самом конце, чтобы лишний раз не запускать
        // com.example.eduardf.audit.TaskActivity.fillViews
        tabHost.setOnTabChangedListener(tabListener);
    }

    /**
     * Настраивает вкладки
     * @return - ссылка на вкладки
     */
    private TabHost setupTabHost() {
        final TabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec tabSpec;
        tabSpec = tabHost.newTabSpec(Tags.COMMON.id);
        tabSpec.setIndicator(getString(R.string.tab_com));
        tabSpec.setContent(R.id.tab1);
        tabHost.addTab(tabSpec);
        tabSpec = tabHost.newTabSpec(Tags.ANALYTICS.id);
        tabSpec.setIndicator(getString(R.string.tab_anl));
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);
        tabSpec = tabHost.newTabSpec(Tags.INDICATORS.id);
        tabSpec.setIndicator(getString(R.string.tab_ind));
        tabSpec.setContent(R.id.tab3);
        tabHost.addTab(tabSpec);
        //Make the analytics tab invisible by default until the audit type is loaded
        visibilityAnalytics(View.GONE);
        return tabHost;
    }

    //Настраивает кнопку Сохранить
    private void setupSaveFAB() {
        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { saveTask(); }
        });
    }

    //Настраивает поле Статус
    private void setupStatus() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.status_list));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner status = findViewById(R.id.status);
        status.setAdapter(adapter);
        status.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (task!=null) task.status = Tasks.Task.Status.toValue(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    //Настраивает ленту
    private void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp
        }
    }

    //Заполняет поля активности
    private void fillViews(Tags... tags) {
        for (Tags tag: tags) {
            switch (tag) {
                case HEAD: //Поля вне закладок + часть полей с Общее
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
                    break;
                case COMMON: //поля вкладки Общее
                    if (!filledTag[Tags.COMMON.index] ) {
                        filledTag[Tags.COMMON.index] = true;
                        typeReference.setKey(task.type_key);
                        organizationReference.setKey(task.organization_key);
                        objectReference.setKey(task.object_key);
                        responsibleReference.setKey(task.responsible_key);
                        setEnablesImageView(R.id.gallery, task.mediaFiles.size()>0);
                    }
                    break;
                case ANALYTICS: //поля вкладки Аналитика
                    if (!filledTag[Tags.ANALYTICS.index]) {
                        filledTag[Tags.ANALYTICS.index] = true;
                        analyticsObjects.setKeys(task.analytics);
                    }
                    break;
                case INDICATORS: //поля вкладки Показатели
                    if (!filledTag[Tags.INDICATORS.index]) {
                        filledTag[Tags.INDICATORS.index] = true;
                        indicatorFragment.setIndicators(task.indicators, task.type_key,
                                ((Switch) findViewById(R.id.by_subject)).isChecked());
                    }
                    break;
            }
        }
    }

    /**
     * Управляет доступностью ImageView
     * @param id - идентификатор ImageView
     * @param enabled - признак доступности
     */
    private void setEnablesImageView(int id, boolean enabled) {
        final ImageButton imageButton = findViewById(id);
        imageButton.setEnabled(enabled);
        imageButton.getDrawable().mutate().setColorFilter(enabled?
                new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN):
                new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN));
    }

    /**
     * Инициализация массива признаков открытых закладок
     */
    void clearFilledTag(Tags... tags) {
        for (Tags tag: tags) {
            filledTag[tag.index] = false;
        }
    }

    /**
     * Управление видимостью вкладки с аналитикой
     * @param visibility - значение видимости: View.VISIBLE, View.INVISIBLE, View.GONE,
     */
    private void visibilityAnalytics(int visibility) {
        final TabHost tabHost = findViewById(android.R.id.tabhost);
        if (visibility != View.VISIBLE && Tags.ANALYTICS.id.equals(tabHost.getCurrentTabTag()))
            tabHost.setCurrentTabByTag(Tags.COMMON.id);
        tabHost.getTabWidget().getChildTabViewAt(Tags.ANALYTICS.index).setVisibility(visibility);
    }

    // управляет доступностью полей: объект, аналитика и показатели
    private void setEnabled(boolean enabled) {
        objectReference.setEnabled(enabled);
        indicatorFragment.setEnabled(enabled);
        analyticsObjects.setEnabled(enabled);
        setEnablesImageView(R.id.fill, enabled);
    }

    //Сохраняет все, что можно, перед поворотом экрана:
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (task != null) task.onSaveInstanceState(outState, ARG_TASK);
        outState.putBooleanArray(ARG_FILLED, filledTag);
        outState.putString(ARG_TAG, ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag()); //текущую закладку восстанавливаем в Create()
        if (mediaFile != null) outState.putParcelable(ARG_MEDIA, new ParcelableMedia(mediaFile));
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (oData != null) {
            if (task != null) {
                if (isFinishing())
                    MediaFiles.washMediaFiles(task.mediaFiles);
                task = null;
            }
            oData = null;
        }
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
     * Вызывается после нажатия на иконку Фото в показателе
     * @param media - предзаполненная запись медиафайла
     */
    @Override
    public void onCallMediaApp(MediaFiles.MediaFile media) {
        mediaFile = media;
        final String filePrefix = UUID.randomUUID().toString();
        final String dirType, fileSuffix, action;
        switch (mediaFile.type) {
            case VIDEO:
                action = MediaStore.ACTION_VIDEO_CAPTURE;
                fileSuffix = ".mp4";
                dirType = Environment.DIRECTORY_MOVIES;
                break;
            case PHOTO: default:
                action = MediaStore.ACTION_IMAGE_CAPTURE;
                fileSuffix = ".jpg";
                dirType = Environment.DIRECTORY_PICTURES;
                break;
        }
        final Intent takePictureIntent = new Intent(action);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            final File file = new File(getExternalFilesDir(dirType), filePrefix+fileSuffix);
            Uri photoURI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoURI = FileProvider.getUriForFile(this, getPackageName()+".fileprovider", file);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                photoURI = Uri.fromFile(file);
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            mediaFile.name = file.getName();
            mediaFile.path = file.getPath();
            mediaFile.date = new Date();
            mediaFile.act = MediaFiles.Act.NoAction;
            startActivityForResult(takePictureIntent, RC_CAMERA);
        }
    }

    /**
     * Вызывается после закрытия активности медиа приложения
     * @param requestCode - код запроса
     * @param resultCode - код результата
     * @param intent - интент (пустой)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case RC_CAMERA:
                if (resultCode == RESULT_OK) {
                    mediaFile.loaded = true;
                    mediaFile.act = MediaFiles.Act.Save;
                    if (getAuditor != null) {
                        mediaFile.author_key  = getAuditor.getKey();
                        mediaFile.author_name = getAuditor.getName();
                    }
                    task.mediaFiles.add(mediaFile);
                    setEnablesImageView(R.id.gallery, true);
                }
                else {
                    MediaFiles.washMediaFile(mediaFile);
                }
                mediaFile = null;
                break;
            case RC_GALLERY:
                if (resultCode == RESULT_OK) {
                    Parcelable[] parcelable = intent.getParcelableArrayExtra(MediaGallery.ARG_MEDIAFILES);
                    if (parcelable != null) {
                        task.mediaFiles.clear();
                        for (Parcelable media: parcelable) {
                            task.mediaFiles.add(((ParcelableMedia)media).mediaFile);
                        }
                    }
                }
                break;
        }
    }

    /**
     * Вызывается при при изменении записи медиафайла в галерее
     * @param updateMediaFile - медиафайл
     */
    @Override
    public void onUpdateMediaFile(int position, MediaFiles.MediaFile updateMediaFile) {
        if (task != null && position >=0 && position < task.mediaFiles.size() ) {
            final MediaFiles.MediaFile mediaFile = task.mediaFiles.get(position);
            mediaFile.act = updateMediaFile.act;
            mediaFile.loaded = updateMediaFile.loaded;
            mediaFile.path = updateMediaFile.path;
            mediaFile.comment = updateMediaFile.comment;
        }
    }

    /**
     * Вызывается при добавлении новой записи медиафайла
     * @param mediaFile - медиафайл
     */
    @Override
    public void onCreateMediaFile(int position, MediaFiles.MediaFile mediaFile) {
        if (task != null) {
            if (position >=0 && position < task.mediaFiles.size()) {
                task.mediaFiles.add(position, mediaFile);
            }
            else {
                task.mediaFiles.add(mediaFile);
            }
        }
    }

    /**
     * Сохраняет задание
     */
    private void saveTask() {
        //НУЖНО ДОБАВИТЬ ПРОВЕРКУ ЗАПОЛНЕНИЯ ОБЯЗАТЕЛЬНЫХ ПОЛЕЙ!!!
        task.date = dateTime.getDate();
        if (filledTag[Tags.COMMON.index]) {
            task.type_key = typeReference.getReferenceKey();
            task.organization_key = organizationReference.getReferenceKey();
            task.object_key = objectReference.getReferenceKey();
            task.responsible_key = responsibleReference.getReferenceKey();
            task.comment = ((EditText) findViewById(R.id.comment)).getText().toString();
        }
        if (filledTag[Tags.ANALYTICS.index]) task.analytics = analyticsObjects.getKeys();
        if (filledTag[Tags.INDICATORS.index]) task.indicators = indicatorFragment.getIndicators();
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
    public void onSaveTaskPostExecute(int status) {
        Intent intent = new Intent();
        intent.putExtra(ARG_STATUS, status);
        setResult(RESULT_OK, intent);
        finish(); //Закрываем активность
    }

    /**
     * Обработчик для получения данных об аудиторе
     */
    public interface GetAuditor {
        String getKey(); //Возвращает guid аудитора
        String getName(); //Возвращает имя аудитора
    }

    //Запускает одноразовый загрузчик задания
    private void startLoader(Bundle arg) {
        Loader loader = getSupportLoaderManager().getLoader(-1);
        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(-1, new Bundle(arg), this);
        else getSupportLoaderManager().initLoader(-1, new Bundle(arg), this);
        //Загрузчик отработает, только при первом открытии задания, потом будет убит в onLoadFinished
        //Там же будет вызван fillViews(""); - Общие поля и первая закладка
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
        final String tab = ((TabHost) findViewById(android.R.id.tabhost)).getCurrentTabTag();
        task = data;
        fillViews(Tags.COMMON); //Заполним общие поля
        if (tab!=null && !tab.equals(Tags.COMMON.id))
            fillViews(Tags.toValue(tab)); //и текущую вкладку
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
        indicatorFragment.setIndicators(task.indicators, task.type_key,
                ((Switch) findViewById(R.id.by_subject)).isChecked());
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    //Асинхронный загрузчик задания
    private static class LoadTask extends AsyncTaskLoader<Tasks.Task> {
        final Activity activity;
        final AuditOData oData;
        final String key;
        private LoadTask(@NonNull Context context, AuditOData oData, String key) {
            super(context);
            this.activity = (Activity) context;
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
            Tasks.Task task = null;
            try {
                task = oData.getTask(key);
            }
            catch (ODataErrorException e) {
                e.snackbarShow(activity, R.id.toolbar);
            }
            return task;
        }
    }

    /**
     * Настройки-предпочтения задания
     */
    private class TaskSet {

        boolean fillActualValue; //Заполнять фактические значения по умолчанию
        String openWithTab; //Открывать задание с вкладки
        boolean clearCopy; //Очищать задание при копировании
        boolean showSubject; //Показывать предметы показателей

        /**
         * Конструктор - заполняет значения из предпочтений
         * @param context - текущий контекст активности
         */
        TaskSet(Context context) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            fillActualValue = preferences.getBoolean(getString(R.string.pref_key_fill_in_fact), false);
            clearCopy = preferences.getBoolean(getString(R.string.pref_key_clear_when_copying), false);
            showSubject = preferences.getBoolean(getString(R.string.pref_key_display_subject), false);
            openWithTab = preferences.getString(getString(R.string.pref_key_task_tab), Tags.COMMON.id);
        }
    }
}
//Фома2018