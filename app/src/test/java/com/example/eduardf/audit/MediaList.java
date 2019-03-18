package com.example.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashSet;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 27.03.18 11:43
 *
 */

/**
 * Created by eduardf on 02.03.2018.
 */

public class MediaList implements Parcelable {

    //Список медиафайлов
    private HashSet<MediaFile> list;

    //Конструктор
    public MediaList() {
        list = new HashSet<MediaFile>();
    }

    //Конструктор из Парсела
    public MediaList(Parcel in) {
        list = new HashSet<>();
        MediaFile[] mf = in.createTypedArray(MediaFile.CREATOR);
        for (MediaFile f: mf) list.add(f);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    //Пишет список медиафайлов в Парсел
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        MediaFile[] mf = new MediaFile[list.size()];
        int i=0;
        for (MediaFile f: list) mf[i++]=f;
        dest.writeTypedArray(mf, flags);
    }

    //Создание экземпляра объекта из парсера
    public static final Creator<MediaList> CREATOR = new Creator<MediaList>() {
        @Override
        public MediaList createFromParcel(Parcel in) {
            return new MediaList(in);
        }

        @Override
        public MediaList[] newArray(int size) {
            return new MediaList[size];
        }
    };

    //Возвращает истину, если список пустой
    public boolean isEmpty() {
        return list.isEmpty();
    }

    //Добавляет медиафайл в список
    public void add(MediaFile a) {
        list.add(a);
    }

    //Внутренний класс медиа-файла
    public static class MediaFile implements Parcelable{

       //Тип файла
        private int Type;
        public final static int PHOTO_FILE = 0; //Фотография
        public final static int VIDEO_FILE = 1; //Видео

        //Имя файла
        private String Name;

        //Возвращает имя файла
        public String Name() {
            return Name;
        }

        //Возвращает тип файла
        public int Type() {
            return Type;
        }

        //Конструктор
        public MediaFile(String Name, int Type) {
            this.Name = Name;
            this.Type = Type;
        }

        //Конструктор из парсера
        public MediaFile(Parcel in) {
            Name = in.readString();
            Type = in.readInt();
        }

        //Пишет медиафайл в Парсел
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(Name());
            dest.writeInt(Type());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        //Запаковываем в парсел   //Создание экземпляра объекта из парсела
        public static final Creator<MediaFile> CREATOR = new Creator<MediaFile>() {
            @Override
            public MediaFile createFromParcel(Parcel in) {
                return new MediaFile(in);
            }

            @Override
            public MediaFile[] newArray(int size) {
                return new MediaFile[size];
            }
        };

    }
}
