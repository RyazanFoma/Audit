package com.example.eduardf.audit;

/*
 * *
 *  * Created by Eduard Fomin on 13.06.19 11:28
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 13.06.19 11:28
 *
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Все для чтения, записи и удаления медиафайлов на стороне 1С
 */
class MediaHttps {

    private static final String SEGMENT = "AuditorSharingMediaFiles";
    private static final String ARG_TASK = "GuidTaskAudit";
    private static final String ARG_INDICATOR = "GuidIndicatorsAudit";
    private static final String ARG_FILENAME = "FileName";
    private static final String ARG_TYPE = "FileType";
    private static final String ARG_AUDITOR = "GuidAudit";
    private static final String ARG_DATE = "DateCreation";
    private static final String ARG_COMMENT = "Comment"; //ггггММддЧЧммсс
    private static final String ARG_FILEPRESENT = "FilePresent";
    private static final String ARG_DELETE = "DeleteMediaFile";
    private static final String DATE_FORMAT_MEDIA = "yyyyMMddHHmmss"; //Шаблон формата даты ггггММддЧЧммсс
    private static final int BUFFER_SIZE = 8 * 1024;

    private final String urlRoot;
    private final Context context;

    /**
     * Конструктор
     * @param context контекст
     */
    MediaHttps(Context context) {
        this.context = context;
        final SharedPreferences pr = PreferenceManager.getDefaultSharedPreferences(context);
        urlRoot = pr.getString("file_path", "");
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(pr.getString("odata_user", ""),
                        pr.getString("odata_password", "").toCharArray());
            }
        });
    }

    /**
     * Создание URL-соединения для получения медиафайла с сервера 1С
     * @param task_key - guid задания
     * @param mediaFile - запись о медиофайле
     * @return - URL-соединение
     */
    private HttpsURLConnection connectionGET(@NonNull String task_key, @NonNull MediaFiles.MediaFile mediaFile) {
        HttpsURLConnection connection = null;
        try {
            final String urlString = urlRoot+"/"+SEGMENT+"?"+
                    ARG_TASK+"="+task_key+"&"+
                    ARG_INDICATOR+"="+mediaFile.indicator_key+"&"+
                    ARG_FILENAME+"="+mediaFile.name;
            connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
//            connection.setReadTimeout(60 * 1000);
//            connection.setConnectTimeout(60 * 1000);
//            int responseCode = connection.getResponseCode();
            connection.connect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("URL request '"+urlRoot+"/"+SEGMENT+"' is malformed !");
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("I/O error of URL-connection !");
        }
        return connection;
    }

    /**
     * Создание URL-соединения для отправки медиафайла на сервер 1С
     * @param task_key - guid задания
     * @param mediaFile - запись о медиофайле
     * @return - URL-соединение
     */
    private HttpsURLConnection connectionPOST(@NonNull String task_key, @NonNull MediaFiles.MediaFile mediaFile) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) (new URL(urlRoot+"/"+SEGMENT)).openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setRequestProperty(ARG_TASK, task_key);
            connection.setRequestProperty(ARG_INDICATOR, mediaFile.indicator_key);
            connection.setRequestProperty(ARG_FILENAME, mediaFile.name);
            connection.setRequestProperty(ARG_TYPE, mediaFile.type.code);
            connection.setRequestProperty(ARG_AUDITOR, mediaFile.author_key);
            connection.setRequestProperty(ARG_DATE,
                    (new SimpleDateFormat(DATE_FORMAT_MEDIA, Locale.US)).format(mediaFile.date));
            connection.setRequestProperty(ARG_COMMENT, mediaFile.comment);
