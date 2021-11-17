/*
 * Created by Eduard Fomin on 16.04.20 9:13
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 16.04.20 9:13
 */

package com.bit.eduardf.audit.TaskList;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bit.eduardf.audit.PaginationListener;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @author e.matsyuk
 */
public class PaginationTool<T> {

    // for first start of items loading then on RecyclerView there are not items and no scrolling
    private static final int EMPTY_LIST_ITEMS_COUNT = 0;
    // default limit for requests
    private static final int DEFAULT_LIMIT = 50;
    // default max attempts to retry loading request
    private static final int MAX_ATTEMPTS_TO_RETRY_LOADING = 3;

    private RecyclerView recyclerView;
    private PagingListener<T> pagingListener;
    private int limit;
    private int emptyListCount;
    private int retryCount;
    private boolean emptyListCountPlusToOffset;

    private PaginationTool() {
    }

    public Observable<T> getPagingObservable() {
        final int startNumberOfRetryAttempt = 0;
        return getScrollObservable(recyclerView, limit, emptyListCount)
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .observeOn(Schedulers.from(BackgroundExecutor.getSafeBackgroundExecutor()))
                .switchMap(offset -> getPagingObservable(pagingListener,
                        pagingListener.onNextPage(offset), startNumberOfRetryAttempt, offset,
                        retryCount));
    }

    private Observable<Integer> getScrollObservable(RecyclerView recyclerView, final int limit, int emptyListCount) {
        return Observable.create(subscriber -> {
            final GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            final RecyclerView.OnScrollListener sl = new PaginationListener(layoutManager) {
                @Override
                protected void loadMoreItems() {
                    subscriber.onNext(layoutManager.getItemCount());
                }

                @Override
                public boolean isLastPage() {
                    return false;
                }

                @Override
                public boolean isLoading() {
                    return false;
                }
            };
            recyclerView.addOnScrollListener(sl);
//            subscriber.add(Subscriptions.create(() -> recyclerView.removeOnScrollListener(sl)));
            if (recyclerView.getAdapter().getItemCount() == emptyListCount) {
                int offset = emptyListCountPlusToOffset ? recyclerView.getAdapter().getItemCount() : recyclerView.getAdapter().getItemCount() - emptyListCount;
                subscriber.onNext(offset);
            }
        });
    }

    private Observable<T> getPagingObservable(final PagingListener<T> listener, Observable<T> observable, final int numberOfAttemptToRetry, final int offset, final int retryCount) {
        return observable
                .onErrorResumeNext(throwable -> {
                    // retry to load new data portion if error occurred
                    if (numberOfAttemptToRetry < retryCount) {
                        int attemptToRetryInc = numberOfAttemptToRetry + 1;
                        return getPagingObservable(listener, listener.onNextPage(offset), attemptToRetryInc, offset, retryCount);
                    } else {
                        return Observable.empty();
                    }
                });
    }

    public static <T> Builder<T> buildPagingObservable(RecyclerView recyclerView, PagingListener<T> pagingListener) {
        return new Builder<>(recyclerView, pagingListener);
    }

    public static class Builder<T> {

        private RecyclerView recyclerView;
        private PagingListener<T> pagingListener;
        private int limit = DEFAULT_LIMIT;
        private int emptyListCount = EMPTY_LIST_ITEMS_COUNT;
        private int retryCount = MAX_ATTEMPTS_TO_RETRY_LOADING;
        private boolean emptyListCountPlusToOffset = false;

        private Builder(RecyclerView recyclerView, PagingListener<T> pagingListener) {
            if (recyclerView == null) {
                throw new PagingException("null recyclerView");
            }
            if (recyclerView.getAdapter() == null) {
                throw new PagingException("null recyclerView adapter");
            }
            if (pagingListener == null) {
                throw new PagingException("null pagingListener");
            }
            this.recyclerView = recyclerView;
            this.pagingListener = pagingListener;
        }

        public Builder<T> setLimit(int limit) {
            if (limit <= 0) {
                throw new PagingException("limit must be greater then 0");
            }
            this.limit = limit;
            return this;
        }

        public Builder<T> setEmptyListCount(int emptyListCount) {
            if (emptyListCount < 0) {
                throw new PagingException("emptyListCount must be not less then 0");
            }
            this.emptyListCount = emptyListCount;
            return this;
        }

        public Builder<T> setRetryCount(int retryCount) {
            if (retryCount < 0) {
                throw new PagingException("retryCount must be not less then 0");
            }
            this.retryCount = retryCount;
            return this;
        }

        public Builder<T> setEmptyListCountPlusToOffset(boolean emptyListCountPlusToOffset) {
            this.emptyListCountPlusToOffset = emptyListCountPlusToOffset;
            return this;
        }

        public PaginationTool<T> build() {
            PaginationTool<T> paginationTool = new PaginationTool<>();
            paginationTool.recyclerView = this.recyclerView;
            paginationTool.pagingListener = pagingListener;
            paginationTool.limit = limit;
            paginationTool.emptyListCount = emptyListCount;
            paginationTool.retryCount = retryCount;
            paginationTool.emptyListCountPlusToOffset = emptyListCountPlusToOffset;
            return paginationTool;
        }

    }

}
//@author e.matsyuk