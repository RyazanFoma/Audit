package com.example.eduardf.audit;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.text.DateFormat.getDateTimeInstance;

/**
 * A fragment representing a items of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class IndicatorFragment extends Fragment implements
        View.OnClickListener,
        CompoundButton.OnCheckedChangeListener,
        /*EditFragment.OnEditFragmentInteractionListener,*/
        DialogIndicator.DialogInteractionListener {

    private static final String ARG_TYPE = "type";
    private static final String ARG_INDICATORS = "indicatorRows";
    private static final String ARG_SUBJECT = "subject";
    private static final String ARG_PATER = "pater";
    private static final String ARG_ARGS = "args"; //Все параметры в одном бандле

    private static final int NAME_LENGTH = 20; //Максимальное количество символов в наименованни предков

    private AuditOData oData; //1С:Предприятие
    private Bundle bArgs; //Аргументы для загрузчика
    private Map<String, Tasks.Task.IndicatorRow> mapRows; //Строки табличной части Показатели
    private ListRecyclerAdapter listRecyclerAdapter; //Адаптер для списка показателей
    private FoldersRecyclerAdapter foldersRecyclerAdapter = null; //Адаптер для папок показателей
    private Stack myStack; //Стек с предками для навигации по папкам показателей
    private OnListFragmentInteractionListener mListener;
    private boolean isGrandpaters; //Выводим отдельный список с папками 1-го уровня
    private boolean isRootPater; //Находимся в корне

    //Пустой конструктор
    public IndicatorFragment() {}

    /**
     * Создание фрагмента для редактирования списка показателей
     * @param type - guid вида аудита
     * @param indicators - список строк табличной части задания Показатели
     * @return - новый экземпляр фрагмента со списком показателей
     */
    public static IndicatorFragment newInstance(String type, List<Tasks.Task.IndicatorRow> indicators) {
        IndicatorFragment fragment = new IndicatorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_PATER, AuditOData.EMPTY_KEY);
        //Пакуем список с показателями в парселейбл-массив
        Parcelable[] parcelables = new Parcelable[indicators.size()];
        int i = 0;
        for(Tasks.Task.IndicatorRow row: indicators) parcelables[i++] = new ParcelableIndicator(row);
        args.putParcelableArray(ARG_INDICATORS, parcelables);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) { //Открываем впервые
            bArgs = getArguments();
        }
        else { //Открываем после поворота
            bArgs = savedInstanceState.getBundle(ARG_ARGS);
        }
        //Распаковываем парселейбл-массив в список показателей
        mapRows = new HashMap<>();
        for(Parcelable row: bArgs.getParcelableArray(ARG_INDICATORS))
            mapRows.put(((ParcelableIndicator)row).indicatorRow.indicator, ((ParcelableIndicator)row).indicatorRow);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //Пакуем список с показателями в парселейбл-массив
        Parcelable[] parcelables = new Parcelable[this.mapRows.size()];
        int i = 0;
        for(String key: this.mapRows.keySet()) parcelables[i++] = new ParcelableIndicator(this.mapRows.get(key));
        bArgs.putParcelableArray(ARG_INDICATORS, parcelables);
        bArgs.putString(ARG_PATER, myStack.peek().id);
        outState.putBundle(ARG_ARGS, bArgs);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_indicator_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Готовим предков начиная с корня
        myStack = new Stack(IndicatorFragment.this);
        Items.Item item = new Items.Item();
        item.id = AuditOData.EMPTY_KEY;
        item.name = getString(R.string.tab_ind);
        myStack.push(item);

        //Расчитываем кол-во колонок для Grid и создаем GridLayoutManager для рециклервью
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        isGrandpaters = (metrics.widthPixels >= getResources().getDimension(R.dimen.min_width_grandpaterns));

        //Готовим рециклервью со списком папок показателей 1го уровня
        RecyclerView foldersView = (RecyclerView) view.findViewById(R.id.grandparents);
        if (isGrandpaters) {
            foldersRecyclerAdapter = new FoldersRecyclerAdapter();
            foldersView.setAdapter(foldersRecyclerAdapter);
            foldersView.setLayoutManager(new LinearLayoutManager(view.getContext()));
            foldersRecyclerAdapter.load();
        }
        else foldersView.setVisibility(View.GONE);

        //Готовим рециклервью со списком показателей
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        listRecyclerAdapter = new ListRecyclerAdapter();
        recyclerView.setAdapter(listRecyclerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        listRecyclerAdapter.load(view);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        oData = new AuditOData(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        oData = null;
        myStack = null;
        mapRows = null;
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.card: //Открываем папки
                bArgs.putString(ARG_PATER, (String) v.getTag());
                listRecyclerAdapter.load(getView()); //Загрузим пункты родителя
                break;
            case R.id.number: {
                int position = (int) v.getTag();
                if (position < listRecyclerAdapter.getItemCount()) {
                    IndList.Ind ind = listRecyclerAdapter.items.get(position);
                    try {
                        DialogIndicator.newInstance(IndicatorFragment.this, position, ind.name, (Float) ind.value, ind.unit).
                                show(getChildFragmentManager(), DialogIndicator.TAG_NUMBER);
                    } catch (Exception e) {
                        return;
                    }
                }
                break;
            }
            case R.id.date: {
                int position = (int) v.getTag();
                if (position < listRecyclerAdapter.getItemCount()) {
                    IndList.Ind ind = listRecyclerAdapter.items.get(position);
                    DialogIndicator.newInstance(IndicatorFragment.this, position, ind.name, (Date) ind.value).
                            show(getChildFragmentManager(), DialogIndicator.TAG_DATE);
                }
                break;
            }
            case R.id.expand: { //Развертываем/свертываем пункт
                IndList.Ind ind = (IndList.Ind) v.getTag();
                ind.expand = !ind.expand;
                listRecyclerAdapter.notifyItemChanged(ind); //Обновим пункт
                break;
            }
            case R.id.comment: {
                //Открываем диалог для редактирования комментария
                int position = (int) v.getTag();
                if (position < listRecyclerAdapter.getItemCount()) {
                    IndList.Ind ind = listRecyclerAdapter.items.get(position);
                    DialogIndicator.newInstance(IndicatorFragment.this, position, ind.name, ind.comment).
                            show(getChildFragmentManager(), DialogIndicator.TAG_COMMENT);
                }
                break;
            }
            default: //Переходим на предка
                bArgs.putString(ARG_PATER, ((Items.Item) v.getTag()).id);
                listRecyclerAdapter.load(getView()); //Загрузим пункты родителя
        }
    }

    //Обработчик нажатия на чекбокс в пункте
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        listRecyclerAdapter.setValue((int) buttonView.getTag(), (Boolean) isChecked);
    }

    //Обработчик изменения значения показателя в диалоговом окне
    @Override
    public void onChangedIndicatorValue(int position, Object value) {
        listRecyclerAdapter.setValue(position, value);
    }

    //Обработчик изменения текста комментария в диалоговом окне
    @Override
    public void onChangedIndicatorComment(int position, String comment) {
        listRecyclerAdapter.setComment(position, comment);
    }

    //Интерфейс обработчика изменения значения в списке показателей
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(String id, Object value, boolean achived, String comment);
    }

    //Адаптер для папок показателей
    public class FoldersRecyclerAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

        private Items items;

        //Конструктор
        private FoldersRecyclerAdapter() {
            items = new Items();
        }

        //Загружает список папок 1го уровня показателей справочника
        private void load() {
            if (bArgs.getString(ARG_TYPE)!=null)
                new LoadGrangpaterns().execute(bArgs.getString(ARG_TYPE));
        }

        //Добавляет список папок 1го уровня показателей справочника в адаптер
        private void loadList(Items list) {
            if (!items.isEmpty())
                items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolderRefs onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reference, parent, false);
            return new ViewHolderRefs(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolderRefs holder, int position) {
            //Текущий пункт
            holder.item = items.get(position);

            // наименование и описание (нужно добавить комментарий хотя бы для видов аудита)
            holder.nameView.setText(holder.item.name);

            // Раскрашиваем карточки разными цветами
            if (myStack.contains(holder.item.id)) holder.cardView.setBackgroundResource(R.color.colorBackgroundItem);
            else holder.cardView.setBackgroundResource(R.color.cardview_light_background);

            //Нажатие на карточку - открытие папки
            holder.cardView.setOnClickListener(IndicatorFragment.this);
            holder.cardView.setTag(holder.item.id); // в теге guid Папки

            holder.deletedView.setVisibility(View.GONE);
            holder.predefinedView.setVisibility(View.GONE);
            holder.descView.setVisibility(View.GONE);;
            holder.imageView.setVisibility(View.GONE);
            holder.checkedView.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    //Адаптер для списка
    public class ListRecyclerAdapter extends RecyclerView.Adapter<ViewHolderInds> {

        private IndList items;

        //Конструктор
        private ListRecyclerAdapter() {
            items = new IndList();
        }

        //Загружает список показателей справочника
        private void load(View view) {
            if (bArgs.getString(ARG_TYPE)!=null) {
                final String pater = bArgs.getString(ARG_PATER);
                isRootPater = pater.equals(AuditOData.EMPTY_KEY);
                new LoadAncestors(view, myStack).execute(pater);
                new LoadIndicators(view).execute(bArgs.getString(ARG_TYPE), pater, bArgs.getString(ARG_SUBJECT));
            }
        }

        //Загружает список показателей (пунктов адаптера) на основе списков из справочника и из документа в адаптер
        private void loadList(Indicators indicators) {
            items.clear();
            for (Indicators.Indicator indicator: indicators) {
                IndList.Ind ind;
                if (indicator.folder) { //Папки добавляем в любом случае
                    //Если есть отдельный список с папками и папка первого уровня
                    if (isGrandpaters && isRootPater)
                        continue; //То, пропускаем папку
                    ind = new IndList(). new Ind();
                    ind.id = indicator.id;
                    ind.name = indicator.name;
                    ind.folder = indicator.folder;
//                    ind.pater = indicator.pater;
                }
                else if(mapRows.containsKey(indicator.id)) { //Элементы, если их id есть в строках документа
                    ind = new IndList(). new Ind();
                    ind.id = indicator.id;
                    ind.name = indicator.name;
                    ind.folder = indicator.folder;
                    ind.pater = indicator.pater;
                    //Реквизиты элемента справочника показателей:
                    ind.desc = indicator.desc;
                    ind.type = indicator.type;
                    ind.subject = indicator.subject;
                    ind.criterion = indicator.criterion;
                    ind.unit = indicator.unit;
                    //Реквизиты строки задания:
                    Tasks.Task.IndicatorRow row = mapRows.get(indicator.id);
                    ind.goal = row.goal;
                    ind.minimum = row.minimum;
                    ind.maximum = row.maximum;
                    ind.error = row.error;
                    ind.value = row.value;
                    ind.comment = row.comment;
                    ind.achived = row.achived;
                }
                else
                    continue;
                items.add(ind);
            }
            notifyDataSetChanged();
            if (isGrandpaters) //Если есть список папок
                foldersRecyclerAdapter.notifyDataSetChanged(); //Обновим список папок, чтобы выделить текущую
        }

        //Возвращает условие достижения цели на русском языке
        private String russianCriterion(IndList.Ind indicator) {
            String russian = "Ошибка: Неизвестный критерий или тип!!!";
            switch (indicator.type) {
                case IS_BOOLEAN:
                    switch (indicator.criterion) {
                        case EQUALLY:
                            russian = String.format("Утверждение должно иметь %s ответ.", (Boolean) indicator.goal? "положительный": "отрицательный");
                            break;
                        case NOT_EQUAL:
                            russian = String.format("Утверждение должно иметь %s ответ.", (Boolean) indicator.goal? "отрицательный": "положительный");
                            break;
                        case NOT_ACCOUNT:
                            russian = "Показатель не участвует в определении результатов";
                            break;
                    }
                    break;
                case IS_NUMERIC:
                    final String unit;
                    if (indicator.unit.isEmpty()) unit = "";
                    else unit = ", indicator.unit";
                    switch (indicator.criterion) {
                        case EQUALLY:
                        case MORE:
                        case MORE_OR_EQUAL:
                        case LESS:
                        case LESS_OR_EQUEL:
                            russian = String.format("Значение должно быть %s %s%s", indicator.criterion.toString(), indicator.goal, unit);
                            break;
                        case NOT_EQUAL:
                            russian = String.format("Значение не должно быть равно %s%s", indicator.goal, unit);
                            break;
                        case IN_RANGE:
                            russian = String.format("Значение должно быть равно от %s до %s%s", indicator.minimum, indicator.maximum, unit);
                            break;
                        case IN_ERROR:
                            russian = String.format("Значение должно быть равно %s%s, с погрешностью %s%%", indicator.goal, unit, indicator.error);
                            break;
                        case NOT_ACCOUNT:
                            russian = "Показатель не участвует в определении результатов";
                            break;
                    }
                    break;
                case IS_DATE:
                    switch (indicator.criterion) {
                        case EQUALLY:
                            russian = String.format("Дата должна быть равна %s", indicator.goal);
                            break;
                        case NOT_EQUAL:
                            russian = String.format("Дата не должна быть равна %s", indicator.goal);
                            break;
                        case MORE: case LESS:
                            russian = String.format("Дата должна быть %s %s", indicator.criterion.toString(), indicator.goal);
                            break;
                        case MORE_OR_EQUAL:
                            russian = String.format("Дата должна быть больше или равна %s", indicator.goal);
                            break;
                        case LESS_OR_EQUEL:
                            russian = String.format("Дата должна быть меньше или равна %s", indicator.goal);
                            break;
                        case IN_RANGE:
                            russian = String.format("Дата должна быть от %s до %s", indicator.minimum, indicator.maximum);
                            break;
                        case NOT_ACCOUNT:
                            russian = "Показатель не участвует в определении результатов";
                            break;
                    }
                    break;
            }
            return russian;
        }

        //Обновляет строку с показателем
        private void notifyItemChanged(IndList.Ind ind) {
            notifyItemChanged(items.indexOf(ind));
        }

        //Устанавливает значение показателя
        private void setValue(int position, Object value) {
            if (position < items.size()) {
                final IndList.Ind ind = items.get(position);
                ind.value = value;
                ind.notifyAchived();
                if (mListener!=null)
                    mListener.onListFragmentInteraction(ind.id, ind.value, ind.achived, ind.comment);
                try {
                    notifyItemChanged(position);
                }
                catch (IllegalStateException e) {
                    return;
                }
            }
        }

        //Устанавливает комментарий показателя
        private void setComment(int position, String comment) {
            if (position < items.size()) {
                IndList.Ind ind = items.get(position);
                ind.comment = comment;
                notifyItemChanged(position);
                if (mListener!=null)
                    mListener.onListFragmentInteraction(ind.id, ind.value, ind.achived, ind.comment);
            }
        }

        @NonNull
        @Override
        public ViewHolderInds onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_indicator, parent, false);
            return new ViewHolderInds(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolderInds holder, int position) {
            //Текущий пункт
            holder.item = items.get(position);
            if (holder.item.folder) { //Папка
                holder.card.setOnClickListener(IndicatorFragment.this); //Папки открываем
                holder.card.setTag(holder.item.id);
                holder.itemLayout.setVisibility(View.VISIBLE);
                holder.name.setText(holder.item.name);
                holder.folder.setVisibility(View.VISIBLE);
                holder.date.setVisibility(View.GONE);
                holder.number.setVisibility(View.GONE);
                holder.checkBox.setVisibility(View.GONE);
                holder.expandLayout.setVisibility(View.GONE);
                holder.expand.setVisibility(View.INVISIBLE);
            }
            else { //Элемент
                holder.card.setOnClickListener(null);
                holder.card.setTag(null);
                holder.folder.setVisibility(View.GONE);
                switch (holder.item.type) {
                    case IS_BOOLEAN:
                        holder.itemLayout.setVisibility(View.GONE);
                        holder.checkBox.setVisibility(View.VISIBLE);
                        holder.checkBox.setText(holder.item.name);
                        holder.checkBox.setChecked((Boolean) holder.item.value);
                        holder.checkBox.setOnCheckedChangeListener(IndicatorFragment.this);
                        holder.checkBox.setTag(position);
                        break;
                    case IS_NUMERIC: {
                        holder.itemLayout.setVisibility(View.VISIBLE);
                        final StringBuffer name = new StringBuffer();
                        name.append(holder.item.name);
                        if (!holder.item.unit.isEmpty()) {
                            name.append(", ");
                            name.append(holder.item.unit);
                        }
                        holder.name.setText(name.toString());
                        holder.number.setVisibility(View.VISIBLE);
                        holder.number.setText(holder.item.type.valueToString(holder.item.value));
                        holder.number.setOnClickListener(IndicatorFragment.this);
                        holder.number.setTag(position);
                        holder.folder.setVisibility(View.GONE);
                        holder.date.setVisibility(View.GONE);
                        holder.checkBox.setVisibility(View.GONE);
                        break;
                    }
                    case IS_DATE:
                        holder.itemLayout.setVisibility(View.VISIBLE);
                        holder.name.setText(holder.item.name);
                        holder.date.setVisibility(View.VISIBLE);
                        holder.date.setOnClickListener(IndicatorFragment.this);
                        holder.date.setTag(position);
                        if (((Date) holder.item.value).getTime() != 0)
                            holder.date.setText(getDateTimeInstance().format((Date) holder.item.value));
                        else
                            holder.date.setText("");
                        holder.folder.setVisibility(View.GONE);
                        holder.number.setVisibility(View.GONE);
                        holder.checkBox.setVisibility(View.GONE);
                        break;
                }

                //Цвет карточки
//                if (holder.item.criterion == Indicators.Criterions.NOT_ACCOUNT) //Не участвует
//                    holder.card.setBackgroundResource(R.color.cardview_light_background); //Белый фон
//                else if (holder.item.achived) //Цель достигнута
//                    holder.card.setBackgroundResource(R.color.colorBackgroundGreen); //Зеленый фон
//                else
//                    holder.card.setBackgroundResource(R.color.colorBackgroundRed); //Красный фон
                holder.expand.setVisibility(View.VISIBLE);
                holder.expand.setOnClickListener(IndicatorFragment.this);
                holder.expand.setTag(holder.item);
                if (!holder.item.expand) { //Пункт свернут
                    holder.expandLayout.setVisibility(View.GONE);
                    holder.expand.setImageResource(R.drawable.ic_black_expand_more_24px);
                }
                else { //Пункт развернут
                    holder.expandLayout.setVisibility(View.VISIBLE);
                    holder.expand.setImageResource(R.drawable.ic_black_expand_less_24px);
                    // иконка с пальцем
                    if (holder.item.criterion == Indicators.Criterions.NOT_ACCOUNT) //Не участвует
                        holder.achived.setImageResource(0); //Без иконки
                    else if (holder.item.achived)  //Цель достигнута
                        holder.achived.setImageResource(R.drawable.ic_black_thumb_up_alt_24px); //Палец вверж
                    else  //Цель не достигнута
                        holder.achived.setImageResource(R.drawable.ic_black_thumb_down_alt_24px); //Палец вниз
                    //Критерий, описание, предмет, комментарий
                    holder.criterion.setText(russianCriterion(holder.item));
                    if (holder.item.desc.isEmpty()) holder.desc.setVisibility(View.GONE);
                    else {
                        holder.desc.setVisibility(View.VISIBLE);
                        holder.desc.setText(holder.item.desc);
                    }
                    if (holder.item.subject.isEmpty()) holder.subject.setVisibility(View.GONE);
                    else {
                        holder.subject.setVisibility(View.VISIBLE);
                        holder.subject.setText(holder.item.subject);
                    }
                    holder.comment.setOnClickListener(IndicatorFragment.this);
                    holder.comment.setTag(position);
                    holder.comment.setText(holder.item.comment);
                }
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    //Класс для загрузки списка показателей в парралельном потоке
    private class LoadIndicators extends AsyncTask<String, Void, Indicators> {
        private ProgressBar progressBar;
        private LoadIndicators(View view) {
            progressBar = view.findViewById(R.id.progressBar);
        }
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }
        @Override
        protected Indicators doInBackground(String... key) {
            return oData.getIndicators(key[0], key[1], key[2]);
        }
        @Override
        protected void onPostExecute(Indicators indicators) {
            listRecyclerAdapter.loadList(indicators);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    //Класс для загрузки списка предков в парралельном потоке
    private class LoadAncestors extends AsyncTask<String, Void, Stack> {
        private Stack stack;
        private LinearLayout ancestors;
        private LoadAncestors(View view, Stack stack) {
            this.stack = stack;
            ancestors = view.findViewById(R.id.ancestors);
        }
        //Загружает в стек всех предков начиная с рождества и заканчивая текущим id
        private void loadStack(String id) {
            if (!AuditOData.EMPTY_KEY.equals(id)) {
                Items.Item item = oData.getItem(AuditOData.ENTITY_SET_INDICATOR, AuditOData.FOLDER_HIERARCHY, id);
                if(!AuditOData.EMPTY_KEY.equals(item.pater))
                    loadStack(item.pater); //Рекурсия!!!
                this.stack.push(item);
            }
        }
        @Override
        protected Stack doInBackground(String... strings) {
            this.stack.clip(0);
            loadStack(strings[0]);
            return this.stack;
        }
        @Override
        protected void onPostExecute(Stack stack) {
            this.stack.addTextView(ancestors, NAME_LENGTH);
        }
    }

    //Класс для загрузки папок показателей 1го уровня в парралельном потоке
    private class LoadGrangpaterns extends AsyncTask<String, Void, Items> {
        @Override
        protected Items doInBackground(String... strings) {
            return oData.getIndicatorFolders(strings[0]);
        }
        @Override
        protected void onPostExecute(Items items) {
            foldersRecyclerAdapter.loadList(items);
        }
    }
}
//Фома2018