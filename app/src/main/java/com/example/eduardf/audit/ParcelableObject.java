package com.example.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * *
 *  * Created by Eduard Fomin on 06.02.19 16:16
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 05.02.19 9:47
 *
 */

/**
 * Упаковщик объекта аудита
 */
public class ParcelableObject implements Parcelable {

    public AObject object = new AObject();

    //Конструктор
    ParcelableObject(AObject object) {
        this.object = object;
    }

    //Создатель экземпляров
    static final Creator<ParcelableObject> CREATOR = new Creator<ParcelableObject>() {

        // распаковываем объект из Parcel
        public ParcelableObject createFromParcel(Parcel in) {
            return new ParcelableObject(in);
        }

        public ParcelableObject[] newArray(int size) {
            return new ParcelableObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(object.id);
        dest.writeString(object.name);
        dest.writeString(object.objectType);
    }

    //Извлекает объект из парсел
    private ParcelableObject(Parcel in) {
        object.id = in.readString();
        object.name = in.readString();
        object.objectType = in.readString();
    }

}
//Фома2019