package com.example.eduardf.audit;

/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import java.util.Date;

public class CameraActivity extends AppCompatActivity implements
        CameraFragment.OnSavedMediaFile {

    private static OnNewMediaFile onNewMediaFile; //Обработчик создания нового медиафайла

    private static String ARG_KEY = "key";
    private static String ARG_NAME = "name";
    private static String ARG_COMMENT = "comment";

    private String mKey, mName, mComment;

    /**
     * Создание интента для старта активности
     * @param fragment - фрагмент, откуда вызывается активность
     * @param key - guid показателя
     * @param name - наименование показателя
     * @param comment - комментарий к показателю
     * @return - интент
     */
    public static Intent intentActivity(Fragment fragment, String key, String name, String comment) {
        Intent intent = new Intent(fragment.getActivity(), CameraActivity.class);
        if (fragment instanceof OnNewMediaFile) {
            onNewMediaFile = (OnNewMediaFile) fragment;
        }
        else {
            throw new RuntimeException(fragment.toString()
                    + " must implement OnNewMediaFile");
        }
        intent.putExtra(ARG_KEY, key);
        intent.putExtra(ARG_NAME, name);
        intent.putExtra(ARG_COMMENT, comment);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragment.newInstance())
                    .commit();
            final Intent intent = getIntent();
            mKey = intent.getStringExtra(ARG_KEY);
            mName = intent.getStringExtra(ARG_NAME);
            mComment = intent.getStringExtra(ARG_COMMENT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_KEY, mKey);
        outState.putString(ARG_NAME, mName);
        outState.putString(ARG_COMMENT, mComment);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mKey = savedInstanceState.getString(ARG_KEY);
        mName = savedInstanceState.getString(ARG_NAME);
        mComment = savedInstanceState.getString(ARG_COMMENT);
    }

    @Override
    public void onSavedMediaFile(String path, String name, MediaFiles.MediaType type, long lastModified) {
        if (onNewMediaFile != null) {
            final MediaFiles.MediaFile mediaFile = new MediaFiles.MediaFile();
            mediaFile.path = path;
            mediaFile.name = name;
            mediaFile.type = type;
            mediaFile.loaded = true;
            mediaFile.act = MediaFiles.Act.Save;
            mediaFile.date = new Date();
            mediaFile.date.setTime(lastModified);
            mediaFile.comment = mComment;
            mediaFile.indicator_key = mKey;
            mediaFile.indicator_name = mName;
//        mediaFile.author_key
//        mediaFile.author_name
            onNewMediaFile.onNewMediaFile(mediaFile);
        }
    }

    public interface OnNewMediaFile {
        void onNewMediaFile(MediaFiles.MediaFile mediaFile);
    }
}