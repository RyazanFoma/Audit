package com.example.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 26.11.18 11:26
 *
 */

/**
 * Класс для упаковки/распаковки строки Показателей задания
 */
public class ParcelableRow implements Parcelable {

    Tasks.Task.IndicatorRow row = new Tasks.Task().new IndicatorRow();

    //Конструктор
    ParcelableRow(Tasks.Task.IndicatorRow row) {
        this.row = row;
    }

    //Создатель экземпляров
    static final Parcelable.Creator<ParcelableRow> CREATOR = new Parcelable.Creator<ParcelableRow>() {

        // распаковываем объект из Parcel
        public ParcelableRow createFromParcel(Parcel in) {
            return new ParcelableRow(in);
        }

        public ParcelableRow[] newArray(int size) {
            return new ParcelableRow[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(row.indicator);
        dest.writeValue(row.goal);
        dest.writeValue(row.minimum);
        dest.writeValue(row.maximum);
        dest.writeFloat(row.error);
        dest.writeValue(row.value);
        dest.writeString(row.comment);
        dest.writeInt(row.achived ? 1: 0);
    }

    //Извлекает объект из парсел
    private ParcelableRow(Parcel in) {
        row.indicator = in.readString();
        row.goal = in.readValue(this.getClass().getClassLoader());
        row.minimum = in.readValue(this.getClass().getClassLoader());
        row.maximum = in.readValue(this.getClass().getClassLoader());
        row.error = in.readFloat();
        row.value = in.readValue(this.getClass().getClassLoader());
        row.comment = in.readString();
        row.achived = (in.readInt() == 1);
    }
}
//Фома2018