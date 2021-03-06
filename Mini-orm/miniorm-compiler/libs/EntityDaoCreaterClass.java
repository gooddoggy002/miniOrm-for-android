package com.miniorm.compiler;

import com.google.auto.service.AutoService;

import com.miniorm.annotation.ManyToMany;
import com.miniorm.annotation.ManyToOne;
import com.miniorm.annotation.OneToMany;
import com.miniorm.annotation.OneToOne;
import com.miniorm.annotation.Table;
import com.miniorm.compiler.utils.CollectionUtils;
import com.miniorm.compiler.utils.Content;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;


import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;


import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;


import javax.lang.model.element.Element;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;


import static com.miniorm.compiler.utils.Content.DAO_NAME;
import static com.miniorm.compiler.utils.Content.TABLE;

/**
 * Created by admin on 2017-03-25.
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({TABLE})
public class EntityDaoCreaterClass extends AbstractProcessor {

    Filer filer;
    Types types;
    Elements elementUtills;
    Map<TypeElement, List<Element>> mirrorListMap;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnv.getFiler();
        elementUtills = processingEnvironment.getElementUtils();
        types = processingEnvironment.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if(CollectionUtils.isEmpty(set))return false;
        Set<? extends Element> table = roundEnvironment.getElementsAnnotatedWith(Table.class);

        if (CollectionUtils.notEmpty(table)) {
            try {
                Class[] annotionclasses={ManyToMany.class,ManyToOne.class,OneToMany.class,OneToOne.class};
                for (Class cls:annotionclasses){
                    Set<? extends Element> annotionSet = roundEnvironment.getElementsAnnotatedWith(cls);
                    if(CollectionUtils.notEmpty(annotionSet)){
                        for (Element proxyEntity:annotionSet){
                            classify(proxyEntity);
                        }

                    }
                }

                createHelper(table);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    private void classify(Element element) {
        TypeMirror mirror = element.getEnclosingElement().asType();
        TypeElement typeElement=   (TypeElement) types.asElement( mirror );
        if (mirror != null) {
            if (mirrorListMap == null) {
                mirrorListMap = new LinkedHashMap<>();
            }
            boolean exist = mirrorListMap.containsKey(typeElement);
            if (exist) {
                List<Element> elements = mirrorListMap.get(typeElement);
                elements.add(element);
            } else {
                List<Element> elements = new LinkedList<>();
                elements.add(element);
                mirrorListMap.put(typeElement, elements);
            }
        }
    }


    private void createHelper( Set<? extends Element> typeElemnts) throws Exception {
        for (Element tableElemnt : typeElemnts) {
            TypeElement typeElement = (TypeElement) tableElemnt;
            String Package_name = elementUtills.getPackageOf(typeElement).toString();
            String parentSimpleName = typeElement.getSimpleName().toString();

            String className = parentSimpleName + Content.NEW_DAO_NAME;
            ClassName parentClassName=      ClassName.get(Package_name,parentSimpleName);
            ClassName typeName = ClassName.get(Content.DAO_PACKAGE_NAME,DAO_NAME);

            TypeName parentTypeName= ParameterizedTypeName.get(typeName,parentClassName);

            TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                    .superclass(parentTypeName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            builder.addMethod(getTableEntityMethodSpec(typeElement,parentClassName));

            TypeSpec typeSpec = builder.build();
            JavaFile javaFile = JavaFile.builder(Package_name, typeSpec)
                    .build();
            javaFile.writeTo(filer);
        }
    }


    /*one 2  one  / many to one*/
    private MethodSpec getTableEntityMethodSpec(Element element,ClassName tableEntity) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getTableEntity").returns(tableEntity)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);
        if(mirrorListMap.containsKey(element)){
            ClassName tableproxyEntity=ClassName.get(tableEntity.packageName(),tableEntity.simpleName()+Content.NEW_CLASS_NAME);
            budiling(builder,tableproxyEntity);
        }
        else
            budiling(builder,tableEntity);

        MethodSpec beyond =
                builder.build();

        return beyond;
    }

    private void budiling(MethodSpec.Builder builder, ClassName returnClass) {
        StringBuilder methods = new StringBuilder();
        methods.append("return  new $T() ");
        builder.addStatement(methods.toString(),returnClass);
    }




    private ClassName getClassNameFromTypeMirror(TypeMirror returnTypeMirror) {
        TypeElement typeElement = (TypeElement) types.asElement(returnTypeMirror);
        String Package_name = elementUtills.getPackageOf(typeElement).toString();
        String parentClassName = typeElement.getSimpleName().toString();
        ClassName className = ClassName.get(Package_name, parentClassName);
        return className;
    }

}
