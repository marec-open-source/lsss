package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract sealed class GraphicalObject permits OffsetPolygon {
   private final List<String> texts;

   GraphicalObject(List<String> texts) {
      this.texts = texts;
   }

   static List<String> readTexts(ByteBuffer byteBuffer) throws DatagramFormatException {
      int n = ByteBufferUtils.readCount(byteBuffer, 1);
      List<String> text = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
         text.add(ByteBufferUtils.readCString(byteBuffer));
      }
      return text;
   }

   static void writeTexts(ByteBuffer byteBuffer, List<String> texts) {
      byteBuffer.putInt(texts.size());
      for (String text : texts) {
         ByteBufferUtils.writeCString(byteBuffer, text);
      }
   }

   protected abstract GraphicalObjectType getType();

   public void write(ByteBuffer byteBuffer) {
      writeContent(byteBuffer);
   }

   protected abstract void writeContent(ByteBuffer byteBuffer);

   public List<String> getTexts() {
      return texts;
   }
}
