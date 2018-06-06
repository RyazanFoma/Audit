package com.example.eduardf.audit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ReferenceManager extends AppCompatActivity implements View.OnClickListener{

    private AuditDB db; //База данных
    ArrayList<Integer> ids = null; //Текущие выбранные элементы
    String sTitle = ""; //Наименование справочника
    private String sTable; //Имя таблицы
    private int iRC; //Сквозной код для идентификации результата выбора
    private int iMode; //Текущий режим
    private RecyclerAdapter recyclerAdapter; //Адаптер для RecyclerView
    private LinearLayoutManager mLayoutManager; //Менеджер для RecyclerView
    private MyStack myStack; //Все имена предков
    private static OnReferenceManagerInteractionListener mListener;

    static final int NO_SELECTED = -1; //Нет элемента для выбора

    private static final int MODE_TUNING = 0; //Режим изменения справочника
    private static final int MODE_SINGLE_CHOICE = 1; //Режим одиночного выбора
    private static final int MODE_MULTIPLE_CHOICE = 2; //Режим множественного выбора

    private static final String ARG_RC = "requestCode"; //Сквозной код для идентификации результата выбора
    private static final String ARG_TITLE = "title"; //Заголовок activity
    private static final String ARG_TABLE = "table"; //Таблица элементов
    static final String ARG_MODE = "mode"; //Режим выбора
    static final String ARG_ID = "id"; //Текущие отмеченные идентификаторы элементов
    static final String ARG_IN = "in"; //Папки для отбора
    static final String ARG_PATER = "pater"; //Текущий родитель
    static final String ARG_STATE = "state"; //Состояние RecyclerView

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
    public static Intent intentActivity(Context context, int requestCode, String table, String title,  ArrayList<Integer> ids) {
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
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_dashboard:
                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        int iPater = 0; //Текущий родитель = 0 - корень списка
        int[] in; //Папки для отбора

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_maneger);

        mLayoutManager = new LinearLayoutManager(this);

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        if (savedInstanceState==null) { //активность запускается впервые
            Intent intent = getIntent();
            iRC = intent.getIntExtra(ARG_RC, NO_SELECTED); //Сквозной код для идентификации результата выбора
            sTable = intent.getStringExtra(ARG_TABLE); //Имя таблицы с данными
            sTitle = intent.getStringExtra(ARG_TITLE); //Заголовок активности
            iMode = intent.getIntExtra(ARG_MODE, MODE_SINGLE_CHOICE); //Режим выбора. По умолчанию - одиночный выбор
            if (iMode==MODE_SINGLE_CHOICE) {
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
            iMode = savedInstanceState.getInt(ARG_MODE);
            sTitle = savedInstanceState.getString(ARG_TITLE);
            ids = savedInstanceState.getIntegerArrayList(ARG_ID);
            iPater = savedInstanceState.getInt(ARG_PATER);
        }

        setTitle(sTitle); //Заголовок активности

        recyclerAdapter = new RecyclerAdapter(db.getItemsByPater(sTable, iPater), mListener);
        if (iMode==MODE_MULTIPLE_CHOICE) recyclerAdapter.setChecked(ids);

        //Выводим всех предков
        myStack = new MyStack(this, sTitle, iPater);
        myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 10);

        // настраиваем список
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(mLayoutManager);
//        recyclerView.setHasFixedSize(true); //Так и не понял, для чего это нужно(

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    //ВСЕ ДЛЯ ПОВОРОТА ЭКРАНА:
    // перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_RC, iRC);
        outState.putString(ARG_TABLE, sTable);
        outState.putInt(ARG_MODE, iMode);
        outState.putString(ARG_TITLE, sTitle);
        outState.putIntegerArrayList(ARG_ID, ids);
        outState.putInt(ARG_PATER, myStack.peek().id);
        outState.putParcelable(ARG_STATE, mLayoutManager.onSaveInstanceState());
    }

    // после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_STATE));
    }

    //Закрывает базу при закрытии Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ids = null;
        recyclerAdapter = null;
        mLayoutManager = null;
        myStack = null;
