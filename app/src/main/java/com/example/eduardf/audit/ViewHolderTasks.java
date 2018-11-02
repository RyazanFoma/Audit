package com.example.eduardf.audit;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

//Хранилище для RecyclerView
public class ViewHolderTasks extends RecyclerView.ViewHolder {
    public final View mView; // общий контейнер
    public final LinearLayout itemView; // контейнер пункта
    public final CardView cardView; // карточка
    public final TextView dateView; // дата
    public final TextView timeView; // время
    public final TextView numberView; // номер
    public final TextView objectView; // объект аудита
    public final TextView typeView; // вид аудита
    public final TextView analyticsView; // аналитика строкой
    public final TextView commentView; // комментарий к заданию
    public final ImageView expandView; // иконка раскрыть/свернуть
    public final ImageView deletedView; // иконка пометки на удаление
    public final ImageView postedView; // иконка документ проведен
    public final ImageView thumbView; // иконка цель аудита достигнута
    public final CheckBox checkedView; // чек-бокс
    public Tasks.Task task; // пункт

    public ViewHolderTasks(View view) {
        super(view);
        this.mView = view;
        itemView = (LinearLayout) view.findViewById(R.id.item);
        cardView = (CardView) view.findViewById(R.id.card);
        dateView = (TextView) view.findViewById(R.id.date);
        timeView = (TextView) view.findViewById(R.id.time);
        numberView = (TextView) view.findViewById(R.id.number);
        typeView = (TextView) view.findViewById(R.id.type);
        objectView = (TextView) view.findViewById(R.id.object);
        analyticsView = (TextView) view.findViewById(R.id.analytics);
        commentView = (TextView) view.findViewById(R.id.comment);
        deletedView = (ImageView) view.findViewById(R.id.deleted);
        postedView = (ImageView) view.findViewById(R.id.posted);
        thumbView = (ImageView) view.findViewById(R.id.thumb);
        checkedView = (CheckBox) view.findViewById(R.id.checked);
        expandView = (ImageView) view.findViewById(R.id.expand);
    }

    @Override
    public String toString() {
        return super.toString() + " '"+typeView.getText()+"/"+objectView.getText()+"/"+analyticsView.getText()+"'";
    }
}
