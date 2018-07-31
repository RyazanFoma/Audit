package com.example.eduardf.audit;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.util.ArraySet;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Set;

import static com.example.eduardf.audit.R.animator.background;

public class TaskList extends AppCompatActivity implements LoaderCallbacks<Cursor>, TabLayout.OnTabSelectedListener {

    //Контекстное меню
    private static final int CM_OPEN_ID = 1;
    private static final int CM_COPY_ID = 2;
    private static final int CM_STATUS_ID = 3;
    private static final int CM_DELETE_ID = 4;
    private static final int CHNG_STATUS = 1;
    private static final int CHNG_STATUS_N = 2;
    private static final int DEL_TASK = 3;
    private static final int DEL_TASKS = 4;

    ListView lvData; //Список заданий
    TaskAdapter cAdapter; //Курсор для чтения заданий
    ProgressDialog pd; //Прогресс для ожидания
    AuditDB db; //База данных
    int iAuditor; //Аудитор для отбора заданий
    int iStatus = 0; //Статус заданий для отбора по закладкам: Утвержден = 0, В работе = 1, Проведен = 2
    int savedId; //id текущей задачи в контектсном меню
    private String sLike = ""; //Строка для отбора по наименованию объектов

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        //Создаем прогресс для показа в ожидании
//        pd = new ProgressDialog(this);
//        pd.setTitle(R.string.progress_title);
//        pd.setMessage(getResources().getString(R.string.progress_msq));

        //Лента инструментов
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp

        //Закладки для отбора по статусу
        final TabLayout tt = (TabLayout) findViewById(R.id.tasktabs);
        TabLayout.Tab tab = tt.getTabAt(iStatus); tab.select(); //Выбираем текущую закладку
        tt.addOnTabSelectedListener(this); //Обработчик закладок ниже

        //Получаем аудитора для отбора заданий
        Intent intent = getIntent();
        iAuditor = intent.getIntExtra("Auditor", 0);

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        // создаем адаптер и настраиваем список
        cAdapter = new TaskAdapter(this,  null, 0);
        lvData = findViewById(R.id.tasklist);
        lvData.setAdapter(cAdapter);
        registerForContextMenu(lvData); // добавляем контекстное меню к списку

