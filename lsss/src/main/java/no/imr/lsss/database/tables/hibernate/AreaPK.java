package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class AreaPK implements BaseNationPK {
   private short nation;
   private int area;

   public AreaPK() {
   }

   public AreaPK(short nation, int area) {
      this.nation = nation;
      this.area = area;
   }

   @Override
   public short getNation() {
      return nation;
   }

   @Override
   public void setNation(short nation) {
      this.nation = nation;
   }

   public int getArea() {
      return area;
   }

   public void setArea(int area) {
      this.area = area;
   }

   @Override
   public String toString() {
      return "AreaPK{" +
            "nation=" + nation +
            ", area=" + area +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AreaPK that
            && nation == that.nation
            && area == that.area;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + area;
      return result;
   }
}
