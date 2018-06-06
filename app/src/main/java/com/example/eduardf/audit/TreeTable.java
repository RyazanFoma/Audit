package com.example.eduardf.audit;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v4.util.ArraySet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Space;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Set;

public class TreeTable extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private AuditDB db; //База данных
    private TableAdapter tableAdapter; //Адаптер списка
    private int iMode; //Режим выбора
    private String sTable; //Имя таблицы
    private String sLike = ""; //Строка для отбора по наименованию объектов
    private int[] in; //Группы эдеметов для отбора
    private int iPater = 0; //Текущий родитель

    static final int NO_SELECTED = -1; //Нет элемента для выбора
    static final int MODE_SINGLE_CHOICE = 1; //Режим одиночного выбора
    static final int MODE_MULTIPLE_CHOICE = 2; //Режим множественного выбора
    static final int MODE_GROUP_CHOICE = 3; //Режим выбора группы

    //Ключи intent - параметры для activity
    static final String ARG_TITLE = "title"; //Заголовок activity
    static final String ARG_TABLE = "table"; //Таблица элементов
    static final String ARG_MODE = "mode"; //Режим выбора
    static final String ARG_ID = "id"; //Текущие отмеченные идентификаторы элементов
    static final String ARG_IN = "in"; //Группы объектов для отбора
    static final String ARG_LIKE = "like"; //Строка для отбора по наименованию

    private static final int CM_OPEN = 1;
    private static final int CM_COPY = 2;
    private static final int CM_DELETE = 3;

    private static final int DEL_ITEM = 1;
    private static final int DEL_ITEMS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tree_table);

        //Инструментальное меню
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Читаем аргументы из интент
        Intent intent = getIntent();
        setTitle(intent.getStringExtra(ARG_TITLE)); //Заголовок активити
        sTable = intent.getStringExtra(ARG_TABLE); //Имя таблицы с данными
        iMode = intent.getIntExtra(ARG_MODE, MODE_SINGLE_CHOICE); //Режим выбора. По умолчанию - одиночный выбор
        in = intent.getIntArrayExtra(ARG_IN); //Группы для отбора

        //Создаем адаптер
        tableAdapter = new TableAdapter(this);

        if (savedInstanceState==null) { //Первое создание активити
            sLike = intent.getStringExtra(ARG_LIKE);
            if (iMode==MODE_MULTIPLE_CHOICE) {
                int[] id = intent.getIntArrayExtra(ARG_ID);
                if (id!=null&&id.length>0) {
                    iPater=db.getPaterById(sTable,id[0]); //Или ищем родителя для первого отмеченного элемента
                    tableAdapter.addIds(id);
                }
            }
            else {
                int id = intent.getIntExtra(ARG_ID,NO_SELECTED); //Потом ищем int
                if (id!=NO_SELECTED) {
                    iPater=db.getPaterById(sTable,id); //Или ищем родителя для первого отмеченного элемента
                    tableAdapter.addId(id);
                }
            }
        }
        else { //Восстанавливаем родителя и строку поиска после поворота экрана
            iPater = savedInstanceState.getInt("pater", 0); //Восстанавливаем текущего родителя
            sLike = savedInstanceState.getString("like", ""); //Восстанавливаем строку поиска
        }

        tableAdapter.loadList(iPater);
        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(tableAdapter);
        listView.setOnItemClickListener(this);
        if (iMode!=MODE_GROUP_CHOICE) registerForContextMenu(listView); // добавляем контекстное меню к списку

