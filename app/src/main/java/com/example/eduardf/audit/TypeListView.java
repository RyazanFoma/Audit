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

// Фрагмент для выбора групп объектов
public class TypeListView extends Fragment {

    private static final String ARG_TABLE = "table"; //Таблица с данными
    private static final String ARG_IDS = "ids"; //ID для списка
    private AuditDB db; //База данных

    private ArrayList<Integer> mIds; //Идентификаторы
    private String sTable; //Таблица

    private static final int NOT_SELECTED = -1; // идентификатор несуществующего элемента

    public TypeListView() {
    }

    public static TypeListView newInstance(String table, Collection<Integer> ids) {
        TypeListView fragment = new TypeListView();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE, table);
        if (ids!=null && ids.size()>0) {
            ArrayList<Integer> arrayList = new ArrayList<Integer>(ids.size());
            for (Integer i: ids ) arrayList.add(i);
            args.putIntegerArrayList(ARG_IDS, arrayList);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            sTable = args.getString(ARG_TABLE);
            if (args.containsKey(ARG_IDS)) mIds = args.getIntegerArrayList(ARG_IDS);
            else mIds = new ArrayList<Integer>();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listview, container, false);

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
                recyclerAdapter = new RecyclerAdapter(db.getItemsByIds(sTable, mIds));
            else {
                Items items = new Items();
                items.add(new Items.Item(NOT_SELECTED,false, NOT_SELECTED, NOT_SELECTED, NOT_SELECTED, getString(R.string.msg_is_empty),""));
                recyclerAdapter = new RecyclerAdapter(items);
            }
            recyclerView.setAdapter(recyclerAdapter);
        }
        return view;
    }

    // когда фрагмент удаляется
    @Override
    public void onDetach() {
        super.onDetach();
        db.close();
    }

    //Адаптел для списка
    public class RecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final Items mValues;

        public RecyclerAdapter(Items items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_itemview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            //Текущий пункт
            holder.mItem = mValues.get(position);
            // наименование и описание
            holder.mNameView.setText(holder.mItem.name);
            if (holder.mItem.id == NOT_SELECTED) holder.mNameView.setTextColor(0xffd0d0d0);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }
}
