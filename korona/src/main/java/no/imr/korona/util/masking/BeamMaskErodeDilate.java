package no.imr.korona.util.masking;

public final class BeamMaskErodeDilate {
   private BeamMaskErodeDilate() {
   }

   /**
    * Replace center value with "outside" value if at least one value is outside within the filter width.
    *
    * @param beamMask    the input mask
    * @param filterWidth the filter width
    * @return the eroded beam mask
    */
   public static FullBeamMask erode(FullBeamMask beamMask, int filterWidth) {
      FullBeamMask result = new FullBeamMask(beamMask);
      for (int sample = 0; sample < beamMask.size(); sample++) {
         if (beamMask.isInside(sample)) {
            int begin = Math.max(0, sample - filterWidth / 2);
            int end = Math.min(begin + filterWidth, beamMask.size());
            if (!beamMask.isAllInside(begin, end)) {
               result.setOutside(sample);
            }
         }
      }
      return result;
   }

   /**
    * Replace center value with "inside" value if at least one value is inside within the filter width.
    *
    * @param beamMask    the input mask
    * @param filterWidth the filter width
    * @return the dilated beam mask
    */
   public static FullBeamMask dilate(FullBeamMask beamMask, int filterWidth) {
      FullBeamMask result = new FullBeamMask(beamMask);
      for (int sample = 0; sample < beamMask.size(); sample++) {
         if (!beamMask.isInside(sample)) {
            int begin = Math.max(0, sample - filterWidth / 2);
            int end = Math.min(begin + filterWidth, beamMask.size());
            if (beamMask.isAnyInside(begin, end)) {
               result.setInside(sample);
            }
         }
      }
      return result;
   }
}
