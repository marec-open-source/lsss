package no.imr.lsss.modules.comment;

import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.time.Instant;

record Comment(long timeInMillis, int standardComment, double value, String text) {

   Comment(long timeInMillis, int standardComment, double value, String text) {
      this.timeInMillis = DatabaseTime.roundMillis(timeInMillis);
      this.standardComment = standardComment;
      this.value = value;
      this.text = text;
   }

   Comment(long timeInMillis, String text) {
      this(timeInMillis, StandardComment.FREE_TEXT_STANDARD_COMMENT, 0, text);
   }

   static Comment fromXml(Element element) throws XmlParseException {
      long timeInMillis = PingMapping.timeValueToMillis(XmlParse.doubleAttribute(element, "time"));
      int standardComment = XmlParse.intAttribute(element, "standardComment", 0);
      double value = XmlParse.doubleAttribute(element, "value");
      String text = standardComment == StandardComment.FREE_TEXT_STANDARD_COMMENT ? element.getText() : "";
      return new Comment(timeInMillis, standardComment, value, text);
   }

   Element toXml() {
      Element element = DocumentFactory.getInstance().createElement("comment")
            .addAttribute("time", Utils.toString(PingMapping.millisToTimeValue(timeInMillis)))
            .addAttribute("value", Utils.toString(value));
      if (standardComment == StandardComment.FREE_TEXT_STANDARD_COMMENT) {
         element.addText(text);
      } else {
         element.addAttribute("standardComment", Integer.toString(standardComment));
      }
      return element;
   }

   Instant toInstant() {
      return Instant.ofEpochMilli(timeInMillis);
   }
}
