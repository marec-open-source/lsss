package no.imr.tools.parameter;

/**
 * A "parameter" for creating a separation in the generated gui.
 */
public final class SeparatorParameter extends VoidParameter {
   public enum SeparatorType {
      LINE, SPACE
   }

   private final SeparatorType separatorType;

   private SeparatorParameter(SeparatorType separatorType) {
      super(new Name("NoName", ""), Unit.NONE, "");

      this.separatorType = separatorType;
   }

   public static SeparatorParameter line() {
      return new SeparatorParameter(SeparatorType.LINE);
   }

   public static SeparatorParameter space() {
      return new SeparatorParameter(SeparatorType.SPACE);
   }

   public SeparatorType getSeparatorType() {
      return separatorType;
   }
}
