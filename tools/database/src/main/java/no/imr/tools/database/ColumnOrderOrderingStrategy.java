package no.imr.tools.database;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedObjectType;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class ColumnOrderOrderingStrategy implements ColumnOrderingStrategy {
   private final Map<String, Class<? extends BaseDatabaseObject>> nameToClass;

   ColumnOrderOrderingStrategy(List<Class<? extends BaseDatabaseObject>> databaseClasses) {
      nameToClass = databaseClasses.stream()
            .collect(Collectors.toMap(Class::getSimpleName, Function.identity()));
   }

   @Override
   public List<Column> orderTableColumns(Table table, Metadata metadata) {
      return sortedColumns(table, table.getColumns());
   }

   @Override
   public List<Column> orderConstraintColumns(Constraint constraint, Metadata metadata) {
      return sortedColumns(constraint.getTable(), constraint.getColumns());
   }

   private List<Column> sortedColumns(Table table, Collection<Column> columns) {
      Class<? extends BaseDatabaseObject> clazz = nameToClass.get(table.getName());
      ColumnOrder columnOrderAnnotation = clazz.getAnnotation(ColumnOrder.class);
      String[] columnOrder = columnOrderAnnotation.value();
      Map<String, Integer> columnNameToIndex = IntStream.range(0, columnOrder.length)
            .boxed()
            .collect(Collectors.toMap(i -> columnOrder[i], Function.identity()));
      return columns.stream()
            .sorted(Comparator.comparingInt(column -> columnNameToIndex.get(column.getName())))
            .toList();
   }

   @Override
   public @Nullable List<Column> orderUserDefinedTypeColumns(UserDefinedObjectType userDefinedType, Metadata metadata) {
      return null;
   }

   @Override
   public void orderTemporaryTableColumns(List<TemporaryTableColumn> temporaryTableColumns, Metadata metadata) {
   }
}
