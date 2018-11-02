package com.example.eduardf.audit;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
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
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import java.util.ArrayList;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

//Активность для работы со списком заданий
public class TaskListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Tasks>,
        DialogsReferenceManager.DialogInteractionListener {

    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private Bundle bArgs; //Агрументы для загрузчика списка
    private RecyclerAdapter recyclerAdapter; //Адаптер для списка
    private RecyclerView.LayoutManager mLayoutManager; //Менеджер для RecyclerView

    private int iModeMenu = ACTION_BAR; //Текущий режим меню
    private static final int ACTION_BAR = 0; //меню действий
    private static final int ACTION_MODE = 1; //контекстное меню
    private static final int ACTION_COPY = 2; //копирование
    private static final int ACTION_MOVE = 3; //перемещение - изменение статуса

    private ActionMode mActionMode; //Контекстное меню

    private final static int CHECKED_STATUS_NULL = 0; //Нет отмеченных пунктов
    private final static int CHECKED_STATUS_ONE = 1; //Помечен один пункт
    private final static int CHECKED_STATUS_SOME = 2; //Помечено несколько пунктов
    private final static int CHECKED_STATUS_ALL = 3; //Помечены все пункты

    //Аргументы для интент и поворота экрана
    private static final String ARG_AUDITOR_KEY = "auditor_key"; //Идентификатор аудитора
    private static final String ARG_STATUS = "status"; //Текущая закладка / статус задания
    private static final String ARG_LIKE = "like"; //Строка поиска
    private static final String ARG_CHECKED = "checked"; //Отмеченные задания
    private static final String ARG_EXPAND = "expand"; //Развернутые задания
    private static final String ARG_MODEMENU = "menumode"; //Режим меню
    private static final String ARG_STATE = "state"; //Состояние списка до поворота
    private static final String ARG_CHECKED_STATUS = "checkedStatus"; //Состояние пометок списка
    private static final String ARG_FROM = "from"; //Статус, откуда копируем/перемещаем

    /*Возвращает интент активности
    auditor_key - идентификатор акдитора для отбора заданий
    */
    public static Intent intentActivity(Context context, String auditor_key) {
        Intent intent = new Intent(context, TaskListActivity.class);
        intent.putExtra(ARG_AUDITOR_KEY, auditor_key);
        return intent;
    }

    //Обработчик выбора пункта нижнего навигационного меню
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            boolean rezult = true;
            iModeMenu = ACTION_MODE;
            mActionMode.invalidate();
            setVisibilityBNV(false);
            switch (item.getItemId()) {
                case R.id.close:
                    recyclerAdapter.stopRows();
                    break;
                case R.id.copy:
                    recyclerAdapter.copyRows();
                    break;
                case R.id.move:
                    recyclerAdapter.moveRows();
                    break;
                default:
                    rezult = false;
            }
            return rezult;
        }
    };

    //Обработчик выбора закладки по статусам
    private TabLayout.OnTabSelectedListener mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (bArgs.getInt(ARG_STATUS, 0) != tab.getPosition()) {
                bArgs.putInt(ARG_STATUS, tab.getPosition());
                getSupportLoaderManager().restartLoader(-1, bArgs, TaskListActivity.this);
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Текущая закладка / статус задания - Утвержден
        int iStatus = 0;

        //Аргументы для загрузчика
        bArgs = new Bundle();
        if (savedInstanceState==null) { //активность запускается впервые
            Intent intent = getIntent();
            bArgs.putString(ARG_AUDITOR_KEY, intent.getStringExtra(ARG_AUDITOR_KEY));
            bArgs.putString(ARG_LIKE, "");
        }
        else { //активность восстатавливаем после поворота экрана
            iStatus = savedInstanceState.getInt(ARG_STATUS); //Текущая закладка / статус задания
            bArgs.putString(ARG_AUDITOR_KEY, savedInstanceState.getString(ARG_AUDITOR_KEY));
            bArgs.putString(ARG_LIKE, savedInstanceState.getString(ARG_LIKE, ""));
        }
        bArgs.putInt(ARG_STATUS, iStatus); //Текущая закладка

        //Закладки для отбора по статусу
        final TabLayout tt = (TabLayout) findViewById(R.id.tabs);
        ((TabLayout.Tab) tt.getTabAt(iStatus)).select(); //Выбираем текущую закладку
        tt.addOnTabSelectedListener(mOnTabSelectedListener);

        //Нижнее навигационное меню, используется для окончания операций копирования и перемещения
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        setVisibilityBNV(false); //Навигационное меню появляется только при копировании и переносе

        //Создает объект OData
        oData = new AuditOData(this);

        //Расчитываем кол-во колонок для Grid и создаем GridLayoutManager для рециклервью
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mLayoutManager = new GridLayoutManager(this, Math.max(1,
                Math.round(((float) metrics.widthPixels) /
                        getResources().getDimension(R.dimen.min_column_task))));

        // настраиваем список
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerAdapter = new RecyclerAdapter(); //Создаем адаптер для рециклервью
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);

        //Неопределенный прогресс-бар движется во время загрузки (см. onCreateLoader, onLoadFinished, onLoaderReset)
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);

        //Запускаем загрузчик для чтения данных
        Loader loader = getSupportLoaderManager().getLoader(-1);
        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(-1, bArgs, this);
        else getSupportLoaderManager().initLoader(-1, bArgs, this);
    }

    //вызывается при уничтожении активности
    @Override
    protected void onDestroy() {
        super.onDestroy();
        recyclerAdapter = null;
        mLayoutManager = null;
        mActionMode = null;
        oData = null;
    }

    //Обработчик возврата назад
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
        outState.putInt(ARG_MODEMENU, iModeMenu);
        recyclerAdapter.onSaveInstanceState(outState);
        outState.putString(ARG_AUDITOR_KEY, bArgs.getString(ARG_AUDITOR_KEY));
        outState.putString(ARG_LIKE, bArgs.getString(ARG_LIKE, ""));
        outState.putInt(ARG_STATUS, bArgs.getInt(ARG_STATUS, 0));
        outState.putParcelable(ARG_STATE, mLayoutManager.onSaveInstanceState());
    }

    // после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        iModeMenu = savedInstanceState.getInt(ARG_MODEMENU, ACTION_BAR);
        if (iModeMenu !=ACTION_BAR && mActionMode == null) mActionMode = startSupportActionMode(mActionModeCallback);
        recyclerAdapter.onRestoreInstanceState(savedInstanceState);
        mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_STATE)); //Состояние списка
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
                if (!query.equals(bArgs.getString(ARG_LIKE, ""))) {
                    bArgs.putString(ARG_LIKE, query);
                    recyclerAdapter.clearChecked();
                    getSupportLoaderManager().restartLoader(-1, bArgs, TaskListActivity.this);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    bArgs.putString(ARG_LIKE, "");
                    recyclerAdapter.saveRows(-1);
                    getSupportLoaderManager().restartLoader(-1, bArgs, TaskListActivity.this);
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
        String sLike = bArgs.getString(ARG_LIKE, "");
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
                startActivity(TaskActivity.intentActivityCreate(this, bArgs.getString(ARG_AUDITOR_KEY), bArgs.getInt(ARG_STATUS)));
                return true;
            case R.id.setting:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // устанавливает видимость нижнего навигационного меню
    private void setVisibilityBNV(boolean visibility) {
        CardView cardView = (CardView) findViewById(R.id.nav_card);
        if (visibility) {
            BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
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
//                    mActionMode.setTitle("Title");
//                    mActionMode.setSubtitle("Subtitle");

                    menu.setGroupVisible(R.id.is_checked, true);
                    menu.setGroupVisible(R.id.mark, true);

                    menu.setGroupEnabled(R.id.is_checked,recyclerAdapter.checkedStatus!=CHECKED_STATUS_NULL); //Доступность группы: Изменить, Копировать, Переместить, Удалить

                    //Придется мутировать иконки - не смог запустить titn (((
                    Drawable ic_copy = getResources().getDrawable(R.drawable.ic_white_file_copy_24px);
                    Drawable ic_move = getResources().getDrawable(R.drawable.ic_white_library_books_24px);
                    Drawable ic_delete = getResources().getDrawable(R.drawable.ic_white_delete_sweep_24px);
                    if (recyclerAdapter.checkedStatus==CHECKED_STATUS_NULL) {
                        ic_copy.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_move.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        ic_delete.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    }
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

            //Если есть текст запроса,
            String sLike = bArgs.getString(ARG_LIKE, "");
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
                    return true;
                case R.id.unmark: //Снять все отметки
                    recyclerAdapter.setCheckedAll(false);
                    mActionMode.invalidate();
                    return true;
                case R.id.delete: //Удаление отмеченных записей
                    int count = recyclerAdapter.tasks.checkedCount();
                    if (count>0) {
                        DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(TaskListActivity.this, count);
                        dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_DELETE);
                    }
                    return true;
                case R.id.copy:
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_copy, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_COPY;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged();
                    recyclerAdapter.saveRows(bArgs.getInt(ARG_STATUS)); //сохраняем, что и откуда копируем
                    setVisibilityBNV(true);
                    return true;
                case R.id.move:
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_move, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_MOVE;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged();
                    recyclerAdapter.saveRows(bArgs.getInt(ARG_STATUS)); //сохраняем, что и откуда перемещаем
                    setVisibilityBNV(true);
                    return true;
                default:
                    return false;
            }
        }

        // вызывается при закрытиии контекстного меню
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            recyclerAdapter.setCheckedAll(false); //Снимаем отметки со всех строк
            mActionMode = null;
            iModeMenu = ACTION_BAR;
            invalidateOptionsMenu();
            setVisibilityBNV(false);
        }
    };

    //ВСЕ ДЛЯ КОЛЛБЭК ЗАГРУЗИКА
    @NonNull
    @Override
    public Loader<Tasks> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == -1) {
            ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            return new LoaderTasks(TaskListActivity.this, oData,
                    args.getString(ARG_AUDITOR_KEY, ""),
                    args.getInt(ARG_STATUS, 0),
                    args.getString(ARG_LIKE, ""));
        }
        else return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Tasks> loader, Tasks data) {
        if (loader.getId() == -1) {
            recyclerAdapter.load(data);
            if (iModeMenu == ACTION_BAR) invalidateOptionsMenu();
            else mActionMode.invalidate();
            ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Tasks> loader) {
        if (loader.getId() == -1) {
            recyclerAdapter.load(null);
            ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
        }
    }

    //ВСЕ интеракшин ДЛЯ ДИАЛОГА ОБ УДАЛЕНИИ
    @Override
    public void onEditGroupPositiveClick(String name) {
        //не используем
    }

    @Override
    public void onCreatGroupPositiveClick(String name) {
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

    //Адаптер для списка. Для заполнения списка используется загрузчик LoaderItems
    private class RecyclerAdapter extends RecyclerView.Adapter<ViewHolderTasks> {

        private final Tasks tasks; //Список заданий
        private int checkedStatus; //Состояние отмеченных заданий в списке заданий
        private ArrayList<String> expandIds = null; //Список id развернутых заданий
        private ArrayList<String> checkedIds = null; //Список id отмеченных для копирования/переноса
        private int checkedFrom; //Вкладка (статус заданий), откуда перемещаем/копируем отмеченные задания

        private RecyclerAdapter() {
            tasks = new Tasks();
        }

        // восстанавливает все, что нужно адаптеру, после поворота экрана
        private void onRestoreInstanceState(Bundle savedInstanceState) {
            checkedStatus = savedInstanceState.getInt(ARG_CHECKED_STATUS);
            expandIds = savedInstanceState.getStringArrayList(ARG_EXPAND);
            if (iModeMenu != ACTION_BAR) {
                checkedIds = savedInstanceState.getStringArrayList(ARG_CHECKED);
                checkedFrom = savedInstanceState.getInt(ARG_FROM);
            }
        }

        // сохраняет все, что нужно адаптеру, перед поворотом экрана
        private void onSaveInstanceState(Bundle outState) {
            outState.putInt(ARG_CHECKED_STATUS, checkedStatus);
            outState.putStringArrayList(ARG_EXPAND, tasks.getExpand());
            if (iModeMenu != ACTION_BAR) {
                outState.putStringArrayList(ARG_CHECKED, tasks.getChecked());
                outState.putInt(ARG_FROM, checkedFrom);
            }
        }

        //Загружает список заданий в адаптер
        private void load(Tasks data) {
            if (!tasks.isEmpty()) tasks.clear();
            if (data!=null) tasks.addAll(data);
            if (iModeMenu != ACTION_BAR) {
                tasks.setChecked(checkedIds);
                updateStatus();
            }
            tasks.setExpand(expandIds); expandIds = null;
            notifyDataSetChanged();
        }

        //обновляет статус отмеченных заданий
        private void updateStatus() {
            int count = count = tasks.size(); //Общее количество заданий
            int checked = tasks.checkedCount(); //Из них отмеченных
            //Сравниваем результат
            if (checked==0 || count==0) checkedStatus = CHECKED_STATUS_NULL;
            else if (checked==count) checkedStatus = CHECKED_STATUS_ALL;
            else checkedStatus = CHECKED_STATUS_SOME;
            mActionMode.invalidate();
        }

        //Сохраняет во внешних переменных список отмеченных элементов
        private void saveRows(int status) {
            checkedIds = tasks.getChecked();
            checkedFrom = status;
        }

        //Завершает операции копирования и перемещения строк
        private void stopRows() {
            notifyDataSetChanged();
            checkedIds = null;
            updateStatus();
        }

        //Класс для выполнения операций копирования заданий в новом потоке с последующим обновлением рециклервью
        private class copyRowsAsyncTask extends AsyncTask<Integer, Void, Void> {
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Void doInBackground(Integer... status) {
                //Перенос заданий по списку
                for (String id : checkedIds) tasks.add(oData.copyTask(id, status[0]));
                return null;
            }
            protected void onPostExecute(Void voids) {
                stopRows();
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
            }
        }

        //Копирует отмеченные задания с возможным с изменением статуса
        private void copyRows() {
            new copyRowsAsyncTask().execute(bArgs.getInt(ARG_STATUS));
        }

        //Класс для выполнения операций перемещения заданий в новом потоке с последующим обновлением рециклервью
        private class moveRowsAsyncTask extends AsyncTask<Integer, Void, Void> {
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Void doInBackground(Integer... status) {
                //Перенос заданий по списку
                for (String id : checkedIds) tasks.add(oData.moveTask(id, status[0]));
                return null;
            }
            protected void onPostExecute(Void voids) {
                stopRows();
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
            }
        }

        //Перемещает отмеченные - меняем статус заданий
        private void moveRows() {
            int checkedTo = bArgs.getInt(ARG_STATUS);
            if (checkedTo != checkedFrom) new moveRowsAsyncTask().execute(checkedTo);
            else stopRows();
        }

        //Класс для выполнения операций пометки на удаление заданий в новом потоке с последующим обновлением рециклервью
        private class deleteRowsAsyncTask extends AsyncTask<Boolean, Void, Void> {
            protected void onPreExecute() {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            }
            protected Void doInBackground(Boolean... delete) {
                //Пометка на удаление отмеченных заданий
                int i = 0;
                for (Object task: tasks) {
                    if (((Tasks.Task) task).checked)
                        tasks.set(i, oData.deleteTask(((Tasks.Task) task).id, delete[0]));
                    i++;
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
            new deleteRowsAsyncTask().execute(delete);
        }

        //Помечает/отменяет отметки всех видимых элементов
        private void setCheckedAll(boolean checked) {
            tasks.setCheckedAll(checked);
            notifyDataSetChanged();
            checkedStatus = checked?CHECKED_STATUS_ALL:CHECKED_STATUS_NULL;
        }

        //Очищает список отмеченных после ввода строки отбора
        private void clearChecked() {
            if (!(checkedIds == null || checkedIds.isEmpty())) checkedIds.clear();
        }

        //Возвращает вью пункта списка
        @NonNull
        @Override
        public ViewHolderTasks onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task_list, parent, false);
            return new ViewHolderTasks(view);
        }

        //Заполняет вью пунта списка
        @Override
        public void onBindViewHolder(@NonNull ViewHolderTasks holder, int position) {
            //Текущий пункт
            holder.task = (Tasks.Task) tasks.get(position);

            //Иконки
            holder.deletedView.setVisibility(holder.task.deleted? View.VISIBLE: View.INVISIBLE);
            holder.postedView.setVisibility(holder.task.posted? View.VISIBLE: View.INVISIBLE);
            holder.thumbView.setVisibility(View.INVISIBLE); //Пока невидима, когда будет окончательно проведенная, выведим картинку

            //Выделяем карточку цветом фона цветом в зависимости от статуса и состояния
            switch (bArgs.getInt(ARG_STATUS)) {
                case 2: //Проведен
                    if (holder.task.deleted) //Помеченные на удаление - серым
                        holder.cardView.setBackgroundResource(R.color.colorBackgroundGrey);
                    else if (holder.task.posted) { //Проведенные
                        holder.thumbView.setVisibility(View.VISIBLE);
                        if (holder.task.achieved) { //Достигшие цели - зеленым + иконка
                            holder.cardView.setBackgroundResource(R.color.colorBackgroundGreen);
                            holder.thumbView.setImageResource(R.drawable.ic_black_thumb_up_alt_24px);
                        } else {//Не достигшие цели - красным
                            holder.cardView.setBackgroundResource(R.color.colorBackgroundRed);
                            holder.thumbView.setImageResource(R.drawable.ic_black_thumb_down_alt_24px);
                        }
                    }
                    else //Остальное - белым
                        holder.cardView.setBackgroundResource(R.color.cardview_light_background);
                    break;
                case 0: //Утвержден
                case 1: //В работе
                default:
                    if (holder.task.deleted) //Помеченные на удаление - серым
                        holder.cardView.setBackgroundResource(R.color.colorBackgroundGrey);
                    else if (holder.task.posted) //Проведенные - желтым
                        holder.cardView.setBackgroundResource(R.color.colorBackgroundYellow);
                    else //Остальное - белым
                        holder.cardView.setBackgroundResource(R.color.cardview_light_background);
            }
            //Дата и время задания, вид и объект аудита
            holder.dateView.setText(getDateInstance().format(holder.task.date));
            holder.objectView.setText(holder.task.object_name);
            holder.typeView.setText(holder.task.type_name);
            //Номер задания, организация, строка с аналитикой, комментарий, появляются в развернутом пункте
            if (!holder.task.expand) { //Свернутый пукнт
                holder.expandView.setImageResource(R.drawable.ic_black_expand_more_24px);
                holder.numberView.setVisibility(View.GONE);
                holder.timeView.setVisibility(View.GONE);
                holder.analyticsView.setVisibility(View.GONE);
                holder.commentView.setVisibility(View.GONE);
            }
            else { //Развернутый пукнт
                holder.expandView.setImageResource(R.drawable.ic_black_expand_less_24px);
                holder.numberView.setVisibility(View.VISIBLE);
                holder.numberView.setText(holder.task.number);
                holder.timeView.setVisibility(View.VISIBLE);
                holder.timeView.setText(getTimeInstance().format(holder.task.date));
                holder.analyticsView.setVisibility(View.VISIBLE);
                holder.analyticsView.setText(holder.task.analytic_names);
                holder.commentView.setVisibility(View.VISIBLE);
                holder.commentView.setText(holder.task.comment);
            }

            //Чекбокс и щелчки на задании:
            switch (iModeMenu) {
                case ACTION_BAR: //Обычный режим:
                    // чекбокс невидим, можно щелкнуть на задании для его редактирования, можно долго щелкнуть для перехода в режим редактирования списка заданий
                    holder.checkedView.setVisibility(View.INVISIBLE);
                    holder.checkedView.setTag(null);
                    holder.checkedView.setOnClickListener(null);
                    // короткий щелчок на задании - открытие формы задания
                    holder.itemView.setTag(holder.task);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Tasks.Task task = ((Tasks.Task) v.getTag());
                            startActivity(TaskActivity.intentActivityEdit(TaskListActivity.this, task.id));
                        }
                    });
                    // длинный щелчок - переход в режим редактирования списка заданий
                    holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                        // Вызывается, когда пользователь долго жмет на пункт для перехода в режим редактирования - action mode
                        public boolean onLongClick(View view) {
                            if (mActionMode != null) return false;
                            tasks.setCheckedAll(false); //Снимаем отметки со всех строк
                            Tasks.Task task = ((Tasks.Task) view.getTag());
                            task.checked=true; //Отмечаем задание на котором долго нажимали
                            notifyDataSetChanged();
                            checkedStatus = CHECKED_STATUS_ONE;
                            iModeMenu = ACTION_MODE; //Включаем контекстное меню
                            mActionMode = startSupportActionMode(mActionModeCallback);
                            return true;
                        }
                    });
                    break;
                case ACTION_MODE: //Режим контектного меню:
                    // чекбокс доступен
                    holder.checkedView.setVisibility(View.VISIBLE);
                    holder.checkedView.setEnabled(true);
                    holder.checkedView.setChecked(holder.task.checked);
                    holder.checkedView.setTag(holder.task);
                    holder.checkedView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Tasks.Task task = (Tasks.Task) v.getTag();
                            task.checked = ((CheckBox) v).isChecked();
                            updateStatus(); //обновляем статус отмеченных заданий для меню действий
                        }
                    });
                    // недоступны щелчки на задании
                    holder.itemView.setTag(null);
                    holder.itemView.setOnClickListener(null);
                    holder.itemView.setOnLongClickListener(null);
                    break;
                case ACTION_COPY:
                case ACTION_MOVE: //Режим копирования или переноса
                    // чекбокс видим, но недоступен для отмеченных заданий, для остальных чекбокс невидим
                    holder.checkedView.setVisibility(holder.task.checked? View.VISIBLE: View.INVISIBLE);
                    holder.checkedView.setChecked(true);
                    holder.checkedView.setEnabled(false);
                    holder.checkedView.setTag(null);
                    holder.checkedView.setOnClickListener(null);
                    // недоступны щелчки на задании
                    holder.itemView.setTag(null);
                    holder.itemView.setOnClickListener(null);
                    holder.itemView.setOnLongClickListener(null);
                    break;
            }
            //Кнопка, свернуть/развернуть
            holder.expandView.setTag(holder.task);
            holder.expandView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tasks.Task task = (Tasks.Task) v.getTag();
                    task.expand = !task.expand;
                    notifyItemChanged(tasks.indexOf(task));
                }
            });
        }

        //Возвращает количество пунктов
        @Override
        public int getItemCount() {
            return tasks.size();
        }
    }

    //Асинхронный загрузчик списка заданий
    private static class LoaderTasks extends AsyncTaskLoader<Tasks> {

        AuditOData oData;
        String auditor_key;
        int status;
        String like;

        private LoaderTasks(Context context, AuditOData oData, String auditor_key, int status, String like) {
            super(context);
            this.oData = oData;
            this.auditor_key = auditor_key;
            this.status = status;
            this.like = like;
        }

        @Override
        protected void onStartLoading() { forceLoad(); }

        @Override
        protected void onStopLoading() { cancelLoad(); }

        @Override
        public Tasks loadInBackground() {
            return oData.getTasks(auditor_key, status, like);
        }
    }
}
