package com.example.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 05.02.19 9:42
 *
 */

/**
 * Упаковщик вида аудита
 */
public class ParcelableType implements Parcelable {

    public AType type = new AType();

    //Конструктор
    ParcelableType(AType type) {
        this.type = type;
    }

    //Создатель экземпляров
    static final Creator<ParcelableType> CREATOR = new Creator<ParcelableType>() {

        // распаковываем объект из Parcel
        public ParcelableType createFromParcel(Parcel in) {
            return new ParcelableType(in);
        }

        public ParcelableType[] newArray(int size) {
            return new ParcelableType[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(type.id);
        dest.writeString(type.name);
        dest.writeInt(type.fillActualValue? 1: 0);
        dest.writeInt(type.openWithIndicators? 1: 0);
        dest.writeInt(type.clearCopy? 1: 0);
        dest.writeInt(type.showSubject? 1: 0);
        dest.writeString(type.selection.id);
        dest.writeStringList(type.objectTypes);
    }

    //Извлекает объект из парсел
    private ParcelableType(Parcel in) {
        type.id = in.readString();
        type.name = in.readString();
        type.fillActualValue = (in.readInt() == 1);
        type.openWithIndicators = (in.readInt() == 1);
        type.clearCopy = (in.readInt() == 1);
        type.showSubject = (in.readInt() == 1);
        type.selection = AType.Selections.toValue(in.readString());
        in.readStringList(type.objectTypes);
    }

}
//Фома2019