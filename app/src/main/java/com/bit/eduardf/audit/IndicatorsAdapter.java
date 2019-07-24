package com.bit.eduardf.audit;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.text.DateFormat.getDateTimeInstance;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 31.01.19 11:38
 *
 */

/**
 * Адаптер списка показателей задания
 */
class IndicatorsAdapter extends RecyclerView.Adapter<ViewHolderInds> {

    private final static String ARG_LIST = "IndicatorsAdapter";
    private final static String ARG_ENABLED = "enabled";

    private List<com.bit.eduardf.audit.IndList.Ind> items; //Список пунктов
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;
    private CheckBox.OnCheckedChangeListener onCheckedChangeListener;
    private boolean enabled = true;

    /**
     * Конструктор
     * @param OnClickListener - обработчик нажатий на пункт для редактирования
     * @param onCheckedChangeListener - обработчик нажатия на флажок пункта
     */
    IndicatorsAdapter(View.OnClickListener OnClickListener,
                      View.OnLongClickListener onLongClickListener,
                      CheckBox.OnCheckedChangeListener onCheckedChangeListener) {
        items = new ArrayList<>();
        this.onClickListener = OnClickListener;
        this.onLongClickListener = onLongClickListener;
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    /**
     * Добавление пункта в список
     * @param item -  показатель
     */
    void addItem(IndList.Ind item) {
        items.add(item);
        notifyItemInserted(items.size());
    }

    /**
     * Чистка списка пунктов
     */
    void clear() {
        if (!items.isEmpty()) {
            int count = items.size();
            items.clear();
            notifyItemRangeRemoved(0, count);
        }
    }

    /**
     * Получить пункт
     * @param position - позиция в списке
     * @return - пункт
     */
    IndList.Ind getItem(int position) {
        IndList.Ind ind = null;
        if (position < items.size()) ind = items.get(position);
        return ind;
    }


    void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        notifyDataSetChanged();
    }

    /**
     * Сохранение списка перед поворотом
     * @param outState - среда для хранения
     */
    void onSaveInstanceState(@NonNull Bundle outState) {
        final Parcelable[] parcelables = new Parcelable[items.size()];
        int i = 0;
        for (IndList.Ind ind: items) parcelables[i++] = new ParcelableInd(ind);
        outState.putParcelableArray(ARG_LIST, parcelables);
        outState.putBoolean(ARG_LIST+ARG_ENABLED, enabled);
    }

    /**
     * Восстановление списка после поворота
     * @param savedInstanceState - среда для хранения
     */
    void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_LIST)) {
            if (!items.isEmpty()) items.clear();
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(ARG_LIST);
            if (parcelables != null)
                for (Parcelable row : parcelables) {
                    final IndList.Ind ind = ((ParcelableInd) row).ind;
                    items.add(ind);
                }
            enabled = savedInstanceState.getBoolean(ARG_LIST+ARG_ENABLED, true);
            setEnabled(enabled);
        }
    }

    /**
     * Обновление пункта с показателем
     * @param ind - показатель
     */
    void notifyItemChanged(IndList.Ind ind) {
        notifyItemChanged(items.indexOf(ind));
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
            holder.card.setOnClickListener(onClickListener); //Папки открываем
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
                    holder.checkBox.setEnabled(enabled);
                    //и устанавливать обработчик
                    if (enabled)  {
                        holder.checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
                        holder.checkBox.setTag(position);
                    }
                    else {
                        holder.checkBox.setOnCheckedChangeListener(null);
                        holder.checkBox.setTag(null);
                    }
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
                    holder.name.setEnabled(enabled);
                    holder.number.setVisibility(View.VISIBLE);
                    holder.number.setText(value);
                    if (enabled)
                        holder.number.setOnClickListener(onClickListener);
                    else
                        holder.number.setOnClickListener(null);
                    holder.number.setTag(position);
                    holder.folder.setVisibility(View.GONE);
                    holder.date.setVisibility(View.GONE);
                    holder.checkBox.setVisibility(View.GONE);
                    break;
                }
                case IS_DATE:
                    holder.itemLayout.setVisibility(View.VISIBLE);
                    holder.name.setText(holder.item.name);
                    holder.name.setEnabled(enabled);
                    holder.date.setVisibility(View.VISIBLE);
                    if (enabled)
                        holder.date.setOnClickListener(onClickListener);
                    else
                        holder.date.setOnClickListener(null);
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
//                if (holder.item.criterion == Indicators.Criteria.NOT_INVOLVED) //Не участвует
//                    holder.card.setBackgroundResource(R.color.cardview_light_background); //Белый фон
//                else if (holder.item.achieved) //Цель достигнута
//                    holder.card.setBackgroundResource(R.color.colorBackgroundGreen); //Зеленый фон
//                else
//                    holder.card.setBackgroundResource(R.color.colorBackgroundRed); //Красный фон

            holder.expand.setVisibility(View.VISIBLE);
            holder.expand.setOnClickListener(onClickListener);
            holder.expand.setTag(holder.item);
            if (!(holder.item.expand && enabled)) { //Пункт свернут
                holder.expandLayout.setVisibility(View.GONE);
                holder.expandImage.setImageResource(R.drawable.ic_black_expand_more_24px);
            }
            else { //Пункт развернут
                holder.expandLayout.setVisibility(View.VISIBLE);
                holder.expandImage.setImageResource(R.drawable.ic_black_expand_less_24px);
                // иконка с пальцем
                if (holder.item.not_involved) //Не участвует
                    holder.achived.setImageResource(0); //Без иконки
                else if (holder.item.achieved)  //Цель достигнута
                    holder.achived.setImageResource(R.drawable.ic_black_thumb_up_alt_24px); //Палец вверж
                else  //Цель не достигнута
                    holder.achived.setImageResource(R.drawable.ic_black_thumb_down_alt_24px); //Палец вниз
                //Критерий, описание, предмет, комментарий
                holder.criterion.setText(holder.item.not_involved? "Показатель не участвует в определении результатов" : holder.item.russianCriterion());
                if (holder.item.desc.isEmpty()) holder.desc.setVisibility(View.GONE);
                else {
                    holder.desc.setVisibility(View.VISIBLE);
                    holder.desc.setText(holder.item.desc);
                }
                holder.camera.setTag(holder.item);
                holder.camera.setOnClickListener(onClickListener);
                holder.camera.setOnLongClickListener(onLongClickListener);
                holder.comment.setTag(position);
                holder.comment.setOnClickListener(onClickListener);
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
//Фома2019