//        if (id!=null&&id.length>0) listView.smoothScrollToPosition(tableAdapter.getItemPosition(id[0]));
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
        setResult(RESULT_CANCELED, null);
    }

    //ВСЕ ДЛЯ ИНСТРУМЕНТАЛЬНОГО МЕНЮ:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//         Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tree_table, menu);
        //Пункты Отметить все и снять отметки доступны только в режиме множественного выбора
        if (iMode != MODE_MULTIPLE_CHOICE) {
            ((MenuItem)menu.findItem(R.id.allmark)).setVisible(false);
            ((MenuItem)menu.findItem(R.id.unmark)).setVisible(false);
            ((MenuItem)menu.findItem(R.id.delete)).setVisible(false);
        }

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if(null!=searchManager ) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        searchView.setIconifiedByDefault(true); //Поиск свернут по умолчанию!!!
        searchView.setQueryHint(getString(R.string.search_hint_name));

        if ((sLike!=null)&&(!sLike.isEmpty())) {
            searchItem.expandActionView();
            searchView.setQuery(sLike, true);
            searchView.clearFocus();
            tableAdapter.loadList(iPater);
            tableAdapter.notifyDataSetChanged();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                sLike = query;
                tableAdapter.loadList(iPater);
                tableAdapter.notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    sLike = "";
                    tableAdapter.loadList(iPater);
                    tableAdapter.notifyDataSetChanged();
                }
                return false;
            }
        });

        //Кнопка выбора не нужна для выбора в одно касание
        if (iMode == MODE_SINGLE_CHOICE) ((MenuItem)menu.findItem(R.id.choose)).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.allmark:
                tableAdapter.checkedAll();
                tableAdapter.notifyDataSetChanged();
                return true;
            case R.id.unmark:
                tableAdapter.uncheckedAll();
                tableAdapter.notifyDataSetChanged();
                return true;
            case R.id.choose:
                intent = new Intent();
                intent.putExtra(ARG_ID, tableAdapter.getIds());
                setResult(RESULT_OK, intent);
                finish(); //Закрываем активити
                return true;
            case R.id.create:
            case R.id.creategroup:
            case R.id.delete:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //ВСЕ ДЛЯ КОНТЕКСТНОГО МЕНЮ:
    //Создает контекстное меню
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_OPEN, 0, R.string.item_open);
        menu.add(0, CM_COPY, 0, R.string.item_copy);
        menu.add(0, CM_DELETE, 0, R.string.item_delete);
    }

    //Контекстное меню
    public boolean onContextItemSelected(MenuItem item) {
        // получаем из пункта контекстного меню данные по пункту списка
        int id = (int) ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;

        switch (item.getItemId()) {
            case CM_DELETE:
                return true;
            case CM_OPEN:
                if (sTable.contentEquals(AuditDB.TBL_TYPE)) {
                    Intent intent = new Intent(this, ActivityType.class);
                    intent.putExtra(ActivityType.ARG_MODE, ActivityType.OPEN_MODE);
                    intent.putExtra(ActivityType.ARG_ID,id);
                    startActivityForResult(intent, 1);
                }
                return true;
            case CM_COPY:
                if (sTable.contentEquals(AuditDB.TBL_TYPE)) {
                    Intent intent = new Intent(this, ActivityType.class);
                    intent.putExtra(ActivityType.ARG_MODE, ActivityType.COPY_MODE);
                    intent.putExtra(ActivityType.ARG_ID,id);
                    startActivityForResult(intent, 1);
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }

    //Обработчик возврата назад
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    //Сохраняет текущее значение статуса перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList("checked",tableAdapter.getChecked()); //Сохраняем отметки
        outState.putInt("pater",iPater); //Сохраняем строку поиска
        outState.putString("like",sLike); //Сохраняем строку поиска
    }

    //Восстанавливает значение статуса после поворота экрана и текущую закладку
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tableAdapter.setChecked(savedInstanceState.getIntegerArrayList("checked"));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TableAdapter.Item item = (TableAdapter.Item) tableAdapter.getItem(position);
        if ((iMode!=MODE_GROUP_CHOICE)&&(item.is_group)) {
            if (item.id == iPater)
                iPater = item.pater; //Закрываем группу
            else
                iPater = item.id; //Открываем группу
            tableAdapter.loadList(iPater);
            tableAdapter.notifyDataSetChanged();
        }
        else if (iMode!=MODE_MULTIPLE_CHOICE) {
            tableAdapter.uncheckedAll(); tableAdapter.notifyDataSetChanged(); //Очищаем выделенные фоном
            LinearLayout line = (LinearLayout) view.findViewById(R.id.line);
            AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(TreeTable.this, R.animator.background);
            set.setTarget(line);
            set.start();
            Intent intent = new Intent(this, TreeTable.class);
            intent.putExtra(ARG_ID,item.id);
            setResult(RESULT_OK, intent);
            line.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish(); //Закрываем активити
                }
            }, 1000L);
        }
    }

    //Возвращает диалоги для смены статуса и удаления
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        switch (id) {
            case DEL_ITEM: //Удаление текущего задания
                adb.setTitle(R.string.ttl_delete)
                        .setMessage(R.string.msg_delete)
                        .setPositiveButton(R.string.btn_delete, clDelRow);
                break;
            case DEL_ITEMS: //Удаления
                adb.setTitle(R.string.ttl_delete)
                        .setMessage(R.string.msg_delete_n)
                        .setPositiveButton(R.string.btn_delete, clDelRows);
                break;
        }
        adb.setNegativeButton(R.string.btn_cancel, null);
        return adb.create();
    }

    //ОБРАБОТЧИКИ ДЛЯ ДИАЛОГОВ
    //Обработчик удаления текущего строки
    Dialog.OnClickListener clDelRow = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) { /*if (which == Dialog.BUTTON_POSITIVE) TableAdapter.deleteRow(savedId);*/ }
    };

    //Обработчик удаления отмеченных строк
    Dialog.OnClickListener clDelRows = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) { /*if (which == Dialog.BUTTON_POSITIVE) TableAdapter.deleteRows();*/ }
    };

    //Класс адаптер списка
    private class TableAdapter extends BaseAdapter implements View.OnClickListener {

        private LayoutInflater inflater;
        private ArrayList<Item> items; //Массив пунктов списка
        private Set<Integer> checked; //Отмеченные пункты

        //Конструктор адаптера
        private TableAdapter(Context context) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            items = new ArrayList<>();
            checked = new ArraySet<>();
        }

        //Загружает в список всех родителей
        private int loadPater(int pater) {
            int next = db.getPaterById(sTable,pater);
            if (next!=NO_SELECTED) {
                int level = loadPater(next); //Рекурсия!!!
                items.add(new Item(true,level,db.getPaterById(sTable,pater),pater,db.getNameById(sTable,pater)));
                return level+1;
            }
            else return 0;
        }

        //Загружает список пунктов
        private void loadList(int pater) {
            items.clear();
            int level = loadPater(pater);
            Cursor c;

            if (iMode==MODE_GROUP_CHOICE) items.add(new Item(true,0,0,0,"<НЕ ВХОДИТ В ГРУППУ>"));

            if (pater<=0)
                c = db.getChildsInPater(sTable,0,in); //Если кореневой уровень, то отбираем папки по массиву in
            else
                c = db.getChildsByPater(sTable,pater,sLike); //Получаем все элементы с отбором по наименованию

            if (c.moveToFirst()) {
                do {
                    boolean is_group = c.getInt(c.getColumnIndex(db.IS_GROUP))!=0;
                    if ((iMode!=MODE_GROUP_CHOICE)||(is_group))
                        items.add(new Item(is_group,level,pater,c.getInt(c.getColumnIndex(db.ID)),c.getString(c.getColumnIndex(db.NAME))));
                } while (c.moveToNext());
            }
        }

        //Удалить id из множества отмеченных
        private boolean remoteId(int id) {
            return checked.remove(id);
        }

        //Добавляет id в множество отмеченных
        private boolean addId(int id) {
            return checked.add(id);
        }

        //Заполняет множество отмеченных массивом id
        private void addIds(int[] ids) {
            uncheckedAll();
            for(int id:ids) addId(id);
        }

        //Возвращает массив отмеченных id
        private int[] getIds() {
            int[] rez = new int[checked.size()];
            int j=0;
            for(Integer i:checked) rez[j++] = i;
            return rez;
        }

        //Возвращает отмеченные
        private ArrayList<Integer> getChecked() {
            return new ArrayList<>(checked);
        }

        //Устанавливает отмеченные
        private void setChecked(ArrayList<Integer> arrayList) {
            uncheckedAll();
            checked.addAll(arrayList);
        }

        private void checkedAll() {
            uncheckedAll();
            for (Item item:items) if (!item.is_group) addId(item.id);
        }

        private void uncheckedAll() {
            checked.clear();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView==null) view = inflater.inflate(R.layout.item_tree_table, parent, false);
            else view = convertView;

            //Отбъект пункта
            Item item = (Item) getItem(position);

            //Отступ, зависящий от уровня
            Space space = (Space) view.findViewById(R.id.space);
            ViewGroup.LayoutParams params = null;
            params = space.getLayoutParams();
            params.width = 32*item.level;
            space.setLayoutParams(params);

            //Устанавливаем пиктограмму, чекбокс, выделение пункта
            LinearLayout line = (LinearLayout) view.findViewById(R.id.line);
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            if (item.is_group) { //Группа
                //Пиктограмма
                imageView.setVisibility(View.VISIBLE);
                if (item.id == iPater) imageView.setImageResource(android.R.drawable.arrow_down_float); //Родительская группа
                else imageView.setImageResource(android.R.drawable.arrow_up_float);
                //У группы не должно быть чекбокса и выделения фоном
                checkBox.setVisibility(View.GONE);
                line.setBackgroundColor(0);
            }
            else { //Элемент
                imageView.setVisibility(View.GONE);
                switch (iMode) {
                    case MODE_MULTIPLE_CHOICE:
                        checkBox.setVisibility(View.VISIBLE);
                        checkBox.setChecked(checked.contains(item.id));
                        checkBox.setOnClickListener(this);
                        checkBox.setTag(item.id);
                        break;
                    case MODE_SINGLE_CHOICE:
                        checkBox.setVisibility(View.GONE);
                        if (!checked.contains(item.id)) line.setBackgroundColor(0);
                        else line.setBackgroundResource(R.color.colorBackgroundAccent);
                        break;
                }
            }

            //Текст
            ((TextView) view.findViewById(R.id.text)).setText(item.name);

            return view;
        }

        private int getItemPosition(int id) {
            int position = 0;
            for (Item item: items) {
                if (item.id == id) break;
                position++;
            }
            return position;
        }

        @Override
        public void onClick(View v) {
            CheckBox checkBox = (CheckBox) v;
            int id = (int) checkBox.getTag();
            if (checkBox.isChecked()) {
                addId((int) id);
                checkBox.setChecked(true);
            } else {
                remoteId((int) id);
                checkBox.setChecked(false);
            }
        }

        //Класс для пунктов списка
        private class Item {
            int id;
            int level;
            boolean is_group;
            int pater;
            String name;

            //Конструктор пункта
            private Item( boolean is_group, int level, int pater, int id, String name) {
                this.id = id;
                this.level = level;
                this.is_group = is_group;
                this.name = name;
                this.pater = pater;
            }
        }
    }
}

