package com.bit.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 26.11.18 11:26
 *
 */

public class ParcelableItem implements Parcelable {
    
    public Items.Item item = new Items.Item();

    //Конструктор
    ParcelableItem(Items.Item item) {
        this.item = item;
    }

    //Создатель экземпляров
    static final Parcelable.Creator<ParcelableItem> CREATOR = new Parcelable.Creator<ParcelableItem>() {

        // распаковываем объект из Parcel
        public ParcelableItem createFromParcel(Parcel in) {
            return new ParcelableItem(in);
        }

        public ParcelableItem[] newArray(int size) {
            return new ParcelableItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(item.id);
        dest.writeString(item.parent);
        dest.writeInt(item.folder? 1: 0);
        dest.writeString(item.name);
        dest.writeInt(item.deleted? 1: 0);
        dest.writeInt(item.predefined? 1: 0);
        dest.writeString(item.prenamed);
        dest.writeInt(item.checked ? 1: 0);
        dest.writeInt(item.selected ? 1: 0);
        dest.writeInt(item.expand ? 1: 0);
    }

    //Извлекает объект из парсел
    private ParcelableItem(Parcel in) {
        item.id = in.readString();
        item.parent = in.readString();
        item.folder = (in.readInt() == 1);
        item.name = in.readString();
        item.deleted = (in.readInt() == 1);
        item.predefined = (in.readInt() == 1);
        item.prenamed = in.readString();
        item.checked = (in.readInt() == 1);
        item.selected = (in.readInt() == 1);
        item.deleted = (in.readInt() == 1);
    }
    
}
