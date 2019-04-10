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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 10.01.19 15:57
 *
 */

//Редактирование справочников с владельцем (ключ): Предметы, Показатели
public class ReferenceEdit extends AppCompatActivity implements View.OnClickListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        DialogsReferenceManager.DialogInteractionListener,
        LoaderManager.LoaderCallbacks<Items> {

    private AuditDB db; //База данных
    private String sKeyTitle = ""; //Наименование владельца для вывода в заголовок активности
    private String sTitle = ""; //Наименование справочника для вывода в список предков
    private String sTable; //Имя таблицы
    private int iModeMenu = ACTION_BAR; //Текущий режим действий
    private RecyclerAdapter recyclerAdapter; //Адаптер для RecyclerView
    private LinearLayoutManager mLayoutManager; //Менеджер для RecyclerView
    private Stack myStack; //Все имена предков
    private String sLike = ""; //Строка для отбора по наименованию
    private ArrayList<Integer> checkedIdsCopyMove; //Список отмеченных для копирования/переноса
    private ArrayList<Integer> checkedIds = new ArrayList<>(); //Список отмеченных для поворота экрана и возврата в активность
    private Bundle bArgs; //Агрументы для загрузчика списка

    private static final int NOT_SELECTED = -1;
    private static final int NAME_LENGTH = 20; //Максимальное количество символов в наименованни предков

    //Режимы меню
    private static final int ACTION_BAR = 0;
    private static final int ACTION_COPY = 1;
    private static final int ACTION_MOVE = 2;

    //Аргументы
    private static final String ARG_TITLE = "title"; //Заголовок активности
    private static final String ARG_TABLE = "table"; //Таблица элементов
    private static final String ARG_PATER = "pater"; //Текущий родитель
    private static final String ARG_STATE = "state"; //Состояние RecyclerView
    private static final String ARG_LIKE = "like"; //Строка поиска
    private static final String ARG_STATUS = "status"; //Статус отметки пунктов для контектного меню
    private static final String ARG_CHECKED = "checked"; //Список с отметками
    private static final String ARG_COPY_MOVE_ID = "copy_move_id"; //пункты для копирования и переноса
    private static final String ARG_KEY_NAME = "key_name"; //имя ключевой колонки
    private static final String ARG_KEY = "key"; //ключ для отбора
    private static final String ARG_MODE_MENU = "mode_action"; //Режим действий

    //Состояние отметок:
    private final static int CHECKED_STATUS_NULL = 0; //Нет отмеченных пунктов
    private final static int CHECKED_STATUS_ONE = 1; //Помечен один пункт
    private final static int CHECKED_STATUS_SOME = 2; //Помечено несколько пунктов
    private final static int CHECKED_STATUS_ALL = 3; //Помечены все пункты

    //ВОЗВРАЩАЕТ ИНТЕНТ ДЛЯ АКТИВНОСТИ
    /*
    context - контекст, откуда открывается активность
    table - имя таблицы базы данных справочника, который собираемся редактировать (Предметы, Показатели)
    title - заголовок активности
    keyName - имя колонки базы данных с ключем владельца справочника
    key -  значение ключа владельца для отбора
     */
    public static Intent intentActivity(Context context, String table, String title, String keyName, int key) {
        Intent intent = new Intent(context, ReferenceEdit.class);
        intent.putExtra(ARG_TABLE, table);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_KEY_NAME, keyName);
        intent.putExtra(ARG_KEY, key);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        int iPater = 0; //Текущий родитель = 0 - корень списка
        String sKeyName; //Имя ключевой колонки для отбора
        int iKey; //Значение ключа для отбора

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_edit);

        //Меню действий
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Навигационное меню
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);

        //Менеджер для рециклер. Может быть грид и т.п.
        mLayoutManager = new LinearLayoutManager(this);

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Устанавливаем основные параметры
        if (savedInstanceState==null) { //активность запускается впервые
            Intent intent = getIntent();
            sTable = intent.getStringExtra(ARG_TABLE); //Имя таблицы с данными
            sTitle = intent.getStringExtra(ARG_TITLE); //Заголовок активности
            sKeyName = intent.getStringExtra(ARG_KEY_NAME); //Имя ключевой колонки
            iKey = intent.getIntExtra(ARG_KEY, NOT_SELECTED); //Значение ключа
        }
        else { //активность восстатавливаем после поворота экрана
            sTable = savedInstanceState.getString(ARG_TABLE);
            sTitle = savedInstanceState.getString(ARG_TITLE);
            sKeyName = savedInstanceState.getString(ARG_KEY_NAME);
            iKey = savedInstanceState.getInt(ARG_KEY, NOT_SELECTED);
            iPater = savedInstanceState.getInt(ARG_PATER);
            sLike = savedInstanceState.getString(ARG_LIKE, ""); //Сохраняем строку поиска
        }
        //Заголовок активности
        setTitle(sTitle);

        //Выводим всех предков
        myStack = new Stack(this);
        //Корень - наименование ключевого элемента справочника-владельца
