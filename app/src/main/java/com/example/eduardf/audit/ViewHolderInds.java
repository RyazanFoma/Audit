package com.example.eduardf.audit;

import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

//Хранилище для RecyclerView
public class ViewHolderInds extends RecyclerView.ViewHolder {
    public final View view; // общий контейнер
    public final CardView card; // карточка
    public final LinearLayout itemLayout; //лайаут пункта для папки, числа и даты
    public final TextView name; // Наименование для папки, числа и даты
    public final ImageView folder; //Значек папки
    public final TextView date; // Значение даты
    public final TextView number; // Значение числа
    public final CheckBox checkBox; // Чекбокс для типа значения boolean
    public final LinearLayout expandLayout; //лайаут от условия до комментария
    public final ImageView achived; //Цель достигнута
    public final TextView criterion; // условие
    public final TextView desc; // описание
    public final TextView subject; // предмет
    public final TextInputLayout commentLayout; // контейнер пункта
    public final TextInputEditText comment; // комментарий
    public final ImageView expand; //Кнопка раскрыть/закрыть
    public IndList.Ind item; // пункт

    public ViewHolderInds(View view) {
        super(view);
        this.view = view;
        card = view.findViewById(R.id.card);
        itemLayout = view.findViewById(R.id.item_layout);
        name = view.findViewById(R.id.name);
        folder = view.findViewById(R.id.folder);
        date = view.findViewById(R.id.date);
        number = view.findViewById(R.id.number);
        checkBox = (CheckBox) view.findViewById(R.id.checkBox);
        expandLayout = (LinearLayout) view.findViewById(R.id.expand_layout);
        achived = (ImageView) view.findViewById(R.id.achived);
        criterion = (TextView) view.findViewById(R.id.criterion);
        desc = (TextView) view.findViewById(R.id.desc);
        subject = (TextView) view.findViewById(R.id.sublect);
        commentLayout = (TextInputLayout) view.findViewById(R.id.comment_layout);
        comment = (TextInputEditText) view.findViewById(R.id.comment);
        expand = (ImageView) view.findViewById(R.id.expand);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
