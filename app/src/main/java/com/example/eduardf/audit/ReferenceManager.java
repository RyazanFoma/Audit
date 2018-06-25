package com.example.eduardf.audit;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ReferenceManager extends AppCompatActivity implements View.OnClickListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        DialogsReferenceManager.DialogInteractionListener {

    private AuditDB db; //База данных
    ArrayList<Integer> ids = null; //Текущие выбранные элементы
    String sTitle = ""; //Наименование справочника
    private String sTable; //Имя таблицы
    private int iRC; //Сквозной код для идентификации результата выбора
    private int iModeChoice; //Текущий режим выбора
    private int iModeMenu = ACTION_BAR; //Текущий режим действий
    private RecyclerAdapter recyclerAdapter; //Адаптер для RecyclerView
    private LinearLayoutManager mLayoutManager; //Менеджер для RecyclerView
    private MyStack myStack; //Все имена предков
    private static OnReferenceManagerInteractionListener mListener;
    private static OnReferenceManagerInteractionChoose mChoose;
    private String sLike = ""; //Строка для отбора по наименованию
    ActionMode mActionMode; //Контекстное меню
    Toolbar toolbar; //Меню действий

    static final int NO_SELECTED = -1; //Нет элемента для выбора

    private static final int MODE_SINGLE_CHOICE = 1; //Режим одиночного выбора
    private static final int MODE_MULTIPLE_CHOICE = 2; //Режим множественного выбора

    private static final int ACTION_BAR = 0;
    private static final int ACTION_MODE = 1;
    private static final int ACTION_COPY = 2;
    private static final int ACTION_MOVE = 3;

    private static final String ARG_RC = "requestCode"; //Сквозной код для идентификации результата выбора
    private static final String ARG_TITLE = "title"; //Заголовок activity
    private static final String ARG_TABLE = "table"; //Таблица элементов
    static final String ARG_MODE = "mode"; //Режим выбора
    static final String ARG_ID = "id"; //Текущие отмеченные идентификаторы элементов
    static final String ARG_IN = "in"; //Папки для отбора
    static final String ARG_PATER = "pater"; //Текущий родитель
    static final String ARG_STATE = "state"; //Состояние RecyclerView
    static final String ARG_LIKE = "like"; //Строка поиска
    static final String ARG_STATUS = "status"; //Статус отметки пунктов для контектного меню
    static final String ARG_CHECKED = "checked"; //Список с отметками
    static final String ARG_ACTION = "action"; //Признак открытого контекстного меню
    static final String ARG_MODE_MENU = "mode_action"; //Режим действий

    final static int CHECKED_STATUS_NULL = 0; //Нет отмеченных пунктов
    final static int CHECKED_STATUS_ONE = 1; //Помечен один пункт
    final static int CHECKED_STATUS_SOME = 2; //Помечено несколько пунктов
    final static int CHECKED_STATUS_ALL = 3; //Помечены все пункты

    //ВОЗВРАЩАЕТ ИНТЕНТ ДЛЯ АКТИВНОСТИ
    //в режиме одиночного выбора
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int id) {
        return intentActivity(context, requestCode, table, title, id, null);
    }
    //в режиме одиночного выбора с отбором по папкам
    public static Intent intentActivity(Context context, int requestCode, String table, String title, int id, int[] in) {
        instanceOf(context);
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
        instanceOf(context);
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
    private static void instanceOf(Context context) {
        if (context instanceof OnReferenceManagerInteractionListener) {
            mListener = (OnReferenceManagerInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
        if (context instanceof OnReferenceManagerInteractionChoose) {
            mChoose = (OnReferenceManagerInteractionChoose) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionChoose");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        int iPater = 0; //Текущий родитель = 0 - корень списка
        int[] in; //Папки для отбора

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_manager);

        //Меню действий
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        mLayoutManager = new LinearLayoutManager(this);

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        if (savedInstanceState==null) { //активность запускается впервые
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

        recyclerAdapter = new RecyclerAdapter(db.getItemsByPater(sTable, iPater, sLike), mListener);
//        if (iMode==MODE_MULTIPLE_CHOICE) recyclerAdapter.setChecked(ids);

        //Выводим всех предков
        myStack = new MyStack(this, sTitle, iPater);
        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 10);

        // настраиваем список
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);
        if (!(savedInstanceState!=null||ids==null||ids.isEmpty()))
            recyclerView.scrollToPosition(recyclerAdapter.getPosition(ids.get(0))); // скролинг до текущего/первого выбранного пункта

        //Карточка с навигацией "+ (+)" открывается только в режиме контекстного меню
        ((CardView) findViewById(R.id.nav_card)).setVisibility(View.GONE);
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
    }

    // после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recyclerAdapter.setChecked(savedInstanceState.getIntegerArrayList(ARG_CHECKED)); //Восстатавливаем отметки пунктов
        recyclerAdapter.checkedStatus = savedInstanceState.getInt(ARG_STATUS); //Статус пометок
        mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_STATE)); //Состояние списка
        //Если до поворота экрана было запущено контекстное меню, то открываем его опять
        iModeMenu = savedInstanceState.getInt(ARG_MODE_MENU, ACTION_BAR); //Режим меню
        if (iModeMenu !=ACTION_BAR && mActionMode == null) mActionMode = startSupportActionMode(mActionModeCallback);
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
            ((CardView) findViewById(R.id.nav_card)).setVisibility(View.VISIBLE);
            ((BottomNavigationView) findViewById(R.id.navigation)).setOnNavigationItemSelectedListener(ReferenceManager.this);

            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            BottomNavigationView bottomMenu = ((BottomNavigationView) findViewById(R.id.navigation));

            switch (iModeMenu) {
                case ACTION_MODE:
                    menu.setGroupVisible(R.id.is_checked, true);
                    menu.setGroupVisible(R.id.mark, true);

                    menu.setGroupEnabled(R.id.is_checked,recyclerAdapter.checkedStatus!=CHECKED_STATUS_NULL); //Доступность группы: Изменить, Копировать, Переместить, Удалить
                    (menu.findItem(R.id.edit)).setEnabled(recyclerAdapter.checkedStatus==CHECKED_STATUS_ONE); //Доступность пункта: Изменить

                    //Придется мутировать иконки - не смог запустить titn (((
                    Drawable ic_edid = getResources().getDrawable(R.drawable.ic_white_edit_24px);
                    Drawable ic_copy = getResources().getDrawable(R.drawable.ic_white_file_copy_24px);
                    Drawable ic_move = getResources().getDrawable(R.drawable.ic_white_library_books_24px);
                    Drawable ic_delete = getResources().getDrawable(R.drawable.ic_white_delete_sweep_24px);
                    if (recyclerAdapter.checkedStatus!=CHECKED_STATUS_ONE) {
                        ic_edid.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                    }
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

                    bottomMenu.findViewById(R.id.create).setVisibility(View.VISIBLE);
                    bottomMenu.findViewById(R.id.close).setVisibility(View.GONE);
                    bottomMenu.findViewById(R.id.copy).setVisibility(View.GONE);
                    bottomMenu.findViewById(R.id.move).setVisibility(View.GONE);
                    break;
                case ACTION_COPY:
                    menu.setGroupVisible(R.id.is_checked, false);
                    menu.setGroupVisible(R.id.mark, false);
                    bottomMenu.findViewById(R.id.create).setVisibility(View.GONE);
                    bottomMenu.findViewById(R.id.close).setVisibility(View.VISIBLE);
                    bottomMenu.findViewById(R.id.copy).setVisibility(View.VISIBLE);
                    bottomMenu.findViewById(R.id.move).setVisibility(View.GONE);
                    break;
                case ACTION_MOVE:
                    menu.setGroupVisible(R.id.is_checked, false);
                    menu.setGroupVisible(R.id.mark, false);
                    bottomMenu.findViewById(R.id.create).setVisibility(View.GONE);
                    bottomMenu.findViewById(R.id.close).setVisibility(View.VISIBLE);
                    bottomMenu.findViewById(R.id.copy).setVisibility(View.GONE);
                    bottomMenu.findViewById(R.id.move).setVisibility(View.VISIBLE);
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
                        return true;
                    }
                case R.id.edit:
                    Items.Item i = recyclerAdapter.checkedItem();
                    if (i.folder) {
                        DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceManager.this, i.name);
                        dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_EDIT);
                        return true;
                    }
                case R.id.copy:
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_copy, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_COPY;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged();
                    return true;
                case R.id.move:
                    Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_move, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    iModeMenu = ACTION_MOVE;
                    mActionMode.invalidate();
                    recyclerAdapter.notifyDataSetChanged();
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
            ((CardView) findViewById(R.id.nav_card)).setVisibility(View.GONE);
        }
    };

    //Инициализирует SearchView для меню
    private SearchView initSearchView(MenuItem searchItem) {
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
                    recyclerAdapter.loadList(db.getItemsByPater(sTable, myStack.peek().id, sLike));
                    if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
                    else mActionMode.invalidate();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    sLike = "";
                    recyclerAdapter.loadList(db.getItemsByPater(sTable, myStack.peek().id, sLike));
                    if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
                    else mActionMode.invalidate();
                }
                return false;
            }
        });

