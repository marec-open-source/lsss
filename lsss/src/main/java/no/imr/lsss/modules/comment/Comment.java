package no.imr.lsss.modules.comment;

import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.time.Instant;

record Comment(Instant time, int standardComment, double value, String text) {

   Comment {
      time = DatabaseTime.truncatedInstant(time);
   }

   Comment(Instant time, String text) {
      this(time, StandardComment.FREE_TEXT_STANDARD_COMMENT, 0, text);
   }

   static Comment fromXml(Element element) throws XmlParseException {
      long epochMilli = (long) (XmlParse.doubleAttribute(element, "time") * 1000);
      int standardComment = XmlParse.intAttribute(element, "standardComment", StandardComment.FREE_TEXT_STANDARD_COMMENT);
      double value = XmlParse.doubleAttribute(element, "value");
      String text = standardComment == StandardComment.FREE_TEXT_STANDARD_COMMENT ? element.getText() : "";
      return new Comment(Instant.ofEpochMilli(epochMilli), standardComment, value, text);
   }

   Element toXml() {
      Element element = DocumentFactory.getInstance().createElement("comment")
            .addAttribute("time", Utils.toString(time.toEpochMilli() / 1000.0))
            .addAttribute("value", Utils.toString(value));
      if (standardComment == StandardComment.FREE_TEXT_STANDARD_COMMENT) {
         element.addText(text);
      } else {
         element.addAttribute("standardComment", Integer.toString(standardComment));
      }
      return element;
   }
}
