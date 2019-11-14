package com.jay.bindview_compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreElements.getPackage;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by huangjie on 2019/11/12.
 * 说明：这个类用来负责管理javapoet的代码生成，butterknife 兼容了dialog，自定义view的情况，这里只实现在activity中的情况
 * 仿源码采用构建者模式
 */
final class BindingSet{

    private final TypeName targetTypeName; //示例值 MainActivity
    private final ClassName bindingClassName; //示例值 MainActivity_ViewBinding
    private final TypeElement enclosingElement; //这是注解元素的父类Element,用于获取父类元素
    private final ImmutableList<ViewBinding> viewBindings; //保存了每一个字段的元素

    private BindingSet(
            TypeName targetTypeName, ClassName bindingClassName, TypeElement enclosingElement,
            ImmutableList<ViewBinding> viewBindings) {
        this.targetTypeName = targetTypeName;
        this.bindingClassName = bindingClassName;
        this.enclosingElement = enclosingElement;
        this.viewBindings = viewBindings;
    }

    /**
     * 从这个方法开始构建代码，这里只实现BindView的代码逻辑
     *
     * @return JavaFile
     */
    JavaFile brewJava() {
        TypeSpec bindingConfiguration = createType();
        return JavaFile.builder(bindingClassName.packageName(), bindingConfiguration)
                .addFileComment("Generated code from Butter Knife. Do not modify!")
                .build();
    }

    private TypeSpec createType() {
        //第一步 先创建类
        TypeSpec.Builder result = TypeSpec.classBuilder(bindingClassName.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addOriginatingElement(enclosingElement); //设置注解处理器的源元素
        //添加解绑接口
        result.addSuperinterface(ClassName.get("com.jay.bindviewlib", "Unbinder"));
        //添加activity字段target
        result.addField(targetTypeName, "target");
        //添加构造方法
        result.addMethod(createBindingConstructorForActivity());
        //添加找id的方法
        result.addMethod(createBindingConstructor());
        //添加解绑的方法
        result.addMethod(createBindingUnbindMethod());
        return result.build();
    }

    /**
     * 示例：MainActivity_BindView(MainActivity target){
     * this(target, target.getWindow().getDecorView())
     * }
     *
     * @return MethodSpec
     */
    private MethodSpec createBindingConstructorForActivity() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(targetTypeName, "target");
        builder.addStatement("this(target, target.getWindow().getDecorView())");
        return builder.build();
    }

    private static final ClassName VIEW = ClassName.get("android.view", "View");

    /**
     * 创建构造方法，这个方法里包含找id的代码
     *
     * @return MethodSpec
     */
    private MethodSpec createBindingConstructor() {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC);
        constructor.addParameter(targetTypeName, "target");
        constructor.addParameter(VIEW, "source");
        constructor.addStatement("this.target = target");
        constructor.addCode("\n");
        //这里循环创建控件赋值代码
        for (ViewBinding binding : viewBindings) {
            addViewBinding(constructor, binding);
        }
        return constructor.build();
    }

    //创建一条赋值代码
    //示例：target.textview1 = (TextView)source.findViewById(id)
    //这里的source = target.getWindow().getDecorView() target是Activity
    private void addViewBinding(MethodSpec.Builder result, ViewBinding binding) {
        FieldViewBinding fieldBinding = requireNonNull(binding.getFieldBinding());
        CodeBlock.Builder builder = CodeBlock.builder()
                .add("target.$L = ", fieldBinding.getName()); //添加代码 target.textview1 =
        builder.add("($T) ", fieldBinding.getType()); //添加强转代码
        builder.add("source.findViewById($L)", binding.getId().code); //找id
        result.addStatement("$L", builder.build()); //将代码添加到方法中
    }


    /**
     * 创建解绑的方法
     *
     * @return MethodSpec
     */
    private MethodSpec createBindingUnbindMethod() {
        MethodSpec.Builder result = MethodSpec.methodBuilder("unbind")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC);
        result.addStatement("$T target = this.target", targetTypeName);
        result.addStatement("if (target == null) throw new $T($S)", IllegalStateException.class,
                "Bindings already cleared.");
        result.addStatement("$N = null","this.target");
        result.addCode("\n");
        for (ViewBinding binding : viewBindings) {
            if (binding.getFieldBinding() != null) {
                result.addStatement("target.$L = null", binding.getFieldBinding().getName());
            }
        }
        return result.build();
    }

    /**
     * 生成代码生成的类的类名
     * @return Name  规则 ActivityName__ViewBinding
     */
    static ClassName getBindingClassName(TypeElement typeElement) {
        String packageName = getPackage(typeElement).getQualifiedName().toString();
        String className = typeElement.getQualifiedName().toString().substring(
                packageName.length() + 1).replace('.', '$');
        return ClassName.get(packageName, className + "_ViewBinding");
    }

    /**
     * 创建一个Builder
     * @param enclosingElement 父类元素，也就是那个Activity
     * @return 这里生成了类名称与类target
     */
    static Builder newBuilder(TypeElement enclosingElement) {
        TypeMirror typeMirror = enclosingElement.asType();

        TypeName targetType = TypeName.get(typeMirror);
        if (targetType instanceof ParameterizedTypeName) {
            targetType = ((ParameterizedTypeName) targetType).rawType;
        }
        ClassName bindingClassName = getBindingClassName(enclosingElement);
        return new Builder(targetType, bindingClassName, enclosingElement);
    }

    static final class Builder {
        private final TypeName targetTypeName;
        private final ClassName bindingClassName;
        private final TypeElement enclosingElement;

        //缓存ViewBinding实例，提升性能
        private final Map<ID, ViewBinding.Builder> viewIdMap = new LinkedHashMap<>();

        private Builder(
                TypeName targetTypeName, ClassName bindingClassName, TypeElement enclosingElement) {
            this.targetTypeName = targetTypeName;
            this.bindingClassName = bindingClassName;
            this.enclosingElement = enclosingElement;
        }


        void addField(ID id, FieldViewBinding binding) {
            getOrCreateViewBindings(id).setFieldBinding(binding);
        }

        private ViewBinding.Builder getOrCreateViewBindings(ID id) {
            ViewBinding.Builder viewId = viewIdMap.get(id);
            if (viewId == null) {
                viewId = new ViewBinding.Builder(id);
                viewIdMap.put(id, viewId);
            }
            return viewId;
        }

        BindingSet build() {
            ImmutableList.Builder<ViewBinding> viewBindings = ImmutableList.builder();
            for (ViewBinding.Builder builder : viewIdMap.values()) {
                viewBindings.add(builder.build());
            }
            return new BindingSet(targetTypeName, bindingClassName, enclosingElement, viewBindings.build());
        }
    }
}
