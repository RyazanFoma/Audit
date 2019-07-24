package com.bit.eduardf.audit;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 10.01.19 13:46
 *
 */

/**
 * Адаптер для списка объектов
 */
class AnalyticsAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

    private final Items items = new Items();
    private boolean enabled = true;
    private final static String ARG_ITEMS = "items";

    /**
     * Формирование списка guid по списку пунктов
     * @return - список guid
     */
    ArrayList<String> getItems() {
        ArrayList<String> ids = new ArrayList<>();
        for (Items.Item item: items) ids.add(item.id);
        return ids;
    }

    /**
     * Добавить пункт в список
     * @param item - пукнт
     */
    void addItem(Items.Item item) {
        int position = getItemCount();
        items.add(item);
        notifyItemInserted(position);
    }

    /**
     * Добавить пункты в список
     * @param addition - список пунктов
     */
    void addItems(Items addition) {
        int position = getItemCount();
        items.addAll(addition);
        notifyItemRangeInserted(position, getItemCount());
    }

    /**
     * Очищает список пунктов
     */
    void clear() {
        if (!items.isEmpty()) {
            int count = items.size();
            items.clear();
            notifyItemRangeRemoved(0, count);
        }
    }

    /**
     * Включает/выключает возможность удалять элементы списка
     * @param enabled - признак включить/выключить
     */
    void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Сохранить список пунктов
     * @param outState - среда для хранения
     */
    void onSaveInstanceState(@NonNull Bundle outState) {
        items.onSaveInstanceState(outState, ARG_ITEMS);
    }

    /**
     * Восстановить список пунктов
     * @param savedInstanceState - среда для хранения
     */
    void onRestoreInstanceState(Bundle savedInstanceState) {
        items.onRestoreInstanceState(savedInstanceState, ARG_ITEMS);
    }

    /**
     * Создать холдер пункта
     * @param parent - группа
     * @param viewType - тип
     * @return - холдер пункта
     */
    @NotNull
    @Override
    public ViewHolderRefs onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_object, parent, false);
        final ViewHolderRefs holder = new ViewHolderRefs(view);
        //Кнопка удалить 'Х'
        holder.deleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enabled) {
                    items.remove(holder.item); // удаляем пукнт из списка
                    notifyItemRemoved(holder.getAdapterPosition());
                    notifyItemRangeChanged(holder.getAdapterPosition(), getItemCount());
                }
            }
        });
        return holder;
    }

    /**
     * Заполнить холдер
     * @param holder - холдер пункта
     * @param position - позиция пукнта
     */
    @Override
    public void onBindViewHolder(@NotNull final ViewHolderRefs holder, final int position) {
        holder.item = items.get(position);
        holder.nameView.setText(holder.item.name);
    }

    /**
     * Количетво пунктов в списке
     * @return - размер списока
     */
    @Override
    public int getItemCount() {
        return items.size();
    }
}
//Фома2019