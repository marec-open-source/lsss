package no.imr.tools.parameter;

import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RangeParameterTest {
   @Test
   void minMax() {
      RangeParameter p = new RangeParameter(new Name("name"),
            0, 0, Unit.NONE);
      p.setMin(2);
      assertEquals(FloatRange.of(2, 2), p.getValue());
      p.setMin(1);
      assertEquals(FloatRange.of(1, 2), p.getValue());

      p.setMax(1);
      assertEquals(FloatRange.of(1, 1), p.getValue());
      p.setMax(0);
      assertEquals(FloatRange.of(0, 0), p.getValue());
   }

   @Test
   void xml() {
      RangeParameter p1 = new RangeParameter(new Name("name"),
            1, 2, Unit.NONE);
      Element xml = p1.toXml();
      assertEquals("""
            <parameter name="name"><parameter name="min">1</parameter><parameter name="max">2</parameter></parameter>""", XmlUtils.toCompactString(xml));

      RangeParameter p2 = new RangeParameter(new Name("name"),
            0, 0, Unit.NONE);
      assertEquals(FloatRange.of(0, 0), p2.getValue());
      p2.fromXml(xml);
      assertEquals(FloatRange.of(1, 2), p2.getValue());
   }
}
