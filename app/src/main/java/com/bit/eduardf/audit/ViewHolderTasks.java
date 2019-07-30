package com.bit.eduardf.audit;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 31.01.19 11:07
 *
 */

//Хранилище для RecyclerView
public class ViewHolderTasks extends RecyclerView.ViewHolder {
    final View mView; // общий контейнер
    final CardView itemView; // карточка
    final TextView dateView; // дата
    final TextView timeView; // время
    final TextView numberView; // номер
    final TextView objectView; // объект аудита
    final TextView typeView; // вид аудита
    final TextView analyticsView; // аналитика строкой
    final TextView commentView; // комментарий к заданию
    final ImageView expandImage; // иконка раскрыть/свернуть
    final View expandView; //Область для расширения/свертки пункта
    final ImageView deletedView; // иконка пометки на удаление
    final ImageView postedView; // иконка документ проведен
    final ImageView thumbView; // иконка цель аудита достигнута
    final CheckBox checkedView; // чек-бокс
    public Tasks.Task task; // пункт

    ViewHolderTasks(View view) {
        super(view);
        this.mView = view;
        itemView = view.findViewById(R.id.item);
        dateView = view.findViewById(R.id.date);
        timeView = view.findViewById(R.id.time);
        numberView = view.findViewById(R.id.number);
        typeView = view.findViewById(R.id.type);
        objectView = view.findViewById(R.id.object);
        analyticsView = view.findViewById(R.id.analytics);
        commentView = view.findViewById(R.id.comment);
        deletedView = view.findViewById(R.id.deleted);
        postedView = view.findViewById(R.id.posted);
        thumbView = view.findViewById(R.id.thumb);
        checkedView = view.findViewById(R.id.checked);
        expandImage = view.findViewById(R.id.expand_image);
        expandView = view.findViewById(R.id.expand);
    }

    @Override
    public String toString() {
        return super.toString() + " '"+typeView.getText()+"/"+objectView.getText()+"/"+analyticsView.getText()+"'";
    }
}