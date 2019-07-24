package com.bit.eduardf.audit;

import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 31.01.19 11:38
 *
 */

//Хранилище для RecyclerView
public class ViewHolderInds extends RecyclerView.ViewHolder {
    public final View view; // общий контейнер
    public final CardView card; // карточка
    final LinearLayout itemLayout; //лайаут пункта для папки, числа и даты
    public final TextView name; // Наименование для папки, числа и даты
    public final ImageView folder; //Значек папки
    public final TextView date; // Значение даты
    public final TextView number; // Значение числа
    public final CheckBox checkBox; // Чекбокс для типа значения boolean
    final LinearLayout expandLayout; //лайаут от условия до комментария
    public final ImageView achived; //Цель достигнута
    public final TextView criterion; // условие
    public final TextView desc; // описание
    public final ImageButton camera; //Фотография
//    final TextInputLayout commentLayout; // контейнер пункта
    public final TextInputEditText comment; // комментарий
    public final View expand; //Область раскрыть/закрыть
    final ImageView expandImage; //Иконка раскрыть/закрыть
    public IndList.Ind item; // пункт

    ViewHolderInds(View view) {
        super(view);
        this.view = view;
        card = view.findViewById(R.id.card);
        itemLayout = view.findViewById(R.id.item_layout);
        name = view.findViewById(R.id.name);
        folder = view.findViewById(R.id.folder);
        date = view.findViewById(R.id.date);
        number = view.findViewById(R.id.number);
        checkBox = view.findViewById(R.id.checkBox);
        expandLayout = view.findViewById(R.id.expand_layout);
        achived = view.findViewById(R.id.achived);
        criterion = view.findViewById(R.id.criterion);
        desc = view.findViewById(R.id.desc);
        camera = view.findViewById(R.id.camera);
//        commentLayout = view.findViewById(R.id.comment_layout);
        comment = view.findViewById(R.id.comment);
        expand = view.findViewById(R.id.expand);
        expandImage = view.findViewById(R.id.expand_image);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
