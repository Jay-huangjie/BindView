package com.jay.bindview_compiler;

import com.google.auto.service.AutoService;
import com.jay.bindview_annotations.BindView;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/**
 * Created by huangjie on 2019/11/12.
 */
@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor {

    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //我们可以从这里获取一些工具类
        mFiler = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //缓存BindingSet并给BindingSet赋值
        Map<TypeElement, BindingSet> bindingMap = findAndParseTargets(roundEnvironment);
        //第二步，循环获取BindingSet并执行brewJava开始绘制代码
        for (Map.Entry<TypeElement, BindingSet> entry : bindingMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            BindingSet binding = entry.getValue();
            JavaFile javaFile = binding.brewJava();
            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 给BindingSet赋值并生成一个map
     * @param env 当前元素环境
     * @return 元素集合
     */
    private Map<TypeElement, BindingSet> findAndParseTargets(RoundEnvironment env) {
        Map<TypeElement, BindingSet.Builder> builderMap = new LinkedHashMap<>();
        Set<TypeElement> erasedTargetNames = new LinkedHashSet<>();

        //这里循环生成了BindingSet.Builder并将值放入了builderMap中
        Set<? extends Element> envs = env.getElementsAnnotatedWith(BindView.class);
        for (Element element : envs) {
            try {
                parseBindView(element, builderMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //从builderMap中取出值并生成BindingSet放入bindingMap中，源码是用的while，并有处理父类的super逻辑，这里直接用for
        Map<TypeElement, BindingSet> bindingMap = new LinkedHashMap<>();
        for (Map.Entry<TypeElement, BindingSet.Builder> entry:builderMap.entrySet()) {
            TypeElement type = entry.getKey();
            BindingSet.Builder builder = entry.getValue();
            bindingMap.put(type, builder.build());
        }
        return bindingMap;
    }

    /**
     * 为BindingSet赋值，从Element元素中获取Activity与控件信息，并保存到BindingSet中
     */
    private void parseBindView(Element element, Map<TypeElement, BindingSet.Builder> builderMap) {
        //获取父类的Element
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        TypeMirror elementType = element.asType();
        if (elementType.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = (TypeVariable) elementType;
            elementType = typeVariable.getUpperBound();
        }
        Name qualifiedName = enclosingElement.getQualifiedName();
        Name simpleName = element.getSimpleName();

        int id = element.getAnnotation(BindView.class).value();
        BindingSet.Builder builder = getOrCreateBindingBuilder(builderMap, enclosingElement);

        String name = simpleName.toString();
        TypeName type = TypeName.get(elementType);
        builder.addField(new ID(id), new FieldViewBinding(name, type));
    }

    /**
     * 创建BindingSet 并且将BindingSet缓存到builderMap中
     */
    private BindingSet.Builder getOrCreateBindingBuilder(
            Map<TypeElement, BindingSet.Builder> builderMap, TypeElement enclosingElement) {
        BindingSet.Builder builder = builderMap.get(enclosingElement);
        if (builder == null) {
            builder = BindingSet.newBuilder(enclosingElement);
            builderMap.put(enclosingElement, builder);
        }
        return builder;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(BindView.class.getCanonicalName()); //将我们自定义的注解添加进去
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}
