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

   public void setParameterEnabledDecider(Predicate<BaseParameter<?>> parameterEnabledDecider) {
      this.parameterEnabledDecider = parameterEnabledDecider;
   }

   public int getTextInputColumns() {
      return textInputColumns;
   }

   public void setTextInputColumns(int textInputColumns) {
      this.textInputColumns = textInputColumns;
   }

   public boolean getHorizontalFill(BaseParameter<?> parameter) {
      return horizontalFill.apply(parameter);
   }

   public void setHorizontalFill(boolean fill) {
      horizontalFill = _ -> fill;
   }

   public Function<BaseParameter<?>, Alignment> getTextAlignment() {
      return textAlignment;
   }

   public void setTextAlignment(Function<BaseParameter<?>, Alignment> textAlignment) {
      this.textAlignment = textAlignment;
   }

   public void setTextAlignment(Alignment alignment) {
      setTextAlignment(_ -> alignment);
   }

   public Alignment getInputFieldAlignment() {
      return inputFieldAlignment;
   }

   public void setInputFieldAlignment(Alignment alignment) {
      inputFieldAlignment = alignment;
   }

   public boolean combineInputAndDescription(BaseParameter<?> parameter) {
      return combineInputAndDescription.apply(parameter);
   }

   public void setCombineInputAndDescription(Function<BaseParameter<?>, Boolean> combineInputAndDescription) {
      this.combineInputAndDescription = combineInputAndDescription;
   }

   public void setCombineInputAndDescription(boolean combine) {
      setCombineInputAndDescription(_ -> combine);
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
