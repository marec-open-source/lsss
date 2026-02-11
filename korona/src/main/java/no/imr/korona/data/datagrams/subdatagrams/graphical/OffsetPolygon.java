package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.List;

public record OffsetPolygon(
      List<String> texts,
      Color color,
      boolean filled,
      List<EchogramOffsetPoint> points
) implements GraphicalObject {

   public static final GraphicalObjectType TYPE = new GraphicalObjectType(GraphicalObjectTypes.OFFSET_POLYGON, OffsetPolygon::new);

   private OffsetPolygon(ByteBuffer byteBuffer) throws DatagramFormatException {
      this(
            GraphicalObject.readTexts(byteBuffer),
            new Color(byteBuffer.getInt()),
            byteBuffer.get() != 0,
            ByteBufferUtils.readCountAndList(byteBuffer, 4 + 4, EchogramOffsetPoint::new)
      );
   }

   @Override
   public void writeContent(ByteBuffer byteBuffer) {
      GraphicalObject.writeTexts(byteBuffer, texts);
      byteBuffer.putInt(color.getRGB());
      byteBuffer.put((byte) (filled ? 1 : 0));
      ByteBufferUtils.writeCountAndList(byteBuffer, points, EchogramOffsetPoint::write);
   }

   @Override
   public GraphicalObjectType getType() {
      return TYPE;
   }
}
