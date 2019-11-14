package com.jay.bindviewlib;

import androidx.annotation.UiThread;

/**
 * Created by huangjie on 2019/11/12.
 * 说明：解绑接口
 */
public interface Unbinder {
    @UiThread
    void unbind();

    Unbinder EMPTY = () -> { };
}
