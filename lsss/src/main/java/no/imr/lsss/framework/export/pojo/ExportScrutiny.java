package no.imr.lsss.framework.export.pojo;

import no.imr.tools.annotations.ReflectionEntryPoint;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ExportScrutiny {
   public List<Channel> channels = new ArrayList<>();
   public List<RestCategory> restCategories = new ArrayList<>();

   @ReflectionEntryPoint
   public ExportScrutiny() {
   }

   public static final class Channel {
      public @Nullable String channelId;
      public List<Category> categories;

      public Channel() {
         categories = new ArrayList<>();
      }

      public Channel(String channelId, List<Category> categories) {
         this.channelId = channelId;
         this.categories = categories;
      }

      @Override
      public String toString() {
         return "{" +
               "channelId='" + channelId + '\'' +
               ", categories=" + categories +
               '}';
      }
   }

   public static final class Category {
      public int id;
      public String initials;
      public float assignment;

      @ReflectionEntryPoint
      public Category() {
         initials = "";
      }

      public Category(int id, String initials, float assignment) {
         this.id = id;
         this.initials = initials;
         this.assignment = assignment;
      }

      @Override
      public String toString() {
         return "{" +
               "id=" + id +
               ", initials='" + initials + '\'' +
               ", assignment=" + assignment +
               '}';
      }
   }

   public static final class RestCategory {
      public int id;
      public String initials;

      @ReflectionEntryPoint
      public RestCategory() {
         initials = "";
      }

      public RestCategory(int id, String initials) {
         this.id = id;
         this.initials = initials;
      }

      @Override
      public String toString() {
         return "{" +
               "id=" + id +
               ", initials='" + initials + '\'' +
               '}';
      }
   }
}
