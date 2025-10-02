package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class StandardCommentPK implements BasePlatformPK {
   private short nation;
   private short platform;
   private int standardComment;

   public StandardCommentPK() {
   }

   public StandardCommentPK(short nation, short platform, int standardComment) {
      this.nation = nation;
      this.platform = platform;
      this.standardComment = standardComment;
   }

   @Override
   public short getNation() {
      return nation;
   }

   @Override
   public void setNation(short nation) {
      this.nation = nation;
   }

   @Override
   public short getPlatform() {
      return platform;
   }

   @Override
   public void setPlatform(short platform) {
      this.platform = platform;
   }

   public int getStandardComment() {
      return standardComment;
   }

   public void setStandardComment(int standardComment) {
      this.standardComment = standardComment;
   }

   @Override
   public String toString() {
      return "StandardCommentPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", standardComment=" + standardComment +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof StandardCommentPK that
            && nation == that.nation
            && platform == that.platform
            && standardComment == that.standardComment;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + standardComment;
      return result;
   }
}
