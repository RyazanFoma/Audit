package com.example.eduardf.audit;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;

//Выбор и редактирование справочников: Виды аудита, Объекты, Аналитика
public class ReferenceChoice extends AppCompatActivity implements
        View.OnClickListener,
        DialogsReferenceManager.DialogInteractionListener,
        LoaderManager.LoaderCallbacks<Items>{

    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private int iRC = -1; //Сквозной код для идентификации результата выбора
    private String sTable; //Имя таблицы
    private int iHierarchy; //Вид иерархии
    ArrayList<String> ids; //Текущие выбранные элементы для подсветки
    private Bundle bArgs; //Агрументы для загрузчика списка
    private Stack myStack; //Стек с именами предков

    private RecyclerAdapter recyclerAdapter; //Адаптер для RecyclerView
    private RecyclerView.LayoutManager mLayoutManager; //Менеджер для RecyclerView

    private static OnReferenceManagerInteractionSingleChoice mSingleChoice;
    private static OnReferenceManagerInteractionMultipleChoice mMultipleChoice;

    private ActionMode mActionMode; //Контекстное меню

    private static final int NAME_LENGTH = 20; //Максимальное количество символов в наименованни предков

    private int iModeChoice; //Текущий режим выбора:
    private static final int MODE_SINGLE_CHOICE = 1; //Режим одиночного выбора
    private static final int MODE_MULTIPLE_CHOICE = 2; //Режим множественного выбора

    private int iModeMenu = ACTION_BAR; //Текущий режим меню:
    private static final int ACTION_BAR = 0; //меню действий
    private static final int ACTION_MODE = 1; //контекстное меню
    private static final int ACTION_COPY = 2; //копирование
    private static final int ACTION_MOVE = 3; //перемещение

    private static final String ARG_RC = "requestCode"; //Сквозной код для идентификации результата выбора
    private static final String ARG_TITLE = "title"; //Заголовок activity
    private static final String ARG_TABLE = "table"; //Таблица элементов
    private static final String ARG_HIERARCHY = "hierarchy"; //Вид иерархии справочника
    private static final String ARG_OWNER = "owner"; //Владелец справочника
    private static final String ARG_MODE = "mode"; //Режим выбора
    private static final String ARG_ID = "id"; //Текущие отмеченные идентификаторы элементов
    private static final String ARG_IN = "in"; //Папки для отбора
    private static final String ARG_PATER = "pater"; //Текущий родитель
    private static final String ARG_STATE = "state"; //Состояние RecyclerView
    private static final String ARG_LIKE = "like"; //Строка поиска
    private static final String ARG_CHECKED_STATUS = "status"; //Статус отметки пунктов для контектного меню
    private static final String ARG_CHECKED = "checked"; //Список с отметками
    private static final String ARG_MODE_MENU = "mode_action"; //Режим действий
    private static final String ARG_COPY_MOVE_ID = "copy_move_id"; //пункты для копирования и переноса
    private static final String ARG_FROM = "from"; //Статус, откуда копируем/перемещаем

    private final static int CHECKED_STATUS_NULL = 0; //Нет отмеченных пунктов
    private final static int CHECKED_STATUS_ONE = 1; //Помечен один пункт
    private final static int CHECKED_STATUS_SOME = 2; //Помечено несколько пунктов
    private final static int CHECKED_STATUS_ALL = 3; //Помечены все пункты

    private final static int LOADER_BACK = -1;
    private final static int LOADER_FIRST = 0;
    private final static int LOADER_RETRY = 1;

    //ВОЗВРАЩАЕТ ИНТЕНТ ДЛЯ АКТИВНОСТИ
    /*  context - контекст родительской активности, откуда вызывается наша активность
        requestCode - уникальный код, для возврата в коллбэк, чтобы отличить - что редактировали
        fragment - фрагмент, из которого вызывается активность для выбора
        table - идентификатор справочника
        title - заголовок нашей активности
        id - идентификатор (для одиночного выбора) или список идентификаторов (для множественного выбора) элементов справочника для подстветки / null
        hierarchy - вид иерархии
        owner - идентификатор владельца справочника для отбора / null
        [in] - список идентификаторов папок первого уровня для отбора
     */
    //в режиме одиночного выбора из фрагмента с отбором по папкам
    public static Intent intentActivity(Fragment fragment, String table, String title, int hierarchy, String owner, String id, ArrayList<String> in) {
        instanceOf(fragment, MODE_SINGLE_CHOICE);
        Intent intent = new Intent(fragment.getActivity(), ReferenceChoice.class);
        intent.putExtra(ARG_MODE, MODE_SINGLE_CHOICE);
        intent.putExtra(ARG_TABLE, table);
        intent.putExtra(ARG_HIERARCHY, hierarchy);
        intent.putExtra(ARG_OWNER, owner);
        intent.putExtra(ARG_TITLE, title);
        ArrayList<String> ids = new ArrayList<String>();
        if (!(id == null || id.isEmpty())) ids.add(id);
        intent.putExtra(ARG_ID, ids);
        if (in!=null) intent.putExtra(ARG_IN, in);
        return intent;
    }
    //в режиме одиночного выбора из фрагмента
    public static Intent intentActivity(Fragment fragment, String table, String title, int hierarchy, String owner, String id) {
        return intentActivity(fragment, table, title, hierarchy, owner, id, null);
    }
    //в режиме одиночного выбора с отбором по папкам
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int hierarchy, String owner, String id, ArrayList<String> in) {
        instanceOf(context, MODE_SINGLE_CHOICE);
        Intent intent = new Intent(context, ReferenceChoice.class);
        intent.putExtra(ARG_MODE, MODE_SINGLE_CHOICE);
        intent.putExtra(ARG_RC, requestCode);
        intent.putExtra(ARG_TABLE, table);
        intent.putExtra(ARG_HIERARCHY, hierarchy);
        intent.putExtra(ARG_OWNER, owner);
        intent.putExtra(ARG_TITLE, title);
        ArrayList<String> ids = new ArrayList<String>();
        if (!(id == null || id.isEmpty())) ids.add(id);
        intent.putExtra(ARG_ID, ids);
        if (in!=null) intent.putExtra(ARG_IN, in);
        return intent;
    }
    //в режиме одиночного выбора
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int hierarchy, String owner, String id) {
        return intentActivity(context, requestCode, table, title, hierarchy, owner, id, null);
    }
    //в режиме множественного выбора из фрагмента
    public static Intent intentActivity(Context context, Fragment fragment, int requestCode, String table, String title, int hierarchy, String owner, ArrayList<String> id) {
        return intentActivity(context, fragment, requestCode, table, title, hierarchy, owner, id, null);
    }
    //в режиме множественного выбора из фрагмента с отбором по папкам
    public static Intent intentActivity(Context context, Fragment fragment, int requestCode, String table, String title, int hierarchy, String owner, ArrayList<String> id, ArrayList<String> in) {
        instanceOf(fragment, MODE_MULTIPLE_CHOICE);
        Intent intent = new Intent(context, ReferenceChoice.class);
        intent.putExtra(ARG_MODE, MODE_MULTIPLE_CHOICE);
        intent.putExtra(ARG_RC, requestCode);
        intent.putExtra(ARG_TABLE, table);
        intent.putExtra(ARG_HIERARCHY, hierarchy);
        intent.putExtra(ARG_OWNER, owner);
        intent.putExtra(ARG_TITLE, title);
        if (id != null) intent.putExtra(ARG_ID, id);
        else intent.putExtra(ARG_ID, new ArrayList<String>());
        if (in!=null) intent.putExtra(ARG_IN, in);
        return intent;
    }
    //в режиме множественного выбора
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int hierarchy, String owner, ArrayList<String> id) {
        return intentActivity(context, requestCode, table, title, hierarchy, owner, id, null);
    }
    //в режиме множественного выбора с отбором по папкам
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int hierarchy, String owner, ArrayList<String> id, ArrayList<String> in) {
        instanceOf(context, MODE_MULTIPLE_CHOICE);
        Intent intent = new Intent(context, ReferenceChoice.class);
        intent.putExtra(ARG_MODE, MODE_MULTIPLE_CHOICE);
        intent.putExtra(ARG_RC, requestCode);
        intent.putExtra(ARG_TABLE, table);
        intent.putExtra(ARG_HIERARCHY, hierarchy);
        intent.putExtra(ARG_OWNER, owner);
        intent.putExtra(ARG_TITLE, title);
        if (id != null) intent.putExtra(ARG_ID, id);
        else intent.putExtra(ARG_ID, new ArrayList<String>());
        if (in!=null) intent.putExtra(ARG_IN, in);
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
                            + " must implement OnListFragmentInteractionSingleChoice");
                }
                break;
            case MODE_MULTIPLE_CHOICE:
                if (context instanceof OnReferenceManagerInteractionMultipleChoice) {
                    mMultipleChoice = (OnReferenceManagerInteractionMultipleChoice) context;
                } else {
                    throw new RuntimeException(context.toString()
                            + " must implement OnListFragmentInteractionMultipleChoice");
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
                    DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceChoice.this);
                    dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_CREATE);
                    return true;
                case R.id.close:
                    recyclerAdapter.stopRows();
                    iModeMenu = ACTION_MODE;
                    mActionMode.invalidate();
                    return true;
                case R.id.copy:
                    recyclerAdapter.copyRows();
                    iModeMenu = ACTION_MODE;
                    mActionMode.invalidate();
                    return true;
                case R.id.move:
                    recyclerAdapter.moveRows();
                    iModeMenu = ACTION_MODE;
                    mActionMode.invalidate();
                    return true;
                case R.id.create:
                    switch (sTable) {
                        case AuditOData.ENTITY_SET_TYPE: //Создание вида аудита
                            startActivity(TypeActivity.intentActivityCreate(ReferenceChoice.this, myStack.peek().id));
                            return true;
    //                    case AuditDB.TBL_OBJECT: //Создание объекта аудита
    //                        startActivity(ObjectActivity.intentActivityCreate(this, myStack.peek().id));
    //                        return true;
    //                    case AuditDB.TBL_ANALYTIC: //Создание аналитики объекта аудита
    //                        startActivity(AnalyticActivity.intentActivityCreate(this, myStack.peek().id));
    //                        return true;
                        default:
                            return false;
                    }
                default:
                    return false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String sOwner; //Владелец
        String sPater; //Текущий родитель
        String sTitle; //Заголовок активности
        String sLike = ""; //Строка для отбора
        int loaderId; //Идентификатор загрузчика

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_choice);

        //Меню действий
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Создает объект OData
        oData = new AuditOData(this);

        //Расчитываем кол-во колонок для Grid и создаем GridLayoutManager для рециклервью
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mLayoutManager = new GridLayoutManager(this,
                Math.max(1, Math.round(((float) metrics.widthPixels) /
                        getResources().getDimension(R.dimen.min_column_reference))));

        // настраиваем список
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerAdapter = new RecyclerAdapter(mSingleChoice);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);

        //Аргументы для загрузчика
        bArgs = new Bundle();

        if (savedInstanceState==null) { //активность запускается впервые
            Intent intent = getIntent();
            iRC = intent.getIntExtra(ARG_RC, -1); //Сквозной код для идентификации результата выбора
            sTitle = intent.getStringExtra(ARG_TITLE);
            sTable = intent.getStringExtra(ARG_TABLE);
            iHierarchy = intent.getIntExtra(ARG_HIERARCHY, 0);
            sOwner = intent.getStringExtra(ARG_OWNER);
            if (intent.hasExtra(ARG_IN))
                bArgs.putStringArrayList(ARG_IN, intent.getStringArrayListExtra(ARG_IN)); //Папки для отбора
            iModeChoice = intent.getIntExtra(ARG_MODE, MODE_SINGLE_CHOICE); //Режим выбора. По умолчанию - одиночный выбор
            iModeMenu = ACTION_BAR;
            ids = intent.getStringArrayListExtra(ARG_ID); //Текущие выбранные элементы
            if (!ids.isEmpty()) {
                loaderId = LOADER_BACK;
                sPater = ids.get(0); //Текущего родителя будем определять по первому выбранному элементу
            }
            else {
                loaderId = LOADER_FIRST;
                sPater = AuditOData.EMPTY_KEY;
            }
        }
        else { //активность восстатавливаем после поворота экрана
            iRC = savedInstanceState.getInt(ARG_RC);
            sTitle = savedInstanceState.getString(ARG_TITLE);
            sTable = savedInstanceState.getString(ARG_TABLE);
            iHierarchy = savedInstanceState.getInt(ARG_HIERARCHY, 0);
            sOwner = savedInstanceState.getString(ARG_OWNER);
            iModeChoice = savedInstanceState.getInt(ARG_MODE);
            ids = savedInstanceState.getStringArrayList(ARG_ID);
            if (savedInstanceState.containsKey(ARG_IN))
                bArgs.putStringArrayList(ARG_IN, savedInstanceState.getStringArrayList(ARG_IN));
            sLike = savedInstanceState.getString(ARG_LIKE, "");
            sPater = savedInstanceState.getString(ARG_PATER);
            loaderId = LOADER_RETRY;
        }
        bArgs.putString(ARG_TABLE, sTable);
        bArgs.putInt(ARG_HIERARCHY, iHierarchy);
        bArgs.putString(ARG_OWNER, sOwner);
        bArgs.putString(ARG_PATER, sPater);
        bArgs.putString(ARG_LIKE, sLike);

        setTitle(sTitle); //Заголовок активности

        //Готовим предков начиная с корня
        myStack = new Stack(this);
        Items.Item item = new Items.Item();
        item.id = AuditOData.EMPTY_KEY;
        item.name = sTitle;
        myStack.push(item);

        //Неопределенный прогресс-бар движется во время загрузки (см. onCreateLoader, onLoadFinished, onLoaderReset)
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);

        //Карточка с навигацией "+ (+)" открывается только в режиме контекстного меню
        ((CardView) findViewById(R.id.nav_card)).setVisibility(View.GONE);

        //Запускаем загрузчик для чтения данных
        Loader loader = getSupportLoaderManager().getLoader(loaderId);
        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(loaderId, bArgs, this);
        else getSupportLoaderManager().initLoader(loaderId, bArgs, this);
    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    // перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, getTitle().toString());
        outState.putInt(ARG_RC, iRC);
        outState.putInt(ARG_MODE, iModeChoice);
        outState.putString(ARG_TABLE, sTable);
        outState.putInt(ARG_HIERARCHY, bArgs.getInt(ARG_HIERARCHY, 0));
        outState.putString(ARG_OWNER, bArgs.getString(ARG_OWNER));
        outState.putString(ARG_PATER, myStack.peek().id);
        outState.putString(ARG_LIKE, bArgs.getString(ARG_LIKE));
        outState.putStringArrayList(ARG_IN, bArgs.getStringArrayList(ARG_IN));
        outState.putParcelable(ARG_STATE, mLayoutManager.onSaveInstanceState());
        outState.putInt(ARG_MODE_MENU, iModeMenu); //Режим меню
        recyclerAdapter.onSaveInstanceState(outState);
    }

    // после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_STATE)); //Состояние списка
        //Если до поворота экрана было запущено контекстное меню, то открываем его опять
        iModeMenu = savedInstanceState.getInt(ARG_MODE_MENU, ACTION_BAR); //Режим меню
        if (iModeMenu !=ACTION_BAR && mActionMode == null)
            mActionMode = startSupportActionMode(mActionModeCallback);
        recyclerAdapter.onRestoreInstanceState(savedInstanceState);
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
            (menu.findItem(R.id.allmark)).setVisible(recyclerAdapter.checkedStatus!=CHECKED_STATUS_ALL);
            (menu.findItem(R.id.unmark)).setVisible(!(menu.findItem(R.id.allmark)).isVisible());
            (menu.findItem(R.id.choice)).setVisible(recyclerAdapter.checkedStatus!=CHECKED_STATUS_NULL);
        }

        //Если есть текст запроса,
        String sLike = bArgs.getString(ARG_LIKE);
        if (!sLike.isEmpty()) { //то переходим в режим поиска
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
//                    mActionMode.setTitle("Title");
//                    mActionMode.setSubtitle("Subtitle");

                    menu.setGroupVisible(R.id.is_checked, true);
                    menu.setGroupVisible(R.id.mark, true);

                    menu.setGroupEnabled(R.id.is_checked,recyclerAdapter.checkedStatus!=CHECKED_STATUS_NULL); //Доступность группы: Изменить, Копировать, Переместить, Удалить
                    (menu.findItem(R.id.edit)).setEnabled(recyclerAdapter.checkedStatus==CHECKED_STATUS_ONE); //Доступность пункта: Изменить

                    //Придется мутировать иконки - не смог запустить titn (((
                    Drawable ic_edit = getResources().getDrawable(R.drawable.ic_white_edit_24px);
                    Drawable ic_copy = getResources().getDrawable(R.drawable.ic_white_file_copy_24px);
                    Drawable ic_move = getResources().getDrawable(R.drawable.ic_white_library_books_24px);
                    Drawable ic_delete = getResources().getDrawable(R.drawable.ic_white_delete_sweep_24px);
                    if (recyclerAdapter.checkedStatus!=CHECKED_STATUS_ONE) ic_edit.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    if (recyclerAdapter.checkedStatus==CHECKED_STATUS_NULL) {
                        ic_copy.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_move.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_delete.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    }
                    (menu.findItem(R.id.edit)).setIcon(ic_edit);
                    (menu.findItem(R.id.copy)).setIcon(ic_copy);
                    (menu.findItem(R.id.move)).setIcon(ic_move);
                    (menu.findItem(R.id.delete)).setIcon(ic_delete);
                    //Триггер: Отметить/Снять
                    (menu.findItem(R.id.allmark)).setVisible(recyclerAdapter.checkedStatus!=CHECKED_STATUS_ALL);
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
            if (!sLike.isEmpty()) { //то переходим в режим поиска
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
                    int count = recyclerAdapter.items.checkedCount();
                    if (count>0) {
                        DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceChoice.this, count);
                        dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_DELETE);
                    }
                    return true;
                case R.id.edit:
                    Items.Item i = recyclerAdapter.items.checkedItemFirst();
                    if (i.folder) { //Редактирование наименований любых папок
                        DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceChoice.this, i.name);
                        dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_EDIT);
                        return true;
                    }
                    else switch (sTable) {
                        case AuditOData.ENTITY_SET_TYPE:
                            startActivity(TypeActivity.intentActivityEdit(ReferenceChoice.this, i.id)); //Редактирование вида аудита
                            return true;
//                        case AuditDB.TBL_OBJECT:
//                            startActivity(ObjectActivity.intentActivityEdit(ReferenceChoice.this, i.id)); //Редактирование объекта аудита
//                            return true;
//                        case AuditDB.TBL_ANALYTIC:
//                            startActivity(AnalyticActivity.intentActivityEdit(ReferenceChoice.this, i.id)); //Редактирование аналитики объекта аудита
//                            return true;
                        default:
                            return false;
                    }
                case R.id.copy:
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_copy, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_COPY;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged();
                    recyclerAdapter.saveRows(); //сохраняем, что и откуда копируем
                    return true;
                case R.id.move:
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_move, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_MOVE;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged();
                    recyclerAdapter.saveRows(); //сохраняем, что и откуда перемещаем
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
            invalidateOptionsMenu();

            //Карточка с навигацией "+ (+)" открывается только в режиме контекстного меню
            setVisibilityBNV(false);
        }
    };

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
                    getSupportLoaderManager().restartLoader(LOADER_RETRY, bArgs, ReferenceChoice.this);
                    if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
                    else mActionMode.invalidate();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    bArgs.putString(ARG_LIKE, "");
                    getSupportLoaderManager().restartLoader(LOADER_RETRY, bArgs, ReferenceChoice.this);
                    if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
                    else mActionMode.invalidate();
                }
                return false;
            }
        });
    }

    // устанавливает видимость нижнего навигационного меню
    private void setVisibilityBNV(boolean visibility) {
        CardView cardView = (CardView) findViewById(R.id.nav_card);
        if (visibility) {
            if (cardView.getVisibility() != View.VISIBLE) {
                cardView.setVisibility(View.VISIBLE);
                BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
                bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
                BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
            }
        }
        else if (!(cardView.getVisibility() == View.GONE || recyclerAdapter.items.isEmpty())) cardView.setVisibility(View.GONE);
    }

    // устанавливает видимость значков в нижнем навигационном меню
    private void setVisibilityIconBNV() {
        BottomNavigationView bottomNavigationView = ((BottomNavigationView) findViewById(R.id.navigation));
        switch (iModeMenu) {
            case ACTION_MODE:
            case ACTION_BAR:
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
                    myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), NAME_LENGTH);
                    bArgs.putString(ARG_PATER, item.id);
                    getSupportLoaderManager().restartLoader(LOADER_RETRY, bArgs, this);
                }
                break;
            case R.id.checked: //Чек-бокс
                item.checked = ((CheckBox) v).isChecked();
                recyclerAdapter.updateStatus(iModeMenu==ACTION_BAR); // Проверяем все пункты, вместе с группами, или только детей, в зависимости от активности контекстного меню.
                break;
            default: //Переход на предков
                myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), NAME_LENGTH);
                bArgs.putString(ARG_PATER, item.id);
                getSupportLoaderManager().restartLoader(LOADER_RETRY, bArgs, this);
                break;
        }
        if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
        else  mActionMode.invalidate();
    }

    //вызывается при редактировании группы
    @Override
    public void onEditGroupPositiveClick(String name) {
        Items.Item item = recyclerAdapter.items.checkedItemFirst();
        item.name = name;
        recyclerAdapter.editGroup(item);
    }

    //вызывается при создании группы
    @Override
    public void onCreatGroupPositiveClick(String name) {
        recyclerAdapter.addGroup(name);
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
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
        return new LoaderItems(this, oData,
                args.getString(ARG_TABLE), args.getInt(ARG_HIERARCHY), args.getString(ARG_OWNER), args.getString(ARG_PATER), (id == LOADER_BACK),
                args.getStringArrayList(ARG_IN), args.getString(ARG_LIKE), myStack);
    }

    //вызывается, когда загрузка закончилась
    @Override
    public void onLoadFinished(@NonNull Loader<Items> loader, Items data) {
        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), NAME_LENGTH);
        recyclerAdapter.loadList(data);
        if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
        else mActionMode.invalidate();
        //Если открываем список первый раз и есть выбранные элементы,
        if (loader.getId() == LOADER_BACK) { // то прокручиваем список на первый выбранный
            ((RecyclerView) findViewById(R.id.list)).scrollToPosition(recyclerAdapter.getPosition()); // скролинг до текущего/первого выбранного пункта
        }
        if (recyclerAdapter.items.isEmpty()) { //Если список пустой, то сделаем видимым навигационное меню с иконками + и (+)
            setVisibilityBNV(true);
            setVisibilityIconBNV();
        }
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
    }

    //вызыватеся для очистки загрузчика
    @Override
    public void onLoaderReset(@NonNull Loader<Items> loader) {
        myStack.clip(-1);
        recyclerAdapter.loadList(null);
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
    }

    //Интерфейс для передачи выбранного пункта для одиночного выбора
    public interface OnReferenceManagerInteractionSingleChoice {
        void onReferenceManagerInteractionListenerSingleChoice(int requestCode, Items.Item item);
    }

    //Интерфейс для передачи выбранных пунктов для множественного выбора
    public interface OnReferenceManagerInteractionMultipleChoice {
        void onReferenceManagerInteractionMultipleChoice(int requestCode, Items items);
    }

    //Адаптер для списка. Для заполнения списка используется загрузчик LoaderItems
    private class RecyclerAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

        private final Items items;
        private final OnReferenceManagerInteractionSingleChoice mListener;
        private int checkedStatus;
        //Список отмеченных для поворота экрана и возврата в активность
        private ArrayList<String> checkedIds = null;
        private ArrayList<String> checkedIdsCopyMove; //Список отмеченных для копирования/переноса
        private String checkedFrom; //Папка, откуда перемещаем/копируем отмеченные

        //Конструктор
        private RecyclerAdapter(OnReferenceManagerInteractionSingleChoice listener) {
            items = new Items();
            mListener = listener;
        }

        // восстанавливает все, что нужно адаптеру, после поворота экрана
        private void onRestoreInstanceState(Bundle savedInstanceState) {
            ids = savedInstanceState.getStringArrayList(ARG_ID);
            checkedStatus = savedInstanceState.getInt(ARG_CHECKED_STATUS);
            if (iModeMenu != ACTION_BAR)
                checkedIds = savedInstanceState.getStringArrayList(ARG_CHECKED);
            if (iModeMenu == ACTION_COPY | iModeMenu == ACTION_MOVE) {
                checkedIdsCopyMove = savedInstanceState.getStringArrayList(ARG_COPY_MOVE_ID);
                checkedFrom = savedInstanceState.getString(ARG_FROM, checkedFrom);
            }
        }

        // сохраняет все, что нужно адаптеру, перед поворотом экрана
        private void onSaveInstanceState(Bundle outState) {
            outState.putStringArrayList(ARG_ID, ids);
            outState.putInt(ARG_CHECKED_STATUS, checkedStatus);
            if (iModeMenu != ACTION_BAR)
                outState.putStringArrayList(ARG_CHECKED, items.getChecked());
            if (iModeMenu == ACTION_COPY | iModeMenu == ACTION_MOVE) {
                outState.putStringArrayList(ARG_COPY_MOVE_ID, checkedIdsCopyMove);
                outState.putString(ARG_FROM, checkedFrom);
            }
        }

        //Загружает список пунктов
        private void loadList(Items data) {
            if (!items.isEmpty()) items.clear();
            if (data!=null) items.addAll(data);
            if (iModeMenu != ACTION_BAR) {
                items.setChecked(checkedIds);
                updateStatus(false);
            }
            notifyDataSetChanged();
        }

        //Возвращает позицию первого выделенного элемента в списке
        private int getPosition() {
            if (!(ids==null || ids.isEmpty())) return items.getPosition(ids.get(0));
            return 0;
        }

        //Помечает/отменяет отметки всех видимых элементов или только детей
        private void setCheckedAll(boolean checked, boolean only_child) {
            items.setCheckedAll(checked, only_child);
            notifyDataSetChanged();
            if (checked) updateStatus(only_child);
            else checkedStatus = CHECKED_STATUS_NULL;
        }

        //Проверяет количество отмеченных пунктов
        private void updateStatus(boolean only_child) {
            int count = 0; //Общее количество пунктов
            int checked = 0; //Из них отмеченных
            //Подсчитываем количество отмеченных и всех
            if (only_child) {
                for(Items.Item item: items) {
                    if (!item.folder) {
                        count++;
                        if (item.checked) checked++;
                    }
                }
            }
            else {
                count = items.size();
                checked = items.checkedCount();
            }
            //Сравниваем результат
            if (checked==0 || count==0) checkedStatus = CHECKED_STATUS_NULL;
            else if (checked==1) checkedStatus = CHECKED_STATUS_ONE;
            else if (checked==count) checkedStatus = CHECKED_STATUS_ALL;
            else checkedStatus = CHECKED_STATUS_SOME;
            if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
            else mActionMode.invalidate();
        }

        //Завершает операции копирования и перемещения строк
        private void stopRows() {
            notifyDataSetChanged();
            checkedIdsCopyMove = null;
            updateStatus(false);
        }

        //Сохраняет во внешних переменных список отмеченных элементов
        private void saveRows() {
            checkedIdsCopyMove = items.getChecked();
            checkedFrom = myStack.peek().id;
        }

        //Класс для выполнения операций копирования в новом потоке с последующим обновлением рециклервью
        private class copyRowsAsyncTask extends AsyncTask<String, Void, Void> {
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Void doInBackground(String... pater) {
                for (String id : checkedIdsCopyMove) items.add(oData.copyItem(sTable, iHierarchy, id, pater[0]));
                return null;
            }
            protected void onPostExecute(Void voids) {
                stopRows();
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
            }
        }

        //Копирует отмеченные задания с возможным с изменением статуса
        private void copyRows() {
            new copyRowsAsyncTask().execute(bArgs.getString(ARG_PATER));
        }

        //Класс для выполнения операций перемещения в новом потоке с последующим обновлением рециклервью
        private class moveRowsAsyncTask extends AsyncTask<String, Void, Boolean> {
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Boolean doInBackground(String... pater) {
                boolean notMoved = false;
                for (String id : checkedIdsCopyMove) {
                    Items.Item item = oData.getItem(sTable, iHierarchy, id);
                    if ((item.folder && myStack.contains(id)) || item.predefined)
                        notMoved = true;
                    else
                        items.add(oData.moveItem(sTable, iHierarchy, id, pater[0]));
                }
                return notMoved;
            }
            protected void onPostExecute(Boolean notMoved) {
                stopRows();
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
                if (notMoved)
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_move_error, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
            }
        }

        //Перемещает отмеченные
        private void moveRows() {
            String checkedTo = myStack.peek().id;
            if (!checkedTo.equals(checkedFrom)) new moveRowsAsyncTask().execute(checkedTo);
            else stopRows();
        }

        //Класс для выполнения операций пометки на удаление в новом потоке с последующим обновлением рециклервью
        private class deleteRowsAsyncTask extends AsyncTask<Boolean, Void, Void> {
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Void doInBackground(Boolean... delete) {
                int position = 0;
                for(Items.Item item: items) {
                    if (item.checked && !item.predefined) { //Отмеченные и не предопределенные
                        items.set(position, oData.deleteItem(sTable, iHierarchy, item.id, delete[0]));
                    }
                    position++;
                }
                return null;
            }
            protected void onPostExecute(Void voids) {
                notifyDataSetChanged();
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
            }
        }

        // Помечает на удаление помеченные строки
        private void deleteRows(boolean delete) {
            new RecyclerAdapter.deleteRowsAsyncTask().execute(delete);
        }

        //Класс для выполнения операций добавления группы в новом потоке с последующим обновлением строки рециклервью
        private class createRowAsyncTask extends AsyncTask<String, Void, Items.Item> {
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Items.Item doInBackground(String... name) {
                return oData.createItem(sTable, iHierarchy, name[0], name[1]);
            }
            protected void onPostExecute(Items.Item item) {
                items.add(item);
                notifyItemInserted(items.size()-1);
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
            }
        }

        //Добавляет группу
        private void addGroup(String name) {
            new createRowAsyncTask().execute(myStack.peek().id, name);
        }

        //Класс для выполнения операций изменение наименования в новом потоке с последующим обновлением строки рециклервью
        private class updateRowAsyncTask extends AsyncTask<Items.Item, Void, Items.Item> {
            int position;
            private updateRowAsyncTask(int position) {
                super();
                this.position = position;
            }
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Items.Item doInBackground(Items.Item... items) {
                return oData.updateItem(sTable, iHierarchy, items[0]);
            }
            protected void onPostExecute(Items.Item item) {
                notifyItemChanged(position, item);
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
            }
        }

        //Изменяет наименование группы
        private void editGroup(Items.Item item) {
            new updateRowAsyncTask(items.indexOf(item)).execute(item);
        }

        //Возвращает список отмеченных пунктов
        private Items getCheckedItems() {
            Items rezult = new Items();
            for (Items.Item item: items) if (item.checked) rezult.add(item);
            return rezult;
        }

        //Очищает список отмеченных после ввода строки отбора
        private void clearChecked() {
            if (!(checkedIds == null || checkedIds.isEmpty())) checkedIds.clear();
        }

        @NonNull
        @Override
        public ViewHolderRefs onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reference, parent, false);
            return new ViewHolderRefs(view);
        }

        // строим вью пункта
        @Override
        public void onBindViewHolder(@NonNull final ViewHolderRefs holder, int position) {
            //Текущий пункт
            holder.item = items.get(position);

            //Иконка + / -
            holder.imageView.setImageResource(holder.item.folder?
                    R.drawable.ic_black_add_circle_outline_24px :
                    R.drawable.ic_baseline_remove_circle_outline_24px);

            // наименование и описание (нужно добавить комментарий хотя бы для видов аудита)
            holder.nameView.setText(holder.item.name);
            holder.deletedView.setVisibility(holder.item.deleted? View.VISIBLE: View.INVISIBLE);
            if (!holder.item.predefined) {
                holder.predefinedView.setVisibility(View.INVISIBLE);
                holder.descView.setText("");
            }
            else {
                holder.predefinedView.setVisibility(View.VISIBLE);
                holder.descView.setText(holder.item.prenamed);
            }

            holder.itemView.setTag(holder.item); // в теге храним пункт

            // Раскрашиваем карточки разными цветами
            if (!ids.contains(holder.item.id)) { //Пункта нет в списке выбранных элементов
                if (holder.item.deleted) //Помеченные на удаление - серым
                    holder.cardView.setBackgroundResource(R.color.colorBackgroundGrey);
                else if (holder.item.predefined) //Предопределенные - желтым
                    holder.cardView.setBackgroundResource(R.color.colorBackgroundYellow);
                else //Остальное белым
                    holder.cardView.setBackgroundResource(R.color.cardview_light_background);
            }
            else { //Пунт был выбран ранее
                if (holder.item.deleted) //Помеченные на удаление - сине-серым
                    holder.cardView.setBackgroundResource(R.color.colorBackgroundDarkGrey);
                else if (holder.item.predefined) //Предопределенные - сине-желтым (зеленым)
                    holder.cardView.setBackgroundResource(R.color.colorBackgroundDarkYellow);
                else //Остальное синим
                    holder.cardView.setBackgroundResource(R.color.colorBackgroundItem);
            }

            switch (iModeMenu) {
                case ACTION_BAR:
                    switch (iModeChoice) {
                        case MODE_SINGLE_CHOICE:
                            holder.checkedView.setVisibility(View.GONE);
                            if (holder.item.folder) {
                                holder.forwardView.setVisibility(View.VISIBLE);
                                holder.itemView.setOnClickListener(ReferenceChoice.this); //Папки открываем
                            }
                            else {
                                holder.forwardView.setVisibility(View.GONE);
                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (null != mSingleChoice)
                                            mSingleChoice.onReferenceManagerInteractionListenerSingleChoice(iRC, holder.item);
                                        finish(); //Закрываем активность
                                    }
                                });
                            }
                            break;
                        case MODE_MULTIPLE_CHOICE:
                            if (holder.item.folder) {
                                holder.itemView.setOnClickListener(ReferenceChoice.this); //Папки открываем
                                holder.forwardView.setVisibility(View.VISIBLE);
                                holder.checkedView.setVisibility(View.GONE);
                                holder.checkedView.setTag(null);
                                holder.checkedView.setOnClickListener(null);
                            }
                            else {
                                holder.itemView.setOnClickListener(null); //Только чек-бокс
                                holder.forwardView.setVisibility(View.GONE);
                                holder.checkedView.setVisibility(View.VISIBLE);
                                holder.checkedView.setChecked(holder.item.checked);
                                holder.checkedView.setTag(holder.item);
                                holder.checkedView.setOnClickListener(ReferenceChoice.this);
                            }
                            break;
                    }

                    holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                        // Called when the user long-clicks on someView
                        public boolean onLongClick(View view) {
                            if (mActionMode != null) {
                                return false;
                            }
                            setCheckedAll(false, false); //Снимаем отметки со всех строк
                            ((Items.Item) view.getTag()).checked=true; //Отмечаем на которой долго нажимали
                            checkedStatus = CHECKED_STATUS_ONE;
                            notifyDataSetChanged();
                            // Start the CAB using the ActionMode.Callback defined above
                            iModeMenu = ACTION_MODE;
                            mActionMode = startSupportActionMode(mActionModeCallback);
                            return true;
                        }
                    });
                    break;
                case ACTION_MODE:
                    holder.forwardView.setVisibility(View.GONE);
                    holder.checkedView.setVisibility(View.VISIBLE);
                    holder.checkedView.setChecked(holder.item.checked);
                    holder.checkedView.setEnabled(true);
                    holder.checkedView.setTag(holder.item);
                    holder.checkedView.setOnClickListener(ReferenceChoice.this);
                    if (holder.item.folder) holder.itemView.setOnClickListener(ReferenceChoice.this); //Папки открываем
                    else holder.itemView.setOnClickListener(null); //Только чек-бокс
                    // недоступны длинные щелчки
                    holder.itemView.setOnLongClickListener(null);
                    break;
                case ACTION_COPY:
                case ACTION_MOVE:
                    // чекбокс видим, но недоступен для отмеченных заданий, для остальных чекбокс невидим
                    if (checkedIdsCopyMove.contains(holder.item.id)) {
                        holder.checkedView.setVisibility(View.VISIBLE);
                        holder.checkedView.setChecked(true);
                        holder.checkedView.setEnabled(false);
                    }
                    else {
                        holder.checkedView.setVisibility(View.INVISIBLE);
                    }
                    if (holder.item.folder) {
                        holder.forwardView.setVisibility(View.VISIBLE);
                        holder.itemView.setOnClickListener(ReferenceChoice.this); //Папки открываем
                    }
                    else {
                        holder.forwardView.setVisibility(View.INVISIBLE);
                        holder.itemView.setOnClickListener(null); //Файлы не трогаем
                    }
                    // недоступны длинные щелчки
                    holder.itemView.setOnLongClickListener(null);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    //Асинхронный загрузчик списка
    private static class LoaderItems extends AsyncTaskLoader<Items> {

        AuditOData oData;
        Stack stack;
        String table;
        int hierarchy;
        String owner;
        String pater;
        ArrayList<String> in;
        String like;
        boolean back;

        // Конструктор
        private LoaderItems(Context context, AuditOData oData, String table, int hierarchy, String owner, String pater, boolean back, ArrayList<String> in, String like, Stack stack) {
            super(context);
            this.oData = oData;
            this.table = table;
            this.hierarchy = hierarchy;
            this.owner = owner;
            this.pater = pater;
            this.in = in;
            this.like = like;
            this.stack = stack;
            this.back = back;
        }

        //Загружает в стек всех предков начиная с рождества и заканчивая текущим id
        private void loadStack(String id) {
            if (!AuditOData.EMPTY_KEY.equals(id)) {
                Items.Item item = oData.getItem(table, hierarchy, id);
                if(!AuditOData.EMPTY_KEY.equals(item.pater))
                    loadStack(item.pater); //Рекурсия!!!
                stack.push(item);
            }
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
            if (back) {
                pater = oData.getItem(table, hierarchy, pater).pater;
                back =false;
            }
            stack.clip(0);
            loadStack(pater);
            if (AuditOData.EMPTY_KEY.equals(pater) && !(in == null || in.isEmpty()))
                return oData.getItems(table, hierarchy, owner, pater, like, in);
            else
                return oData.getItems(table, hierarchy, owner, pater, like);
        }
    }
}
