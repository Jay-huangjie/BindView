package com.jay.bindview_compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

/**
 * Created by huangjie on 2019/11/12.
 * 说明：保存字段信息
 */
final class FieldViewBinding {
    //字段名称
    private final String name;
    //字段类型
    private final TypeName type;

    FieldViewBinding(String name,TypeName type){
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TypeName getType() {
        return type;
    }

    public ClassName getRawType() {
        if (type instanceof ParameterizedTypeName) {
            return ((ParameterizedTypeName) type).rawType;
        }
        return (ClassName) type;
    }
}
