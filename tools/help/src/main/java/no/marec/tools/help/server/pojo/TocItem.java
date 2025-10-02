package no.marec.tools.help.server.pojo;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class TocItem {
   public int page;
   public @Nullable String anchor;
   public @Nullable String text;
   public @Nullable String style;
   public @Nullable List<TocItem> items;

   public TocItem() {
   }
}
