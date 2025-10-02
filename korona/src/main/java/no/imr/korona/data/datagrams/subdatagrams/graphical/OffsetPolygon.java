package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.List;

public final class OffsetPolygon extends GraphicalObject {
   public static final GraphicalObjectType TYPE = new GraphicalObjectType(GraphicalObjectTypes.OFFSET_POLYGON, OffsetPolygon::new);

   private final Color color;
   private final boolean filled;
   private final List<EchogramOffsetPoint> points;

   public OffsetPolygon(List<String> texts, Color color, boolean filled, List<EchogramOffsetPoint> points) {
      super(texts);

      this.color = color;
      this.filled = filled;
      this.points = points;
   }

   private OffsetPolygon(ByteBuffer byteBuffer) throws DatagramFormatException {
      super(readTexts(byteBuffer));

      int rgb = byteBuffer.getInt();
      color = new Color(rgb);
      filled = byteBuffer.get() != 0;
      points = ByteBufferUtils.readCountAndList(byteBuffer, 4 + 4, EchogramOffsetPoint::new);
   }

   @Override
   protected void writeContent(ByteBuffer byteBuffer) {
      writeTexts(byteBuffer, getTexts());
      byteBuffer.putInt(color.getRGB());
      byteBuffer.put((byte) (filled ? 1 : 0));
      ByteBufferUtils.writeCountAndList(byteBuffer, points, EchogramOffsetPoint::write);
   }

   @Override
   protected GraphicalObjectType getType() {
      return TYPE;
   }

   public List<EchogramOffsetPoint> getPoints() {
      return points;
   }

   public Color getColor() {
      return color;
   }

   public boolean isFilled() {
      return filled;
   }
}
