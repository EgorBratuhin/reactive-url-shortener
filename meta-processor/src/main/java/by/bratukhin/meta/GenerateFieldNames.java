package by.bratukhin.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// Annotation indicating that a field name constants class should be generated for the annotated type.
///
/// The generated class has the same name as the annotated type suffixed with
/// `Fields` and contains constants for each field declared in the annotated type.
///
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateFieldNames {

}
