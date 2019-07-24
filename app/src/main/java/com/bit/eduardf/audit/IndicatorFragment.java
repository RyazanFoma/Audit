package com.bit.eduardf.audit;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 24.01.19 11:58
 *
 */

/**
 * Просмотр списка показателей и ввод значений показателей в задании
 */
public class IndicatorFragment extends Fragment implements
        View.OnClickListener,
        View.OnLongClickListener,
        CheckBox.OnCheckedChangeListener,
        DialogIndicator.DialogInteractionListener,
        LoadIndList.OnLoadIndListExecute {

    private static final String ARG_INDICATORS = "indicators";
    private static final String ARG_SUBJECT = "subject";
    private static final String ARG_STACK = "stack";

    private AuditOData oData; //1С:Предприятие

    private OnScrollUpListener onScrollUpListener; //Ссылка на обработчик скролинга списка показателей ввниз
    private static int iScroll; //Количество скроллинга для вызова обработчика

    private OnCallMediaApp onCallMediaApp; //Обработчик вызова медиа-приложения

    //Стек с предками для навигации по папкам показателей
    private Stack stack = new Stack() {
        @Override
        Item getItem(String id) {
            Item item = null;
            final IndList.Ind ind = indicators.get(id);
            if (ind != null) {
                item = new Item();
                item.id = id;
                item.name = ind.name;
                item.pater = ind.pater;
            }
            return item;
        }
    };
    private IndList indicators = new IndList(); //Дерево показателей
    private boolean bySubject; //Признак вывода показателей по предметам

    //Адаптер для списка показателей
    private IndicatorsAdapter indicatorAdapter = new IndicatorsAdapter(this, this, this);
    //Адаптер для папок показателей
    private FoldersAdapter foldersAdapter = new FoldersAdapter(this, stack);
    //Признак вывода списка с папками 1-го уровня
    private boolean isGrandpaters;

    //Пустой конструктор
    public IndicatorFragment() {}

    /**
     * Установка показателей фрагмента
     * @param indicatorRows - список показателей задания
     * @param type - guid вида аудита
     * @param bySubject - признак вывода показателей по предметам
     */
    void setIndicators(final List<Tasks.Task.IndicatorRow> indicatorRows,
                       final String type,
                       final boolean bySubject) {
        this.bySubject = bySubject;
        foldersAdapter.clear();
        indicatorAdapter.clear();

        //Готовим корень предков
        if (!stack.isEmpty()) stack.clear();
        final Items.Item item = new Items.Item();
        item.name = bySubject ? getString(R.string.tab_sbj): getString(R.string.tab_ind);
        stack.push(item);

        //Начинаем загрузку показателей
        if (!(type == null || indicatorRows == null || indicatorRows.isEmpty()))
            new LoadIndList(this, oData, type, bySubject).
                    execute(indicatorRows.toArray(new Tasks.Task.IndicatorRow[0]));
        else {
            indicatorAdapter.notifyDataSetChanged();
            foldersAdapter.notifyDataSetChanged();
            final View view = getView();
            if (view != null)
                stack.addTextView((LinearLayout) view.findViewById(R.id.ancestors),
                    IndicatorFragment.this);
        }
    }

    /**
     * Получение списка показателей для задания
     * @return - показатели
     */
    ArrayList<Tasks.Task.IndicatorRow> getIndicators() {
        final ArrayList<Tasks.Task.IndicatorRow> indicatorRows = new ArrayList<>();
        for (Map.Entry<String, IndList.Ind> entry: indicators.entrySet()) {
            final IndList.Ind ind = entry.getValue();
            if (!ind.folder) {
                final Tasks.Task.IndicatorRow row = new Tasks.Task(). new IndicatorRow();
                row.indicator = ind.id;
                row.achived = ind.achieved;
                row.comment = ind.comment;
                row.error = ind.error;
                row.goal = ind.goal;
                row.maximum = ind.maximum;
                row.minimum = ind.minimum;
                row.value = ind.value;
                indicatorRows.add(row);
            }
        }
        return indicatorRows;
    }

    /**
     * Включает/выключает фрагмент
     * @param enabled - признак включить/выключить
     */
    void setEnabled(final boolean enabled) {
        if (getView()!=null) indicatorAdapter.setEnabled(enabled);
    }

    /**
     * Вызывается при создании фрагмента
     * @param savedInstanceState - среда хранения
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) { //Открываем после поворота
            bySubject = savedInstanceState.getBoolean(ARG_SUBJECT);
            stack.onRestoreInstanceState(savedInstanceState, ARG_STACK);
            indicators.onRestoreInstanceState(savedInstanceState, ARG_INDICATORS);
            foldersAdapter.onRestoreInstanceState(savedInstanceState);
            indicatorAdapter.onRestoreInstanceState(savedInstanceState);
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

    /**
     * Вызывается при присоединении фрагмента
     * @param context - контекст активности
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        oData = new AuditOData(context);
        if (context instanceof OnScrollUpListener) onScrollUpListener = (OnScrollUpListener) context;
        if (context instanceof OnCallMediaApp) onCallMediaApp = (OnCallMediaApp) context;
        //Выводим отдельный список папок первого уровня в зависимости от размера экрана;
        isGrandpaters = isLargeTablet(context);
    }

    /**
     * Вызывается при создании вью фрагмента
     * @param inflater - выдуватель
     * @param container - контейнер для вью
     * @param savedInstanceState - среда для хранения
     * @return - вью фрагмента
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_indicator_list, container, false);
    }

    /**
     * Вызывается полсе создания вью фрагмента
     * @param view - вью фрагмента
     * @param savedInstanceState - среда хранения
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Готовим рециклервью со списком папок показателей 1го уровня
        RecyclerView foldersView = view.findViewById(R.id.grandparents);
        if (isGrandpaters) {
            foldersView.setAdapter(foldersAdapter);
            foldersView.setLayoutManager(new LinearLayoutManager(view.getContext())); //???
        }
        else foldersView.setVisibility(View.GONE);

        //Список предков
        if (savedInstanceState != null)
            stack.addTextView((LinearLayout) view.findViewById(R.id.ancestors), this);

        //Готовим рециклервью со списком показателей
        RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setAdapter(indicatorAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext())); //???
        //Обрабатываем скролинг списка вниз, чтобы сделать закладки невидимыми
        if (onScrollUpListener != null)
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (SCROLL_STATE_IDLE == newState)
                        onScrollUpListener.onScrollUpListener(iScroll<=0? View.VISIBLE: View.GONE);
                }
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    iScroll = dy;
                }
            });
    }

    /**
     * Сохранение данных перед поворотом эрана
     * @param outState - среда хранения
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        bySubject = outState.getBoolean(ARG_SUBJECT);
        indicators.onSaveInstanceState(outState, ARG_INDICATORS);
        stack.onSaveInstanceState(outState, ARG_STACK);
        indicatorAdapter.onSaveInstanceState(outState);
        foldersAdapter.onSaveInstanceState(outState);
    }

    /**
     * Вызывается перед убийством фрагмента
     */
    @Override
    public void onDetach() {
        super.onDetach();
        oData = null;
        stack = null;
        indicators = null;
    }

    /**
     * Вызывается при нажатиях на вью-элементы фрагмента
     * @param v - вью-элемент фрагмента
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.card: //Открываем папки
                update((String) v.getTag());
                break;
            case R.id.number: {
                final int position = (int) v.getTag();
                if (position < indicatorAdapter.getItemCount()) {
                    final IndList.Ind ind = indicatorAdapter.getItem(position);
                    if (ind != null) {
                        final Float value = ind.value != null? (Float) ind.value: 0f;
                        DialogIndicator.newInstance(IndicatorFragment.this, position, ind.name, value, ind.unit).
                                show(getChildFragmentManager(), DialogIndicator.TAG_NUMBER);
                    }
                }
                break;
            }
            case R.id.date: {
                int position = (int) v.getTag();
                if (position < indicatorAdapter.getItemCount()) {
                    final IndList.Ind ind = indicatorAdapter.getItem(position);
                    if (ind != null) {
                        DialogIndicator.newInstance(IndicatorFragment.this, position, ind.name, (Date) ind.value).
                                show(getChildFragmentManager(), DialogIndicator.TAG_DATE);
                    }
                }
                break;
            }
            case R.id.expand: { //Развертываем/свертываем пункт
                final IndList.Ind ind = (IndList.Ind) v.getTag();
                ind.expand = !ind.expand;
                indicatorAdapter.notifyItemChanged(ind); //Обновим пункт
                break;
            }
            case R.id.comment: {
                //Открываем диалог для редактирования комментария
                int position = (int) v.getTag();
                if (position < indicatorAdapter.getItemCount()) {
                    final IndList.Ind ind = indicatorAdapter.getItem(position);
                    if (ind != null) {
                        DialogIndicator.newInstance(IndicatorFragment.this, position, ind.name, ind.comment).
                                show(getChildFragmentManager(), DialogIndicator.TAG_COMMENT);
                    }
                }
                break;
            }
            case R.id.camera: {
                if (onCallMediaApp != null) {
                    final IndList.Ind ind = (IndList.Ind) v.getTag();
                    final MediaFiles.MediaFile mediaFile = new MediaFiles.MediaFile();
                    mediaFile.type = MediaFiles.MediaType.PHOTO;
                    mediaFile.indicator_key = ind.id;
                    mediaFile.indicator_name = ind.name;
                    mediaFile.comment = ind.comment;
                    onCallMediaApp.onCallMediaApp(mediaFile);
                }
                break;
            }
            default: //Переходим на предка
                update(((Items.Item) v.getTag()).id);
        }
    }

    /**
     * Вызывается при нажатии на чекбокс показатель
     * @param buttonView - чекбокс
     * @param isChecked - значение чекбокса
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setValue((int) buttonView.getTag(), isChecked);
    }

    /**
     * Обработчик изменения значения показателя в диалоговом окне
     * @param position - позиция показателя в списке адаптера
     * @param value - новое значение показателя
     */
    @Override
    public void onChangedIndicatorValue(int position, Object value) {
        setValue(position, value);
    }

    /**
     * Обработчик изменения текста комментария в диалоговом окне
     * @param position - позиция показателя в списке адаптера
     * @param comment - новое значение комментария
     */
    @Override
    public void onChangedIndicatorComment(int position, String comment) {
        final IndList.Ind ind = indicatorAdapter.getItem(position);
        if (ind != null) {
            ind.comment = comment;
            indicatorAdapter.notifyItemChanged(position);
        }
    }

    /**
     * Вызывается перед загрузкой дерева показателей
     */
    @Override
    public void onLoadIndListPreExecute() {
        final View view = getView();
        if (view != null) view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    /**
     * Вызывается после загрузки дерева показателей
     * Заполняет адаптеры данными
     * @param indicators - дерево показателей
     */
    @Override
    public void onLoadIndListPostExecute(IndList indicators) {

        this.indicators = indicators;

        //Заполняем адаптеры папок и показателей
        if (isGrandpaters) foldersAdapter.clear();
        indicatorAdapter.clear();
        for (Map.Entry<String, IndList.Ind> entry: indicators.entrySet()) {
            final IndList.Ind ind = entry.getValue();
            if(ind.pater == null) {
                if (isGrandpaters && ind.folder) {
                    // Добавляем в папки
                    final Items.Item folder = new Items.Item();
                    folder.id = ind.id;
                    folder.name = ind.name;
                    foldersAdapter.addItem(folder);
                }
                else {
                    // Добавляем в таблицу
                    indicatorAdapter.addItem(ind);
                }
            }
        }

        final View view = getView();
        if (view != null) {
            stack.addTextView((LinearLayout) view.findViewById(R.id.ancestors),
                    IndicatorFragment.this);
            view.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Обновить фрагмент по родителю
     * @param pater - текущий родитель
     */
    private void update(final String pater) {
        //Заполняем адаптер показателей
        indicatorAdapter.clear();
        for (Map.Entry<String, IndList.Ind> entry: indicators.entrySet()) {
            final IndList.Ind ind = entry.getValue();
            if (pater == null) {
                if (ind.pater == null && !(isGrandpaters && ind.folder)) {
                    indicatorAdapter.addItem(ind);
                }
            }
            else {
                if (pater.equals(ind.pater)) {
                    indicatorAdapter.addItem(ind);
                }
            }
        }
        //Заполняем стек с предками
        stack.clip(0);
        stack.loadStack(pater);
        final View view = getView();
        if (view != null) {
            stack.addTextView((LinearLayout) view.findViewById(R.id.ancestors),
                    IndicatorFragment.this);
        }
        //Обновим список папок для обозначения текущей папки первого уровня
        foldersAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.camera) {
            if (onCallMediaApp != null) {
                final IndList.Ind ind = (IndList.Ind) v.getTag();
                final MediaFiles.MediaFile mediaFile = new MediaFiles.MediaFile();
                mediaFile.type = MediaFiles.MediaType.VIDEO;
                mediaFile.indicator_key = ind.id;
                mediaFile.indicator_name = ind.name;
                mediaFile.comment = ind.comment;
                onCallMediaApp.onCallMediaApp(mediaFile);
                return true;
            }
        }
        return false;
    }

    /**
     * Интерфейс обработчика скроллинга списка вверх
     */
    public interface OnScrollUpListener {
        void onScrollUpListener(int visibility);
    }

    public interface OnCallMediaApp {
        void onCallMediaApp(MediaFiles.MediaFile mediaFile);
    }

    /**
     * Устанавливает новое значение показателю. Вызывается обработчиком диалога
     * @param position - позиция показателя в списке адаптера
     * @param value - новое значение
     */
    private void setValue(int position, Object value) {
        final IndList.Ind ind = indicatorAdapter.getItem(position);
        if (ind != null) {
            ind.value = value;
            ind.notifyAchived();
            indicatorAdapter.notifyItemChanged(position);
        }
    }

}
//Фома2018