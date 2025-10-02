package no.imr.deepvision.lsss.engine.data;

public enum Direction {
   LEFT("Left"), RIGHT("Right");

   final String dirName;

   Direction(String dirName) {
      this.dirName = dirName;
   }
}
