package com.example.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * *
 *  * Created by Eduard Fomin on 11.06.19 11:44
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 29.05.19 10:52
 *
 */

public class ParcelableMedia implements Parcelable {

    MediaFiles.MediaFile mediaFile = new MediaFiles.MediaFile();

    //Конструктор
    ParcelableMedia(MediaFiles.MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    //Создатель экземпляров
    static public final Creator<ParcelableMedia> CREATOR = new Creator<ParcelableMedia>() {

        // распаковываем объект из Parcel
        public ParcelableMedia createFromParcel(Parcel in) {
            return new ParcelableMedia(in);
        }

        public ParcelableMedia[] newArray(int size) {
            return new ParcelableMedia[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(mediaFile.name);
        dest.writeString(mediaFile.path);
        dest.writeString(mediaFile.type.toString());
        dest.writeString(mediaFile.indicator_key);
        dest.writeString(mediaFile.indicator_name);
        dest.writeString(mediaFile.author_key);
        dest.writeString(mediaFile.author_name);
        dest.writeLong(mediaFile.date.getTime());
        dest.writeString(mediaFile.comment);
        dest.writeInt(mediaFile.loaded ? 1: 0);
        dest.writeString(mediaFile.act.toString());
    }

    //Извлекает объект из парсел
    private ParcelableMedia(Parcel in) {
        mediaFile.name = in.readString();
        mediaFile.path = in.readString();
        mediaFile.type = MediaFiles.MediaType.toValue(in.readString());
        mediaFile.indicator_key = in.readString();
        mediaFile.indicator_name = in.readString();
        mediaFile.author_key = in.readString();
        mediaFile.author_name = in.readString();
        mediaFile.date.setTime(in.readLong());
        mediaFile.comment = in.readString();
        mediaFile.loaded = (in.readInt() == 1);
        mediaFile.act = MediaFiles.Act.toValue(in.readString());
    }
}
//Фома2018