//            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
//                @Override
//                public boolean onClose() {
//                    searchView.clearFocus(); //Очищает фокус с поиска, чтобы не открывалась клавиатура -  не сработало(((
//                    return true;
//                }
//            });
        return searchView;
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
                    recyclerAdapter.loadList(db.getItemsByPater(sTable, item.id, sLike));
                }
                break;
            case R.id.checked: //Чек-бокс
                item.checked = ((CheckBox) v).isChecked();
                recyclerAdapter.checkedStatus(iModeMenu==ACTION_BAR); // Проверяем все пункты, вместе с группами, или только детей, в зависимости от активности контекстного меню.
                break;
            default: //Переход на предков
                myStack.clip(item);
                myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 10);
                recyclerAdapter.loadList(db.getItemsByPater(sTable, item.id, sLike));
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
            case R.id.move:
            case R.id.create:
            default:
                return false;
        }
    }

    @Override
    public void onEditGroupPositiveClick(String name) {
        Items.Item item = recyclerAdapter.checkedItem();
        item.name = name;
        recyclerAdapter.editGroup(item);
    }

    @Override
    public void onCreatGroupPositiveClick(String name) {
        recyclerAdapter.addGroup(name);
    }

    @Override
    public void onDeletePositiveClick() {
        recyclerAdapter.deleteRows();
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

        // заполняет вью списком предков
        private void addTextView(LinearLayout linearLayout, int limit) {
            linearLayout.removeAllViews(); // удаляем предыдущий список
            // все родители их стека
            for (Items.Item item:items.getItems()) {
                TextView textView = new TextView(context);
                textView.setTag(item);
                textView.setText((item.name.length()>limit)? item.name.substring(0, limit): item.name);
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

        private void clip(Items.Item item) {
            int index = NO_SELECTED;
            int j=0;
            // ищем позицию пункта
            for(Items.Item myItem:items.getItems()) if(myItem.equals(item)) {index=j; break;} else j++;
            // удаляем все, что правее начиная с последнего
            if (index!=NO_SELECTED) for(int i=items.size()-1;i>index;i--) items.remove(i);
        }

        private Items.Item peek() {
            Items.Item item = null;
            if (! items.isEmpty()) item = items.get(items.size()-1);
            return item;
        }
    }

    //Адаптер для списка
    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

        private final Items mValues;
        private final OnReferenceManagerInteractionListener mListener;
        private int checkedStatus;

        private RecyclerAdapter(Items items, OnReferenceManagerInteractionListener listener) {
            mValues = items;
            mListener = listener;
            checkedStatus = CHECKED_STATUS_NULL;
        }

        //Загружает список пунктов
        private void loadList(Items items) {
            mValues.clear();
            mValues.addAll(items);
            notifyDataSetChanged();
            checkedStatus = CHECKED_STATUS_NULL;
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

        //Проверяет количество отмеченных объектов
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

        //Отмечает пункты по списки
        private void setChecked(ArrayList<Integer> checked) { for(Items.Item item:mValues.getItems()) item.checked = checked.contains(item.id); }

        //Удалаяет отмеченные
        private void deleteRows() {
            for(Items.Item item: mValues.getItems())
                if (item.checked)
                    if (!item.folder || (item.folders+item.files)==0) db.deleteRecord(sTable, item.id);
                    else {
                        Snackbar.make((View) findViewById(R.id.list), "Невозможно удалить '"+item.name+"'. Содержит: "+item.folders+" папок, "+item.files+" файлов", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        break;
                    }
            loadList(db.getItemsByPater(sTable, myStack.peek().id, sLike));
            mActionMode.invalidate();
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
            loadList(db.getItemsByPater(sTable, myStack.peek().id, sLike));
            mActionMode.invalidate();
        }

        //Изменяет группу
        private void editGroup(Items.Item item) {
            db.updateRecord(sTable, item);
            notifyDataSetChanged();
        }

        //Возвращает первый попавшийся отчеченный пункт
        Items.Item checkedItem() {
            for (Items.Item item: mValues.getItems()) if (item.checked) return item;
            return null;
        }

        @NonNull
        @Override
        public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reference_manager, parent, false);
            return new RecyclerAdapter.ViewHolder(view);
        }

        // строим вью пункта
        @Override
        public void onBindViewHolder(@NonNull final RecyclerAdapter.ViewHolder holder, int position) {
            //Текущий пункт
            holder.mItem = mValues.get(position);
            // наименование и описание

            //Иконка + / -
            holder.mImageView.setImageResource(holder.mItem.folder?
                    R.drawable.ic_black_add_circle_outline_24px :
                    R.drawable.ic_baseline_remove_circle_outline_24px);

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

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final LinearLayout mItemView;
            public final CardView mCardView;
            public final ImageView mImageView;
            public final TextView mNameView;
            public final TextView mDescView;
            public final ImageView mForwardView;
            public final CheckBox mCheckedView;
            public Items.Item mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mItemView = (LinearLayout) view.findViewById(R.id.item);
                mCardView = (CardView) view.findViewById(R.id.card);
                mImageView = (ImageView) view.findViewById(R.id.image);
                mNameView = (TextView) view.findViewById(R.id.name);
                mDescView = (TextView) view.findViewById(R.id.desc);
                mForwardView =  (ImageView) view.findViewById(R.id.forward);
                mCheckedView = (CheckBox) view.findViewById(R.id.checked);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mNameView.getText() + "'";
            }
        }
    }
}
