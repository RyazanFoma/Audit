package com.example.eduardf.audit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//Активность для выбора группы
public class GroupTable extends Fragment implements View.OnClickListener {

    private AuditDB db; //База данных
    private String sTable; //Имя таблицы
    private int iMode; //Текущий режим
    private int iId; //Текущий выбор для одиночного режима
    private ArrayList<Integer> listId; //Текущий выбор для множественного режима
    private int iPater = 0; //Текущий родитель
    private TextView viewAncestor; //Список родителей

    private RecyclerAdapter recyclerAdapter; //Адаптер для RecyclerView
    private MyStack myStack; //Все имена предков

    private OnGroupTableInteractionListener mListener;

    static final int NO_SELECTED = -1; //Нет элемента для выбора
    //Аргументы интент
    private final static String ARG_TABLE = "table"; //Имя таблицы
    private final static String ARG_MODE = "mode"; //Режим выбора
    private final static String ARG_ID = "id"; //Текущий элемент

    static final int MODE_SINGLE_CHOICE = 1; //Режим одиночного выбора
    static final int MODE_MULTIPLE_CHOICE = 2; //Режим множественного выбора

    public GroupTable() {
    }

    // создание фрагмента для одиночного выбора
    public static GroupTable newInstance(final String table, final int id) {
        final GroupTable fragment = new GroupTable();
        final Bundle args = new Bundle();
        args.putString(ARG_TABLE, table);
        args.putInt(ARG_MODE, MODE_SINGLE_CHOICE);
        args.putInt(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    // создание фрагмента для множественного выбора
    public static GroupTable newInstance(final String table, final Set<Integer> id) {
        final GroupTable fragment = new GroupTable();
        final Bundle args = new Bundle();
        args.putString(ARG_TABLE, table);
        args.putInt(ARG_MODE, MODE_MULTIPLE_CHOICE);
        args.putIntegerArrayList(ARG_ID, new ArrayList(id));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            sTable = args.getString(ARG_TABLE);
            iMode = args.getInt(ARG_MODE);
            switch(iMode) {
                case MODE_SINGLE_CHOICE:
                    iId = args.getInt(ARG_ID, NO_SELECTED);
                    break;
                case MODE_MULTIPLE_CHOICE:
                    listId = args.getIntegerArrayList(ARG_ID);
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // создаем вью фрагмента
        View view = inflater.inflate(R.layout.group_table, container, false);

        Context context = view.getContext();
        // открываем подключение к БД
        db = new AuditDB(context);
        db.open();

        // определяем текущего родителя
        switch (iMode) {
            case MODE_SINGLE_CHOICE:
                if (iId!=NO_SELECTED) iPater = db.getPaterById(sTable, iId);
                break;
            case MODE_MULTIPLE_CHOICE:
                if (!listId.isEmpty()) iPater = db.getPaterById(sTable, listId.get(0)); //Первый в списке
                break;
        }
        if (iPater==NO_SELECTED) iPater=0;

        // создаем адаптер для списка
        recyclerAdapter = new RecyclerAdapter(db.getGroupsByPater(sTable, iPater),mListener );
        if (iMode==MODE_MULTIPLE_CHOICE) recyclerAdapter.setChecked(listId);

        // настраиваем список
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);

        //Выводим всех предков
        myStack = new MyStack(iPater);
        (viewAncestor = (TextView) view.findViewById(R.id.ancestor)).setText(myStack.toString());
        ((ImageView) view.findViewById(R.id.undo)).setOnClickListener(this);

        return view;
    }

    // обработчик прикремления фрагмента
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGroupTableInteractionListener) {
            mListener = (OnGroupTableInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    // обработчик открепления фрагмента
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        db.close();
    }

    //Интерфейс для передачи выбранного пункта из фрагмента
    public interface OnGroupTableInteractionListener {
        void OnGroupTableInteractionListener(Item item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.undo: //Список предков
                if (!myStack.empty()) { //Назад к предыдущему родителю
                    myStack.pop();
                    viewAncestor.setText(myStack.toString());
                    recyclerAdapter.loadList(db.getGroupsByPater(sTable, myStack.empty()?0:myStack.peek().id));
                    recyclerAdapter.notifyDataSetChanged();
                }
                else  //Закрываем активность без выбора
                    getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                break;
            case R.id.image: //Кнопка + || -
                Item item = (Item) v.getTag();
                if (item.child > 0) { //Проваливаемся в группу
                    myStack.push(item);
                    viewAncestor.setText(myStack.toString());
                    recyclerAdapter.loadList(db.getGroupsByPater(sTable, myStack.empty() ? 0 : item.id));
                    recyclerAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.checked: //Чек-бокс
                ((Item) v.getTag()).checked = ((CheckBox) v).isChecked();
                break;
        }
    }

    //Класс стек для вывода текущего положения в дереве
    private class MyStack {
        //Используем список для организации стека, чтобы была возможность получить toString с наименованиями пунктов
        private List<Item> list;

        //Конструктор
        private MyStack(int pater) {
            list = new ArrayList<Item> ();
            load(pater);
        }

        //Загружает в список всех предков родителя
        private void load(int id) {
            if (id>0) {
                int pater = db.getPaterById(sTable, id);
                if(pater>0) load(pater); //Рекурсия
                list.add(new Item(id, pater,0, db.getNameById(sTable, id), null));
            }
        }

        //Возвращает истину, если стек пустой
        private boolean empty() { return list.isEmpty(); }

        //Возвращает последний пункт
        private Item peek() {
            int size = list.size();
            if (size>0) return list.get(size-1);
            return null;
        }

        //Возвращает последний пункт и удаляет его из стека
        private Item pop() {
            Item item = peek();
            int size = list.size();
            if (size>0) list.remove(size-1);
            return item;
        }

        //Добавляет пункт в стек
        private void push(Item item) { list.add(item); }

        //Возвращает содержимое стека в виде строки с наименованиями пунктов с разделителем ">"
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            for(Item item:list) stringBuffer.append(item.name+"> ");
            return stringBuffer.toString();
        }
    }

    //Пункт списка
    public static class Item {
        int id;
        boolean checked;
        int pater;
        int child;
        String name;
        String desc;

        //Конструктор пункта
        public Item( int id, int pater, int child, String name, String desc) {
            this.id = id;
            this.checked = false;
            this.pater = pater;
            this.child = child;
            this.name = name;
            this.desc = desc;
        }
    }

    //Адаптер для списка
    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

        private final List<Item> mValues;
        private final OnGroupTableInteractionListener mListener;

        public RecyclerAdapter(List<Item> items, OnGroupTableInteractionListener listener) {
            mValues = items;
            mListener = listener;
        }

        //Загружает список пунктов
        private void loadList(List<Item> items) {
            mValues.clear();
            mValues.addAll(items);
        }

        //Отмечает пункты с по текущему выбору
        private void setChecked(List<Integer> ids) { for(Item item:mValues) item.checked=ids.contains(item.id); }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate((iMode==MODE_SINGLE_CHOICE)?
                            R.layout.group_table_item1:
                            R.layout.group_table_item2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            //Текущий пункт
            holder.mItem = mValues.get(position);
            // заполняем вью пункта
            holder.mImageView.setImageResource((holder.mItem.child>0)?
                    R.drawable.ic_baseline_add_circle_outline_24px:
                    R.drawable.ic_baseline_remove_circle_outline_24px);
            holder.mNameView.setText(holder.mItem.name);
            holder.mDescView.setText(holder.mItem.desc);
            holder.mCheckedView.setChecked(holder.mItem.checked);

            // обработчик выбора пункта
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) mListener.OnGroupTableInteractionListener(holder.mItem);
                }
            });

            // обработчик нажания на иконку + / -
            holder.mImageView.setOnClickListener(GroupTable.this);
            holder.mImageView.setTag(holder.mItem);

            holder.mCheckedView.setOnClickListener(GroupTable.this);
            holder.mCheckedView.setTag(holder.mItem);

            //Выделяем
            holder.mView.setBackgroundResource((holder.mItem.id == iId)?R.color.colorBackgroundAccent:0);
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
            public final CheckBox mCheckedView;
            public Item mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mImageView = (ImageView) view.findViewById(R.id.image);
                mNameView = (TextView) view.findViewById(R.id.name);
                mDescView = (TextView) view.findViewById(R.id.desc);
                mCheckedView = (CheckBox) view.findViewById(R.id.checked);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mNameView.getText() + "'";
            }
        }
    }

}
