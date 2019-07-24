package com.bit.eduardf.audit;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 18.01.19 14:00
 *
 */

/**
 * Адаптер папок показателей или предметов 1го уровня
 */
class FoldersAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

    private final static String ARG_FOLDERS = "FoldersAdapter";

    private Items items;
    private final View.OnClickListener onClickListener;
    private final Stack stack;

    /**
     * Конструктор
     * @param OnClickListener - обработчик нажатия на папку
     * @param stack - стек с предками
     */
    FoldersAdapter(View.OnClickListener OnClickListener, Stack stack) {
        items = new Items();
        this.onClickListener = OnClickListener;
        this.stack = stack;
    }

//    /**
//     * Загрузка пунктов в адаптер
//     * @param list - список пунктов
//     */
//    void loadList(Items list) {
//        if (!items.isEmpty())
//            items.clear();
//        items.addAll(list);
//        notifyDataSetChanged();
//    }

    /**
     * Добавление пункта в список
     * @param item -  показатель
     */
    void addItem(Items.Item item) {
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
     * Упаковка пунктов списка
     * @param outState - среда хранения
     */
    void onSaveInstanceState(@NonNull Bundle outState) {
        items.onSaveInstanceState(outState, ARG_FOLDERS);
    }

    /**
     * Распаковка пунктов списка
     * @param savedInstanceState - среда хранения
     */
    void onRestoreInstanceState(Bundle savedInstanceState) {
        items.onRestoreInstanceState(savedInstanceState, ARG_FOLDERS);
    }

    @NonNull
    @Override
    public ViewHolderRefs onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reference, parent, false);
        final ViewHolderRefs holder = new ViewHolderRefs(view);
        //Нажатие на карточку - открытие папки
        holder.cardView.setOnClickListener(onClickListener);
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
        if (stack.contains(holder.item.id)) holder.cardView.setBackgroundResource(R.color.colorBackgroundItem);
        else holder.cardView.setBackgroundResource(R.color.cardview_light_background);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
//Фома2019