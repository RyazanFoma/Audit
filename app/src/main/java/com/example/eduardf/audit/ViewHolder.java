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
public class ViewHolder extends RecyclerView.ViewHolder {
    public final View mView; // общий контейнер
    public final LinearLayout mItemView; // контейнер пункта
    public final CardView mCardView; // карточка
    public final ImageView mImageView; // иконка
    public final TextView mNameView; // наименование
    public final TextView mDescView; // описание
    public final ImageView mForwardView; // иконка "вперед"
    public final CheckBox mCheckedView; // чек-бокс
    public final ImageButton mDeleteView; // кнопка "удалить"
    public Items.Item mItem; // пункт

    public ViewHolder(View view) {
        super(view);
        mView = view;
        mItemView = (LinearLayout) view.findViewById(R.id.item);
        mCardView = (CardView) view.findViewById(R.id.card);
        mImageView = (ImageView) view.findViewById(R.id.image);
        mNameView = (TextView) view.findViewById(R.id.name);
        mDescView = (TextView) view.findViewById(R.id.desc);
        mForwardView =  (ImageView) view.findViewById(R.id.forward);
        mCheckedView = (CheckBox) view.findViewById(R.id.checked);
        mDeleteView = (ImageButton) view.findViewById(R.id.delete);
    }

    @Override
    public String toString() {
        return super.toString() + " '" + mNameView.getText() + "'";
    }
}
