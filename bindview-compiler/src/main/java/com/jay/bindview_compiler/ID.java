package com.jay.bindview_compiler;

import com.squareup.javapoet.CodeBlock;

/**
 * Created by huangjie on 2019/11/12.
 * 说明：用于保存id信息
 */
final class ID {
    /**
     * value及注解中的value id
     */
    final CodeBlock code;

    ID(int value){
        this.code = CodeBlock.of("$L", value);
    }
}
