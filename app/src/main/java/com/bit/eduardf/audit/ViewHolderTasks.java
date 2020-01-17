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
    final View groupView; // область группы
    final TextView nameView; // имя группы
    final View elementView; // область элемента
    final TextView numberView; // номер
    final TextView dateView; // дата
    final TextView timeView; // время
    final View drawableView; //Область с иконками
    final ImageView deletedView; // пометка на удаление
    final ImageView postedView; // документ проведен
    final ImageView thumbView; // цель достигнута
    final TextView objectView; // объект аудита
    final TextView analyticsView; // аналитика строкой
    final TextView typeView; // вид аудита
    final TextView commentView; // комментарий к заданию
    final ImageView expandImage; // раскрыть/свернуть
    final CheckBox checkedView; // чек-бокс
    public Tasks.Task task; // пункт

    ViewHolderTasks(View view) {
        super(view);
        this.mView = view;
        itemView = view.findViewById(R.id.item);
        groupView = view.findViewById(R.id.group);
        nameView = view.findViewById(R.id.name);
        elementView = view.findViewById(R.id.element);
        dateView = view.findViewById(R.id.date);
        timeView = view.findViewById(R.id.time);
        numberView = view.findViewById(R.id.number);
        typeView = view.findViewById(R.id.type);
        objectView = view.findViewById(R.id.object);
        analyticsView = view.findViewById(R.id.analytics);
        commentView = view.findViewById(R.id.comment);
        drawableView = view.findViewById(R.id.drawable);
        deletedView = view.findViewById(R.id.deleted);
        postedView = view.findViewById(R.id.posted);
        thumbView = view.findViewById(R.id.thumb);
        checkedView = view.findViewById(R.id.checked);
        expandImage = view.findViewById(R.id.expand);
    }

    @Override
    public String toString() {
        return super.toString() + " '"+typeView.getText()+"/"+objectView.getText()+"/"+analyticsView.getText()+"'";
    }
}
