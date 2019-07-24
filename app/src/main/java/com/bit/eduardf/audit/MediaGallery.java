package com.bit.eduardf.audit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.util.Date;
import java.util.UUID;

/*
 * *
 *  * Created by Eduard Fomin on 18.06.19 11:43
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 18.06.19 11:43
 *
 */

public class MediaGallery extends AppCompatActivity implements
        LoadMedia.OnLoadMedia,
        View.OnClickListener,
        DialogGallery.OnDialogGalleryListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager viewPager;
    private static String task;
    private static OnChangeMediaFiles onChangeMediaFiles;
    private MediaFiles.MediaFile mediaFile = null;

    private static final String ARG_TITLE = "title";
    private static final String ARG_TASK = "task";
    static final String ARG_MEDIAFILES = "MediaFiles";
    private static final String ARG_PAGER = "pager";

    private static final int RC_CAMERA = 1;

    /**
     * Новый экземпляр интента для открытия галлереи
     * @param context - контекст
     * @param title - заголовок активности
     * @param task - guid задания
     * @param mediaFiles - список записей медиафайлов
     * @return - интент активности с галлереей
     */
    public static Intent newInstance(@NonNull Context context, @NonNull String title, @NonNull String task, @NonNull MediaFiles mediaFiles) {
        if (context instanceof OnChangeMediaFiles) {
            onChangeMediaFiles = (OnChangeMediaFiles) context;
        }
        final Intent intent = new Intent(context, MediaGallery.class);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_TASK, task);
        final Parcelable[] parcelable = new Parcelable[mediaFiles.size()];
        int i = 0;
        for (MediaFiles.MediaFile mediaFile: mediaFiles) {
            parcelable[i++] = new ParcelableMedia(mediaFile);
        }
        intent.putExtra(ARG_MEDIAFILES, parcelable);
        return intent;
    }

    /**
     * Добавить фотографию - короткое нажание на FloatingActionButton
     */
    private View.OnClickListener fabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mediaFile = new MediaFiles.MediaFile();
            mediaFile.type = MediaFiles.MediaType.PHOTO;
            callCamera();
        }
    };

    /**
     * Добавить видещзапись - длинное нажание на FloatingActionButton
     */
    private View.OnLongClickListener fabOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            mediaFile = new MediaFiles.MediaFile();
            mediaFile.type = MediaFiles.MediaType.VIDEO;
            callCamera();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        setupToolBar(); //Лента инструментов
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = findViewById(R.id.container);
        viewPager.setAdapter(mSectionsPagerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(fabOnClickListener);
        fab.setOnLongClickListener(fabOnLongClickListener);

        if (savedInstanceState == null) {
            final Intent intent = getIntent();
            setTitle(intent.getStringExtra(ARG_TITLE));
            task = intent.getStringExtra(ARG_TASK);
            mSectionsPagerAdapter.addAllItems(intent.getParcelableArrayExtra(ARG_MEDIAFILES));
        }
        else {
            setTitle(savedInstanceState.getString(ARG_TITLE));
            task = savedInstanceState.getString(ARG_TASK);
            mSectionsPagerAdapter.onRestoreInstanceState(savedInstanceState);
            viewPager.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_PAGER));
        }
        new LoadMedia(this, new MediaHttps(this), task).
                execute(mSectionsPagerAdapter.toArrayMediaFile());
    }

    //Настраивает ленту
    private void setupToolBar() {
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp
        }
    }

    /**
     * Вызывается при создании меню
     * @param menu - меню
     * @return - признак успешности обработки
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
        return true;
    }

    /**
     * Вызывается перед открытием и обновлением (invalidateOptionsMenu();) меню
     * @param menu - меню
     * @return - признак успешности обработки
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final int position = viewPager.getCurrentItem();
        if (position >= 0) {
            final MediaFiles.MediaFile mediaFile = mSectionsPagerAdapter.getMediaFile(position);
            setEnableMenuItem(menu, R.id.delete, mediaFile.loaded);
            setEnableMenuItem(menu, R.id.edit,
                    mediaFile.loaded && mediaFile.act == MediaFiles.Act.Save);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Установка доступности пункта меню
     * @param menu - меню
     * @param id - идентификатор пункта
     * @param enabled - признак доступности
     */
    private void setEnableMenuItem(@NonNull Menu menu, int id, boolean enabled) {
        final MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setEnabled(enabled);
            item.getIcon().mutate().setColorFilter(enabled?
                    new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN):
                    new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN));
        }
    }

    /**
     * Вызывается при выборе пункта меню
     * @param item - пункт меню
     * @return - признак успешной обработки
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                mSectionsPagerAdapter.switchDeleteMark(viewPager);
                return true;
            case R.id.edit: {
                final int position = viewPager.getCurrentItem();
                final MediaFiles.MediaFile mediaFile = mSectionsPagerAdapter.getMediaFile(position);
                DialogGallery.newInstance(MediaGallery.this, position, mediaFile.author_name, mediaFile.comment).
                        show(getSupportFragmentManager(), DialogGallery.TAG_COMMENT);
                return true;
            }
            case R.id.settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Сохранение перед поворотом экрана
     * @param outState - среда для хранения
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, getTitle().toString());
        outState.putString(ARG_TASK, task);
        mSectionsPagerAdapter.onSaveInstanceState(outState);
        outState.putParcelable(ARG_PAGER, viewPager.onSaveInstanceState());
    }

    /**
     * Вызывается при нажатии Назад
     * @return - признак успешной обработки
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Вызывается при закрытии активности с камерой
     * @param requestCode - код запроса
     * @param resultCode - код результата
     * @param intent - интент с данными
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RC_CAMERA) {
                if (resultCode == RESULT_OK) {
                    final int position = viewPager.getCurrentItem();
                    if (position >= 0) {
                        final MediaFiles.MediaFile source = mSectionsPagerAdapter.getMediaFile(position);
                        mediaFile.loaded = true;
                        mediaFile.act = MediaFiles.Act.Save;
                        mediaFile.author_name = source.author_name;
                        mediaFile.author_key = source.author_key;
                        mediaFile.indicator_key = source.indicator_key;
                        mediaFile.indicator_name = source.indicator_name;
                        mediaFile.comment = source.comment;
                        mSectionsPagerAdapter.addItem(position, mediaFile);
                        onChangeMediaFiles.onCreateMediaFile(position, mediaFile);
                        invalidateOptionsMenu();
                    }
                }
                else {
                    MediaFiles.washMediaFile(mediaFile);
                }
                mediaFile = null;
        }
    }

    /**
     * Вызывается перед загрузкой медиафайлов
     */
    @Override
    public void onPreLoad() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    /**
     * Вызыывается после загрузки каждого медиафайла
     * @param mediaFile - запись о загруженном медиафайле
     */
    @Override
    public void onProgressLoad(MediaFiles.MediaFile mediaFile) {
        final int position = mSectionsPagerAdapter.updateItem(mediaFile);
        if (onChangeMediaFiles != null) {
            onChangeMediaFiles.onUpdateMediaFile(position, mediaFile);
        }
        mSectionsPagerAdapter.notifyDataSetChanged();
    }

    /**
     * Вызывается после загрузки всех медиафайлов
     */
    @Override
    public void onPostLoad() {
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    /**
     * Вызывается для обработки нажатий в активности
     * @param v - вью, на котором нажали
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_play:
                if (mSectionsPagerAdapter.primaryFragment != null) {
                    ((PlaceholderFragment) mSectionsPagerAdapter.primaryFragment).play();
                }
                break;
            case R.id.delete_mark:
                mSectionsPagerAdapter.switchDeleteMark(viewPager);
                break;
        }
    }

    /**
     * Вызов активности с камерой
     */
    public void callCamera() {
        final String filePrefix = UUID.randomUUID().toString();
        final String dirType, fileSuffix, action;
        switch (mediaFile.type) {
            case VIDEO:
                action = MediaStore.ACTION_VIDEO_CAPTURE;
                fileSuffix = ".mp4";
                dirType = Environment.DIRECTORY_MOVIES;
                break;
            case PHOTO: default:
                action = MediaStore.ACTION_IMAGE_CAPTURE;
                fileSuffix = ".jpg";
                dirType = Environment.DIRECTORY_PICTURES;
                break;
        }
        final Intent takePictureIntent = new Intent(action);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            final File file = new File(getExternalFilesDir(dirType), filePrefix+fileSuffix);
            Uri photoURI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoURI = FileProvider.getUriForFile(this, "com.example.eduardf.audit.fileprovider", file);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                photoURI = Uri.fromFile(file);
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            mediaFile.name = file.getName();
            mediaFile.path = file.getPath();
            mediaFile.date = new Date();
            mediaFile.act = MediaFiles.Act.NoAction;
            startActivityForResult(takePictureIntent, RC_CAMERA);
        }
    }

    /**
     * Вызывается после редактирования комментария к медиафайлу в диалоге
     * @param position - позиция записи медиафайла
     * @param comment - значение комментария
     */
    @Override
    public void onChangedComment(int position, String comment) {
        mSectionsPagerAdapter.changeComment(position, comment);
    }

    /**
     * Интерфейс обработчиков по изменению и добавлению записей медиафайлов
     */
    public interface OnChangeMediaFiles {
        /**
         * Изменение записи
         * @param mediaFile - медиафайл
         */
        void onUpdateMediaFile(int position, MediaFiles.MediaFile mediaFile);

        /**
         * Добавление новой записи
         * @param mediaFile - медиафайл
         */
        void onCreateMediaFile(int position, MediaFiles.MediaFile mediaFile);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_MEDIAFILE = "mediafile";
        private static final String ARG_POSITION = "position";

        private Context context;
        private MediaFiles.MediaFile mediaFile;
        private TextView indicator;
        private TextView comment;
        private ImageView photoView;
        private ImageView deleteView;
        private ImageView playView;
        private VideoView videoView;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(/*Context context, */MediaFiles.MediaFile mediaFile) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putParcelable(ARG_MEDIAFILE, new ParcelableMedia(mediaFile));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle arguments = getArguments();
            if (arguments != null && arguments.containsKey(ARG_MEDIAFILE)) {
                final ParcelableMedia parcelableMedia = arguments.getParcelable(ARG_MEDIAFILE);
                if (parcelableMedia != null) {
                    mediaFile = parcelableMedia.mediaFile;
                }
            }
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_gallery, container, false);
            indicator = rootView.findViewById(R.id.indicator);
            comment = rootView.findViewById(R.id.comment);
            photoView = rootView.findViewById(R.id.picture);
            deleteView = rootView.findViewById(R.id.delete_mark);
            playView = rootView.findViewById(R.id.video_play);
            videoView = rootView.findViewById(R.id.video);
            if (savedInstanceState == null) {
                if (mediaFile != null && mediaFile.loaded && !mediaFile.path.isEmpty()) {
                    int deleteVisibility = View.GONE;
                    int playVisibility = View.GONE;
                    indicator.setText(mediaFile.indicator_name);
                    if (mediaFile.act == MediaFiles.Act.Remove ||
                            mediaFile.act == MediaFiles.Act.RemovePostSave) {
                        deleteVisibility = View.VISIBLE;
                    }
                    Bitmap bitmap = null;
                    switch (mediaFile.type) {
                        case PHOTO:
                            playVisibility = View.GONE;
                            bitmap = BitmapFactory.decodeFile(mediaFile.path);
                            break;
                        case VIDEO:
                            playVisibility = View.VISIBLE;
                            bitmap = ThumbnailUtils.createVideoThumbnail(mediaFile.path,
                                    MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
                            break;
                    }
                    photoView.setImageBitmap(bitmap);
                    playView.setVisibility(playVisibility);
                    deleteView.setVisibility(deleteVisibility);
                    final String text = mediaFile.comment + "\n" + mediaFile.author_name + ", " + mediaFile.date.toString();
                    comment.setText(text);
                }
            }
            else {
                final int position = savedInstanceState.getInt(ARG_POSITION, -1);
                if (position > -1) {
                    videoView.seekTo(position);
                    videoView.start();
                }
            }
            return rootView;
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            this.context = context;
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            if (videoView != null && videoView.isPlaying()) {
                outState.putInt(ARG_POSITION, videoView.getCurrentPosition());
            }
        }

        void play() {
            indicator.setVisibility(View.GONE);
            comment.setVisibility(View.GONE);
            photoView.setVisibility(View.GONE);
            playView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoPath(mediaFile.path);
            videoView.requestFocus();
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp){
                    videoView.setMediaController(new MediaController(context));
                    videoView.start();
                }
            });
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    indicator.setVisibility(View.VISIBLE);
                    comment.setVisibility(View.VISIBLE);
                    photoView.setVisibility(View.VISIBLE);
                    playView.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private final MediaFiles mediaFiles;
        private Fragment primaryFragment;

        SectionsPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
            mediaFiles = new MediaFiles();
        }

        /**
         * Сохранение перед поворотом
         * @param outState - среда для хранения
         */
        void onSaveInstanceState(Bundle outState) {
            final Parcelable[] parcelable = new Parcelable[mediaFiles.size()];
            int i = 0;
            for (MediaFiles.MediaFile mediaFile: mediaFiles) {
                parcelable[i++] = new ParcelableMedia(mediaFile);
            }
            outState.putParcelableArray(ARG_MEDIAFILES, parcelable);
        }

        /**
         * Восстановление после поворота
         * @param savedInstanceState - среда для хранения
         */
        void onRestoreInstanceState(Bundle savedInstanceState) {
            addAllItems(savedInstanceState.getParcelableArray(ARG_MEDIAFILES));
        }

        /**
         * Вызывается для установка текущего фрагмента.
         * Используем для запоминания первичного фрагмента
         * @param container - контейнер
         * @param position - позиция пункта
         * @param object - текущий фрагмент
         */
        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (!object.equals(primaryFragment)) {
                primaryFragment = ((Fragment) object);
            }
            super.setPrimaryItem(container, position, object);
        }

