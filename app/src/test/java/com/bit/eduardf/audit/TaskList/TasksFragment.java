/*
 * Created by Eduard Fomin on 13.04.20 12:01
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 13.04.20 12:01
 */

package com.bit.eduardf.audit.TaskList;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bit.eduardf.audit.AuditOData;
import com.bit.eduardf.audit.R;
import com.bit.eduardf.audit.Tasks;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.bit.eduardf.audit.ReferenceChoice.ACTION_BAR;

public class TasksFragment extends Fragment {

    private final static int LIMIT = 50;
    private TasksAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    private View progressBar;
    Disposable disposable;
    private AuditOData oData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fmt_tasks, container, false);
        setRetainInstance(true);
        init(rootView, savedInstanceState);
        return rootView;
    }

    private void init(View view, Bundle savedInstanceState) {
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        GridLayoutManager recyclerViewLayoutManager = new GridLayoutManager(getActivity(), 1);
//        recyclerViewLayoutManager.supportsPredictiveItemAnimations();
        // init adapter for the first time
        if (savedInstanceState == null) {
            recyclerViewAdapter = new TasksAdapter(this, ACTION_BAR);
            recyclerViewAdapter.setHasStableIds(true);
        }
        recyclerView.setSaveEnabled(true);

        recyclerView.setLayoutManager(recyclerViewLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        // if all items was loaded we don't need Pagination
        if (recyclerViewAdapter.isAllLoaded()) {
            return;
        }

        Observable<Integer> scrolling = Observable.create(source -> {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (!source.isDisposed()) {
                        source.onNext(1);
                    }
                }
            });
        });
        scrolling.subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .observeOn(Schedulers.from(BackgroundExecutor.getSafeBackgroundExecutor()));
//                .switchMap(offset -> offset);

        Observable <Tasks.Task> pagination = Observable.create(emitter -> {
            try {
                for (Tasks.Task task: oData.getTasks(null, null, null, 0, 100)) {
                    emitter.onNext(task);
                }
                emitter.onComplete();
            }
            catch (Exception e) {
                emitter.onError(e);
            }
        });

        disposable = pagination
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(__ -> progressBar.setVisibility(View.VISIBLE))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(task -> {recyclerViewAdapter.addNewItem(task);
                recyclerViewAdapter.notifyItemInserted(recyclerViewAdapter.getItemCount()-1);},
                Throwable::printStackTrace,
                () -> progressBar.setVisibility(View.INVISIBLE));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(android.content.Context context) {
        super.onAttach(context);
        oData = new AuditOData(context);
    }

    @Override
    public void onDestroyView() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        // for memory leak prevention (RecycleView is not unsubscibed from adapter DataObserver)
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        oData = null;
        super.onDestroyView();
    }

}
//Фома2020