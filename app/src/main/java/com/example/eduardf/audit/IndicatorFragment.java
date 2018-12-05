package com.example.eduardf.audit;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
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

    private AuditOData oData; //1С:Предприятие
    private Bundle bArgs; //Аргументы для загрузчика
    private Map<String, Tasks.Task.IndicatorRow> mapRows; //Строки табличной части Показатели
    private ListRecyclerAdapter listRecyclerAdapter; //Адаптер для списка показателей
    private FoldersRecyclerAdapter foldersRecyclerAdapter = null; //Адаптер для папок показателей
    private Stack myStack; //Стек с предками для навигации по папкам показателей
    private OnListFragmentInteractionListener mListener; //Ссылка на обработчик изменения значения показателя
    private OnScrollUpListener onScrollUpListener; //Ссылка на обработчик скролинга списка показателей ввниз
    private static int iScroll; //Количество скроллинга для вызова обработчика
    private boolean isGrandpaters; //Выводим отдельный список с папками 1-го уровня
    private boolean bySubject; //Выводим показатели по предметам

    //Пустой конструктор
    public IndicatorFragment() {}

    /**
     * Создание фрагмента для редактирования списка показателей
     * @param type - guid вида аудита
     * @param indicators - список строк табличной части задания Показатели
     * @param bySubject - признак вывода показателей по предметам
     * @return - новый экземпляр фрагмента со списком показателей
     */
    public static IndicatorFragment newInstance(String type, List<Tasks.Task.IndicatorRow> indicators, boolean bySubject) {
        IndicatorFragment fragment = new IndicatorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_PATER, AuditOData.EMPTY_KEY);
        //Пакуем список с показателями в парселейбл-массив
        Parcelable[] parcelables = new Parcelable[indicators.size()];
        int i = 0;
        for(Tasks.Task.IndicatorRow row: indicators) parcelables[i++] = new ParcelableRow(row);
        args.putParcelableArray(ARG_INDICATORS, parcelables);
        args.putBoolean(ARG_SUBJECT, bySubject);
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
            mapRows.put(((ParcelableRow)row).row.indicator, ((ParcelableRow)row).row);
        //По предметам
        bySubject = bArgs.getBoolean(ARG_SUBJECT);
        //Данные для вью
        myStack = new Stack() {
            @Override
            Item getItem(String id) {
                return null;
            }
        };
        foldersRecyclerAdapter = new FoldersRecyclerAdapter();
        listRecyclerAdapter = new ListRecyclerAdapter();
        if (savedInstanceState == null) {
            //Готовим предков начиная с корня
            Items.Item item = new Items.Item();
            item.id = AuditOData.EMPTY_KEY;
            item.name = bySubject ? getString(R.string.tab_sbj): getString(R.string.tab_ind);
            myStack.push(item);
        }
        else {
            foldersRecyclerAdapter.onRestoreInstanceState(savedInstanceState);
            listRecyclerAdapter.onRestoreInstanceState(savedInstanceState);
        }
    }

    /**
     * Определяет размер экрана устройства
     * @param context - текущая активность
     * @return - true, если экран равен или больше или равен большому
     */
    private static boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
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
        if (context instanceof OnScrollUpListener)
            onScrollUpListener = (OnScrollUpListener) context;

        //Выводим отдельный список папок первого уровня в зависимости от размера экрана;
        isGrandpaters = isLargeTablet(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_indicator_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Готовим рециклервью со списком папок показателей 1го уровня
        RecyclerView foldersView = (RecyclerView) view.findViewById(R.id.grandparents);
        if (isGrandpaters) {
            foldersView.setAdapter(foldersRecyclerAdapter);
            foldersView.setLayoutManager(new LinearLayoutManager(view.getContext()));
            if (savedInstanceState == null) foldersRecyclerAdapter.load();
        }
        else foldersView.setVisibility(View.GONE);

        //Готовим рециклервью со списком показателей
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setAdapter(listRecyclerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        //Обрабатываем скролинг списка вниз, чтобы сделать закладки невидимыми
        if (onScrollUpListener != null)
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (SCROLL_STATE_IDLE == newState)
                        onScrollUpListener.onScrollUpListener(iScroll<=0? View.VISIBLE: View.GONE);;
                }
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    iScroll = dy;
                }
            });
        //Грузим или восстанавливаем список предков
        if (savedInstanceState == null)
            listRecyclerAdapter.load(view);
        else
            myStack.addTextView((LinearLayout) view.findViewById(R.id.ancestors), this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        bArgs.putString(ARG_PATER, myStack.peek().id); //Обновим родителя, т.к. он мог измениться
        outState.putBundle(ARG_ARGS, bArgs);
        if (isGrandpaters) foldersRecyclerAdapter.onSaveInstanceState(outState);
        listRecyclerAdapter.onSaveInstanceState(outState);
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
                final int position = (int) v.getTag();
                if (position < listRecyclerAdapter.getItemCount()) {
                    final IndList.Ind ind = listRecyclerAdapter.items.get(position);
                    final Float value = ind.value != null? (Float) ind.value: 0f;
                    DialogIndicator.newInstance(IndicatorFragment.this, position, ind.name, value, ind.unit).
                            show(getChildFragmentManager(), DialogIndicator.TAG_NUMBER);
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
                listRecyclerAdapter.load(getView()); //Загрузим список нового родителя
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

    //Интерфейс обработчика скроллинга списка вверх
    public interface OnScrollUpListener {
        void onScrollUpListener(int visibility);
    }

    //Адаптер для папок показателей
    public class FoldersRecyclerAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

        private final static String ARG_FOLDERS = "folders";

        private Items items;

        //Конструктор
        private FoldersRecyclerAdapter() {
            items = new Items();
        }

        //Загружает список папок 1го уровня показателей справочника
        private void load() {
            if (bArgs.getString(ARG_TYPE)!=null)
                new LoadGrangpaterns(bySubject).execute(bArgs.getString(ARG_TYPE));
        }

        //Добавляет список папок 1го уровня показателей справочника в адаптер
        private void loadList(Items list) {
            if (!items.isEmpty())
                items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        //Упаковывает список
        private void onSaveInstanceState(@NonNull Bundle outState) {
            items.onSaveInstanceState(outState, ARG_FOLDERS);
        }

        //Распаковывает список
        private void onRestoreInstanceState(Bundle savedInstanceState) {
            items.onRestoreInstanceState(savedInstanceState, ARG_FOLDERS);
        }

        @NonNull
        @Override
        public ViewHolderRefs onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reference, parent, false);
            final ViewHolderRefs holder = new ViewHolderRefs(view);
            //Нажатие на карточку - открытие папки
            holder.cardView.setOnClickListener(IndicatorFragment.this);
            //Остальное скроем
            holder.deletedView.setVisibility(View.GONE);
            holder.predefinedView.setVisibility(View.GONE);
            holder.descView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.GONE);
            holder.checkedView.setVisibility(View.GONE);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolderRefs holder, int position) {
            //Текущий пункт
            holder.item = items.get(position);
            //Только наименование
            holder.nameView.setText(holder.item.name);
            holder.cardView.setTag(holder.item.id); // в теге guid Папки
            // Раскрашиваем карточки разными цветами
            if (myStack.contains(holder.item.id)) holder.cardView.setBackgroundResource(R.color.colorBackgroundItem);
            else holder.cardView.setBackgroundResource(R.color.cardview_light_background);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    //Адаптер для списка показателей
    public class ListRecyclerAdapter extends RecyclerView.Adapter<ViewHolderInds> {

        private final static String ARG_LIST = "list";
        private final static String ARG_STACK = "stack";

        private IndList items;

        //Конструктор
        private ListRecyclerAdapter() {
            items = new IndList();
        }

        //Загружает список показателей справочника
        private void load(View view) {
            if (bArgs.getString(ARG_TYPE)!=null) {
                final String pater = bArgs.getString(ARG_PATER);
                new LoadAncestors(view, myStack, bySubject).execute(pater);
                new LoadIndicators(view, bySubject, pater.equals(AuditOData.EMPTY_KEY)).execute(bArgs.getString(ARG_TYPE), pater);
            }
        }

        //Загружает список показателей (пунктов адаптера) на основе списков из справочника и из документа в адаптер
        private void loadList(Indicators indicators, boolean isRoot) {
            items.clear();
            for (Indicators.Indicator indicator: indicators) {
                IndList.Ind ind;
                if (indicator.folder) { //Папки добавляем в любом случае
                    //Если есть отдельный список с папками и папка первого уровня
                    if (isGrandpaters && isRoot)
                        continue; //То, пропускаем папку
                    ind = new IndList(). new Ind();
                    ind.id = indicator.id;
                    ind.name = indicator.name;
                    ind.folder = true;
                }
                else if(mapRows.containsKey(indicator.id)) { //Элементы, если их id есть в строках документа
                    ind = new IndList(). new Ind();
                    ind.id = indicator.id;
                    ind.name = indicator.name;
                    ind.folder = false;
                    //Реквизиты элемента справочника показателей:
                    ind.desc = indicator.desc;
                    ind.type = indicator.type;
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
                    ind.achieved = row.achived;
                }
                else
                    continue;
                items.add(ind);
            }
            notifyDataSetChanged();
            if (isGrandpaters) //Если есть список папок
                foldersRecyclerAdapter.notifyDataSetChanged(); //Обновим список папок, чтобы выделить текущую
        }

        //Упаковывает список
        private void onSaveInstanceState(@NonNull Bundle outState) {
            items.onSaveInstanceState(outState, ARG_LIST);
            myStack.onSaveInstanceState(outState, ARG_STACK);
        }

        //Распаковывает список
        private void onRestoreInstanceState(Bundle savedInstanceState) {
            items.onRestoreInstanceState(savedInstanceState, ARG_LIST);
            myStack.onRestoreInstanceState(savedInstanceState, ARG_STACK);
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
                    mListener.onListFragmentInteraction(ind.id, ind.value, ind.achieved, ind.comment);
                notifyItemChanged(position);
            }
        }

        //Устанавливает комментарий показателя
        private void setComment(int position, String comment) {
            if (position < items.size()) {
                IndList.Ind ind = items.get(position);
                ind.comment = comment;
                notifyItemChanged(position);
                if (mListener!=null)
                    mListener.onListFragmentInteraction(ind.id, ind.value, ind.achieved, ind.comment);
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
                        //Чтобы не вызвать обработчик при присвоении значения = null
                        holder.checkBox.setOnCheckedChangeListener(null);
                        //теперь можем присваивать значение
                        if (holder.item.value != null)
                            holder.checkBox.setChecked((Boolean) holder.item.value);
                        else
                            holder.checkBox.setChecked(false);
                        //и устанавливать обработчик
                        holder.checkBox.setOnCheckedChangeListener(IndicatorFragment.this);
                        holder.checkBox.setTag(position);
                        break;
                    case IS_NUMERIC: {
                        final StringBuilder name = new StringBuilder();
                        name.append(holder.item.name);
                        if (!holder.item.unit.isEmpty()) name.append(", ").append(holder.item.unit);
                        String value = "";
                        if (holder.item.value != null) {
                            final float a = (Float) holder.item.value;
                            if (a == (long) a) value = String.format(Locale.US, "%d", (long) a);
                            else value = String.format(Locale.US, "%s", a);
                        }
                        holder.itemLayout.setVisibility(View.VISIBLE);
                        holder.name.setText(name.toString());
                        holder.number.setVisibility(View.VISIBLE);
                        holder.number.setText(value);
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
                        if (!(holder.item.value == null || ((Date) holder.item.value).getTime() == 0))
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
//                else if (holder.item.achieved) //Цель достигнута
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
                    else if (holder.item.achieved)  //Цель достигнута
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

    //Класс для загрузки списка показателей
    private class LoadIndicators extends AsyncTask<String, Void, Indicators> {
        private ProgressBar progressBar;
        private boolean bySubject;
        private boolean isRoot;
        private LoadIndicators(View view, boolean bySubject, boolean isRoot) {
            progressBar = view.findViewById(R.id.progressBar);
            this.bySubject = bySubject;
            this.isRoot = isRoot;
        }
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }
        @Override
        protected Indicators doInBackground(String... key) {
            Indicators indicators;
            if (bySubject) {
                indicators = oData.getSubjects(key[0], key[1]);
                indicators.addAll(oData.getIndicators(key[0], null, key[1]));
            }
            else
                indicators = oData.getIndicators(key[0], key[1], null);
            return indicators;
        }
        @Override
        protected void onPostExecute(Indicators indicators) {
            listRecyclerAdapter.loadList(indicators, isRoot);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    //Класс для загрузки списка предков
    private class LoadAncestors extends AsyncTask<String, Void, Stack> {
        private Stack stack;
        private LinearLayout ancestors;
        private boolean bySubject;
        private LoadAncestors(View view, Stack stack, boolean bySubject) {
            this.stack = stack;
            ancestors = view.findViewById(R.id.ancestors);
            this.bySubject = bySubject;
        }

        @Override
        protected Stack doInBackground(String... strings) {
            final Stack stack_add = new Stack() {
                @Override
                Item getItem(String id) {
                    return oData.getItem(
                            bySubject? AuditOData.Set.SUBJECT:
                                    AuditOData.Set.INDICATOR, id);
                }
            };
            stack_add.loadStack(strings[0]);
            stack.clip(0);
            stack.addAll(stack_add);
            return stack;
        }
        @Override
        protected void onPostExecute(Stack stack) {
            this.stack.addTextView(ancestors, IndicatorFragment.this);
        }
    }

    //Класс для загрузки папок показателей 1го уровня
    private class LoadGrangpaterns extends AsyncTask<String, Void, Items> {

        private boolean bySubject;

        private LoadGrangpaterns(boolean bySubject) {
            this.bySubject = bySubject;
        }
        @Override
        protected Items doInBackground(String... strings) {
            return bySubject? oData.getSubjectFirstLayer(strings[0]): oData.getIndicatorFoldersFirstLayer(strings[0]);
        }
        @Override
        protected void onPostExecute(Items items) {
            foldersRecyclerAdapter.loadList(items);
        }
    }
}
//Фома2018