//        @Override
//        public int getItemPosition(@NonNull Object object) {
//            if (mediaFiles.contains((MediaFiles.MediaFile)object)) {
//                return mediaFiles.indexOf((MediaFiles.MediaFile)object);
//            } else {
//                return POSITION_NONE;
//            }
//        }

        /**
         * Обновляет значение записи медиафайла
         * @param mediaFile - запись о медиафайле
         * @return - позиция обновленного элемента
         */
        int updateItem(@NonNull MediaFiles.MediaFile mediaFile) {
            final String name = mediaFile.name;
            int position = 0;
            for (MediaFiles.MediaFile item: mediaFiles) {
                if (name.equals(item.name)) {
                    item.loaded = mediaFile.loaded;
                    item.path = mediaFile.path;
                    if (onChangeMediaFiles != null) {
                        onChangeMediaFiles.onUpdateMediaFile(position, mediaFile);
                    }
                    notifyDataSetChanged();
                    break;
                }
                position++;
            }
            return position;
        }

        /**
         * Добавляет медиафайл в текущую позицию списка вью-пейджера
         * @param mediaFile - запись о медиафайле
         */
        public void addItem(int position, @NonNull MediaFiles.MediaFile mediaFile) {
            if (position >= 0 && position <= getCount()) {
                mediaFiles.add(position, mediaFile);
                notifyDataSetChanged();
            }
        }

        /**
         * Создание массива для загрузки из списка записей медиафайлов
         * @return - массив
         */
        MediaFiles.MediaFile[] toArrayMediaFile() {
            final MediaFiles.MediaFile[] array = new MediaFiles.MediaFile[mediaFiles.size()];
            return mediaFiles.toArray(array);
        }

        /**
         * Добавляет коллекцию медиафайлов в список вью-пейджера из Parcelable[]
         * @param parcelable - коллекция медиафайлов
         */
        private void addAllItems(Parcelable[] parcelable) {
            if (parcelable != null && parcelable.length > 0) {
                for (Parcelable media: parcelable) {
                    mediaFiles.add(((ParcelableMedia) media).mediaFile);
                }
                notifyDataSetChanged();
            }
        }

        /**
         * Получить запись медиафайла из списка
         * @param position - позиция элемента, необязательный параметр
         * @return - запись медиафайла
         */
        MediaFiles.MediaFile getMediaFile(int position) {
            return mediaFiles.get(position);
        }

