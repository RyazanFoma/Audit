package com.example.eduardf.audit;

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
 *  * Last modified 27.09.18 15:44
 *
 */

//Хранилище для RecyclerView
public class ViewHolderRefs extends RecyclerView.ViewHolder {
    public final View view; // общий контейнер
    public final LinearLayout itemView; // контейнер пункта
    public final CardView cardView; // карточка
    public final ImageView imageView; // иконка
    public final TextView nameView; // наименование
    public final ImageView deletedView; // иконка
    public final ImageView predefinedView; // иконка
    public final TextView descView; // описание
    public final ImageView forwardView; // иконка "вперед"
    public final CheckBox checkedView; // чек-бокс
    public final ImageView deleteView; //Кнопка удалить
    public Items.Item item; // пункт

    public ViewHolderRefs(View view) {
        super(view);
        this.view = view;
        itemView = (LinearLayout) view.findViewById(R.id.item);
        cardView = (CardView) view.findViewById(R.id.card);
        imageView = (ImageView) view.findViewById(R.id.image);
        nameView = (TextView) view.findViewById(R.id.name);
        descView = (TextView) view.findViewById(R.id.desc);
        forwardView =  (ImageView) view.findViewById(R.id.forward);
        checkedView = (CheckBox) view.findViewById(R.id.checked);
        deletedView = (ImageView) view.findViewById(R.id.deleted);
        predefinedView = (ImageView) view.findViewById(R.id.predefined);
        deleteView = (ImageView) view.findViewById(R.id.delete);
    }

    @Override
    public String toString() {
        return super.toString() + " '" + nameView.getText() + "'";
    }
}
