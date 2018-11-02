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
public class TypeListEdit extends Fragment {

    private static final String ARG_TABLE = "table"; //Таблица с данными
    private static final String ARG_IDS = "ids"; //Помеченные элементы - id группы первого уровня
    private static final String ARG_CODE = "code"; //Запрашиваемый код, возвращается в интерапт
    private AuditDB db; //База данных

    private int iCode; //Запрашиваемый код, возвращается в интерапт
    private ArrayList<Integer> mIds; //Список помеченных элементов
    private String sTable; //Таблица
    private OnTypeListEditInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TypeListEdit() {
    }

    public static TypeListEdit newInstance(int requested, String table, Collection<Integer> ids) {
        TypeListEdit fragment = new TypeListEdit();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listedit, container, false);

        Context context = view.getContext();
        // открываем подключение к БД
        db = new AuditDB(context);
        db.open();

        // Set the adapter
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            RecyclerAdapter recyclerAdapter = new RecyclerAdapter(db.getGroupsByPater(sTable,0), mListener);
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.setChecked(mIds);
        }
        return view;
    }

    // когда фрагмент присоединяется к активности
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnTypeListEditInteractionListener) {
            mListener = (OnTypeListEditInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnTypeListEditInteractionListener");
        }
    }

    // когда фрагмент удаляется
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        db.close();
    }

    //Интерфейс управления списком групп объектов в активности вид аудита
    public interface OnTypeListEditInteractionListener {
        void onTypeListEditInteraction(int requested, Items.Item item);
    }

    //Адаптел для списка
    public class RecyclerAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

        private final Items mValues;
        private final TypeListEdit.OnTypeListEditInteractionListener mListener;


        public RecyclerAdapter(Items items, TypeListEdit.OnTypeListEditInteractionListener listener) {
            mValues = items;
            mListener = listener;
        }

        //Отмечает пункты по списку
        private void setChecked(ArrayList<Integer> checked) {
            for(Items.Item item:mValues)
                item.checked = checked.contains(item.id);
        }

        @Override
        public ViewHolderRefs onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_itemedit, parent, false);
            return new ViewHolderRefs(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolderRefs holder, int position) {
            //Текущий пункт
            holder.item = mValues.get(position);
            // наименование и описание
            holder.nameView.setText(holder.item.name);
//            holder.mDescView.setText(holder.mItem.desc);
            // только чек-бокс
            holder.checkedView.setVisibility(View.VISIBLE);
            holder.checkedView.setChecked(holder.item.checked);
            holder.checkedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.item.checked = holder.checkedView.isChecked();
                    if (null != mListener) mListener.onTypeListEditInteraction(iCode, holder.item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }
}
