package com.example.eduardf.audit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;

// Фрагмент для просмотра и редактирования (удаления элементов) справочников
public class ObjectListEdit extends Fragment {

    private static final String ARG_TABLE = "table"; //Таблица с данными
    private static final String ARG_IDS = "ids"; //Элементы справочника
    private static final String ARG_CODE = "code"; //Запрашиваемый код, возвращается в интерапт
    private AuditDB db; //База данных

    private int iCode; //Запрашиваемый код, возвращается в интерапт
    private ArrayList<Integer> mIds; //Список элементов
    private String sTable; //Таблица
    private OnObjectListEditInteractionListener mListener;

    private static final int NOT_SELECTED = -1; // идентификатор несуществующего элемента

    // пустой конструктор, все в Create
    public ObjectListEdit() {
    }

    // создает фрагмент на основе аргументов
    public static ObjectListEdit newInstance(int requested, String table, Collection<Integer> ids) {
        ObjectListEdit fragment = new ObjectListEdit();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE, table);
        args.putInt(ARG_CODE, requested);
        if (ids!=null && ids.size()>0) {
            ArrayList<Integer> arrayList = new ArrayList<Integer>(ids.size());
            for (Integer i: ids ) arrayList.add(i);
            args.putIntegerArrayList(ARG_IDS, arrayList);
        }
        fragment.setArguments(args);
        return fragment;
    }

    // принимает аргументы
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            sTable = args.getString(ARG_TABLE);
            iCode = args.getInt(ARG_CODE);
            if (args.containsKey(ARG_IDS)) mIds = args.getIntegerArrayList(ARG_IDS);
            else mIds = new ArrayList<Integer>();
        }
    }

    // создает вью фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_objectlistedit, container, false);

        Context context = view.getContext();
        // открываем подключение к БД
        db = new AuditDB(context);
        db.open();

        // Set the adapter
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            RecyclerAdapter recyclerAdapter;
            if (mIds.size()>0)
                recyclerAdapter = new RecyclerAdapter(db.getItemsPathByIds(sTable, mIds), mListener);
            else {
                Items items = new Items();
                items.add(new Items.Item(NOT_SELECTED,false, NOT_SELECTED, NOT_SELECTED, NOT_SELECTED, getString(R.string.msg_is_empty),""));
                recyclerAdapter = new RecyclerAdapter(items, null);
            }

            recyclerView.setAdapter(recyclerAdapter);
        }
        return view;
    }

    // когда фрагмент присоединяется к активности, проверяем, есть ли наша интеракшион?
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnObjectListEditInteractionListener) {
            mListener = (OnObjectListEditInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnObjectListEditInteractionListener");
        }
    }

    // когда фрагмент удаляется
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        db.close();
    }

    //Интерфейс управления списком аналитик объекта (удаление)
    public interface OnObjectListEditInteractionListener {
        void onObjectListEditInteractionListener(int requested, int id);
    }

    //Адаптер для списка
    public class RecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final Items mValues;
        private final ObjectListEdit.OnObjectListEditInteractionListener mListener;

        public RecyclerAdapter(Items items, ObjectListEdit.OnObjectListEditInteractionListener listener) {
            mValues = items;
            mListener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_objectedit, parent, false);
            return new ViewHolder(view);
        }

        // заполняет пункт данными
        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            //Текущий пункт
            holder.mItem = mValues.get(position);
            // наименование и описание
            holder.mNameView.setText(holder.mItem.name);
            holder.mDescView.setText(holder.mItem.desc);
            // только удалить
            if (holder.mItem.id != NOT_SELECTED) { //Есть что удалить? Или это служебный элемент
                holder.mDeleteView.setVisibility(View.VISIBLE);
                holder.mDeleteView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mValues.remove(holder.mItem); // удаляем пукнт из списка
                        notifyItemRemoved(holder.getAdapterPosition());
                        if (null != mListener) mListener.onObjectListEditInteractionListener(iCode, holder.mItem.id); // вызываем интеракшин с id удаленного пункта
                        if (mValues.isEmpty()) { //если удалены все пункты списка, то добавляем служебный
                            mValues.add(new Items.Item(NOT_SELECTED,false, NOT_SELECTED, NOT_SELECTED, NOT_SELECTED, getString(R.string.msg_is_empty),""));
                            notifyItemInserted(0);
                        }
                    }
                });
            }
            else { //Для служебного элемента
                holder.mDeleteView.setVisibility(View.GONE);
                holder.mNameView.setTextColor(0xffd0d0d0);
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }
}
