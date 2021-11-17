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
    public final View mView; // общий контейнер
    public final CardView itemView; // карточка
    public final View groupView; // область группы
    public final TextView nameView; // имя группы
    public final View elementView; // область элемента
    public final TextView numberView; // номер
    public final TextView dateView; // дата
    public final TextView timeView; // время
    public final View drawableView; //Область с иконками
    public final ImageView deletedView; // пометка на удаление
    public final ImageView postedView; // документ проведен
    public final ImageView thumbView; // цель достигнута
    public final TextView objectView; // объект аудита
    public final TextView analyticsView; // аналитика строкой
    public final TextView typeView; // вид аудита
    public final TextView commentView; // комментарий к заданию
    public final ImageView expandImage; // раскрыть/свернуть
    public final CheckBox checkedView; // чек-бокс
    public Tasks.Task task; // пункт

    public ViewHolderTasks(View view) {
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