        // создаем загрузчик для чтения данных
        getSupportLoaderManager().initLoader(1, null, this);
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
    }

    //Обновляет список заданий в случае успешного сохранения задания
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK) getSupportLoaderManager().restartLoader(1, null, this);
    }

    //ВСЕ ДЛЯ ИНСТРУМЕНТАЛЬНОГО МЕНЮ:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task_list, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if(null!=searchManager ) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        searchView.setIconifiedByDefault(true); //Поиск свернут по умолчанию
        searchView.setQueryHint(getString(R.string.search_hint_object));

        //Если есть текст запроса,
        if (!sLike.isEmpty()) { //то переходим в режим поиска
            searchItem.expandActionView();
            searchView.setQuery(sLike, true);
            searchView.clearFocus();
            getSupportLoaderManager().restartLoader(1, null, this);
        }

        //Обработчик текста запроса для поиска
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                sLike = query;
                getSupportLoaderManager().restartLoader(1, null, TaskList.this);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    sLike = "";
                    getSupportLoaderManager().restartLoader(1, null, TaskList.this);
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.task_allmark:
                cAdapter.checkedAll();
                cAdapter.notifyDataSetChanged();
                return true;
            case R.id.task_unmark:
                cAdapter.uncheckedAll();
                cAdapter.notifyDataSetChanged();
                return true;
            case R.id.task_create:
                // получаем из пункта контекстного меню данные по пункту списка
                startActivityForResult(AuditTask.intentActivityCreate(TaskList.this,
                        iAuditor, iStatus ), 1);
                return true;
            case R.id.task_status:
                showDialog(CHNG_STATUS_N);
                return true;
            case R.id.task_del:
                showDialog(DEL_TASKS);
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

    //ВСЕ ДЛЯ КОНТЕКСТНОГО МЕНЮ:
    //Создает контекстное меню
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_OPEN_ID, 0, R.string.cnt_item_task_open);
        menu.add(0, CM_COPY_ID, 0, R.string.cnt_item_task_copy);
        menu.add(0, CM_STATUS_ID, 0, R.string.cnt_item_task_status);
        menu.add(0, CM_DELETE_ID, 0, R.string.cnt_item_task_delete);
    }

    //Контекстное меню
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case CM_DELETE_ID:
                // извлекаем id записи и удаляем соответствующую запись в БД
                showDialog(DEL_TASK);
                return true;
            case CM_OPEN_ID:
                // получаем из пункта контекстного меню данные по пункту списка
                startActivityForResult(AuditTask.intentActivityEdit(TaskList.this,
                        (int) ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id), 1);
                return true;
            case CM_COPY_ID:
                return true;
            case CM_STATUS_ID:
                showDialog(CHNG_STATUS);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    //Сохраняет текущее значение статуса перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("iStatus", iStatus); //Сохраняем текущий статус, соответствующий закладке
        outState.putIntegerArrayList("checkedTask",cAdapter.getCheckedTask()); //Сохраняем отметки заданий
        outState.putString("like",sLike); //Сохраняем строку поиска
    }

    //Восстанавливает значение статуса после поворота экрана и текущую закладку
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        iStatus = savedInstanceState.getInt("iStatus", 1);
        final TabLayout tt = (TabLayout) findViewById(R.id.tasktabs);
        TabLayout.Tab tab = tt.getTabAt(iStatus);
        tab.select();
        cAdapter.addCheckedTask(savedInstanceState.getIntegerArrayList("checkedTask"));
        sLike = savedInstanceState.getString("like", ""); //Сохраняем строку поиска
    }

    //ВСЕ ДЛЯ ЗАКЛАДОК:
    //Слушатель для выбора закладки по статусам
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (iStatus != tab.getPosition()) {
            iStatus = tab.getPosition();
            cAdapter.uncheckedAll();
            getSupportLoaderManager().restartLoader(1, null, this);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    //Возвращает диалоги для смены статуса и удаления
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        switch (id) {
            case CHNG_STATUS: //Смена статуса текущего задания
                adb.setTitle(R.string.ttl_status)
                        .setSingleChoiceItems(R.array.status_list, iStatus, null)
                        .setPositiveButton(R.string.btn_change, clChngStatus);
                break;
            case CHNG_STATUS_N: //Смена статуса отмеченных заданий
                adb.setTitle(R.string.ttl_status_n)
                        .setSingleChoiceItems(R.array.status_list, iStatus, null)
                        .setPositiveButton(R.string.btn_change, clChngStatusN);
                break;
            case DEL_TASK: //Удаление текущего задания
                adb.setTitle(R.string.ttl_delete)
                        .setMessage(R.string.msg_delete)
                        .setPositiveButton(R.string.btn_delete, clDelTask);
                break;
            case DEL_TASKS: //Удаления
                adb.setTitle(R.string.ttl_delete)
                        .setMessage(R.string.msg_delete_n)
                        .setPositiveButton(R.string.btn_delete, clDelTasks);
                break;
        }
        adb.setNegativeButton(R.string.btn_cancel, null);
        return adb.create();
    }

    //ОБРАБОТЧИКИ ДЛЯ ДИАЛОГОВ
    //Обработчик изменения статуса текущего задания
    Dialog.OnClickListener clChngStatus = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            int position = (((AlertDialog) dialog).getListView()).getCheckedItemPosition(); //Выбранный статус
            if (which == Dialog.BUTTON_POSITIVE && position != iStatus) cAdapter.changeStatus(savedId, position);
        }
    };

    //Обработчик изменения статуса текущего задания
    Dialog.OnClickListener clChngStatusN = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            int position = (((AlertDialog) dialog).getListView()).getCheckedItemPosition(); //Выбранный статус
            if (which == Dialog.BUTTON_POSITIVE && position != iStatus) cAdapter.changeAllStatus(position);
        }
    };

    //Обработчик удаления текущего задания
    Dialog.OnClickListener clDelTask = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) { if (which == Dialog.BUTTON_POSITIVE) cAdapter.deleteTask(savedId); }
    };

    //Обработчик удаления отмеченных заданий
    Dialog.OnClickListener clDelTasks = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) { if (which == Dialog.BUTTON_POSITIVE) cAdapter.deleteTasks(); }
    };

    //Внутренний класс Курсор адаптер для списка заданий
    class TaskAdapter extends CursorAdapter implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        //Inflater для построения View строки списка
        LayoutInflater inflater;
        Set<Integer> checkedTask;

        //Конструктор нашего адаптера
        private TaskAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            checkedTask = new ArraySet<>();
        }

        //Возвращает View для строки списка
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = inflater.inflate(R.layout.task_list_item, parent, false);
            // присваиваем чекбоксу обработчик CheckBox
            view.setLongClickable(true); //Для контекстного меню
            view.setOnClickListener(this); //Слушатель для открытия задания по клику на строке (onClick)
            ((CheckBox)view.findViewById(R.id.checkBox)).setOnCheckedChangeListener(this); //Слушатель для установки/снятия флажка на строке (onCheckedChanged)
            return view;
        }

        //Заполняет строку списка
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Integer idTask = cursor.getInt(cursor.getColumnIndex(AuditDB.ID));
            CheckBox checkBox = view.findViewById(R.id.checkBox);
            checkBox.setTag(idTask);
            checkBox.setChecked(checkedTask.contains(idTask));
            ((TextView) view.findViewById(R.id.date)).setText(cursor.getString(cursor.getColumnIndex(AuditDB.DATE)));
            ((TextView) view.findViewById(R.id.type)).setText(cursor.getString(cursor.getColumnIndex(AuditDB.TBL_TYPE)));
            ((TextView) view.findViewById(R.id.object)).setText(cursor.getString(cursor.getColumnIndex(AuditDB.TBL_OBJECT)));
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Integer id = (Integer) buttonView.getTag();
            if (isChecked) checkedTask.add(id); else checkedTask.remove(id);
        }

        //Очищает список отмеченных заданий
        public void uncheckedAll() {checkedTask.clear();}

        //Заполняет список отмеченных заданий всеми заданиями закладки
        public void checkedAll() {
            Cursor cursor = getCursor();
            if (cursor.moveToFirst()) {
                do {
                    checkedTask.add(cursor.getInt(cursor.getColumnIndex(AuditDB.ID)));
                } while (cursor.moveToNext());
            }
        }

        //Возвращает отмеченные задания
        public ArrayList<Integer> getCheckedTask() {
            return new ArrayList<>(checkedTask);
        }

        //Добавляет список в отмеченные задания
        public void addCheckedTask(ArrayList<Integer> arrayList) {
            checkedTask.addAll(arrayList);
        }

        //Удалаяет отмеченные задания
        final public void deleteTasks() {
            if (!checkedTask.isEmpty()) {
                for(Integer id:checkedTask) db.delTask(id);
                getSupportLoaderManager().restartLoader(1, null, TaskList.this);
            }
        }

        //Удалает задание по id
        final public void deleteTask(int id) {
            db.delTask(id);
            getSupportLoaderManager().restartLoader(1, null, TaskList.this);
        }

        //Изменяет статус у отмеченных заданий
        final public void changeAllStatus(int status) {
            if (!checkedTask.isEmpty()) {
                for(Integer id:checkedTask) db.changeStatusById(id, status);
                getSupportLoaderManager().restartLoader(1, null, TaskList.this);
            }
        }

        //Изменяет статус задания по id
        final public void changeStatus(int id, int status) {
            db.changeStatusById(id, status);
            getSupportLoaderManager().restartLoader(1, null, TaskList.this);
        }

        //Обработчик нажатия на пункт списка заданий для открытия задания с анимацией
        @Override
        public void onClick(View v) {
            final Integer id = (Integer) ((CheckBox) v.findViewById(R.id.checkBox)).getTag();
            LinearLayout line = (LinearLayout) v.findViewById(R.id.line);
            AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(TaskList.this, R.animator.background);
            set.setTarget(line);
            set.start();
            line.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(AuditTask.intentActivityEdit(TaskList.this, id), 1);
                }
            }, 1000L);
        }
    }

    //Загрузчик для списка
    @Override
    public  Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
//        pd.show();
        return new MyCursorLoader(this, db, iAuditor, iStatus, sLike);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        cAdapter.changeCursor(cursor);
//        pd.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cAdapter.changeCursor(null);
    }

    //Мой загрузчик для таблицы списка заданий
    static class MyCursorLoader extends CursorLoader {

        AuditDB db;
        int auditor, status;
        String like;

        public MyCursorLoader(Context context, AuditDB db, int auditor, int status, String like) {
            super(context);
            this.db = db;
            this.auditor = auditor;
            this.status = status;
            this.like = like;
        }

        @Override
        public Cursor loadInBackground() {
//            Cursor cursor = db.getTasksByAuditor(auditor,status);
//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            return db.getTasksByAuditor(auditor,status,like);
        }

    }

}
