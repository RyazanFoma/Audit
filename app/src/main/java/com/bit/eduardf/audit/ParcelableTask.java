package com.bit.eduardf.audit;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;

import static java.text.DateFormat.getDateTimeInstance;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 26.11.18 11:26
 *
 */

class ParcelableTask implements Parcelable {

    Tasks.Task task = new Tasks.Task();

    //Конструктор
    ParcelableTask(Tasks.Task task) {
        this.task = task;
    }

    //Создатель экземпляров
    static public final Creator<ParcelableTask> CREATOR = new Creator<ParcelableTask>() {

        // распаковываем объект из Parcel
        public ParcelableTask createFromParcel(Parcel in) {
            return new ParcelableTask(in);
        }

        public ParcelableTask[] newArray(int size) {
            return new ParcelableTask[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Пакует объект в парсел
    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(task.id);
        dest.writeString(getDateTimeInstance().format(task.date));
        dest.writeInt(task.status.number);
        dest.writeString(task.type_key);
        dest.writeString(task.type_name);
        dest.writeString(task.object_key);
        dest.writeString(task.object_name);
        dest.writeInt(task.achieved? 1: 0);
        dest.writeInt(task.deleted? 1: 0);
        dest.writeInt(task.posted? 1: 0);
        dest.writeString(task.number);
        dest.writeString(task.analytic_names);
        dest.writeString(task.comment);
        dest.writeInt(task.checked? 1: 0);
        dest.writeInt(task.expand? 1: 0);
    }

    //Извлекает объект из парсел
    private ParcelableTask(Parcel in) {
        task.id = in.readString();
        try {
            task.date = getDateTimeInstance().parse(in.readString());
        }
        catch (NullPointerException e) {
            e.printStackTrace(); //Здесь нужно сообщить о неправильной дате
            throw new RuntimeException("Error on parsing of date 'null'.");
        }
        catch (ParseException e) {
            e.printStackTrace(); //Здесь нужно сообщить о неправильной дате
            throw new RuntimeException("Error on parsing of date.");
        }
        task.status = Tasks.Task.Status.toValue(in.readInt());
        task.type_key = in.readString();
        task.type_name = in.readString();
        task.object_key = in.readString();
        task.object_name = in.readString();
        task.achieved = (in.readInt() == 1);
        task.deleted = (in.readInt() == 1);
        task.posted = (in.readInt() == 1);
        task.number = in.readString();
        task.analytic_names = in.readString();
        task.comment = in.readString();
        task.checked = (in.readInt() == 1);
        task.expand = (in.readInt() == 1);
    }
}
//Фома2018