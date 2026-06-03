package by.bratukhin.meta;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

///
/// Annotation processor that generates field name constants classes for types
/// annotated with [GenerateFieldNames].
///
/// For each annotated type, a new class named `<TypeName>Fields` is generated in the same package.
/// The generated class contains constants for every field declared in the original type.
///
@AutoService(Processor.class)
@SupportedAnnotationTypes("by.bratukhin.meta.GenerateFieldNames")
public class FieldMetaProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(GenerateFieldNames.class).stream()
            .filter(TypeElement.class::isInstance)
            .map(TypeElement.class::cast)
            .forEach(typeElement -> {
                try {
                    generateMetaClass(typeElement);
                }
                catch (Exception e) {
                    processingEnv.getMessager()
                        .printError("Failed to generate meta class: " + e.getMessage());
                }
            });

        return true;
    }

    private void generateMetaClass(TypeElement typeElement) throws IOException {
        String metaClassName = typeElement.getSimpleName().toString() + "Fields";

        TypeSpec.Builder metaClassBuilder = TypeSpec.classBuilder(metaClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        typeElement.getEnclosedElements().stream()
            .filter(VariableElement.class::isInstance)
            .map(VariableElement.class::cast)
            .map(variableElement -> variableElement.getSimpleName().toString())
            .map(FieldMetaProcessor::buildFieldSpec)
            .forEach(metaClassBuilder::addField);

        JavaFile.builder(getPackageName(typeElement), metaClassBuilder.build())
            .build()
            .writeTo(processingEnv.getFiler());
    }

    private static FieldSpec buildFieldSpec(String fieldName) {
        return FieldSpec.builder(String.class, fieldName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", fieldName)
            .build();
    }

    private String getPackageName(TypeElement typeElement) {
        return processingEnv.getElementUtils()
            .getPackageOf(typeElement)
            .getQualifiedName()
            .toString();
    }

}
