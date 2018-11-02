package com.example.eduardf.audit;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

// Фрагмент для просмотра и редактирования (удаления элементов) справочников
public class ObjectListEdit extends Fragment implements ReferenceChoice.OnReferenceManagerInteractionMultipleChoice {

    private static final String ARG_REQUESTCODE = "requestcode"; //Запрашиваемый код, возвращается в интерапт
    private static final String ARG_TABLE = "table"; //Таблица с данными
    private static final String ARG_TITLE = "title"; //Наименование таблициы для заголовка активности с выбором
    private static final String ARG_IDS = "ids"; //Элементы справочника
    private static final String ARG_ARGS = "args"; //Все параметры в одном бандле

    private AuditOData oData; //1С:Предприятие
    private OnObjectListEditInteractionListener mListener; //Для интеракшен по изменению списка
    private RecyclerAdapter recyclerAdapter;
    private Bundle bArgs; //Аргументы для загрузчика

    // пустой конструктор, все в Create
    public ObjectListEdit() {
    }

    // создает фрагмент на основе аргументов
    public static ObjectListEdit newInstance(int requested, String table, String title, ArrayList<String> ids) {
        ObjectListEdit fragment = new ObjectListEdit();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE, table);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_REQUESTCODE, requested);
        if (!(ids==null || ids.isEmpty())) args.putStringArrayList(ARG_IDS, ids);
        fragment.setArguments(args);
        return fragment;
    }

    // принимает аргументы
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) { //Открываем впервые
            bArgs = getArguments();
        }
        else { //Открываем после поворота
            bArgs = savedInstanceState.getBundle(ARG_ARGS);
        }
    }

    // создает вью фрагмента
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_objectlistedit, container, false);
    }

    //заполняет View фрагмента
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ImageButton) view.findViewById(R.id.add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(ReferenceChoice.intentActivity(getActivity(), ObjectListEdit.this, bArgs.getInt(ARG_REQUESTCODE),
                        bArgs.getString(ARG_TABLE), bArgs.getString(ARG_TITLE), AuditOData.ELEMENT_HIERARCHY, null,
                        recyclerAdapter.getItems(), null));
                //Перенести иерархию, собственника и in в параметры
            }
        });
        //Расчитываем кол-во колонок для Grid и создаем GridLayoutManager для рециклервью
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //Настраиваем рециклервью
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerAdapter = new RecyclerAdapter(mListener);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),
                Math.max(1, Math.round(((float) metrics.widthPixels) /
                        getResources().getDimension(R.dimen.min_column_reference)))));
        //Запускаем загрузчик для чтения данных
        recyclerAdapter.load(view);
    }

    //Перед поворотом
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        bArgs.putStringArrayList(ARG_IDS, recyclerAdapter.getItems());
        outState.putBundle(ARG_ARGS, bArgs);
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
        oData = new AuditOData(context);
    }

    // когда фрагмент удаляется
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        oData = null;
    }

    //вызывается из активности для множественного выбора элементов справочника
    @Override
    public void onReferenceManagerInteractionMultipleChoice(int requestCode, Items items) {
        recyclerAdapter.addItems(items);
        if (null != mListener) mListener.onObjectListEditAdd(bArgs.getInt(ARG_REQUESTCODE), items);
    }

    //Интерфейс управления списком аналитик объекта (удаление)
    public interface OnObjectListEditInteractionListener {
        void onObjectListEditAdd(int requestcode, Items items);
        void onObjectListEditDelete(int requestcode, String key);
    }

    //Адаптер для списка
    public class RecyclerAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

        private final Items items;
        private final ObjectListEdit.OnObjectListEditInteractionListener mListener;

        private RecyclerAdapter(ObjectListEdit.OnObjectListEditInteractionListener listener) {
            items = new Items();
            mListener = listener;
        }

        //Загрузка пунктов
        private void load(View view) {
            List<String> ids = bArgs.getStringArrayList(ARG_IDS);
            if (!(ids == null || ids.isEmpty()))
                new loadItems(view, ids).execute(bArgs.getString(ARG_TABLE));
        }

        //Класс для выполнения загрузки пукнтов в потоке с обновлением рециклервью
        private class loadItems extends AsyncTask<String, Integer, Void> {
            List<String> list;
            View view;
            private loadItems(View view, List<String> list) {
                this.list = list;
                this.view = view;
            }
            protected void onPreExecute() {
                ((ProgressBar) view.findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
//                items.clear();
            }
            protected Void doInBackground(String... table) {
                int pos = 0;
                for(String key: list) {
                    Items.Item item = new Items.Item();
                    item.id = key;
                    item.name = oData.getName(table[0], key);
                    items.add(item);
//                    publishProgress(pos++);
                    if (isCancelled()) break;
                }
                return null;
            }
//            protected void onProgressUpdate(Integer... pos) { Существенно замедляет процесс загрузки
//                notifyItemInserted(pos[0]);
//            }
            protected void onPostExecute(Void voids) {
                bArgs.remove(ARG_IDS);
                notifyDataSetChanged();
                ((ProgressBar) view.findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
            }
        }

        //Возвращает список giud из текущего списка пунктов
        private ArrayList<String> getItems() {
            ArrayList<String> ids = new ArrayList<String>();
            for (Items.Item item: items) ids.add(item.id);
            return ids;
        }

        //Добавляет пункты в список
        private void addItems(Items addition) {
            int position = getItemCount();
            items.addAll(addition);
            notifyItemRangeInserted(position, getItemCount());
        }

        @Override
        public ViewHolderRefs onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_objectedit, parent, false);
            return new ViewHolderRefs(view);
        }

        // заполняет пункт данными
        @Override
        public void onBindViewHolder(final ViewHolderRefs holder, final int position) {
            //Текущий пункт
            holder.item = items.get(position);
            // наименование и описание
            holder.nameView.setText(holder.item.name);
//            holder.descView.setText(holder.mItem.desc);

            // только удалить
            holder.deleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) mListener.onObjectListEditDelete(bArgs.getInt(ARG_REQUESTCODE), holder.item.id); // вызываем интеракшин с id удаленного пункта
                    items.remove(holder.item); // удаляем пукнт из списка
                    notifyItemRemoved(holder.getAdapterPosition());
                    notifyItemRangeChanged(holder.getAdapterPosition(), getItemCount());
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
//Фома2018