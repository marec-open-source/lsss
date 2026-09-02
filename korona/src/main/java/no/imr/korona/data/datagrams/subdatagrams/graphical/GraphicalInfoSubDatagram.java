package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class GraphicalInfoSubDatagram extends BaseSubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.GRAPHICAL_INFO,
         "Graphical info", GraphicalInfoSubDatagram::new);

   private final List<GraphicalObject> graphicalObjects;
   private final List<String> texts;

   public GraphicalInfoSubDatagram(Instant instant) {
      super(instant);

      texts = new ArrayList<>();
      graphicalObjects = new ArrayList<>();
   }

   private GraphicalInfoSubDatagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      texts = GraphicalObject.readTexts(byteBuffer);
      graphicalObjects = ByteBufferUtils.readCountAndList(byteBuffer, 4, GraphicalInfoSubDatagram::readGraphicalObject);
   }

   private static GraphicalObject readGraphicalObject(ByteBuffer byteBuffer) throws DatagramFormatException {
      int typeId = byteBuffer.getInt();
      GraphicalObjectType graphicalObjectType = GraphicalObjectTypes.ID_TO_TYPE.get(typeId);
      if (graphicalObjectType == null) {
         throw new DatagramFormatException("Unknown type id: " + typeId);
      }
      return graphicalObjectType.reader().read(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      GraphicalObject.writeTexts(byteBuffer, texts);
      ByteBufferUtils.writeCountAndList(byteBuffer, graphicalObjects, GraphicalInfoSubDatagram::writeGraphicalObject);
   }

   private static void writeGraphicalObject(GraphicalObject graphicalObject, ByteBuffer byteBuffer) {
      byteBuffer.putInt(graphicalObject.getType().id());
      graphicalObject.write(byteBuffer);
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   public void addGraphicalObject(GraphicalObject graphicalObject) {
      graphicalObjects.add(graphicalObject);
   }

   public List<GraphicalObject> getGraphicalObjects() {
      return graphicalObjects;
   }

   public void addText(String text) {
      texts.add(text);
   }

   public List<String> getTexts() {
      return texts;
   }
}
