package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class StandardComment implements BasePlatformObject<StandardCommentPK> {
   public static final int FREE_TEXT_STANDARD_COMMENT = 0;

   private StandardCommentPK compId;

   // Properties:
   private String text;

   // Referenced tables:
   private Platform platform;

   public StandardComment() {
   }

   public StandardComment(StandardCommentPK compId) {
      this.compId = compId;
   }

   public StandardComment(StandardCommentPK compId, String text) {
      this.compId = compId;
      this.text = text;
   }

   @EmbeddedId
   @Override
   public StandardCommentPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(StandardCommentPK compId) {
      this.compId = compId;
   }

   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = DatabaseUtils.nullToEmpty(text);
   }

   public Platform getPlatform() {
      return platform;
   }

   public void setPlatform(Platform platform) {
      this.platform = platform;
   }

   @Override
   public String toString() {
      return "StandardComment{" +
            "compId=" + compId +
            ", text='" + text + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof StandardComment that
            && Objects.equals(compId, that.compId)
            && Objects.equals(text, that.text);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Objects.hashCode(text);
      return result;
   }
}
