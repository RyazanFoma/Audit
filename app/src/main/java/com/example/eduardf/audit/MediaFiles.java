package com.example.eduardf.audit;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/*
 * *
 *  * Created by Eduard Fomin on 29.05.19 10:15
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 29.05.19 10:15
 *
 */

/**
 * Список медиафайлов задания на аудит
 */
class MediaFiles extends ArrayList<MediaFiles.MediaFile> {

    static class MediaFile {
        String name; //Наименование медиафайла с расширением
        String path; //Путь к файлу на мобильном устройстве, если loaded == true
        MediaType type; //Тип медиафайла
        String indicator_key; //Guid показателя
        String indicator_name; //Наименование показателя
        String author_key; //Guid аудитора
        String author_name; //Наименование аудитора
        Date date; //Дата создания медиафайла
        String comment; //Комментарий к медиафайлу
        boolean loaded; //Медиафайл загружен на мобильное устройство
        Act act; //Действия с медиафайлом

        MediaFile() {
            name = "";
            path = "";
            type = MediaType.PHOTO;
            indicator_key = "";
            indicator_name = "";
            author_key = "";
            author_name = "";
            date = new Date();
            comment = "";
            act = Act.NoAction;
        }
    }

    /**
     * Типы медиа файлов
     */
    enum MediaType {
        PHOTO("Картинка", "Picture"),
        VIDEO("ВидеоФайл", "VideoFile"),
        AUDIO("АудиоФайл", "AudioFile");

        private String id;
        String code; //Для возврата файла в 1С

        MediaType(String id, String code) {
            this.id = id;
            this.code = code;
        }

        @Override
        public String toString() {
            return this.id;
        }

        /**
         * Определяет значения перечисления по индентификатору
         * @param id - идентификатор
         * @return - значение перечисления или null
         */
        static public MediaType toValue(final String id) {
            switch (id) {
                case "Картинка":
                    return PHOTO;
                case "ВидеоФайл":
                    return VIDEO;
                case "АудиоФайл":
                    return AUDIO;
            }
            return null;
        }
    }

    /**
     * Действия с файлом
     */
    enum Act {
        NoAction,
        Save,
        Remove,
        RemovePostSave;

        static public Act toValue(final String id) {
            switch (id) {
                case "NoAction":
                    return NoAction;
                case "Save":
                    return Save;
                case "Remove":
                    return Remove;
                case "RemovePostSave":
                    return RemovePostSave;
            }
            return null;
        }
    }

    /**
     * Удаление медиафайле на мобильном устройстве
     * @param mediaFiles - записи о медиофайле
     */
    static void washMediaFiles(@NonNull MediaFiles mediaFiles) {
        for (MediaFiles.MediaFile mediaFile: mediaFiles) {
            if (mediaFile.loaded) {
                washMediaFile(mediaFile);
            }
        }
    }

    static void washMediaFile(@NonNull MediaFiles.MediaFile mediaFile) {
        final File file = new File(mediaFile.path);
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("This media file is lost '"+mediaFile.name+"'.");
            }
        }
    }

}
//Фома2019