package no.imr.korona.region.schooledit;

import no.imr.korona.data.util.geometry.EchogramPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class SchoolEditUtils {
   private SchoolEditUtils() {
   }

   static List<List<EchogramPoint>> leftToRightLists(Collection<EchogramPoint> echogramPoints) {
      if (echogramPoints.isEmpty()) {
         return List.of();
      }
      List<List<EchogramPoint>> leftToRightLists = new ArrayList<>();
      List<EchogramPoint> currentList = new ArrayList<>();
      EchogramPoint lastPoint = null;
      boolean leftToRight = true;
      for (EchogramPoint echogramPoint : echogramPoints) {
         if (lastPoint != null) {
            if (leftToRight && echogramPoint.pingIndex().compareTo(lastPoint.pingIndex()) < 0) {
               leftToRight = false;
               if (!currentList.isEmpty()) {
                  leftToRightLists.add(currentList);
                  currentList = new ArrayList<>();
                  currentList.add(lastPoint);
               }
            }
            if (!leftToRight && echogramPoint.pingIndex().compareTo(lastPoint.pingIndex()) > 0) {
               leftToRight = true;
               if (!currentList.isEmpty()) {
                  Collections.reverse(currentList);
                  leftToRightLists.add(currentList);
                  currentList = new ArrayList<>();
                  currentList.add(lastPoint);
               }
            }
         }
         currentList.add(echogramPoint);
         lastPoint = echogramPoint;
      }
      //add last segment
      if (!currentList.isEmpty()) {
         if (!leftToRight) {
            Collections.reverse(currentList);
         }
         leftToRightLists.add(currentList);
      }
      return leftToRightLists;
   }
}
