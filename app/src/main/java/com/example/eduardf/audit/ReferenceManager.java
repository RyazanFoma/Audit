package com.example.eduardf.audit;

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
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ReferenceManager extends AppCompatActivity implements View.OnClickListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        DialogsReferenceManager.DialogInteractionListener,
        LoaderManager.LoaderCallbacks<Items>{

    private AuditDB db; //База данных
    private ArrayList<Integer> ids = null; //Текущие выбранные элементы
    private String sTitle = ""; //Наименование справочника
    private String sTable; //Имя таблицы
    private int iRC; //Сквозной код для идентификации результата выбора
    private int iModeChoice; //Текущий режим выбора
    private int iModeMenu = ACTION_BAR; //Текущий режим действий
    private RecyclerAdapter recyclerAdapter; //Адаптер для RecyclerView
    private LinearLayoutManager mLayoutManager; //Менеджер для RecyclerView
    private MyStack myStack; //Все имена предков
    private static OnReferenceManagerInteractionListener mListener;
    private static OnReferenceManagerInteractionChoose mChoose;
    private int[] in; //Папки на первом уровне для отбора
    private String sLike = ""; //Строка для отбора по наименованию
    private ActionMode mActionMode; //Контекстное меню
    private ArrayList<Integer> checkedIdsCopyMove; //Список отмеченных для копирования/переноса
    private ArrayList<Integer> checkedIds = new ArrayList<>(); //Список отмеченных для поворота экрана и возврата в активность
    private Bundle args; //Агрументы для загрузчика списка
    private boolean scrollFirst = false; //признак для перехода на первый выбранный элемент

    private static final int NO_SELECTED = -1; //Нет элемента для выбора

    private static final int MODE_SINGLE_CHOICE = 1; //Режим одиночного выбора
    private static final int MODE_MULTIPLE_CHOICE = 2; //Режим множественного выбора

    private static final int ACTION_BAR = 0;
    private static final int ACTION_MODE = 1;
    private static final int ACTION_COPY = 2;
    private static final int ACTION_MOVE = 3;

    private static final String ARG_RC = "requestCode"; //Сквозной код для идентификации результата выбора
    private static final String ARG_TITLE = "title"; //Заголовок activity
    private static final String ARG_TABLE = "table"; //Таблица элементов
    private static final String ARG_MODE = "mode"; //Режим выбора
    private static final String ARG_ID = "id"; //Текущие отмеченные идентификаторы элементов
    private static final String ARG_IN = "in"; //Папки для отбора
    private static final String ARG_PATER = "pater"; //Текущий родитель
    private static final String ARG_STATE = "state"; //Состояние RecyclerView
    private static final String ARG_LIKE = "like"; //Строка поиска
    private static final String ARG_STATUS = "status"; //Статус отметки пунктов для контектного меню
    private static final String ARG_CHECKED = "checked"; //Список с отметками
    private static final String ARG_MODE_MENU = "mode_action"; //Режим действий
    private static final String ARG_COPY_MOVE_ID = "copy_move_id"; //пункты для копирования и переноса

    private final static int CHECKED_STATUS_NULL = 0; //Нет отмеченных пунктов
    private final static int CHECKED_STATUS_ONE = 1; //Помечен один пункт
    private final static int CHECKED_STATUS_SOME = 2; //Помечено несколько пунктов
    private final static int CHECKED_STATUS_ALL = 3; //Помечены все пункты

    //ВОЗВРАЩАЕТ ИНТЕНТ ДЛЯ АКТИВНОСТИ
    //в режиме одиночного выбора
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int id) {
        return intentActivity(context, requestCode, table, title, id, null);
    }
    //в режиме одиночного выбора с отбором по папкам
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int id, int[] in) {
        instanceOf(context, MODE_SINGLE_CHOICE);
        Intent intent = new Intent(context, ReferenceManager.class);
        intent.putExtra(ARG_RC, requestCode);
        intent.putExtra(ARG_TABLE, table);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_MODE, MODE_SINGLE_CHOICE);
        intent.putExtra(ARG_ID, id);
        if (in!=null) intent.putExtra(ARG_IN, in);
        return intent;
    }
    //в режиме множественного выбора
    public static Intent intentActivity(Context context, int requestCode, String table, String title, ArrayList<Integer> ids) {
        return intentActivity(context, requestCode, table, title, ids,null);
    }
    //в режиме множественного выбора с отбором по папкам
    public static Intent intentActivity(Context context, int requestCode, String table, String title, ArrayList<Integer> ids, int[] in) {
        instanceOf(context, MODE_MULTIPLE_CHOICE);
        Intent intent = new Intent(context, ReferenceManager.class);
        intent.putExtra(ARG_RC, requestCode);
        intent.putExtra(ARG_TABLE, table);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_MODE, MODE_MULTIPLE_CHOICE);
        intent.putExtra(ARG_ID,ids);
        intent.putIntegerArrayListExtra(ARG_ID,ids);
        if (in!=null) intent.putExtra(ARG_IN, in);
        return intent;
    }
    //Проверяет наличие обработчика выбора в предыдущей активности
    private static void instanceOf(Context context, int mode) {
        switch (mode) {
            case MODE_SINGLE_CHOICE:
                if (context instanceof OnReferenceManagerInteractionListener) {
                    mListener = (OnReferenceManagerInteractionListener) context;
                } else {
                    throw new RuntimeException(context.toString()
                            + " must implement OnListFragmentInteractionListener");
                }
                break;
            case MODE_MULTIPLE_CHOICE:
                if (context instanceof OnReferenceManagerInteractionChoose) {
                    mChoose = (OnReferenceManagerInteractionChoose) context;
                } else {
                    throw new RuntimeException(context.toString()
                            + " must implement OnListFragmentInteractionChoose");
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        int iPater = 0; //Текущий родитель = 0 - корень списка

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_manager);

        //Меню действий
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        mLayoutManager = new LinearLayoutManager(this);

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        if (savedInstanceState==null) { //активность запускается впервые
            scrollFirst = true;
            Intent intent = getIntent();
            iRC = intent.getIntExtra(ARG_RC, NO_SELECTED); //Сквозной код для идентификации результата выбора
            sTable = intent.getStringExtra(ARG_TABLE); //Имя таблицы с данными
            sTitle = intent.getStringExtra(ARG_TITLE); //Заголовок активности
            iModeChoice = intent.getIntExtra(ARG_MODE, MODE_SINGLE_CHOICE); //Режим выбора. По умолчанию - одиночный выбор
            iModeMenu = ACTION_BAR;
            if (iModeChoice==MODE_SINGLE_CHOICE) {
                int id = intent.getIntExtra(ARG_ID, NO_SELECTED); //Текущий выбранный элемент
                ids = new ArrayList<Integer>();
                if (id!=NO_SELECTED) ids.add(id); //Добавляем в список выбранных элементов, даже если он там будет один
            }
            else ids = intent.getIntegerArrayListExtra(ARG_ID); //Текущие выбранные элементы
            if (intent.hasExtra(ARG_IN)) in = intent.getIntArrayExtra(ARG_IN); //Папки для отбора
            // определяем текущего родителя
            if (!(ids==null||ids.isEmpty())) iPater = db.getPaterById(sTable, ids.get(0)); //Первый в списке
            if (iPater==NO_SELECTED) iPater=0;
        }
        else { //активность восстатавливаем после поворота экрана
            iRC = savedInstanceState.getInt(ARG_RC);
            sTable = savedInstanceState.getString(ARG_TABLE);
            iModeChoice = savedInstanceState.getInt(ARG_MODE);
            sTitle = savedInstanceState.getString(ARG_TITLE);
            ids = savedInstanceState.getIntegerArrayList(ARG_ID);
            iPater = savedInstanceState.getInt(ARG_PATER);
            sLike = savedInstanceState.getString(ARG_LIKE, ""); //Сохраняем строку поиска
        }

        setTitle(sTitle); //Заголовок активности

        recyclerAdapter = new RecyclerAdapter(mListener);

        //Выводим всех предков
        myStack = new MyStack(this, sTitle, iPater);
        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 20);

        // настраиваем список
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);

        //Карточка с навигацией "+ (+)" открывается только в режиме контекстного меню
        ((CardView) findViewById(R.id.nav_card)).setVisibility(View.GONE);

        //Аргументы для загрузчика
        args = new Bundle();
        args.putString(ARG_TABLE, sTable);
        args.putIntArray(ARG_IN, in);
        args.putString(ARG_LIKE, sLike);

        // создаем загрузчик для чтения данных
        Loader loader = getSupportLoaderManager().getLoader(iPater);
        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(iPater, args, this);
        else getSupportLoaderManager().initLoader(iPater, args, this);
    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    // перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_RC, iRC); //Сквозной идентификатор активности
        outState.putString(ARG_TABLE, sTable); //Таблица с данными
        outState.putInt(ARG_MODE, iModeChoice); //Режим выбора
        outState.putString(ARG_TITLE, sTitle); //Заголовок активности
        outState.putIntegerArrayList(ARG_ID, ids); //Избранные пункты
        outState.putInt(ARG_PATER, myStack.peek().id); //Текущий родитель
        outState.putParcelable(ARG_STATE, mLayoutManager.onSaveInstanceState()); //Состояние списка
        outState.putString(ARG_LIKE,sLike); //Строка поиска
        outState.putIntegerArrayList(ARG_CHECKED, recyclerAdapter.getChecked()); //Отмеченные пункты
        outState.putInt(ARG_STATUS, recyclerAdapter.checkedStatus); //Статус отметок
        outState.putInt(ARG_MODE_MENU, iModeMenu); //Режим меню
        if (iModeMenu == ACTION_COPY | iModeMenu == ACTION_MOVE)
            outState.putIntegerArrayList(ARG_COPY_MOVE_ID, checkedIdsCopyMove); //Пункты для копирования и переноса

    }

    // после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        checkedIds = savedInstanceState.getIntegerArrayList(ARG_CHECKED); //Отмеченные до поворота пункты, восстанавливаем после загрузки всех пунктов
        recyclerAdapter.checkedStatus = savedInstanceState.getInt(ARG_STATUS); //Статус пометок
        mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_STATE)); //Состояние списка
        //Если до поворота экрана было запущено контекстное меню, то открываем его опять
        iModeMenu = savedInstanceState.getInt(ARG_MODE_MENU, ACTION_BAR); //Режим меню
        if (iModeMenu !=ACTION_BAR && mActionMode == null) mActionMode = startSupportActionMode(mActionModeCallback);
        if (iModeMenu == ACTION_COPY | iModeMenu == ACTION_MOVE) checkedIdsCopyMove = savedInstanceState.getIntegerArrayList(ARG_COPY_MOVE_ID);
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
//        mListener = null; //Хорошо бы собрать мусор, но при повороте экрана не будет работать выбор элемента(((
        // закрываем подключение при выходе
        db.close();
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
            (menu.findItem(R.id.choose)).setVisible(recyclerAdapter.checkedStatus!=CHECKED_STATUS_NULL);
        }

        //Если есть текст запроса,
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
            case R.id.choose:
                if (null != mChoose)
                    mChoose.onReferenceManagerInteractionChoose(ReferenceManager.this, iRC, recyclerAdapter.getChecked());
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
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.cont_reference_manager, menu);
            initSearchView(menu.findItem(R.id.search)); //инициализируем поиск
            //Карточка с навигацией "+ (+)" открывается только в режиме контекстного меню
            setVisibilityBNV(true);
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
                    Drawable ic_edid = getResources().getDrawable(R.drawable.ic_white_edit_24px);
                    Drawable ic_copy = getResources().getDrawable(R.drawable.ic_white_file_copy_24px);
                    Drawable ic_move = getResources().getDrawable(R.drawable.ic_white_library_books_24px);
                    Drawable ic_delete = getResources().getDrawable(R.drawable.ic_white_delete_sweep_24px);
                    if (recyclerAdapter.checkedStatus!=CHECKED_STATUS_ONE) ic_edid.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    if (recyclerAdapter.checkedStatus==CHECKED_STATUS_NULL) {
                        ic_copy.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_move.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_delete.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    }
                    (menu.findItem(R.id.edit)).setIcon(ic_edid);
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
                    int count = recyclerAdapter.checkedCount();
                    if (count>0) {
                        DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceManager.this, count);
                        dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_DELETE);
                    }
                    return true;
                case R.id.edit:
                    Items.Item i = recyclerAdapter.checkedItemFirst();
                    if (i.folder) { //Редактирование наименований любых папок
                        DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceManager.this, i.name);
                        dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_EDIT);
                        return true;
                    }
                    else switch (sTable) {
                        case AuditDB.TBL_TYPE:
                            startActivity(TypeActivity.intentActivityEdit(ReferenceManager.this, i.id)); //Редактирование вида аудита
                            return true;
                        case AuditDB.TBL_OBJECT:
                            startActivity(ObjectActivity.intentActivityEdit(ReferenceManager.this, i.id)); //Редактирование объекта аудита
                            return true;
                        case AuditDB.TBL_ANALYTIC:
                            startActivity(AnalyticActivity.intentActivityEdit(ReferenceManager.this, i.id)); //Редактирование аналитики объекта аудита
                            return true;
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
            recyclerAdapter.notifyDataSetChanged();
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
        if(null!=searchManager ) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        searchView.setIconifiedByDefault(true); //Поиск свернут по умолчанию
        searchView.setQueryHint(getString(R.string.search_hint_name));

        //Обработчик текста запроса для поиска
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!sLike.equals(query)) {
                    sLike = query;
                    args.putString(ARG_LIKE, sLike);
                    getSupportLoaderManager().restartLoader(myStack.peek().id, args, ReferenceManager.this);
                    if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
                    else mActionMode.invalidate();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    sLike = "";
                    args.putString(ARG_LIKE, sLike);
                    getSupportLoaderManager().restartLoader(myStack.peek().id, args, ReferenceManager.this);
                    if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
                    else mActionMode.invalidate();
                }
                return false;
            }
        });

