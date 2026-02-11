package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public sealed interface GraphicalObject permits OffsetPolygon {

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

   GraphicalObjectType getType();

   List<String> texts();

   default void write(ByteBuffer byteBuffer) {
      writeContent(byteBuffer);
   }

   void writeContent(ByteBuffer byteBuffer);
}
