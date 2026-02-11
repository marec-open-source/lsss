package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@ColumnOrder({
      "parName",

      // Properties:
      "parValue",
})
public class DBParameter implements BaseDatabaseObject {
   private String parName;

   // Properties:
   private String parValue;

   // Referenced tables:
   // <none>

   public DBParameter() {
   }

   public DBParameter(String parName, String parValue) {
      this.parName = parName;
      this.parValue = parValue;
   }

   @Override
   public Object primaryKey() {
      return parName;
   }

   @Id
   @Column(length = 40)
   public String getParName() {
      return parName;
   }

   public void setParName(String parName) {
      this.parName = DatabaseUtils.nullToEmpty(parName);
   }

   @Column(length = 200)
   public String getParValue() {
      return parValue;
   }

   public void setParValue(String parValue) {
      this.parValue = DatabaseUtils.nullToEmpty(parValue);
   }

   @Override
   public String toString() {
      return parName + " " + parValue;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof DBParameter that
            && Objects.equals(parName, that.parName)
            && Objects.equals(parValue, that.parValue);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(parName);
      result = 31 * result + Objects.hashCode(parValue);
      return result;
   }
}
