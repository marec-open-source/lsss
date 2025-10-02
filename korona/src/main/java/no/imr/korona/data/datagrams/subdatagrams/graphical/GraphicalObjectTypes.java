package no.imr.korona.data.datagrams.subdatagrams.graphical;

import java.util.Map;

final class GraphicalObjectTypes {
   static final int OFFSET_POLYGON = 1;

   static final Map<Integer, GraphicalObjectType> ID_TO_TYPE = Map.of(
         OffsetPolygon.TYPE.id(), OffsetPolygon.TYPE
   );

   private GraphicalObjectTypes() {
   }
}
