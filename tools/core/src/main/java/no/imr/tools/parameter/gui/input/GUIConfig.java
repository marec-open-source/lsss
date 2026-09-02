package no.imr.tools.parameter.gui.input;

import com.google.common.util.concurrent.Runnables;
import no.imr.tools.parameter.BaseParameter;

import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.util.function.Function;
import java.util.function.Predicate;

public final class GUIConfig {
   private Predicate<BaseParameter<?>> parameterEnabledDecider = _ -> true;
   private int textInputColumns = 10;
   private Function<BaseParameter<?>, Boolean> horizontalFill = parameter -> parameter.getProperty(BaseParameter.KEY_HORIZONTAL_FILL);
   private Function<BaseParameter<?>, Alignment> textAlignment = parameter -> parameter.getProperty(BaseParameter.KEY_LEFT_ALIGNED) ? Alignment.LEFT : Alignment.RIGHT;
   private Alignment inputFieldAlignment = Alignment.RIGHT;
   private Function<BaseParameter<?>, Boolean> combineInputAndDescription = parameter -> parameter.getProperty(BaseParameter.KEY_COMBINE_INPUT_AND_DESCRIPTION);
   private Runnable doRelayout = Runnables.doNothing();

   public GUIConfig() {
   }

   public boolean isParameterEnabled(BaseParameter<?> parameter) {
      return parameter.isEnabled() && parameterEnabledDecider.test(parameter);
   }

   public GUIConfig setParameterEnabledDecider(Predicate<BaseParameter<?>> parameterEnabledDecider) {
      this.parameterEnabledDecider = parameterEnabledDecider;
      return this;
   }

   public int getTextInputColumns() {
      return textInputColumns;
   }

   public GUIConfig setTextInputColumns(int textInputColumns) {
      this.textInputColumns = textInputColumns;
      return this;
   }

   public boolean getHorizontalFill(BaseParameter<?> parameter) {
      return horizontalFill.apply(parameter);
   }

   public GUIConfig setHorizontalFill(boolean fill) {
      horizontalFill = _ -> fill;
      return this;
   }

   public Function<BaseParameter<?>, Alignment> getTextAlignment() {
      return textAlignment;
   }

   public GUIConfig setTextAlignment(Function<BaseParameter<?>, Alignment> textAlignment) {
      this.textAlignment = textAlignment;
      return this;
   }

   public GUIConfig setTextAlignment(Alignment alignment) {
      return setTextAlignment(_ -> alignment);
   }

   public Alignment getInputFieldAlignment() {
      return inputFieldAlignment;
   }

   public GUIConfig setInputFieldAlignment(Alignment alignment) {
      inputFieldAlignment = alignment;
      return this;
   }

   public boolean combineInputAndDescription(BaseParameter<?> parameter) {
      return combineInputAndDescription.apply(parameter);
   }

   public GUIConfig setCombineInputAndDescription(Function<BaseParameter<?>, Boolean> combineInputAndDescription) {
      this.combineInputAndDescription = combineInputAndDescription;
      return this;
   }

   public GUIConfig setCombineInputAndDescription(boolean combine) {
      return setCombineInputAndDescription(_ -> combine);
   }

   public Runnable getDoRelayout() {
      return doRelayout;
   }

   public void setDoRelayout(Runnable doRelayout) {
      this.doRelayout = doRelayout;
   }

   public enum Alignment {
      LEFT(GridBagConstraints.WEST, JTextField.LEFT),
      RIGHT(GridBagConstraints.EAST, JTextField.RIGHT);

      private final int gridBagConstraintsAnchor;
      private final int textFieldHorizontalAlignment;

      Alignment(int gridBagConstraintsAnchor, int textFieldHorizontalAlignment) {
         this.gridBagConstraintsAnchor = gridBagConstraintsAnchor;
         this.textFieldHorizontalAlignment = textFieldHorizontalAlignment;
      }

      public int getGridBagConstraintsAnchor() {
         return gridBagConstraintsAnchor;
      }

      public int getTextFieldHorizontalAlignment() {
         return textFieldHorizontalAlignment;
      }
   }
}
