package com.example.eduardf.audit;

import android.graphics.ColorFilter;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.Nullable;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 07.12.18 10:25
 *
 */

//Выбор и редактирование справочников: Виды аудита, Объекты, Аналитика
public class ReferenceChoice extends AppCompatActivity implements
        View.OnClickListener,
        View.OnLongClickListener,
        DialogsReferenceManager.DialogInteractionListener,
        LoaderManager.LoaderCallbacks<Items> {

    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private int iRC = -1; //Сквозной код для идентификации результата выбора
    private AuditOData.Set sTable; //Имя таблицы
    private Bundle bArgs; //Агрументы для загрузчика списка
    private ArrayList<String> ids; //Текущие выбранные элементы для подсветки
    private ArrayList<String> checkedIdsCopyMove; //Список отмеченных для копирования/переноса
    private String checkedFrom; //Папка, откуда перемещаем/копируем отмеченные

    //Постраничная загрузка
    private CurrentPage currentPage = new CurrentPage(12); //Текущая страница загрузки
    private boolean loadingPage = false; //Признак процесса загрузки

    private Stack myStack; //Стек с именами предков

    private ReferenceAdapter recyclerAdapter; //Адаптер для RecyclerView
    private RecyclerView.LayoutManager mLayoutManager; //Менеджер для RecyclerView

    private static OnReferenceManagerInteractionSingleChoice mSingleChoice;
    private static OnReferenceManagerInteractionMultipleChoice mMultipleChoice;

    private ActionMode mActionMode; //Контекстное меню

    private int iModeChoice; //Текущий режим выбора:
    static final int MODE_SINGLE_CHOICE = 1; //Режим одиночного выбора
    static final int MODE_MULTIPLE_CHOICE = 2; //Режим множественного выбора

    private int iModeMenu; //Текущий режим меню:
    static final int ACTION_BAR = 0; //меню действий
    static final int ACTION_MODE = 1; //контекстное меню
    static final int ACTION_COPY = 2; //копирование
    static final int ACTION_MOVE = 3; //перемещение

    private static final String ARG_RC = "requestCode"; //Сквозной код для идентификации результата выбора
    private static final String ARG_TITLE = "title"; //Заголовок activity
    private static final String ARG_TABLE = "table"; //Таблица элементов
    private static final String ARG_OWNER = "owner"; //Владелец справочника
    private static final String ARG_MODE = "mode"; //Режим выбора
    private static final String ARG_ID = "id"; //Текущие отмеченные идентификаторы элементов

    private static final String ARG_PATER = "pater"; //Текущий родитель
    private final static String ARG_STACK = "stack";
    private static final String ARG_STATE = "state"; //Состояние RecyclerView
    private static final String ARG_LIKE = "like"; //Строка поиска
    private static final String ARG_MODE_MENU = "mode_action"; //Режим действий
    private static final String ARG_PAGE = "page";
    private static final String ARG_LOADING = "loading";
    private static final String ARG_LAST = "last";
    private static final String ARG_COPY_MOVE_ID = "copy_move_id"; //пункты для копирования и переноса
    private static final String ARG_FROM = "from"; //Статус, откуда копируем/перемещаем
    private static final String ARG_PARENTTYPES = "parenttype"; //Типы родительских справочников
    private static final String ARG_TYPEKEY = "typekey"; //Вид аудита для отбора по ручным связям
    private static final String ARG_OBJECTKEY = "objectkey"; //Объект для отбора по ручным связям

    //ВОЗВРАЩАЕТ ИНТЕНТ ДЛЯ АКТИВНОСТИ
    /*  context - контекст родительской активности, откуда вызывается наша активность
        fragment - фрагмент, из которого вызывается активность для выбора
        requestCode - уникальный код, для возврата в коллбэк, чтобы отличить - что редактировали
        table - идентификатор справочника
        title - заголовок нашей активности
        id - идентификатор (для одиночного выбора) или список идентификаторов (для множественного
        выбора) элементов справочника для подстветки / null
        owner - идентификатор владельца справочника для отбора / null
        parentTypes - типы родительских справочников / null
        typeKey - guid вида аудита
        objectKey - guid объекта аудита
     */
    //в режиме одиночного выбора из фрагмента
    public static Intent intentActivity(Fragment fragment, AuditOData.Set table, String title,
                                        String owner, String id) {
        instanceOf(fragment, MODE_SINGLE_CHOICE);
        ArrayList<String> ids = new ArrayList<>();
        if (!(id == null || id.isEmpty())) ids.add(id);
        return newIntent(fragment.getActivity(), MODE_SINGLE_CHOICE, -1, table, title, owner, ids);
    }
    //с отбором по типам родительских справочников
    public static Intent intentActivity(Fragment fragment, AuditOData.Set table, String title,
                                        String owner, ArrayList<String> parentTypes,
                                        String id) {
        Intent intent = intentActivity(fragment, table, title, owner, id);
        intent.putStringArrayListExtra(ARG_PARENTTYPES, parentTypes);
        return intent;
    }
    //с отбором по ручной связи
    public static Intent intentActivity(Fragment fragment, AuditOData.Set table, String title,
                                        String owner, String typeKey, String objectKey,
                                        String id) {
        Intent intent = intentActivity(fragment, table, title, owner, id);
        intent.putExtra(ARG_TYPEKEY, typeKey);
        intent.putExtra(ARG_OBJECTKEY, objectKey);
        return intent;
    }
    //в режиме одиночного выбора из активности
    public static Intent intentActivity(Context context, int requestCode, AuditOData.Set table,
                                        String title, String owner, String id) {
        instanceOf(context, MODE_SINGLE_CHOICE);
        ArrayList<String> ids = new ArrayList<>();
        if (!(id == null || id.isEmpty())) ids.add(id);
        return newIntent(context, MODE_SINGLE_CHOICE, requestCode, table, title, owner, ids);
    }
    //с отбором по типам родительских справочников
    public static Intent intentActivity(Context context, int requestCode, AuditOData.Set table,
                                        String title, String owner, ArrayList<String> parentTypes,
                                        String id) {
        Intent intent = intentActivity(context, requestCode, table, title, owner, id);
        intent.putStringArrayListExtra(ARG_PARENTTYPES, parentTypes);
        return intent;
    }
    //с отбором по ручной связи
    public static Intent intentActivity(Context context, int requestCode, AuditOData.Set table,
                                        String title, String owner, String typeKey, String objectKey,
                                        String id) {
        Intent intent = intentActivity(context, requestCode, table, title, owner, id);
        intent.putExtra(ARG_TYPEKEY, typeKey);
        intent.putExtra(ARG_OBJECTKEY, objectKey);
        return intent;
    }
    //в режиме множественного выбора из фрагмента
    public static Intent intentActivity(Context context, Fragment fragment, int requestCode,
                                        AuditOData.Set table, String title, String owner,
                                        ArrayList<String> ids) {
        instanceOf(fragment, MODE_MULTIPLE_CHOICE);
        return newIntent(context, MODE_MULTIPLE_CHOICE, requestCode, table, title, owner,
                ids != null? ids: new ArrayList<String>());
    }
    //с отбором по типам родительских справочников
    public static Intent intentActivity(Context context, Fragment fragment, int requestCode,
                                        AuditOData.Set table, String title, String owner,
                                        ArrayList<String> parentTypes,
                                        ArrayList<String> id) {
        Intent intent = intentActivity(context, fragment, requestCode, table, title, owner, id);
        intent.putStringArrayListExtra(ARG_PARENTTYPES, parentTypes);
        return intent;
    }
    //с отбором по ручной связи
    public static Intent intentActivity(Context context, Fragment fragment, int requestCode,
                                        AuditOData.Set table, String title, String owner,
                                        String typeKey, String objectKey,
                                        ArrayList<String> id) {
        Intent intent = intentActivity(context, fragment, requestCode, table, title, owner, id);
        intent.putExtra(ARG_TYPEKEY, typeKey);
        intent.putExtra(ARG_OBJECTKEY, objectKey);
        return intent;
    }
    //в режиме множественного выбора из активности
    public static Intent intentActivity(Context context, int requestCode, AuditOData.Set table,
                                        String title, String owner, ArrayList<String> ids) {
        instanceOf(context, MODE_MULTIPLE_CHOICE);
        return newIntent(context, MODE_MULTIPLE_CHOICE, requestCode, table, title, owner,
                ids != null? ids: new ArrayList<String>());
    }
    //с отбором по типам родительских справочников
    public static Intent intentActivity(Context context, int requestCode, AuditOData.Set table,
                                        String title, String owner, ArrayList<String> parentTypes,
                                        ArrayList<String> id) {
        Intent intent = intentActivity(context, requestCode, table, title, owner, id);
        intent.putStringArrayListExtra(ARG_PARENTTYPES, parentTypes);
        return intent;
    }
    //с отбором по ручной связи
    public static Intent intentActivity(Context context, int requestCode, AuditOData.Set table,
                                        String title, String owner,
                                        String typeKey, String objectKey, ArrayList<String> id) {
        Intent intent = intentActivity(context, requestCode, table, title, owner, id);
        intent.putExtra(ARG_TYPEKEY, typeKey);
        intent.putExtra(ARG_OBJECTKEY, objectKey);
        return intent;
    }

    private static Intent newIntent(Context context, int mode, int requestCode, AuditOData.Set table,
                                    String title, String owner, ArrayList<String> ids) {
        Intent intent = new Intent(context, ReferenceChoice.class);
        intent.putExtra(ARG_MODE, mode);
        intent.putExtra(ARG_RC, requestCode);
        intent.putExtra(ARG_TABLE, table.toString());
        intent.putExtra(ARG_OWNER, owner);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_ID, ids);
        return intent;
    }

    //Проверяет наличие обработчика выбора в предыдущей активности
    private static void instanceOf(Object context, int mode) {
        switch (mode) {
            case MODE_SINGLE_CHOICE:
                if (context instanceof OnReferenceManagerInteractionSingleChoice) {
                    mSingleChoice = (OnReferenceManagerInteractionSingleChoice) context;
                } else {
                    throw new RuntimeException(context.toString()
                            + " must implement OnReferenceManagerInteractionSingleChoice");
                }
                break;
            case MODE_MULTIPLE_CHOICE:
                if (context instanceof OnReferenceManagerInteractionMultipleChoice) {
                    mMultipleChoice = (OnReferenceManagerInteractionMultipleChoice) context;
                } else {
                    throw new RuntimeException(context.toString()
                            + " must implement OnReferenceManagerInteractionMultipleChoice");
                }
                break;
        }
    }

    //Обработчик выбора пункта нижнего навигационного меню
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.create_group:
                    DialogsReferenceManager.newInstance(ReferenceChoice.this).
                            show(getSupportFragmentManager(), DialogsReferenceManager.TAG_CREATE_GROUP);
                    return true;
                case R.id.copy:
                    recyclerAdapter.copyRows(checkedIdsCopyMove, myStack.peek().id);
                    recyclerAdapter.setModeMenu(iModeMenu = ACTION_MODE);
                    mActionMode.invalidate();
                    return true;
                case R.id.move:
                    if (!checkedFrom.equals(myStack.peek().id)) {
                        recyclerAdapter.moveRows(checkedIdsCopyMove, myStack.peek().id);
                    }
                    recyclerAdapter.setModeMenu(iModeMenu = ACTION_MODE);
                    mActionMode.invalidate();
                    return true;
                case R.id.close:
                    checkedIdsCopyMove = null;
                    recyclerAdapter.updateStatus(false);
                    recyclerAdapter.notifyDataSetChanged(iModeMenu = ACTION_MODE);
                    mActionMode.invalidate();
                    return true;
                case R.id.create:
                    DialogsReferenceManager.newInstance(ReferenceChoice.this).
                            show(getSupportFragmentManager(), DialogsReferenceManager.TAG_CREATE);
                    return true;
                default:
                    return false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_choice);

        String sOwner; //Владелец
        String sPater = AuditOData.EMPTY_KEY; //Текущий родитель
        String sTitle; //Заголовок активности
        String sLike = ""; //Строка для отбора
        ArrayList<String> parentTypes = null; //Типы родительских справочников
        String typeKey = null; //Guid вида аудита для отбора по ручным связям
        String objectKey = null; //Guid объекта для отбора по ручным связям

        //Меню действий
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Создает объект OData
        oData = new AuditOData(this);

        bArgs = new Bundle(); //Аргументы для загрузчика
        myStack = new Stack() {
            @Override
            Item getItem(String id) {
                return null;
            }
        }; //Предки

        if (savedInstanceState==null) { //активность запускается впервые
            Intent intent = getIntent();
            iRC = intent.getIntExtra(ARG_RC, -1); //Сквозной код для идентификации результата выбора
            sTitle = intent.getStringExtra(ARG_TITLE);
            sTable = AuditOData.Set.toValue(intent.getStringExtra(ARG_TABLE));
            sOwner = intent.getStringExtra(ARG_OWNER);
            iModeChoice = intent.getIntExtra(ARG_MODE, MODE_SINGLE_CHOICE); //Режим выбора. По умолчанию - одиночный выбор
            iModeMenu = ACTION_BAR;
            ids = intent.getStringArrayListExtra(ARG_ID); //Текущие выбранные элементы
            Items.Item item = new Items.Item();
            item.id = AuditOData.EMPTY_KEY;
            item.name = getResources().getString(R.string.btn_top);
            myStack.push(item);
            if (intent.hasExtra(ARG_PARENTTYPES))
                parentTypes = intent.getStringArrayListExtra(ARG_PARENTTYPES);
            if (intent.hasExtra(ARG_TYPEKEY)) {
                typeKey = intent.getStringExtra(ARG_TYPEKEY);
                objectKey = intent.getStringExtra(ARG_OBJECTKEY);
            }
        }
        else { //активность восстатавливаем после поворота экрана
            iRC = savedInstanceState.getInt(ARG_RC);
            sTitle = savedInstanceState.getString(ARG_TITLE);
            sTable = AuditOData.Set.toValue(savedInstanceState.getString(ARG_TABLE));
            sOwner = savedInstanceState.getString(ARG_OWNER);
            iModeChoice = savedInstanceState.getInt(ARG_MODE);
            iModeMenu = savedInstanceState.getInt(ARG_MODE_MENU, ACTION_BAR); //Режим меню
            ids = savedInstanceState.getStringArrayList(ARG_ID);
            sLike = savedInstanceState.getString(ARG_LIKE, "");
            sPater = savedInstanceState.getString(ARG_PATER);
            if (savedInstanceState.containsKey(ARG_PARENTTYPES))
                parentTypes = savedInstanceState.getStringArrayList(ARG_PARENTTYPES);
            if (savedInstanceState.containsKey(ARG_TYPEKEY)) {
                typeKey = savedInstanceState.getString(ARG_TYPEKEY);
                objectKey = savedInstanceState.getString(ARG_OBJECTKEY);
            }
        }
        bArgs.putString(ARG_TABLE, sTable.toString());
        bArgs.putString(ARG_OWNER, sOwner);
        bArgs.putString(ARG_PATER, sPater);
        bArgs.putString(ARG_LIKE, sLike);
        bArgs.putStringArrayList(ARG_PARENTTYPES, parentTypes);
        bArgs.putString(ARG_TYPEKEY, typeKey);
        bArgs.putString(ARG_OBJECTKEY, objectKey);

        setTitle(sTitle); //Заголовок активности

        //Расчитываем кол-во колонок для Grid и создаем GridLayoutManager для рециклервью
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mLayoutManager = new GridLayoutManager(this,
                Math.max(1, Math.round(((float) metrics.widthPixels) /
                        getResources().getDimension(R.dimen.min_column_reference))));

        // настраиваем список
        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerAdapter = new ReferenceAdapter(this, oData, sTable, ids, myStack,
                iModeChoice, iModeMenu);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addOnScrollListener(new PaginationListener((GridLayoutManager) mLayoutManager) {
            @Override
            protected void loadMoreItems() {
                //Запускаем загрузчик для чтения данных
                Loader loader = getSupportLoaderManager().getLoader(0);
                if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(0, bArgs, ReferenceChoice.this);
                else getSupportLoaderManager().initLoader(0, bArgs, ReferenceChoice.this);
            }

            @Override
            public boolean isLastPage() {
                return currentPage.isLastPage();
            }

            @Override
            public boolean isLoading() {
                return loadingPage;
            }
        });

        //Загружаем первую страницу
        if (savedInstanceState == null) loadFirstPage();
    }

    private void loadFirstPage() {
        currentPage.setPageByPosition(0);
        recyclerAdapter.clearList();
        Loader loader = getSupportLoaderManager().getLoader(0);
        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(0, bArgs, ReferenceChoice.this);
        else getSupportLoaderManager().initLoader(0, bArgs, ReferenceChoice.this);
    }

    @Override
    public boolean onLongClick(View v) {
        if (sTable.editable && v.getId() == R.id.item) { //Таблицу можно редактировать
            if (mActionMode != null) {
                return false;
            }
            recyclerAdapter.setCheckedAll(false, false); //Снимаем отметки со всех строк
            ((Items.Item) v.getTag()).checked = true; //Отмечаем на которой долго нажимали
            iModeMenu = ACTION_MODE;
            recyclerAdapter.updateStatus(false);
            recyclerAdapter.notifyDataSetChanged(iModeMenu);
            // Start the CAB using the ActionMode.Callback defined above
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        else { //Таблица не редактируемая
            Snackbar.make(v, R.string.msg_not_editable, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
        return true;
    }

    //Класс для определения текущего родителя
//    private class LoadPater extends AsyncTask<String, Void, String> {
//        private AuditOData.Set table;
//        private LoadPater(AuditOData.Set table) {
//            this.table = table;
//        }
//        @Override
//        protected void onPreExecute() {
//            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
//        }
//        @Override
//        protected String doInBackground(String... key) {
//            return oData.getItem(table, key[0]).pater;
//        }
//        @Override
//        protected void onPostExecute(String pater) {
//            bArgs.putString(ARG_PATER, pater);
//            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
//        }
//    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    // перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, getTitle().toString());
        outState.putInt(ARG_RC, iRC);
        outState.putInt(ARG_MODE, iModeChoice);
        outState.putString(ARG_TABLE, sTable.toString());
        outState.putString(ARG_OWNER, bArgs.getString(ARG_OWNER));
        outState.putString(ARG_PATER, myStack.peek().id);
        outState.putString(ARG_LIKE, bArgs.getString(ARG_LIKE));
        outState.putStringArrayList(ARG_PARENTTYPES, bArgs.getStringArrayList(ARG_PARENTTYPES));
        outState.putString(ARG_TYPEKEY, bArgs.getString(ARG_TYPEKEY));
        outState.putString(ARG_OBJECTKEY, bArgs.getString(ARG_OBJECTKEY));
        outState.putParcelable(ARG_STATE, mLayoutManager.onSaveInstanceState());
        outState.putInt(ARG_MODE_MENU, iModeMenu); //Режим меню
        outState.putStringArrayList(ARG_ID, ids);
        recyclerAdapter.onSaveInstanceState(outState);
        myStack.onSaveInstanceState(outState, ARG_STACK);
        if (iModeMenu == ACTION_COPY | iModeMenu == ACTION_MOVE) {
            outState.putStringArrayList(ARG_COPY_MOVE_ID, checkedIdsCopyMove);
            outState.putString(ARG_FROM, checkedFrom);
        }
        outState.putInt(ARG_PAGE, currentPage.skip()); //Текущая страница загрузки
        outState.putBoolean(ARG_LOADING, loadingPage);
        outState.putBoolean(ARG_LAST, currentPage.isLastPage());
    }

    // после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_STATE)); //Состояние списка
        recyclerAdapter.onRestoreInstanceState(savedInstanceState);
        //Если до поворота экрана было запущено контекстное меню, то открываем его опять
        if (iModeMenu !=ACTION_BAR && mActionMode == null)
            mActionMode = startSupportActionMode(mActionModeCallback);
        ids = savedInstanceState.getStringArrayList(ARG_ID);
        myStack.onRestoreInstanceState(savedInstanceState, ARG_STACK);
        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), this);
        if (iModeMenu == ACTION_COPY | iModeMenu == ACTION_MOVE) {
            checkedIdsCopyMove = savedInstanceState.getStringArrayList(ARG_COPY_MOVE_ID);
            checkedFrom = savedInstanceState.getString(ARG_FROM, checkedFrom);
        }
        currentPage.setPageByPosition(savedInstanceState.getInt(ARG_PAGE));
        loadingPage = savedInstanceState.getBoolean(ARG_LOADING);
        currentPage.setLastPage(savedInstanceState.getBoolean(ARG_LAST));
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ids = null;
        recyclerAdapter = null;
        mLayoutManager = null;
        mActionMode = null;
        myStack = null;
        // закрываем подключение при выходе
        oData = null;
    }

    //ВСЕ ДЛЯ МЕНЮ ДЕЙСТВИЙ:
    //Инициализирует SearchView для меню
    private void initSearchView(MenuItem searchItem) {
        SearchView searchView = (SearchView) searchItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (null != searchManager) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        searchView.setIconifiedByDefault(true); //Поиск свернут по умолчанию
        searchView.setQueryHint(getString(R.string.search_hint_name));

        //Обработчик текста запроса для поиска
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!bArgs.getString(ARG_LIKE, "").equals(query)) {
                    bArgs.putString(ARG_LIKE, query);
                    loadFirstPage();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    bArgs.putString(ARG_LIKE, "");
                    loadFirstPage();
                }
                return false;
            }
        });
    }

    //Создает меню действий
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_reference_manager, menu);
        initSearchView(menu.findItem(R.id.search)); //инициализируем поиск
        return true;
    }

    //Готовит меню действий
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /*|| getResources().getConfiguration().orientation==ORIENTATION_PORTRAIT*/
        if (iModeChoice==MODE_SINGLE_CHOICE) menu.setGroupVisible(R.id.groupe_choice,false);
        else {
            (menu.findItem(R.id.allmark)).setVisible(recyclerAdapter.checkedStatus()!=ReferenceAdapter.CHECKED_STATUS_ALL);
            (menu.findItem(R.id.unmark)).setVisible(!(menu.findItem(R.id.allmark)).isVisible());
            (menu.findItem(R.id.choice)).setVisible(recyclerAdapter.checkedStatus()!=ReferenceAdapter.CHECKED_STATUS_NULL);
        }

        //Если есть текст запроса,
        String sLike = bArgs.getString(ARG_LIKE);
        if (!(sLike == null || sLike.isEmpty())) { //то переходим в режим поиска
            MenuItem searchItem = menu.findItem(R.id.search);
            searchItem.expandActionView();
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setIconified(false);
            searchView.setQuery(sLike, true);
        }
        else {
            MenuItem searchItem = menu.findItem(R.id.search);
            searchItem.collapseActionView();
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setIconified(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    // вызывается при нажатии на пункт меню действий
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.choice:
                if (null != mMultipleChoice)
                    mMultipleChoice.onReferenceManagerInteractionMultipleChoice(iRC,
                            recyclerAdapter.getCheckedItems());
                finish(); //Закрываем активность
                return true;
            case R.id.allmark:
                recyclerAdapter.setCheckedAll(true, true);
                invalidateOptionsMenu();
                return true;
            case R.id.unmark:
                recyclerAdapter.setCheckedAll(false, true);
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Обработчик возврата назад
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //ВСЕ ДЛЯ КОНТЕКСТНОГО МЕНЮ
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.cont_reference_manager, menu);
            initSearchView(menu.findItem(R.id.search)); //инициализируем поиск
            setVisibilityBNV(true); //Устанавливаем видимость навигационного меню
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            switch (iModeMenu) {
                case ACTION_MODE:
                    menu.setGroupVisible(R.id.is_checked, true);
                    menu.setGroupVisible(R.id.mark, true);
                    //Меняем цвета и доступность иконок
                    final Drawable ic_edit = (menu.findItem(R.id.edit)).getIcon();
                    final Drawable ic_copy = (menu.findItem(R.id.copy)).getIcon();
                    final Drawable ic_move = (menu.findItem(R.id.move)).getIcon();
                    final Drawable ic_delete = (menu.findItem(R.id.delete)).getIcon();
                    final ColorFilter colorFilterGrey = new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    if (recyclerAdapter.checkedStatus()==ReferenceAdapter.CHECKED_STATUS_ONE) {
                        (menu.findItem(R.id.edit)).setEnabled(true);
                        ic_edit.mutate().setColorFilter(null);
                    }
                    else {
                        (menu.findItem(R.id.edit)).setEnabled(false);
                        ic_edit.mutate().setColorFilter(colorFilterGrey);
                    }
                    ColorFilter colorFilterCheked;
                    if (recyclerAdapter.checkedStatus()!=ReferenceAdapter.CHECKED_STATUS_NULL) {
                        menu.setGroupEnabled(R.id.is_checked, true);
                        colorFilterCheked = null;
                    }
                    else {
                        menu.setGroupEnabled(R.id.is_checked, false);
                        colorFilterCheked = colorFilterGrey;
                    }
                    ic_copy.mutate().setColorFilter(colorFilterCheked);
                    ic_move.mutate().setColorFilter(colorFilterCheked);
                    ic_delete.mutate().setColorFilter(colorFilterCheked);
                    //Триггер: Отметить/Снять
                    (menu.findItem(R.id.allmark)).setVisible(
                            recyclerAdapter.checkedStatus()!=ReferenceAdapter.CHECKED_STATUS_ALL);
                    (menu.findItem(R.id.unmark)).setVisible(!(menu.findItem(R.id.allmark)).isVisible());
                    break;
                case ACTION_COPY:
                    menu.setGroupVisible(R.id.is_checked, false);
                    menu.setGroupVisible(R.id.mark, false);
                    break;
                case ACTION_MOVE:
                    menu.setGroupVisible(R.id.is_checked, false);
                    menu.setGroupVisible(R.id.mark, false);
                    break;
            }

            //Устанавливаем видимость значков в нижнем навигационном меню
            setVisibilityIconBNV();

            //Если есть текст запроса,
            String sLike = bArgs.getString(ARG_LIKE);
            if (!(sLike == null || sLike.isEmpty())) { //то переходим в режим поиска
                MenuItem searchItem = menu.findItem(R.id.search);
                searchItem.expandActionView();
                SearchView searchView = (SearchView) searchItem.getActionView();
                searchView.setIconified(false);
                searchView.setQuery(sLike, true);
            }
            else { //выходим из режима поиска
                MenuItem searchItem = menu.findItem(R.id.search);
                searchItem.collapseActionView();
                SearchView searchView = (SearchView) searchItem.getActionView();
                searchView.setIconified(true);
            }

            return true; // Return false if nothing is done
        }

        // вызывается по нажатию на пункт контекстного меню
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.allmark: //Отметить все
                    recyclerAdapter.setCheckedAll(true, false);
                    mActionMode.invalidate();
                    return true;
                case R.id.unmark: //Снять все отметки
                    recyclerAdapter.setCheckedAll(false, false);
                    mActionMode.invalidate();
                    return true;
                case R.id.delete: //Удаление отмеченных записей
                    int count = recyclerAdapter.checkedCount();
                    if (count>0) DialogsReferenceManager.newInstance(ReferenceChoice.this, count).
                            show(getSupportFragmentManager(), DialogsReferenceManager.TAG_DELETE);
                    return true;
                case R.id.edit:
                    Items.Item i = recyclerAdapter.checkedItemFirst();
                    if (i.folder) //Редактирование наименований папок
                        DialogsReferenceManager.newInstance(ReferenceChoice.this, i.name).
                                show(getSupportFragmentManager(), DialogsReferenceManager.TAG_EDIT_GROUP);
                    else //Редактирование наименований элементов
                        DialogsReferenceManager.newInstance(ReferenceChoice.this, i.name).
                                show(getSupportFragmentManager(), DialogsReferenceManager.TAG_EDIT);
                    return true;
                case R.id.copy:
                    Snackbar.make(findViewById(R.id.list), R.string.msg_place_copy, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_COPY;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged(iModeMenu);
                    checkedIdsCopyMove = recyclerAdapter.getChecked();
                    checkedFrom = myStack.peek().id;
                    return true;
                case R.id.move:
                    Snackbar.make(findViewById(R.id.list), R.string.msg_place_move, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_MOVE;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged(iModeMenu);
                    checkedIdsCopyMove = recyclerAdapter.getChecked();
                    checkedFrom = myStack.peek().id;
                    return true;
                default:
                    return false;
            }
        }

        // вызывается при закрытиии контекстного меню
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            recyclerAdapter.setCheckedAll(false, false); //Снимаем отметки со всех строк
            mActionMode = null;
            iModeMenu = ACTION_BAR;
            recyclerAdapter.setModeMenu(iModeMenu);
            invalidateOptionsMenu();
            //Карточка с навигацией "+ (+)" открывается только в режиме контекстного меню
            setVisibilityBNV(false);
        }
    };

    // устанавливает видимость нижнего навигационного меню
    private void setVisibilityBNV(boolean visibility) {
        CardView cardView = findViewById(R.id.nav_card);
        if (visibility) {
            if (cardView.getVisibility() != View.VISIBLE) {
                cardView.setVisibility(View.VISIBLE);
                BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
                bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
                BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
            }
        }
        else if (!(cardView.getVisibility() == View.GONE || recyclerAdapter.isEmpty())) cardView.setVisibility(View.GONE);
    }

    // устанавливает видимость значков в нижнем навигационном меню
    private void setVisibilityIconBNV() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        switch (iModeMenu) {
            case ACTION_MODE | ACTION_BAR:
                default:
                bottomNavigationView.findViewById(R.id.create).setVisibility(View.VISIBLE);
                bottomNavigationView.findViewById(R.id.close).setVisibility(View.GONE);
                bottomNavigationView.findViewById(R.id.copy).setVisibility(View.GONE);
                bottomNavigationView.findViewById(R.id.move).setVisibility(View.GONE);
                break;
            case ACTION_COPY:
                bottomNavigationView.findViewById(R.id.create).setVisibility(View.GONE);
                bottomNavigationView.findViewById(R.id.close).setVisibility(View.VISIBLE);
                bottomNavigationView.findViewById(R.id.copy).setVisibility(View.VISIBLE);
                bottomNavigationView.findViewById(R.id.move).setVisibility(View.GONE);
                break;
            case ACTION_MOVE:
                bottomNavigationView.findViewById(R.id.create).setVisibility(View.GONE);
                bottomNavigationView.findViewById(R.id.close).setVisibility(View.VISIBLE);
                bottomNavigationView.findViewById(R.id.copy).setVisibility(View.GONE);
                bottomNavigationView.findViewById(R.id.move).setVisibility(View.VISIBLE);
                break;
        }
    }

    // вызывается при нажатии на пункт списка, чек-бокс и предков
    @Override
    public void onClick(View v) {
        Items.Item item = (Items.Item) v.getTag();
        switch (v.getId()) {
            case R.id.item: //Весь пункт
                if (item.folder) { //Проваливаемся в группу
                    myStack.push(item);
                    myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), this);
                    bArgs.putString(ARG_PATER, item.id);
                    loadFirstPage();
                }
                else { //Выбираем пункт в одиночном выборе
                    if (null != mSingleChoice)
                        mSingleChoice.onReferenceManagerInteractionListenerSingleChoice(iRC, item);
                    finish(); //Закрываем активность
                }
                break;
            case R.id.checked: //Чек-бокс
                item.checked = ((CheckBox) v).isChecked();
                recyclerAdapter.updateStatus(iModeMenu == ACTION_BAR);
                break;
            default: //Переход на предков
                myStack.clip(item);
                myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), this);
                bArgs.putString(ARG_PATER, item.id);
                loadFirstPage();
                break;
        }
        if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
        else  mActionMode.invalidate();
    }

    //вызывается при редактировании группы
    @Override
    public void onEditGroupPositiveClick(String name) {
        recyclerAdapter.editRow(name);
    }

    //вызывается при создании группы
    @Override
    public void onCreateGroupPositiveClick(String name) {
        recyclerAdapter.addRow(name, true);
    }

    //вызывается при изменении элемента
    @Override
    public void onEditElementPositiveClick(String name) {
        recyclerAdapter.editRow(name);
    }

    //вызывается при создании элемента
    @Override
    public void onCreateElementPositiveClick(String name) {
        recyclerAdapter.addRow(name, false);
    }

    //вызывается при удалении строк
    @Override
    public void onDeletePositiveClick() {
        recyclerAdapter.deleteRows(true);
    }

    @Override
    public void onDeleteNegativeClick() {
        recyclerAdapter.deleteRows(false);
    }

    //создает загрузчик
    @NonNull
    @Override
    public Loader<Items> onCreateLoader(int id, @Nullable Bundle args) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        loadingPage = true;
        if (args == null) throw new RuntimeException("The loader has't a arguments for loading items.");
        if (args.getString(ARG_TYPEKEY)==null)
            return new LoaderItems(this, oData,
                    AuditOData.Set.toValue(args.getString(ARG_TABLE)), args.getString(ARG_OWNER),
                    args.getString(ARG_PATER), args.getString(ARG_LIKE),
                    args.getStringArrayList(ARG_PARENTTYPES),
                    currentPage);
        else
            return new LoaderItems(this, oData, args.getString(ARG_LIKE),
                    args.getString(ARG_TYPEKEY), args.getString(ARG_OBJECTKEY),
                    currentPage);
    }

    //вызывается, когда загрузка закончилась
    @Override
    public void onLoadFinished(@NonNull Loader<Items> loader, Items data) {
        currentPage.nextPage(data.size());
        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), this);
        recyclerAdapter.loadList(data);
        if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
        else mActionMode.invalidate();
        if (recyclerAdapter.isEmpty()) { //Если список пустой, то сделаем видимым навигационное меню с иконками + и (+)
            setVisibilityBNV(true);
            setVisibilityIconBNV();
        }
        loadingPage = false;
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    //вызыватеся для очистки загрузчика
    @Override
    public void onLoaderReset(@NonNull Loader<Items> loader) {
        myStack.clip(-1);
        recyclerAdapter.loadList(null);
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    //Интерфейс для передачи выбранного пункта для одиночного выбора
    public interface OnReferenceManagerInteractionSingleChoice {
        void onReferenceManagerInteractionListenerSingleChoice(int requestCode, Items.Item item);
    }

    //Интерфейс для передачи выбранных пунктов для множественного выбора
    public interface OnReferenceManagerInteractionMultipleChoice {
        void onReferenceManagerInteractionMultipleChoice(int requestCode, Items items);
    }

    //Асинхронный загрузчик списка
    private static class LoaderItems extends AsyncTaskLoader<Items> {

        AuditOData oData;
        AuditOData.Set table;
        String owner;
        String pater;
        String like;
        CurrentPage currentPage;
        ArrayList<String> parentTypes;
        String typeKey;
        String objectKey;

        /**
         * Конструктор загрузчика элементов справочников (кроме аналитики с ручной связью)
         * @param context - контект
         * @param oData - связь с 1С
         * @param table - справочник
         * @param owner - guid владельца
         * @param pater - guid родителя
         * @param like - строка поиска по наименованию
         * @param parentTypes - типы родительских справочников для отбора / null
         * @param currentPage - параметры порционной загрузки
         */
        private LoaderItems(Context context, AuditOData oData, AuditOData.Set table, String owner,
                            String pater, String like, ArrayList<String> parentTypes,
                            CurrentPage currentPage) {
            super(context);
            this.oData = oData;
            this.table = table;
            this.owner = owner;
            this.pater = pater;
            this.like = like;
            this.parentTypes = parentTypes;
            this.currentPage = currentPage;
        }

        /**
         * Конструктор загрузчика аналитики по установленному вручную соответствию типу и объекту аудита
         * @param context - контект
         * @param oData - связь с 1С
         * @param like - строка поиска по наименованию
         * @param typeKey - guid вида аудита
         * @param objectKey - guid объекта аудита
         * @param currentPage - параметры порционной загрузки
         */
        private LoaderItems(Context context, AuditOData oData, String like,
                            String typeKey, String objectKey, CurrentPage currentPage) {
            super(context);
            this.oData = oData;
            this.like = like;
            this.typeKey = typeKey;
            this.objectKey = objectKey;
            this.currentPage = currentPage;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public Items loadInBackground() {
            if (typeKey == null) //все, кроме аналитики с ручной связью
                return oData.getItems(table, owner, pater, like, parentTypes,
                        currentPage.skip(), currentPage.top());
            else // Аналитика с ручной связью
                return oData.getAnalytics(typeKey, objectKey, like,
                        currentPage.skip(), currentPage.top());
        }
    }
}
//Фома2018