//            connection.setReadTimeout(60 * 1000);
//            connection.setConnectTimeout(60 * 1000);
//            int responseCode = connection.getResponseCode();
            connection.connect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("URL request '"+urlRoot+"/"+SEGMENT+"' is malformed !");
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("I/O error of URL-connection !");
        }
        return connection;
    }

    /**
     * Создание URL-соединения для удаления медиафайла на сервере 1С
     * @param task_key - guid задания
     * @param mediaFile - запись о медиофайле
     * @return - URL-соединение
     */
    private HttpsURLConnection connectionDEL(@NonNull String task_key, @NonNull MediaFiles.MediaFile mediaFile) {
        HttpsURLConnection connection = null;
        try {
            final String urlString = urlRoot+"/"+SEGMENT+"?"+
                    ARG_TASK+"="+task_key+"&"+
                    ARG_INDICATOR+"="+mediaFile.indicator_key+"&"+
                    ARG_FILENAME+"="+mediaFile.name+"&"+
                    ARG_DELETE+"=true";
            connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
//            connection.setReadTimeout(60 * 1000);
//            connection.setConnectTimeout(60 * 1000);
//            int responseCode = connection.getResponseCode();
            connection.connect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("URL request '"+urlRoot+"/"+SEGMENT+"' is malformed !");
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("I/O error of URL-connection !");
        }
        return connection;
    }

    /**
     * Чтение медиафайла с сервера 1С
     * @param task_key - guid задания
     * @param mediaFile - запись о медиофайле
     */
    void readMediaFile(@NonNull String task_key, @NonNull MediaFiles.MediaFile mediaFile) {
        final HttpsURLConnection connection = connectionGET(task_key, mediaFile);
        final String filePresent = connection.getHeaderField(ARG_FILEPRESENT);
        if (filePresent==null || !filePresent.equals("true")) {
            throw new RuntimeException("This media file '"+mediaFile.name+"' was not found on server 1C !");
        }
        try {


            final String dirType;
            switch (mediaFile.type) {
                case VIDEO:
                    dirType = Environment.DIRECTORY_MOVIES;
                    break;
                case PHOTO: default:
                    dirType = Environment.DIRECTORY_PICTURES;
                    break;
            }
            final File file = new File(context.getExternalFilesDir(dirType), mediaFile.name);
            final InputStream inputStream = connection.getInputStream();
            final OutputStream outputStream = new FileOutputStream(file);
            final byte[] buffer = new byte[BUFFER_SIZE];
                try {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                mediaFile.path = file.getPath();
                mediaFile.loaded = true;
            }
            catch (IOException e) {
                mediaFile.loaded = !file.delete();
                e.printStackTrace();
                throw new RuntimeException("Error reading / writing file '"+mediaFile.name+"' on 1C server !");
            }
            finally {
                outputStream.flush();
                outputStream.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error of URL-connection !");
        }
        finally {
//            Map<String, List<String>> map = connection.getHeaderFields();
            connection.disconnect();
        }
    }

    /**
     * Запись медиафайла на сервер 1С
     * @param task_key - guid задания
     * @param mediaFile - запись о медиофайле
     */
    private void writeMediaFile(@NonNull String task_key, @NonNull MediaFiles.MediaFile mediaFile) {
        final HttpsURLConnection connection = connectionPOST(task_key, mediaFile);
        int code;
        String message;
        try {
            final InputStream inputStream = new FileInputStream(new File(mediaFile.path));
            final OutputStream outputStream = connection.getOutputStream();
            final byte[] buffer = new byte[BUFFER_SIZE];
            try {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Error reading / writing file '"+mediaFile.name+"' on 1C server !");
            }
//            finally {
//                outputStream.flush();
//                outputStream.close();
//                inputStream.close();
//            }
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error of URL-connection !");
        }
        finally {
            Map<String, List<String>> map = connection.getHeaderFields();
            final List<String> list = map.get("error");
            if (list != null) {
                final String error = list.get(0);
                if (error != null && error.equals("true")) {
                    new RuntimeException("Error writing media file");
                }
            }
//            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
            connection.disconnect();
        }
    }

    /**
     * Удаление медиафала на сервере
     * @param task_key - guid задания
     * @param mediaFile - запись о медиофайле
     */
    private void deleteMediaFile(@NonNull String task_key, @NonNull MediaFiles.MediaFile mediaFile) {
        final HttpsURLConnection connection = connectionDEL(task_key, mediaFile);
        final String filePresent = connection.getHeaderField(ARG_FILEPRESENT);
        if (filePresent==null || !filePresent.equals("true")) {
            throw new RuntimeException("This media file '"+mediaFile.name+"' was not found on server 1C !");
        }
//            Map<String, List<String>> map = connection.getHeaderFields();
        connection.disconnect();
    }

    /**
     * Обновление медиафайлов задания на сервере
     * @param task_key - guid задания
     * @param mediaFiles - записи медиафалов
     */
    void updateMediaFiles(@NonNull String task_key, @NonNull MediaFiles mediaFiles) {
        for (MediaFiles.MediaFile mediaFile: mediaFiles) {
            if (mediaFile.loaded) {
                switch (mediaFile.act) {
                    case Save:
                        writeMediaFile(task_key, mediaFile);
                        break;
                    case Remove:
                    case RemovePostSave:
                        deleteMediaFile(task_key, mediaFile);
                        break;
                    case NoAction: default:
                        break;
                }
            }
        }
    }

}
//Фома2019