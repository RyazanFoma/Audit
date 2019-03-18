package com.example.eduardf.audit;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 19.09.18 15:56
 *
 */

//Расширение класса GridLayout с автоматическим подбором количества колонок
//All right https://github.com/kevinmeresse/AutoGridLayout
public class AutoGridLayout extends GridLayout {

    private int defaultColumnCount;
    private int columnWidth;

    public AutoGridLayout(Context context) {
        super(context);
        init(null, 0);
    }

    public AutoGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public AutoGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AutoGridLayout, 0, defStyleAttr);
        try {
            columnWidth = a.getDimensionPixelSize(R.styleable.AutoGridLayout_columnWidth, 0);

            int[] set = { android.R.attr.columnCount /* id 0 */ };
            a = getContext().obtainStyledAttributes(attrs, set, 0, defStyleAttr);
            defaultColumnCount = a.getInt(0, 10);
        } finally {
            a.recycle();
        }

        /* Initially set columnCount to 1, will be changed automatically later. */
        setColumnCount(1);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        int width = MeasureSpec.getSize(widthSpec);
        if (columnWidth > 0 && width > 0) {
            int totalSpace = width - getPaddingRight() - getPaddingLeft();
            int columnCount = Math.max(1, totalSpace / columnWidth);
            setColumnCount(columnCount);
        } else {
            setColumnCount(defaultColumnCount);
        }
    }
}
//All right https://github.com/kevinmeresse/AutoGridLayout