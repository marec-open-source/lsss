package no.imr.lsss.framework.config.survey;

import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.DoubleCsvListParameter;
import no.imr.tools.parameter.DoubleParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * For configuring the interpretation grid.
 */
public final class GridConf extends ConfigurationUnit {
   private static final Unit HORIZONTAL_GRID_SIZE_UNIT = new Unit("In units of horizontal grid size");

   private final HeaderParameter databaseGridHeader = new HeaderParameter("Database grid");

   public final ObjectParameter<PingMapping> horizontalGridUnit = new ObjectParameter<>(
         new Name("HorizontalGridUnit", "Unit of horizontal grid"),
         PingMapping.DISTANCE, PingMapping.values(),
         "[nmi, ping, sec]") {
      @Override
      public String toString(PingMapping pingMapping) {
         return pingMapping.getUnitString();
      }
   };

   private final SeparatorParameter horizontalSeparator = SeparatorParameter.line();

   public final DoubleParameter horizontalGridSize = new DoubleParameter(
         new Name("HorizontalGridSize", "Horizontal grid size"),
         1, HORIZONTAL_GRID_SIZE_UNIT, ValueConstraints.gt(0.0),
         "Echogram channels");

   public final DoubleParameter minHorizontalGridSize = new DoubleParameter(
         new Name("MinHorizontalGridSize", "Minimum horizontal grid size"),
         0.1, HORIZONTAL_GRID_SIZE_UNIT, ValueConstraints.gt(0.0),
         "Used when storing next to an excluded interval");

   private final SeparatorParameter schoolHorizontalSeparator = SeparatorParameter.space();

   public final DoubleParameter schoolHorizontalGridSize = new DoubleParameter(
         new Name("SchoolHorizontalGridSize", "School horizontal grid size"),
         0.05, HORIZONTAL_GRID_SIZE_UNIT, ValueConstraints.gt(0.0),
         "Should evenly divide the echogram horizontal grid size");

   private final SeparatorParameter verticalSeparator = SeparatorParameter.line();

   public final FloatParameter verticalGridSizePelagic = new FloatParameter(
         new Name("VerticalGridSizePelagic", "Echogram vertical grid size - pelagic"),
         10, Unit.METER, ValueConstraints.gt(0f));

   public final FloatParameter verticalGridSizeBottom = new FloatParameter(
         new Name("VerticalGridSizeBottom", "Echogram vertical grid size - bottom"),
         5, Unit.METER, ValueConstraints.gt(0f));

   private final SeparatorParameter verticalSchoolSeparator = SeparatorParameter.line();

   public final FloatParameter schoolVerticalGridSizePelagic = new FloatParameter(
         new Name("SchoolVerticalGridSizePelagic", "School vertical grid size - pelagic"),
         5f, Unit.METER, ValueConstraints.gt(0f));

   public final FloatParameter schoolVerticalGridSizeBottom = new FloatParameter(
         new Name("SchoolVerticalGridSizeBottom", "School vertical grid size - bottom"),
         1f, Unit.METER, ValueConstraints.gt(0f));

   private final SeparatorParameter verticalExtentSeparator = SeparatorParameter.line();

   public final FloatParameter verticalExtentBottom = new FloatParameter(
         new Name("VerticalExtentBottom", "Vertical extent - bottom"),
         10, Unit.METER, ValueConstraints.gt(0f),
         "Bottom grids go from z = 0 up to minus this value");

   private final HeaderParameter displaySettingsHeader = new HeaderParameter("Display settings");

   public final DoubleCsvListParameter preferredHorizontalSizes = new DoubleCsvListParameter(
         new Name("PreferredHorizontalSize", "Echogram horizontal display - preferred size"),
         List.of(5.0), HORIZONTAL_GRID_SIZE_UNIT,
         "Comma-separated list");

   public final BooleanParameter automaticDetailMode = new BooleanParameter(
         new Name("AutomaticDetailMode", "Automatic DETAIL mode"),
         false,
         "Automatically select DETAIL data loading mode when pressing a preferred size button");

   GridConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("GridConf", "Grid"),
            "The grid used when storing to the database");
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            databaseGridHeader,
            horizontalGridUnit,
            //---
            horizontalSeparator,
            horizontalGridSize,
            minHorizontalGridSize,
            //---
            schoolHorizontalSeparator,
            schoolHorizontalGridSize,
            //---
            verticalSeparator,
            verticalGridSizePelagic,
            verticalGridSizeBottom,
            //---
            verticalSchoolSeparator,
            schoolVerticalGridSizePelagic,
            schoolVerticalGridSizeBottom,
            //---
            verticalExtentSeparator,
            verticalExtentBottom,
            //---
            displaySettingsHeader,
            preferredHorizontalSizes,
            automaticDetailMode
      );
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      return UserProfile.SURVEY_SETUP;
   }

   public boolean doesSchoolGridEvenlyDivideEchogramGrid() {
      BigDecimal schoolGrid = BigDecimal.valueOf(schoolHorizontalGridSize.getDoubleValue());
      BigDecimal echogramGrid = BigDecimal.valueOf(horizontalGridSize.getDoubleValue());
      try {
         return echogramGrid.divide(schoolGrid, RoundingMode.UNNECESSARY).stripTrailingZeros().scale() <= 0;
      } catch (ArithmeticException _) {
         return false;
      }
   }
}
