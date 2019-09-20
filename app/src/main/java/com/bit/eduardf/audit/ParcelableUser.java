package com.bit.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 26.11.18 11:26
 *
 */

public class ParcelableUser implements Parcelable {

    static final String USER_ID = "id";
    static final String USER_NAME = "name";
    static final String USER_PASSWORD = "password";
    static final String USER_TYPE = "type";
    static final String USER_ORGANIZATION = "organization";
    static final String USER_OBJECT = "object";
    static final String USER_RESPONSIBLE = "responsible";

    public Map<String, Object> user = new HashMap<>();

    //Конструктор
    ParcelableUser(Map<String, Object> user) {
        this.user = user;
    }

    //Создатель экземпляров
    static final Creator<ParcelableUser> CREATOR = new Creator<ParcelableUser>() {

        // распаковываем объект из Parcel
        public ParcelableUser createFromParcel(Parcel in) {
            return new ParcelableUser(in);
        }

        public ParcelableUser[] newArray(int size) {
            return new ParcelableUser[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(user.get(USER_ID).toString());
        dest.writeString(user.get(USER_NAME).toString());
        dest.writeString(user.get(USER_PASSWORD).toString());
        dest.writeString(user.get(USER_TYPE).toString());
        dest.writeString(user.get(USER_ORGANIZATION).toString());
        dest.writeString(user.get(USER_OBJECT).toString());
        dest.writeString(user.get(USER_RESPONSIBLE).toString());
    }

    //Извлекает объект из парсел
    private ParcelableUser(Parcel in) {
        user.put(USER_ID, in.readString());
        user.put(USER_NAME, in.readString());
        user.put(USER_PASSWORD, in.readString());
        user.put(USER_TYPE, in.readString());
        user.put(USER_ORGANIZATION, in.readString());
        user.put(USER_OBJECT, in.readString());
        user.put(USER_RESPONSIBLE, in.readString());
    }
    
}