//        myStack.push(new Items.Item(0,true, NOT_SELECTED, NOT_SELECTED, NOT_SELECTED,
//                db.getName(sKeyName, iKey),null));
        loadStack(iPater); //Добавляем в стек всех предков в т.ч. родителя
        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), NAME_LENGTH);

        // настраиваем список
        recyclerAdapter = new RecyclerAdapter();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);

        //Аргументы для загрузчика
        bArgs = new Bundle();
        bArgs.putString(ARG_TABLE, sTable);
        bArgs.putString(ARG_KEY_NAME, sKeyName);
        bArgs.putInt(ARG_KEY, iKey);

        //Запускаем загрузчик для чтения данных
        updateLoader();
    }

    //Создает или рестартует загрузчик данных. Id загрузчика эквивалентен значению Родителя
    private void updateLoader() {
//        int loaderId = myStack.peek().id;
//        // создаем загрузчик для чтения данных
//        Loader loader = getSupportLoaderManager().getLoader(loaderId);
//        if (loader != null && !loader.isReset()) getSupportLoaderManager().restartLoader(loaderId, bArgs, this);
//        else getSupportLoaderManager().initLoader(loaderId, bArgs, this);
    }

    //Загружает в стек всех предков родителя
    private void loadStack(int id) {
        if (id>0) {
            int pater = db.getPater(sTable, id);
            if(pater>0) loadStack(pater); //Рекурсия
//            myStack.push(new Items.Item(id, true, NOT_SELECTED, NOT_SELECTED,0, db.getName(sTable, id), null));
        }
    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    // перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TABLE, sTable); //Таблица с данными
        outState.putString(ARG_TITLE, sTitle); //Заголовок активности
        outState.putString(ARG_KEY_NAME, bArgs.getString(ARG_KEY_NAME)); //Ключевое поле
        outState.putInt(ARG_KEY, bArgs.getInt(ARG_KEY)); //Значение ключа
//        outState.putInt(ARG_PATER, myStack.peek().id); //Текущий родитель
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
        if (iModeMenu == ACTION_COPY | iModeMenu == ACTION_MOVE)
            checkedIdsCopyMove = savedInstanceState.getIntegerArrayList(ARG_COPY_MOVE_ID); //Пункты для копирования и переноса
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        recyclerAdapter = null;
        mLayoutManager = null;
        myStack = null;
        // закрываем подключение при выходе
        db.close();
    }

    //ВСЕ ДЛЯ МЕНЮ ДЕЙСТВИЙ:
    //Создает меню действий
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cont_reference_manager, menu);
        initSearchView(menu.findItem(R.id.search)); //инициализируем поиск
        return true;
    }

    //Готовит меню действий
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (iModeMenu) {
            case ACTION_BAR:
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

        //Устанавливаем видимость значков в навигационном меню
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

        return super.onPrepareOptionsMenu(menu);
    }

    // вызывается при нажатии на пункт меню действий
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.allmark: //Отметить все
                recyclerAdapter.setCheckedAll(true, false);
                invalidateOptionsMenu();
                return true;
            case R.id.unmark: //Снять все отметки
                recyclerAdapter.setCheckedAll(false, false);
                invalidateOptionsMenu();
                return true;
            case R.id.delete: //Удаление отмеченных записей
                int count = recyclerAdapter.checkedCount();
                if (count>0) {
                    DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceEdit.this, count);
                    dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_DELETE);
                }
                return true;
            case R.id.edit:
                Items.Item i = recyclerAdapter.checkedItemFirst();
                if (i.folder) { //Редактирование наименований любых папок
                    DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(ReferenceEdit.this, i.name);
                    dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_EDIT_GROUP);
                    return true;
                }
                else switch (sTable) {
                    case AuditDB.TBL_SUBJECT: //Редактирование предмета аудита
//                        startActivity(TypeActivity.intentActivityEdit(ReferenceEdit.this, i.id));
                        return true;
                    case AuditDB.TBL_INDICATOR: //Редактирование показателя аудита
//                        startActivity(TypeActivity.intentActivityEdit(ReferenceEdit.this, i.id));
                        return true;
                    default:
                        return super.onOptionsItemSelected(item);
                }
            case R.id.copy:
                Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_copy, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                iModeMenu = ACTION_COPY;
                invalidateOptionsMenu();
                checkedIdsCopyMove = recyclerAdapter.getChecked(); //Сохраняем отмеченные элементы для последующего копирования
                return true;
            case R.id.move:
                Snackbar.make((View) findViewById(R.id.list), R.string.msg_place_move, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                iModeMenu = ACTION_MOVE;
                invalidateOptionsMenu();
                checkedIdsCopyMove = recyclerAdapter.getChecked(); //Сохраняем отмеченные элементы для последующего перемещения
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
                    bArgs.putString(ARG_LIKE, sLike);
                    updateLoader();
                    if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    sLike = "";
                    bArgs.putString(ARG_LIKE, sLike);
                    updateLoader();
                    if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
                }
                return false;
            }
        });
    }

    // устанавливает видимость значков в нижнем навигационном меню
    private void setVisibilityIconBNV() {
        BottomNavigationView bottomNavigationView = ((BottomNavigationView) findViewById(R.id.navigation));
        switch (iModeMenu) {
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

    //Обработчик возврата назад
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // вызывается при нажатии на пункт списка, чек-бокс и предков
    @Override
    public void onClick(View v) {
        Items.Item item = (Items.Item) v.getTag();
        switch (v.getId()) {
            case R.id.item: //Весь пункт
                if (item.folder) { //Проваливаемся в группу
                    myStack.push(item);
                    myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), NAME_LENGTH);
                    updateLoader();
                }
                break;
            case R.id.checked: //Чек-бокс
                item.checked = ((CheckBox) v).isChecked();
                recyclerAdapter.checkedStatus(false); // Проверяем все пункты, вместе с группами
                break;
            default: //Переход на предков
                myStack.clip(item);
                myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), NAME_LENGTH);
                updateLoader();
                break;
        }
        if (iModeMenu==ACTION_BAR) invalidateOptionsMenu();
    }

    // вызывается при нажатии на пункт навигационного меню
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_group:
                DialogFragment dialogEditGroup = DialogsReferenceManager.newInstance(this);
                dialogEditGroup.show(getSupportFragmentManager(), DialogsReferenceManager.TAG_CREATE_GROUP);
                return true;
            case R.id.close:
                iModeMenu = ACTION_BAR;
                invalidateOptionsMenu();
                return true;
            case R.id.copy: //Копируем предварительно отмеченные элементы в текущего родителя
                for(Integer id: checkedIdsCopyMove) {
                    int newId = NOT_SELECTED;
//                    switch (sTable) {
//                        case AuditDB.TBL_SUBJECT:
//                            newId = db.copySubject(id, myStack.peek().id);
//                            break;
//                        case AuditDB.TBL_INDICATOR:
////                        newId = db.copyIndicator(id, myStack.peek().id);
//                            break;
//                        default:
//                            newId = db.copyRecord(sTable, id, myStack.peek().id);
//                    }
                    recyclerAdapter.insert(db.getItem(sTable, newId, sLike));
                }
                checkedIdsCopyMove = null;
                iModeMenu = ACTION_BAR;
                invalidateOptionsMenu();
                recyclerAdapter.notifyDataSetChanged(); //Перерисуем все пункты, чтобы вернуть чекбоксы
                return true;
            case R.id.move: //Перемещаем предварительно отмеченные элементы в текущего родителя
                for(Integer id: checkedIdsCopyMove) {
//                    if (!myStack.contains(id)) db.moveRecord(sTable, id, myStack.peek().id);
//                    else { //Нельзя перемещать в своих потомков
//                        Snackbar.make((View) findViewById(R.id.items), R.string.msg_move_error, Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
//                        break;
//                    }
                    recyclerAdapter.insert(db.getItem(sTable, id, sLike));
                }
                checkedIdsCopyMove = null;
                iModeMenu = ACTION_BAR;
                invalidateOptionsMenu();
                recyclerAdapter.notifyDataSetChanged(); //Перерисуем все пункты, чтобы вернуть чекбоксы
                return true;
            case R.id.create:
                switch (sTable) {
                    case AuditDB.TBL_SUBJECT: //Создание предмета аудита
//                        startActivity(TypeActivity.intentActivityCreate(this, myStack.peek().id));
                        return true;
                    case AuditDB.TBL_INDICATOR: //Создание показателя аудита
//                        startActivity(TypeActivity.intentActivityCreate(this, myStack.peek().id));
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    //вызывается при редактировании группы
    @Override
    public void onEditGroupPositiveClick(String name) {
        Items.Item item = recyclerAdapter.checkedItemFirst(); //Ищем первый-единственный отмеченный пункт
        item.name = name;
        db.updateRecord(sTable, item);
        recyclerAdapter.notifyDataSetChanged(); //При этом, положение элемента не меняется
    }

    //вызывается при создании группы
    @Override
    public void onCreateGroupPositiveClick(String name) {
//        Items.Item item = new Items.Item(NOT_SELECTED, true, NOT_SELECTED, NOT_SELECTED, myStack.peek().id, id, "");
//        item.id = db.insertRecord(sTable, item, bArgs.getString(ARG_KEY_NAME), bArgs.getInt(ARG_KEY, NOT_SELECTED));
//        myStack.push(item);
//        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), NAME_LENGTH);
//        updateLoader(); //Т.к. проваливаемся в новую группу, то обновим загрузчик
//        invalidateOptionsMenu();
    }

    //вызывается при удалении строк
    @Override
    public void onDeletePositiveClick() {
        recyclerAdapter.deleteRows();
        invalidateOptionsMenu();
    }

    @Override
    public void onDeleteNegativeClick() {

    }

    //создает загрузчик
    @NonNull
    @Override
    public Loader<Items> onCreateLoader(int id, @Nullable Bundle args) {
        return new LoaderItems(this, db, args.getString(ARG_TABLE), id, args.getString(ARG_KEY_NAME), args.getInt(ARG_KEY), args.getString(ARG_LIKE));
    }

    //вызывается, когда загрузка закончилась
    @Override
    public void onLoadFinished(@NonNull Loader<Items> loader, Items data) {
//        if (loader.getId() == myStack.peek().id) { //Защита от повторной загрузки ненужного результата
//            recyclerAdapter.loadList(data);
//            setVisibilityIconBNV();
//        }
    }

    //вызыватеся для очистки загрузчика
    @Override
    public void onLoaderReset(@NonNull Loader<Items> loader) {
        recyclerAdapter.loadList(null);
    }

    //Адаптер для списка. Для заполнения списка используется загрузчик LoaderItems
    private class RecyclerAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

        private final Items mValues; //Данные для рециклер
        private int checkedStatus; //Статус отметок

        private RecyclerAdapter() {
            mValues = new Items();
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
            checkedIds=null; //Список больше не нужен
            checkedStatus(false);
            notifyDataSetChanged();
        }

        //Помечает/отменяет отметки всех видимых элементов или только детей
        private void setCheckedAll(boolean checked, boolean only_child) {
            if (only_child) { for(Items.Item item:mValues) if (!item.folder) item.checked=checked; }
            else { for(Items.Item item:mValues) item.checked=checked; }
            notifyDataSetChanged();
            checkedStatus = checked?checkedStatus(only_child):CHECKED_STATUS_NULL;
        }

        //Проверяет количество отмеченных пунктов
        private int checkedStatus(boolean only_child) {
            int count = 0; //Общее количество пунктов
            int checked = 0; //Из них отмеченных

            //Подсчитываем количество отмеченных и всех
            if (only_child) {
                for(Items.Item item:mValues) {
                    if (!item.folder) {
                        count++;
                        if (item.checked) checked++;
                    }
                }
            }
            else {
                count = mValues.size();
                for(Items.Item item:mValues) if (item.checked) checked++;
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
//            for(Items.Item item:mValues) if (item.checked) checked.addItem(item.id);
            return checked;
        }

        //Отмечает пункты по списку
        private void setChecked(ArrayList<Integer> checked) {
            if (!(checked==null || checked.isEmpty()))
                for(Items.Item item: mValues) item.checked = checked.contains(item.id);
        }

        //Удалаяет отмеченные
        private void deleteRows() {
            int position = 0;
            int count = mValues.size();
            for(int i=0; i<count; i++) {
                Items.Item item = mValues.get(position);
                if (item.checked) {
//                    if (!item.folder)
//                        switch (sTable) {
//                            case AuditDB.TBL_SUBJECT:
//                                db.delSubject(item.id);
//                                break;
//                            case AuditDB.TBL_INDICATOR:
////                                db.delIndicator(item.id);
//                                break;
//                            default:
//                                db.deleteRecord(sTable, item.id);
//                        }
//                    else if ((item.folders+item.files)==0) db.deleteRecord(sTable, item.id);
//                    else {
//                        Snackbar.make((View) findViewById(R.id.items), "Невозможно удалить '"+item.id+"'. Содержит: "+item.folders+" папок, "+item.files+" файлов", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
//                        break;
//                    }
                    remove(position);
                }
                else position++;
            }
        }

        //Удаляет пункт и обновляет рециклер
        private void remove(int position) {
            mValues.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position,mValues.size()-position);
        }

        //Добавляет пункт и обновляет рециклер
        private void insert(Items.Item item) {
            mValues.add(item);
            notifyItemInserted(mValues.size()-1);
        }

        //Добавляет пункты и обновляет рециклер
        private void insertAll(Items items) {
            mValues.addAll(items);
            notifyItemRangeInserted(mValues.size()-1-items.size(), items.size());
        }

        //Возвращает количество отмеченных строк
        private int checkedCount() {
            int i = 0;
            for(Items.Item item: mValues) if (item.checked) i++;
            return i;
        }

        //Возвращает первый попавшийся отчеченный пункт
        private Items.Item checkedItemFirst() {
            for (Items.Item item: mValues) if (item.checked) return item;
            return null;
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
//            //Текущий пункт
//            holder.mItem = mValues.getItem(position);
//
//            //Иконка + / -
//            holder.mImageView.setImageResource(holder.mItem.folder?
//                    R.drawable.ic_black_add_circle_outline_24px :
//                    R.drawable.ic_baseline_remove_circle_outline_24px);
//
//            // наименование и описание
//            holder.mNameView.setText(holder.mItem.id);
////            holder.mDescView.setText(holder.mItem.desc);
//            holder.mItemView.setTag(holder.mItem); // в теге храним пункт
//
//            switch (iModeMenu) {
//                case ACTION_BAR:
//                    holder.mForwardView.setVisibility(View.GONE);
//                    holder.mCheckedView.setVisibility(View.VISIBLE);
//                    holder.mCheckedView.setChecked(holder.mItem.checked);
//                    holder.mCheckedView.setTag(holder.mItem);
//                    holder.mCheckedView.setOnClickListener(ReferenceEdit.this);
//                    if (holder.mItem.folder) holder.mItemView.setOnClickListener(ReferenceEdit.this); //Папки открываем
//                    else holder.mItemView.setOnClickListener(null); //Только чек-бокс
//                    break;
//                case ACTION_COPY:
//                case ACTION_MOVE:
//                    holder.mCheckedView.setVisibility(View.GONE);
//                    if (holder.mItem.folder) {
//                        holder.mForwardView.setVisibility(View.VISIBLE);
//                        holder.mItemView.setOnClickListener(ReferenceEdit.this); //Папки открываем
//                    }
//                    else {
//                        holder.mForwardView.setVisibility(View.GONE);
//                        holder.mItemView.setOnClickListener(null); //Файлы не трогаем
//                    }
//                    break;
//            }
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
        String keyName;
        int key;
        String like;

        private LoaderItems(Context context, AuditDB db, String table, int pater, String keyName, int key, String like) {
            super(context);
            this.db = db;
            this.table = table;
            this.pater = pater;
            this.keyName = keyName;
            this.key = key;
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
            return db.getItemsByPater(table, pater, keyName, key, like);
        }
    }

}
