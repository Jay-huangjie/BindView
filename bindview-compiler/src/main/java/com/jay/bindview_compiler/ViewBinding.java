package com.jay.bindview_compiler;

import javax.annotation.Nullable;

/**
 * Created by huangjie on 2019/11/12.
 * 说明：用于保存view信息,仿源码采用构建者模式
 */
final class ViewBinding {

    private final ID id;

    @Nullable
    private final FieldViewBinding fieldBinding;


    private ViewBinding(ID id, @Nullable FieldViewBinding fieldBinding) {
        this.id = id;
        this.fieldBinding = fieldBinding;
    }

    public ID getId() {
        return id;
    }

    @Nullable
    public FieldViewBinding getFieldBinding() {
        return fieldBinding;
    }

    static final class Builder {
        private final ID id;

        @Nullable
        private FieldViewBinding fieldBinding;

        Builder(ID id) {
            this.id = id;
        }

        public void setFieldBinding(FieldViewBinding fieldBinding) {
            if (this.fieldBinding != null) {
                throw new AssertionError();
            }
            this.fieldBinding = fieldBinding;
        }

        public ViewBinding build() {
            return new ViewBinding(id, fieldBinding);
        }
    }
}