//        /**
//         * Переключает признак удаления для всех медиафайлов
//         */
//        void switchAllDeleteMark() {
//            boolean allDeletedMark = false;
//            for (MediaFiles.MediaFile mediaFile: mediaFiles) {
//                if (mediaFile.act != MediaFiles.Act.Remove && mediaFile.act != MediaFiles.Act.RemovePostSave) {
//                    allDeletedMark = true;
//                    break;
//                }
//            }
//            if (allDeletedMark) {
//                int position = 0;
//                for (MediaFiles.MediaFile mediaFile: mediaFiles) {
//                    switch (mediaFile.act) {
//                        case NoAction:
//                            mediaFile.act = MediaFiles.Act.Remove;
//                            break;
//                        case Save:
//                            mediaFile.act = MediaFiles.Act.RemovePostSave;
//                            break;
//                        default:
//                            position++;
//                            continue;
//                    }
//                    if (onChangeMediaFiles != null) {
//                        onChangeMediaFiles.onUpdateMediaFile(position++, mediaFile);
//                    }
//                }
//            }
//            else {
//                int position = 0;
//                for (MediaFiles.MediaFile mediaFile: mediaFiles) {
//                    switch (mediaFile.act) {
//                        case Remove:
//                            mediaFile.act = MediaFiles.Act.NoAction;
//                            break;
//                        case RemovePostSave:
//                            mediaFile.act = MediaFiles.Act.Save;
//                            break;
//                        default:
//                            position++;
//                            continue;
//                    }
//                    if (onChangeMediaFiles != null) {
//                        onChangeMediaFiles.onUpdateMediaFile(position++, mediaFile);
//                    }
//                }
//            }
//            notifyDataSetChanged();
//        }

        /**
         * Изменение комментария в записи медиафайла
         * @param position - позиция записи медиафайла
         * @param comment - комментарий
         */
        void changeComment(int position, String comment) {
            if (position >= 0 && position < getCount()) {
                final MediaFiles.MediaFile mediaFile = mediaFiles.get(position);
                mediaFile.comment = comment;
                if (onChangeMediaFiles != null) {
                    onChangeMediaFiles.onUpdateMediaFile(position, mediaFile);
                }
                notifyDataSetChanged();
            }
        }

        /**
         * Переключает признак удаления для текущего медиафайла
         * @param viewPager - вью-пейджер для определения текущего медиафайла
         */
        void switchDeleteMark(ViewPager viewPager) {
            final View view = primaryFragment.getView();
            if (view != null) {
                final int position = viewPager.getCurrentItem();
                final MediaFiles.MediaFile mediaFile = mediaFiles.get(position);
                int visibility = View.GONE;
                switch (mediaFile.act) {
                    case NoAction:
                        mediaFile.act = MediaFiles.Act.Remove;
                        visibility = View.VISIBLE;
                        break;
                    case Save:
                        mediaFile.act = MediaFiles.Act.RemovePostSave;
                        visibility = View.VISIBLE;
                        break;
                    case Remove:
                        mediaFile.act = MediaFiles.Act.NoAction;
                        break;
                    case RemovePostSave:
                        mediaFile.act = MediaFiles.Act.Save;
                        break;
                }
                if (onChangeMediaFiles != null) {
                    onChangeMediaFiles.onUpdateMediaFile(position, mediaFile);
                }
                view.findViewById(R.id.delete_mark).setVisibility(visibility);
                VideoView videoView = view.findViewById(R.id.video);
                if (videoView.isPlaying()) {
                    videoView.stopPlayback();
                    videoView.setVisibility(View.GONE);
                    view.findViewById(R.id.indicator).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.comment).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.picture).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.video_play).setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(/*MediaGallery.this, */mediaFiles.get(position));
        }

        @Override
        public int getCount() {
            return mediaFiles.size();
        }

    }
}
//Фома2019