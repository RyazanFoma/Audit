package com.bit.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 30.01.19 14:55
 *
 */

public class ParcelableInd implements Parcelable {

    IndList.Ind ind = new IndList.Ind();

    //Конструктор
    ParcelableInd(IndList.Ind ind) {
        this.ind = ind;
    }

    //Создатель экземпляров
    static final Parcelable.Creator<ParcelableInd> CREATOR = new Parcelable.Creator<ParcelableInd>() {

        // распаковываем объект из Parcel
        public ParcelableInd createFromParcel(Parcel in) {
            return new ParcelableInd(in);
        }

        public ParcelableInd[] newArray(int size) {
            return new ParcelableInd[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(ind.id);
        dest.writeString(ind.name);
        dest.writeString(ind.pater);
        dest.writeInt(ind.folder? 1: 0);
        dest.writeString(ind.desc);
        dest.writeString(ind.type.id);
        dest.writeInt(ind.not_involved ? 1: 0);
        dest.writeString(ind.criterion.id);
        dest.writeString(ind.subject);
        dest.writeString(ind.unit);
        dest.writeValue(ind.goal);
        dest.writeValue(ind.minimum);
        dest.writeValue(ind.maximum);
        dest.writeFloat(ind.error);
        dest.writeValue(ind.value);
        dest.writeString(ind.comment);
        dest.writeInt(ind.achieved ? 1: 0);
        dest.writeInt(ind.expand ? 1: 0);
    }

    //Извлекает объект из парсел
    private ParcelableInd(Parcel in) {
        ind.id = in.readString();
        ind.name = in.readString();
        ind.pater = in.readString();
        ind.folder = (in.readInt() == 1);
        ind.desc = in.readString();
        ind.type = Indicators.Types.toValue(in.readString());
        ind.not_involved = (in.readInt() == 1);
        ind.criterion = Indicators.Criteria.toValue(in.readString());
        ind.subject = in.readString();
        ind.unit = in.readString();
        ind.goal = in.readValue(this.getClass().getClassLoader());
        ind.minimum = in.readValue(this.getClass().getClassLoader());
        ind.maximum = in.readValue(this.getClass().getClassLoader());
        ind.error = in.readFloat();
        ind.value = in.readValue(this.getClass().getClassLoader());
        ind.comment = in.readString();
        ind.achieved = (in.readInt() == 1);
        ind.expand = (in.readInt() == 1);
    }
}
//Фома2018