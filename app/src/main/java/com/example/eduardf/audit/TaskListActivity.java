package com.example.eduardf.audit;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static com.example.eduardf.audit.TaskListAdapter.CHECKED_STATUS_ALL;
import static com.example.eduardf.audit.TaskListAdapter.CHECKED_STATUS_NULL;

/**
 * Список заданий
 */
public class TaskListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Tasks>,
        DialogsReferenceManager.DialogInteractionListener,
        View.OnClickListener,
        View.OnLongClickListener,
        TaskListAdapter.OnInvalidateActivity {

    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private String sAuditor; //guid аудитора
    private Tasks.Task.Status mStatus; //Статус заданий текущей закладки
    private String sLike; //Строка отбора по наименованию объекта
    private TaskListAdapter recyclerAdapter; //Адаптер для списка
    private RecyclerView.LayoutManager mLayoutManager; //Менеджер для RecyclerView
    private ArrayList<String> checkedIds; //Список отмеченных для копирования/переноса
    private Tasks.Task.Status checkedFrom; //Статус заданий, откуда перемещаем/копируем отмеченные
    private static int iScroll; //Количество скроллинга для отработки скрытия закладок

    private int iModeMenu = ACTION_BAR; //Текущий режим меню
    private static final int ACTION_BAR = 0; //меню действий
    private static final int ACTION_MODE = 1; //контекстное меню
    private static final int ACTION_COPY = 2; //копирование
    private static final int ACTION_MOVE = 3; //перемещение - изменение статуса

    private ActionMode mActionMode; //Контекстное меню

    //Аргументы для интент и поворота экрана
    private static final String ARG_AUDITOR_KEY = "auditor_key"; //Идентификатор аудитора
    private static final String ARG_STATUS = "status"; //Текущая закладка / статус задания
    private static final String ARG_LIKE = "like"; //Строка поиска
    private static final String ARG_MODE_MENU = "menu_mode"; //Режим меню
    private static final String ARG_STATE = "state"; //Состояние списка до поворота
    private static final String ARG_CHECKED = "checked"; //Отмеченные задания
    private static final String ARG_FROM = "from"; //Статус, откуда копируем/перемещаем

    /**
     * Интент активности списка заданий
     * @param context - контекст
     * @param auditor_key - guid аудитора
     * @return - интент
     */
    public static Intent intentActivity(Context context, String auditor_key) {
        Intent intent = new Intent(context, TaskListActivity.class);
        intent.putExtra(ARG_AUDITOR_KEY, auditor_key);
        return intent;
    }

    /**
     * Обработчик выбора пункта нижнего навигационного меню
     */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.copy:
                    recyclerAdapter.copyRows(checkedIds, mStatus);
                    break;
                case R.id.move:
                    if (!mStatus.equals(checkedFrom)) {
                        recyclerAdapter.moveRows(checkedIds, mStatus);
                    }
                    break;
                case R.id.close:
                    break;
                default:
                    return false;
            }
            setVisibilityBNV(false);
            checkedIds = null;
            recyclerAdapter.notifyDataSetChanged(iModeMenu = ACTION_MODE);
            mActionMode.invalidate();
            return true;
        }
    };

    /**
     * Обработчик выбора закладки по статусам
     */
    private TabLayout.OnTabSelectedListener mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (mStatus.number != tab.getPosition()) {
                mStatus = Tasks.Task.Status.toValue(tab.getPosition());
                loader();
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {}

        @Override
        public void onTabReselected(TabLayout.Tab tab) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        //Меню действий
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Текущая закладка / статус задания - Утвержден
        int iStatus = 0;

        if (savedInstanceState==null) { //активность запускается впервые
            Intent intent = getIntent();
            mStatus = Tasks.Task.Status.APPROVED; //Поменять на значение из настроек
            sAuditor = intent.getStringExtra(ARG_AUDITOR_KEY);
            sLike = "";
        }
        else { //активность восстатавливаем после поворота экрана
            mStatus = Tasks.Task.Status.toValue(savedInstanceState.getInt(TaskActivity.ARG_STATUS, 0));
            sAuditor = savedInstanceState.getString(ARG_AUDITOR_KEY);
            sLike = savedInstanceState.getString(ARG_LIKE, "");
        }

        //Закладки для отбора по статусу
        final TabLayout tt = findViewById(R.id.tabs);
        tt.addOnTabSelectedListener(mOnTabSelectedListener);
        final TabLayout.Tab tab = tt.getTabAt(iStatus);
        if (tab != null) tab.select();

        //Нижнее навигационное меню, используется для окончания операций копирования и перемещения
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        setVisibilityBNV(false); //Навигационное меню появляется только при копировании и переносе

        //Создает объект OData
        oData = new AuditOData(this);

        //Расчитываем кол-во колонок для Grid и создаем GridLayoutManager для рециклервью
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mLayoutManager = new StaggeredGridLayoutManager(Math.max(1,
                Math.round(((float) metrics.widthPixels) /
                        (getResources().getDimension(R.dimen.column_task)+
                                2 * getResources().getDimension(R.dimen.field_vertical_margin)))),
                StaggeredGridLayoutManager.VERTICAL);
        ((StaggeredGridLayoutManager) mLayoutManager).setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);

        // настраиваем список
        RecyclerView recyclerView = findViewById(R.id.list);
        //Создаем адаптер для рециклервью
        recyclerAdapter = new TaskListAdapter(this, oData, iModeMenu);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);
        //Обрабатываем скролинг списка вниз, чтобы сделать закладки невидимыми
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (SCROLL_STATE_IDLE == newState)
                    findViewById(R.id.tabs).setVisibility(iScroll<=0? View.VISIBLE: View.GONE);
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                iScroll = dy;
            }
        });

        //Запускаем загрузчик для чтения данных
        if (savedInstanceState == null) loader();
    }

    /**
     * Вызов загрузчика заданий
     */
    private void loader() {
        final Bundle bArgs = new Bundle(); //Агрументы для загрузчика списка
        bArgs.putInt(ARG_STATUS, mStatus.number); //Текущая закладка
        bArgs.putString(ARG_AUDITOR_KEY, sAuditor);
        bArgs.putString(ARG_LIKE, sLike);
        final Loader loader = getSupportLoaderManager().getLoader(0);
        if (loader != null && !loader.isReset())
            getSupportLoaderManager().restartLoader(0, bArgs, TaskListActivity.this);
        else
            getSupportLoaderManager().initLoader(0, bArgs, TaskListActivity.this);
    }

    /**
     * вызывается перед уничтожением активности
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        recyclerAdapter = null;
        mLayoutManager = null;
        mActionMode = null;
        oData = null;
    }

    /**
     *  Вызывается после закрытия формы задания
     * @param requestCode - запрашиваемый код - не используется
     * @param resultCode - код результата
     * @param data - результат
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data != null) { //Проверяем статус задания
                final int status = data.getIntExtra(ARG_STATUS, 0);
                if (status != mStatus.number) { //Задание не из текущей закладки
                    final TabLayout.Tab tab = ((TabLayout) findViewById(R.id.tabs)).getTabAt(status);
                    if (tab != null) tab.select();
                }
            }
        }
    }

    /**
     * Вызывается при нажатии на кнопку назад
     * @return - true
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    //Сохраняет важные значения перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_AUDITOR_KEY, sAuditor);
        outState.putString(ARG_LIKE, sLike);
        outState.putInt(ARG_STATUS, mStatus.number);
        recyclerAdapter.onSaveInstanceState(outState);
        outState.putParcelable(ARG_STATE, mLayoutManager.onSaveInstanceState());
        outState.putInt(ARG_MODE_MENU, iModeMenu);
        if (iModeMenu == ACTION_COPY || iModeMenu == ACTION_MOVE) {
            outState.putStringArrayList(ARG_CHECKED, checkedIds);
            outState.putInt(ARG_FROM, checkedFrom.number);
        }
    }

    // после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recyclerAdapter.onRestoreInstanceState(savedInstanceState);
        mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_STATE)); //Состояние списка
        iModeMenu = savedInstanceState.getInt(ARG_MODE_MENU, ACTION_BAR);
        if (iModeMenu != ACTION_BAR && mActionMode == null) mActionMode = startSupportActionMode(mActionModeCallback);
        if (iModeMenu == ACTION_COPY || iModeMenu == ACTION_MOVE) {
            checkedIds = savedInstanceState.getStringArrayList(ARG_CHECKED);
            checkedFrom = Tasks.Task.Status.toValue(savedInstanceState.getInt(ARG_FROM));
            setVisibilityBNV(true);
        }
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
                if (!query.equals(sLike)) {
                    sLike = query;
                    loader();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    sLike = "";
                    loader();
                }
                return false;
            }
        });
    }

    //Создает меню действий
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task_list, menu);
        initSearchView(menu.findItem(R.id.search)); //инициализируем поиск
        return true;
    }

    //Готовит меню действий
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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
            case R.id.create:
                startActivityForResult(TaskActivity.intentActivityCreate(this,
                        sAuditor, mStatus), 0);
                return true;
            case R.id.setting:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // устанавливает видимость нижнего навигационного меню
    private void setVisibilityBNV(boolean visibility) {
        CardView cardView = findViewById(R.id.nav_card);
        if (visibility) {
            BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
            if (cardView.getVisibility() != View.VISIBLE) {
                cardView.setVisibility(View.VISIBLE);
                bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
                BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
            }
            switch (iModeMenu) {
                case ACTION_COPY:
                    bottomNavigationView.findViewById(R.id.close).setVisibility(View.VISIBLE);
                    bottomNavigationView.findViewById(R.id.copy).setVisibility(View.VISIBLE);
                    bottomNavigationView.findViewById(R.id.move).setVisibility(View.GONE);
                    break;
                case ACTION_MOVE:
                    bottomNavigationView.findViewById(R.id.close).setVisibility(View.VISIBLE);
                    bottomNavigationView.findViewById(R.id.copy).setVisibility(View.GONE);
                    bottomNavigationView.findViewById(R.id.move).setVisibility(View.VISIBLE);
                    break;
            }
        }
        else cardView.setVisibility(View.GONE);
    }

    //ВСЕ ДЛЯ КОНТЕКСТНОГО МЕНЮ
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.cont_task_list, menu);
            initSearchView(menu.findItem(R.id.search)); //инициализируем поиск
//            setVisibilityBNV(true); //Устанавливаем видимость навигационного меню
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            switch (iModeMenu) {
                case ACTION_MODE:
                    final int checkedStatus = recyclerAdapter.getStatus();

                    menu.setGroupVisible(R.id.is_checked, true);
                    menu.setGroupVisible(R.id.mark, true);

                    menu.setGroupEnabled(R.id.is_checked,checkedStatus != CHECKED_STATUS_NULL); //Доступность группы: Изменить, Копировать, Переместить, Удалить

                    Drawable ic_copy = getResources().getDrawable(R.drawable.ic_white_file_copy_24px);
                    Drawable ic_move = getResources().getDrawable(R.drawable.ic_white_library_books_24px);
                    Drawable ic_delete = getResources().getDrawable(R.drawable.ic_white_delete_sweep_24px);
                    if (checkedStatus == CHECKED_STATUS_NULL) {
                        ic_copy.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_move.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_delete.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    }
                    (menu.findItem(R.id.copy)).setIcon(ic_copy);
                    (menu.findItem(R.id.move)).setIcon(ic_move);
                    (menu.findItem(R.id.delete)).setIcon(ic_delete);
                    //Триггер: Отметить/Снять
                    (menu.findItem(R.id.allmark)).setVisible(checkedStatus != CHECKED_STATUS_ALL);
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
                    recyclerAdapter.setCheckedAll(true);
                    mActionMode.invalidate();
                    break;
                case R.id.unmark: //Снять все отметки
                    recyclerAdapter.setCheckedAll(false);
                    mActionMode.invalidate();
                    break;
                case R.id.delete: //Удаление отмеченных записей
                    final int count = recyclerAdapter.checkedCount();
                    if (count>0)
                        DialogsReferenceManager.newInstance(TaskListActivity.this, count).
                                show(getSupportFragmentManager(), DialogsReferenceManager.TAG_DELETE);
                    break;
                case R.id.copy:
                    Snackbar.make(findViewById(R.id.list), R.string.msg_place_copy, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    recyclerAdapter.notifyDataSetChanged(iModeMenu = ACTION_COPY);
                    mActionMode.invalidate();
                    checkedIds = recyclerAdapter.getChecked();
                    checkedFrom = mStatus;
                    setVisibilityBNV(true);
                    break;
                case R.id.move:
                    Snackbar.make(findViewById(R.id.list), R.string.msg_place_move, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    recyclerAdapter.notifyDataSetChanged(iModeMenu = ACTION_MOVE);
                    mActionMode.invalidate();
                    checkedIds = recyclerAdapter.getChecked();
                    checkedFrom = mStatus;
                    setVisibilityBNV(true);
                    break;
                default:
                    return false;
            }
            return true;
        }

        // вызывается при закрытиии контекстного меню
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            recyclerAdapter.setCheckedAll(false); //Снимаем отметки со всех строк
            mActionMode = null;
            recyclerAdapter.notifyDataSetChanged(iModeMenu = ACTION_BAR);
            invalidateOptionsMenu();
            setVisibilityBNV(false);
        }
    };

    //ВСЕ ДЛЯ КОЛЛБЭК ЗАГРУЗИКА
    @NonNull
    @Override
    public Loader<Tasks> onCreateLoader(int id, @Nullable Bundle args) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        String auditor = "";
        String like = "";
        Tasks.Task.Status status = Tasks.Task.Status.APPROVED;
        if (args!=null) {
            auditor = args.getString(ARG_AUDITOR_KEY);
            like = args.getString(ARG_LIKE);
            status = Tasks.Task.Status.toValue(args.getInt(ARG_STATUS));
        }
        return new LoaderTasks(TaskListActivity.this, oData, auditor, status, like);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Tasks> loader, Tasks data) {
        recyclerAdapter.load(data);
        if ((iModeMenu == ACTION_COPY || iModeMenu == ACTION_MOVE) && !checkedIds.isEmpty())
            recyclerAdapter.setChecked(checkedIds);
        if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
        else mActionMode.invalidate();
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Tasks> loader) {
        recyclerAdapter.load(null);
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    //ВСЕ интеракшин ДЛЯ ДИАЛОГА ОБ УДАЛЕНИИ
    @Override
    public void onEditGroupPositiveClick(String name) {
        //не используем
    }

    @Override
    public void onCreateGroupPositiveClick(String name) {
        //не используем
    }

    @Override
    public void onEditElementPositiveClick(String name) {
        //не используем
    }

    @Override
    public void onCreateElementPositiveClick(String name) {
        //не используем
    }

    // вызывается при нажатии позитивной кнопки диалога о пометке строк на удаление
    @Override
    public void onDeletePositiveClick() {
        recyclerAdapter.deleteRows(true); //Отметить на удаление
    }

    // вызывается при нажатии негативной кнопки диалога о пометке строк на удаление
    @Override
    public void onDeleteNegativeClick() {
        recyclerAdapter.deleteRows(false); //Снять отметку на удаление
    }

    @Override
    public void onClick(View v) {
        final Tasks.Task task = (Tasks.Task) v.getTag();
        switch (v.getId()) {
            case R.id.item:
                startActivityForResult(
                        TaskActivity.intentActivityEdit(this, task.id), 0);
                break;
            case R.id.checked:
                task.checked = ((CheckBox) v).isChecked();
                mActionMode.invalidate();
                break;
            case R.id.expand:
                task.expand = !task.expand;
                recyclerAdapter.notifyItemChanged(task);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mActionMode != null) {
            return false;
        }
        recyclerAdapter.setCheckedAll(false); //Снимаем отметки со всех строк
        ((Tasks.Task) v.getTag()).checked = true; //Отмечаем на которой долго нажимали
        iModeMenu = ACTION_MODE;
        recyclerAdapter.getStatus();
        recyclerAdapter.notifyDataSetChanged(iModeMenu);
        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = startSupportActionMode(mActionModeCallback);

        return true;
    }

    /**
     * Вызывается после завершения операций копирования или перемещения
     */
    @Override
    public void onInvalidateActivity() {
        mActionMode.invalidate();
    }

    //Асинхронный загрузчик списка заданий
    private static class LoaderTasks extends AsyncTaskLoader<Tasks> {

        AuditOData oData;
        String auditor;
        Tasks.Task.Status status;
        String like;

        private LoaderTasks(Context context, AuditOData oData, String auditor, Tasks.Task.Status status,
                            String like) {
            super(context);
            this.oData = oData;
            this.auditor = auditor;
            this.status = status;
            this.like = like;
        }

        @Override
        protected void onStartLoading() { forceLoad(); }

        @Override
        protected void onStopLoading() { cancelLoad(); }

        @Override
        public Tasks loadInBackground() {
            return oData.getTasks(auditor, status, like);
        }
    }

}
//Фома2018