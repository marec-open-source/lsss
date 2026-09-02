package no.imr.tools.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// This annotation is for tables where all columns are part of the primary key.
/// In this case Hibernate's upsert does not work. See #1603.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TableWithOnlyPrimaryKeyColumns {
}