//        mListener = null; //Хорошо бы собрать мусор, но при повороте экрана не будет работать выбор элемента(((
        // закрываем подключение при выходе
        db.close();
    }

    @Override
    public void onClick(View v) {
        Items.Item item = (Items.Item) v.getTag();
        switch (v.getId()) {
            case R.id.image: //Кнопка + || -
                if (item.folder) { //Проваливаемся в группу
                    myStack.push(item);
                    myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 10);
                    recyclerAdapter.loadList(db.getItemsByPater(sTable, item.id));
                    recyclerAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.checked: //Чек-бокс
                item.checked = ((CheckBox) v).isChecked();
                break;
            default:
                myStack.clip(item);
                myStack.addTextView((LinearLayout) findViewById(R.id.ancestors), 10);
                recyclerAdapter.loadList(db.getItemsByPater(sTable, item.id));
                recyclerAdapter.notifyDataSetChanged();
                break;
        }
    }

    //Интерфейс для передачи выбранного пункта из фрагмента
    public interface OnReferenceManagerInteractionListener {
        void OnReferenceManagerInteractionListener(Context context, int requestCode, Items.Item item);
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
            items.add(new Items.Item(0,true, NO_SELECTED, title,null));  //Корень
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
                push(new Items.Item(id, true,0, db.getNameById(sTable, id), null));
            }
        }

        private void clip(Items.Item item) {
            int index = NO_SELECTED;
            int j=0;
            // ищем позицию пункта
            for(Items.Item myItem:items.getItems()) if(myItem.equals(item)) index=j; else j++;
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

        public RecyclerAdapter(Items items, OnReferenceManagerInteractionListener listener) {
            mValues = items;
            mListener = listener;
        }

        //Загружает список пунктов
        private void loadList(Items items) {
            mValues.clear();
            mValues.addAll(items);
        }

        //Отмечает пункты с по текущему выбору
        private void setChecked(List<Integer> ids) { for(Items.Item item:mValues.getItems()) item.checked=ids.contains(item.id); }

        @Override
        public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.reference_manager_item, parent, false);
            return new RecyclerAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerAdapter.ViewHolder holder, int position) {
            //Текущий пункт
            holder.mItem = mValues.get(position);
            // наименование и описание
            holder.mNameView.setText(holder.mItem.name);
            holder.mDescView.setText(holder.mItem.desc);

            if (iMode==MODE_SINGLE_CHOICE) { // очищаем фон у вью, которые были раньше выбранным пунктом
                holder.mNameView.setBackgroundResource(0);
                holder.mDescView.setBackgroundResource(0);
            }

            if (holder.mItem.folder) { // папка
                // иконка + / -
                holder.mImageView.setImageResource(R.drawable.ic_baseline_add_circle_outline_24px);
                // Стрелка > vs чек-бокс
                holder.mForwardView.setVisibility(View.VISIBLE);
                holder.mCheckedView.setVisibility(View.GONE);
            }
            else { // файл
                // иконка + / -
                holder.mImageView.setImageResource(R.drawable.ic_baseline_remove_circle_outline_24px);
                // Стрелка > vs чек-бокс
                if (iMode==MODE_MULTIPLE_CHOICE) {
                    holder.mForwardView.setVisibility(View.GONE);
                    holder.mCheckedView.setVisibility(View.VISIBLE);
                    holder.mCheckedView.setChecked(holder.mItem.checked);
                }
                else {
                    if (ids.contains(holder.mItem.id)) { // выделяем фон у выбранного пункта
                        holder.mNameView.setBackgroundResource(R.color.colorBackgroundItem);
                        holder.mDescView.setBackgroundResource(R.color.colorBackgroundItem);
                    }
                    holder.mForwardView.setVisibility(View.VISIBLE);
                    holder.mCheckedView.setVisibility(View.GONE);
                }
            }

            // обработчик выбора пункта
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) mListener.OnReferenceManagerInteractionListener(ReferenceManager.this, iRC, holder.mItem);
                }
            });

            holder.mImageView.setOnClickListener(ReferenceManager.this);
            holder.mImageView.setTag(holder.mItem);

            holder.mCheckedView.setOnClickListener(ReferenceManager.this);
            holder.mCheckedView.setTag(holder.mItem);

            //Выделяем
            holder.mView.setBackgroundResource((holder.mItem.checked)?R.color.colorBackgroundAccent:0);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final ImageView mImageView;
            public final TextView mNameView;
            public final TextView mDescView;
            public final ImageView mForwardView;
            public final CheckBox mCheckedView;
            public Items.Item mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
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
