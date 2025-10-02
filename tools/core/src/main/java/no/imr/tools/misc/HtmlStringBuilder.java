package no.imr.tools.misc;

import com.google.common.html.HtmlEscapers;

import java.util.function.Consumer;

public final class HtmlStringBuilder {
   private final StringBuilder stringBuilder;

   private HtmlStringBuilder(StringBuilder stringBuilder) {
      this.stringBuilder = stringBuilder;
   }

   public HtmlStringBuilder() {
      this(new StringBuilder("<html>"));
   }

   public static HtmlStringBuilder withoutInitialHtmlTag() {
      return new HtmlStringBuilder(new StringBuilder());
   }

   public HtmlStringBuilder html(String html) {
      stringBuilder.append(html);
      return this;
   }

   public HtmlStringBuilder text(String text) {
      stringBuilder.append(HtmlEscapers.htmlEscaper().escape(text));
      return this;
   }

   public HtmlStringBuilder text(int text) {
      stringBuilder.append(text);
      return this;
   }

   public HtmlStringBuilder multilineText(String text) {
      stringBuilder.append(HtmlEscapers.htmlEscaper().escape(text).replace("\n", "<br>"));
      return this;
   }

   public HtmlStringBuilder withBuilder(Consumer<HtmlStringBuilder> consumer) {
      consumer.accept(this);
      return this;
   }

   @Override
   public String toString() {
      return stringBuilder.toString();
   }

   public String build() {
      return stringBuilder.toString();
   }
}