//        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
//            @Override
//            public boolean onClose() {
//                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                View view = ReferenceManager.this.getCurrentFocus();
//                //If no view currently has focus, create a new one, just so we can grab a window token from it
//                if (view == null) {
//                    view = new View(ReferenceManager.this);
//                }
//                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
//                return true;
//            }
//        });
    }

//    // принудительно скрыть клавиатуру
//    public void hideSoftKeyboard(View view) {
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
//    }
//
//    // принудительно показать клавиатуру
//    public void showSoftKeyboard(View view) {
//        if (view.requestFocus()) {
//            InputMethodManager imm = (InputMethodManager)
//                    getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
//        }
//    }

    // устанавливает видимость нижнего всего навигационного меню
    private void setVisibilityBNV(boolean visibility) {
        CardView cardView = (CardView) findViewById(R.id.nav_card);
        if (visibility) {
            if (cardView.getVisibility() != View.VISIBLE) {
                cardView.setVisibility(View.VISIBLE);
                BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
                bottomNavigationView.setOnNavigationItemSelectedListener(ReferenceManager.this);
                BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
            }
        }
        else if (!(cardView.getVisibility() == View.GONE || recyclerAdapter.mValues.isEmpty())) cardView.setVisibility(View.GONE);
    }

    // устанавливает видимость значков в нижнем навигационном меню
    private void setVisibilityIconBNV() {
        BottomNavigationView bottomNavigationView = ((BottomNavigationView) findViewById(R.id.navigation));
        switch (iModeMenu) {
//                case ACTION_MODE:
//                case ACTION_CREATE:
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
                    myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 10);
                    getSupportLoaderManager().initLoader(item.id, args, this);
                }
                break;
            case R.id.checked: //Чек-бокс
                item.checked = ((CheckBox) v).isChecked();
                recyclerAdapter.checkedStatus(iModeMenu==ACTION_BAR); // Проверяем все пункты, вместе с группами, или только детей, в зависимости от активности контекстного меню.
                break;
            default: //Переход на предков
                myStack.clip(item);
                myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 10);
                getSupportLoaderManager().initLoader(item.id, args, this);
                break;
        }
        if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
        else  mActionMode.invalidate();
    }

    // вызывается при нажатии на пункт навигационного меню
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_group:
                DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceManager.this);
                dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_CREATE);
                return true;
            case R.id.close:
                iModeMenu = ACTION_MODE;
                mActionMode.invalidate();
                recyclerAdapter.notifyDataSetChanged();
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
                    case AuditDB.TBL_TYPE: //Создание вида аудита
                        startActivity(TypeActivity.intentActivityCreate(this, myStack.peek().id));
                        return true;
                    case AuditDB.TBL_OBJECT: //Создание объекта аудита
                        startActivity(ObjectActivity.intentActivityCreate(this, myStack.peek().id));
                        return true;
                    case AuditDB.TBL_ANALYTIC: //Создание аналитики объекта аудита
                        startActivity(AnalyticActivity.intentActivityCreate(this, myStack.peek().id));
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    @Override
    public void onEditGroupPositiveClick(String name) {
        Items.Item item = recyclerAdapter.checkedItemFirst();
        item.name = name;
        recyclerAdapter.editGroup(item);
    }

    @Override
    public void onCreatGroupPositiveClick(String name) {
        recyclerAdapter.addGroup(name);
        mActionMode.invalidate();
    }

    @Override
    public void onDeletePositiveClick() {
        recyclerAdapter.deleteRows();
        mActionMode.invalidate();
    }

    @NonNull
    @Override
    public Loader<Items> onCreateLoader(int id, @Nullable Bundle args) {
        return new LoaderItems(this, db, args.getString(ARG_TABLE), id, args.getIntArray(ARG_IN), args.getString(ARG_LIKE));
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Items> loader, Items data) {
        if (loader.getId() == myStack.peek().id) { //Защита от повторной загрузки ненужного результата
            recyclerAdapter.loadList(data);
            //Если открываем списко первый раз и есть выбранные элементы,
            if (!(scrollFirst||ids==null||ids.isEmpty())) { // то прокручиваем список на первый выбранный
                scrollFirst = false;
                ((RecyclerView) findViewById(R.id.list)).scrollToPosition(recyclerAdapter.getPosition(ids.get(0))); // скролинг до текущего/первого выбранного пункта
            }
            if (recyclerAdapter.mValues.isEmpty()) { //Если список пустой, то сделаем видимым навигационное меню с иконками + и (+)
                setVisibilityBNV(true);
                setVisibilityIconBNV();
            }
        }
//        else getSupportLoaderManager().destroyLoader(loader.getId());
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Items> loader) {
        recyclerAdapter.loadList(null);
    }

    //Интерфейс для передачи выбранного пункта для одиночного выбора
    public interface OnReferenceManagerInteractionListener {
        void onReferenceManagerInteractionListener(Context context, int requestCode, Items.Item item);
    }

    //Интерфейс для передачи выбранных пунктов для множественного выбора
    public interface OnReferenceManagerInteractionChoose {
        void onReferenceManagerInteractionChoose(Context context, int requestCode, List<Integer> ids);
    }

    //Класс стек для вывода текущего положения в дереве
    private class MyStack {
        //Используем список для организации стека, чтобы была возможность получить toString с наименованиями пунктов
        private Items items;
        private Context context;

        //Конструктор
        private MyStack(Context context, String title, int pater) {
            this.context = context;
            items = new Items();
            items.add(new Items.Item(0,true, NO_SELECTED, NO_SELECTED,NO_SELECTED, title,null));  //Корень
            load(pater); //Подгружаем всех предков
        }

        private void push(Items.Item item) { items.add(item);}

        // заполняет вью списком предков с ограничением по длине наименования
        private void addTextView(LinearLayout linearLayout, int limit) {
            linearLayout.removeAllViews(); // удаляем предыдущий список
            // все родители их стека
            for (Items.Item item:items.getItems()) {
                TextView textView = new TextView(context);
                textView.setTag(item);
                textView.setText((item.name.length()<=limit+5)?
                        item.name:
                        item.name.substring(0, limit/2)+" ... "+item.name.substring(item.name.length()-limit/2, item.name.length()));
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_navigate_next_24px, 0);
                textView.setOnClickListener(ReferenceManager.this);
                linearLayout.addView(textView);
            }
        }

        //Загружает в список всех предков родителя
        private void load(int id) {
            if (id>0) {
                int pater = db.getPaterById(sTable, id);
                if(pater>0) load(pater); //Рекурсия
                push(new Items.Item(id, true, NO_SELECTED, NO_SELECTED,0, db.getNameById(sTable, id), null));
            }
        }

        //Обрезаем стек до указанного пункта
        private void clip(Items.Item item) {
            int index = NO_SELECTED;
            int j=0;
            // ищем позицию пункта
            for(Items.Item myItem:items.getItems()) if(myItem.equals(item)) {index=j; break;} else j++;
            // удаляем все, что правее начиная с последнего
            if (index!=NO_SELECTED) for(int i=items.size()-1;i>index;i--) items.remove(i);
        }

        //Возвращает ссылку на верхний пункт стека
        private Items.Item peek() {
            Items.Item item = null;
            if (! items.isEmpty()) item = items.get(items.size()-1);
            return item;
        }

        //Возвращает true, если указанный элемент является родителем текущего
        private boolean contains(int id) {
            for (Items.Item item:items.getItems()) if (item.id == id) return true;
            return false;
        }
    }

    //Адаптер для списка. Для заполнения списка используется загрузчик LoaderItems
    private class RecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final Items mValues;
        private final OnReferenceManagerInteractionListener mListener;
        private int checkedStatus;

        private RecyclerAdapter(OnReferenceManagerInteractionListener listener) {
            mValues = new Items();
            mListener = listener;
            checkedStatus = CHECKED_STATUS_NULL;
        }

        //Загружает список пунктов
        private void loadList(Items items) {
            if (!mValues.isEmpty()) { //Если возврат в активность, а не поворот экрана
                checkedIds = getChecked(); //Возьмем отмеченные из текущего списка
                mValues.clear(); //Список очистим
            } //Если был поворот экрана, то checkedIds уже восстановлен в onRestore...
            if (items!=null) mValues.addAll(items); //Добавим загруженные пункты
            setChecked(checkedIds); //Установим отмеченные
            checkedIds.clear(); //Список больше не нужен
            checkedStatus(iModeMenu == ACTION_BAR);
            notifyDataSetChanged();
        }

        //Возвращает позицию пункта по id, если не найден 0
        private int getPosition(int id) {
            int position = 0;
            int i=0;
            for(Items.Item item:mValues.getItems()) if (item.id==id) { position=i; break;} else i++;
            return position;
        }

        //Помечает/отменяет отметки всех видимых элементов или только детей
        private void setCheckedAll(boolean checked, boolean only_child) {
            if (only_child) { for(Items.Item item:mValues.getItems()) if (!item.folder) item.checked=checked; }
            else { for(Items.Item item:mValues.getItems()) item.checked=checked; }
            notifyDataSetChanged();
            checkedStatus = checked?checkedStatus(only_child):CHECKED_STATUS_NULL;
        }

        //Проверяет количество отмеченных пунктов
        private int checkedStatus(boolean only_child) {
            int count = 0; //Общее количество пунктов
            int checked = 0; //Из них отмеченных

            //Подсчитываем количество отмеченных и всех
            if (only_child) {
                for(Items.Item item:mValues.getItems()) {
                    if (!item.folder) {
                        count++;
                        if (item.checked) checked++;
                    }
                }
            }
            else {
                count = mValues.size();
                for(Items.Item item:mValues.getItems()) if (item.checked) checked++;
            }

            //Сравниваем результат
            if (checked==0 || count==0) checkedStatus = CHECKED_STATUS_NULL;
            else if (checked==1) checkedStatus = CHECKED_STATUS_ONE;
            else if (checked==count) checkedStatus = CHECKED_STATUS_ALL;
            else checkedStatus = CHECKED_STATUS_SOME;

            return checkedStatus;
        }

        //Возвращает список отмеченных пунктов
        private ArrayList<Integer> getChecked() {
            ArrayList<Integer> checked = new ArrayList<Integer>();
            for(Items.Item item:mValues.getItems()) if (item.checked) checked.add(item.id);
            return checked;
        }

        //Отмечает пункты по списку
        private void setChecked(ArrayList<Integer> checked) {
            if (!(checked==null || checked.isEmpty()))
                for(Items.Item item: mValues.getItems()) item.checked = checked.contains(item.id);
        }

        //Удалаяет отмеченные
        private void deleteRows() {
            for(Items.Item item: mValues.getItems())
                if (item.checked)
                    if (!item.folder)
                        switch (sTable) {
                            case AuditDB.TBL_TYPE:
                                db.delType(item.id);
                                break;
                            case AuditDB.TBL_OBJECT:
                                db.delObject(item.id);
                                break;
                            case AuditDB.TBL_ANALYTIC:
                                db.delAnalytic(item.id);
                                break;
                            default:
                                db.deleteRecord(sTable, item.id);
                        }
                    else if ((item.folders+item.files)==0) db.deleteRecord(sTable, item.id);
                    else {
                        Snackbar.make((View) findViewById(R.id.list), "Невозможно удалить '"+item.name+"'. Содержит: "+item.folders+" папок, "+item.files+" файлов", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        break;
                    }
            getSupportLoaderManager().restartLoader(myStack.peek().id, args,ReferenceManager.this);
        }

        //Сохраняет во внешних переменных список отмеченных элементов
        private void saveRows() {
            checkedIdsCopyMove = getChecked();
        }

        //Копирует отмеченные
        private void copyRows() {
            for(Integer id: checkedIdsCopyMove)
                switch (sTable) {
                    case AuditDB.TBL_TYPE:
                        db.copyType(id, myStack.peek().id);
                        break;
                    case AuditDB.TBL_OBJECT:
                        db.copyObject(id, myStack.peek().id);
                        break;
                    case AuditDB.TBL_ANALYTIC:
                        db.copyAnalytic(id, myStack.peek().id);
                        break;
                    default:
                        db.copyRecord(sTable, id, myStack.peek().id);
                }
            getSupportLoaderManager().restartLoader(myStack.peek().id, args, ReferenceManager.this);
        }

        //Перемещает отмеченные
        private void moveRows() {
            for(Integer id: checkedIdsCopyMove) {
                if (!myStack.contains(id)) db.moveRecord(sTable, id, myStack.peek().id);
                else {
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_move_error, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                }
            }
            getSupportLoaderManager().restartLoader(myStack.peek().id, args, ReferenceManager.this);
        }

        //Возвращает количество отмеченных строк
        private int checkedCount() {
            int i = 0;
            for(Items.Item item: mValues.getItems()) if (item.checked) i++;
            return i;
        }

        //Добавляет группу
        private void addGroup(String name) {
            db.insertRecord(sTable, new Items.Item(NO_SELECTED, true, NO_SELECTED, NO_SELECTED, myStack.peek().id, name, ""));
            getSupportLoaderManager().restartLoader(myStack.peek().id, args, ReferenceManager.this);
        }

        //Изменяет группу
        private void editGroup(Items.Item item) {
            db.updateRecord(sTable, item);
            notifyDataSetChanged();
        }

        //Возвращает первый попавшийся отчеченный пункт
        private Items.Item checkedItemFirst() {
            for (Items.Item item: mValues.getItems()) if (item.checked) return item;
            return null;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reference_manager, parent, false);
            return new ViewHolder(view);
        }

        // строим вью пункта
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            //Текущий пункт
            holder.mItem = mValues.get(position);

            //Иконка + / -
            holder.mImageView.setImageResource(holder.mItem.folder?
                    R.drawable.ic_black_add_circle_outline_24px :
                    R.drawable.ic_baseline_remove_circle_outline_24px);

            // наименование и описание
            holder.mNameView.setText(holder.mItem.name);
            holder.mDescView.setText(holder.mItem.desc);
            holder.mItemView.setTag(holder.mItem); // в теге храним пункт

            //Выделяем цветом выбранные ранее пункты
            holder.mCardView.setBackgroundResource((ids.contains(holder.mItem.id))?
                    R.color.colorBackgroundItem:
                    R.color.cardview_light_background); // выделяем выбранные

            switch (iModeMenu) {
                case ACTION_BAR:
                    switch (iModeChoice) {
                        case MODE_SINGLE_CHOICE:
                            holder.mCheckedView.setVisibility(View.GONE);
                            if (holder.mItem.folder) {
                                holder.mForwardView.setVisibility(View.VISIBLE);
                                holder.mItemView.setOnClickListener(ReferenceManager.this); //Папки открываем
                            }
                            else {
                                holder.mForwardView.setVisibility(View.GONE);
                                holder.mItemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (null != mListener)
                                            mListener.onReferenceManagerInteractionListener(ReferenceManager.this, iRC, holder.mItem);
                                        finish(); //Закрываем активность
                                    }
                                });
                            }
                            break;
                        case MODE_MULTIPLE_CHOICE:
                            if (holder.mItem.folder) {
                                holder.mItemView.setOnClickListener(ReferenceManager.this); //Папки открываем
                                holder.mForwardView.setVisibility(View.VISIBLE);
                                holder.mCheckedView.setVisibility(View.GONE);
                                holder.mCheckedView.setTag(null);
                                holder.mCheckedView.setOnClickListener(null);
                            }
                            else {
                                holder.mItemView.setOnClickListener(null); //Только чек-бокс
                                holder.mForwardView.setVisibility(View.GONE);
                                holder.mCheckedView.setVisibility(View.VISIBLE);
                                holder.mCheckedView.setChecked(holder.mItem.checked);
                                holder.mCheckedView.setTag(holder.mItem);
                                holder.mCheckedView.setOnClickListener(ReferenceManager.this);
                            }
                            break;
                    }

                    holder.mItemView.setOnLongClickListener(new View.OnLongClickListener() {
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
                    holder.mForwardView.setVisibility(View.GONE);
                    holder.mCheckedView.setVisibility(View.VISIBLE);
                    holder.mCheckedView.setChecked(holder.mItem.checked);
                    holder.mCheckedView.setTag(holder.mItem);
                    holder.mCheckedView.setOnClickListener(ReferenceManager.this);
                    if (holder.mItem.folder) holder.mItemView.setOnClickListener(ReferenceManager.this); //Папки открываем
                    else holder.mItemView.setOnClickListener(null); //Только чек-бокс
                    break;
                case ACTION_COPY:
                case ACTION_MOVE:
                    holder.mCheckedView.setVisibility(View.GONE);
                    if (holder.mItem.folder) {
                        holder.mForwardView.setVisibility(View.VISIBLE);
                        holder.mItemView.setOnClickListener(ReferenceManager.this); //Папки открываем
                    }
                    else {
                        holder.mForwardView.setVisibility(View.GONE);
                        holder.mItemView.setOnClickListener(null); //Файлы не трогаем
                    }
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }

    //Асинхронный загрузчик списка
    private static class LoaderItems extends AsyncTaskLoader<Items> {

        AuditDB db;
        String table;
        int pater;
        int[] in;
        String like;

        private LoaderItems(Context context, AuditDB db, String table, int pater, int[] in, String like) {
            super(context);
            this.db = db;
            this.table = table;
            this.pater = pater;
            this.in = in;
            this.like = like;
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
            return db.getItemsByPater(table, pater, in, like);
        }
    }
}
