package com.example.eduardf.audit;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

/*
 * *
 *  * Created by Eduard Fomin on 13.06.19 14:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 13.06.19 14:42
 *
 */

/**
 * Загрузка медиафайлов с сервера 1С на мобильное устройство
 * вход: массив записей медиафайлов
 */
class LoadMedia extends AsyncTask<MediaFiles.MediaFile, MediaFiles.MediaFile, Void> {

    final private OnLoadMedia onLoadMedia;
    final private MediaHttps mediaHttps;
    final private String task;

    /**
     * Конструктор
     * @param onLoadMedia - контекст с обратным вызовом для обработки до и после загрузки
     * @param mediaHttps - https-соединение
     * @param task - guid задания на аудит
     */
    LoadMedia(@NonNull OnLoadMedia onLoadMedia, @NonNull MediaHttps mediaHttps, @NonNull String task) {
        this.onLoadMedia = onLoadMedia;
        this.mediaHttps = mediaHttps;
        this.task = task;
    }

    @Override
    protected void onPreExecute() {
        onLoadMedia.onPreLoad();
    }
    @Override
    protected Void doInBackground(MediaFiles.MediaFile... mediaFiles) {
        for (MediaFiles.MediaFile mediaFile: mediaFiles) {
            if (!mediaFile.loaded) {
                mediaHttps.readMediaFile(task, mediaFile);
                publishProgress(mediaFile);
            }
        }
        return null;
    }
    @Override
    protected void onProgressUpdate(MediaFiles.MediaFile... mediaFiles) {
        onLoadMedia.onProgressLoad(mediaFiles[0]);
    }
    @Override
    protected void onPostExecute(Void v) {
        onLoadMedia.onPostLoad();
    }

    /**
     * Обратный вызов для возврата загруженных данных
     */
    interface OnLoadMedia{
        void onPreLoad(); //перед загрузкой
        void onProgressLoad(MediaFiles.MediaFile mediaFile);
        void onPostLoad(); //после загрузки
    }
}
//Фома2019