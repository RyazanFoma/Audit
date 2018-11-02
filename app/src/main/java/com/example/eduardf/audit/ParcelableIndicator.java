package com.example.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

//Класс для упаковки строки с показателем табличной части задания на аудит
public class ParcelableIndicator implements Parcelable {

    public Tasks.Task.IndicatorRow indicatorRow;

    public ParcelableIndicator(Tasks.Task.IndicatorRow indicatorRow) {
        this.indicatorRow = indicatorRow;
    }

    private static final Parcelable.Creator<ParcelableIndicator> CREATOR = new Parcelable.Creator<ParcelableIndicator>() {

        // распаковываем объект из Parcel
        public ParcelableIndicator createFromParcel(Parcel in) {
            return new ParcelableIndicator(in);
        }

        public ParcelableIndicator[] newArray(int size) {
            return new ParcelableIndicator[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(indicatorRow.indicator);
        dest.writeString(indicatorRow.type.toString());
        dest.writeValue(indicatorRow.goal);
        dest.writeValue(indicatorRow.minimum);
        dest.writeValue(indicatorRow.maximum);
        dest.writeFloat(indicatorRow.error);
        dest.writeValue(indicatorRow.value);
        dest.writeString(indicatorRow.comment);
        dest.writeInt(indicatorRow.achived ? 1: 0);
    }

    //Извлекает объект из парсел
    private ParcelableIndicator(Parcel in) {
        indicatorRow.indicator = in.readString();
        indicatorRow.type = Indicators.Types.toValue(in.readString());
        indicatorRow.goal = in.readValue(this.getClass().getClassLoader());
        indicatorRow.minimum = in.readValue(this.getClass().getClassLoader());
        indicatorRow.maximum = in.readValue(this.getClass().getClassLoader());
        indicatorRow.error = in.readFloat();
        indicatorRow.value = in.readValue(this.getClass().getClassLoader());
        indicatorRow.comment = in.readString();
        indicatorRow.achived = in.readInt() == 1;
    }
}
//Фома2018