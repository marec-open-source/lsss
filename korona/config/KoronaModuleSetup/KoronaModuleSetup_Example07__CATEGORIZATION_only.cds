<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">CATEGORIZATION ONLY. Last changed 2023.02.28</parameter>
            <parameter name="Comment">Categorization = "species identification"
</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">6. SCHOOL DETECTION</parameter>
            <parameter name="Comment">The modules between TemporaryComputationsBeginModule and TemporaryComputationsEndModule do school detection.</parameter>
         </parameters>
      </module>
      <module name="TemporaryComputationsBeginModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">The two following ThresholdModule do the thresholding at -120 and -20 dB. This means: all values stronger than -20 dB are set to -20 dB, and
all values weaker than -120 dB are set to -120 dB. The reason for this is that the RegionModule stops school detection if there is any point stronger
than -20 dB or weaker than -120 dB. This is not really a bug in RegionModule, but is unintended. In a future release of LSSS, the two ThresholdModule
will probably have no function. </parameter>
         </parameters>
      </module>
      <module name="ThresholdModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">1000</parameter>
            <parameter name="Threshold">-20</parameter>
            <parameter name="CutBelow">false</parameter>
            <parameter name="CutAbove">true</parameter>
            <parameter name="BelowVal">-120</parameter>
            <parameter name="AboveVal">-20</parameter>
         </parameters>
      </module>
      <module name="ThresholdModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">1000</parameter>
            <parameter name="Threshold">-120</parameter>
            <parameter name="CutBelow">true</parameter>
            <parameter name="CutAbove">false</parameter>
            <parameter name="BelowVal">-120</parameter>
            <parameter name="AboveVal">0</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">Do some slight smoothing to improve school detection.</parameter>
         </parameters>
      </module>
      <module name="SmootherModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="OnlyLastChannel">false</parameter>
            <parameter name="MaskPelagic">false</parameter>
            <parameter name="MaskBottom">true</parameter>
            <parameter name="MaskSecondBottom">true</parameter>
            <parameter name="MaskNoise">false</parameter>
            <parameter name="MaskRegion">none</parameter>
            <parameter name="MaskTrack">none</parameter>
            <parameter name="MinPing">0</parameter>
            <parameter name="MaxPing">2</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">25</parameter>
            <parameter name="VerticalWidth">0.25</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">The ExpressionModule generates a new channel intended for school detection. There are alternative expressions: the first is used if possible, then the next,
and finally the last. The last one is C1 ("channel 1") that always exist. This makes the module setup more portable between ships.</parameter>
         </parameters>
      </module>
      <module name="ExpressionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Expression">
               <value>(C1+C2+C3+C4)/4</value>
               <value>(C1+C2+C3)/3</value>
               <value>(C1+C2)/2</value>
               <value>C1</value>
            </parameter>
         </parameters>
      </module>
      <module name="SchoolDetectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="ProcessLast">true</parameter>
            <parameter name="Channel">21</parameter>
            <parameter name="MinDepth">10</parameter>
            <parameter name="MaxDepth"/>
            <parameter name="Threshold">-62</parameter>
            <parameter name="Density">
               <parameter name="min">-120</parameter>
               <parameter name="max">-20</parameter>
            </parameter>
            <parameter name="MaxSv">
               <parameter name="min"/>
               <parameter name="max"/>
            </parameter>
            <parameter name="Length">
               <parameter name="min">2</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Thickness">
               <parameter name="min">2</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Area">
               <parameter name="min">2</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Compactness">
               <parameter name="min"/>
               <parameter name="max"/>
            </parameter>
            <parameter name="FillHoles">true</parameter>
         </parameters>
      </module>
      <module name="TemporaryComputationsEndModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">7. ACOUSTIC LIBRARY CATEGORIZATION ("species identification")</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="TemporaryComputationsBeginModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">Smooth data inside a school to improve categorization.</parameter>
         </parameters>
      </module>
      <module name="SmootherModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="OnlyLastChannel">false</parameter>
            <parameter name="MaskPelagic">false</parameter>
            <parameter name="MaskBottom">false</parameter>
            <parameter name="MaskSecondBottom">true</parameter>
            <parameter name="MaskNoise">false</parameter>
            <parameter name="MaskRegion">outside</parameter>
            <parameter name="MaskTrack">none</parameter>
            <parameter name="MinPing">0</parameter>
            <parameter name="MaxPing">100</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">25</parameter>
            <parameter name="VerticalWidth">2</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="CategorizationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="CategoryCount">3</parameter>
            <parameter name="ActiveFeatures">
               <parameter name="R18">true</parameter>
               <parameter name="R70">true</parameter>
               <parameter name="R120">true</parameter>
               <parameter name="R200">true</parameter>
               <parameter name="R333">false</parameter>
               <parameter name="Sv38">true</parameter>
            </parameter>
            <parameter name="ActiveCategories">
               <parameter name="bottom">true</parameter>
               <parameter name="blindzone">true</parameter>
               <parameter name="noise">true</parameter>
               <parameter name="mackerel">true</parameter>
               <parameter name="herring">true</parameter>
               <parameter name="resonant_18">true</parameter>
               <parameter name="horse_mack">false</parameter>
               <parameter name="capelin">false</parameter>
               <parameter name="squid">false</parameter>
               <parameter name="krill_north">true</parameter>
               <parameter name="krill_south">false</parameter>
               <parameter name="salp_south">false</parameter>
               <parameter name="sandeel">false</parameter>
               <parameter name="tobis1">false</parameter>
               <parameter name="tobis2">false</parameter>
               <parameter name="amphipod_south">false</parameter>
               <parameter name="mac_thyssanoessa">false</parameter>
               <parameter name="blue_whiting">false</parameter>
               <parameter name="gobies">false</parameter>
               <parameter name="reds">false</parameter>
               <parameter name="mags">false</parameter>
               <parameter name="amphipod_libel">false</parameter>
               <parameter name="krill_thyssanoessa">true</parameter>
               <parameter name="herring_nvg">false</parameter>
               <parameter name="cod">false</parameter>
               <parameter name="capelin_2">false</parameter>
               <parameter name="anchovy">false</parameter>
               <parameter name="sardine">false</parameter>
            </parameter>
            <parameter name="Categorizer">Gauss</parameter>
            <parameter name="Discriminant">Aposteriori</parameter>
            <parameter name="ContextualCorrection">ICM</parameter>
            <parameter name="ContextualIterations">3</parameter>
            <parameter name="UseMinLogSv">true</parameter>
            <parameter name="MinLogSv">-100</parameter>
         </parameters>
      </module>
      <module name="SchoolCategorizationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="UseCellCategorization">false</parameter>
            <parameter name="Horizontal cell size">16</parameter>
            <parameter name="Vertical cell size">25</parameter>
            <parameter name="Upper sample offset">5</parameter>
            <parameter name="Lower sample offset">5</parameter>
            <parameter name="HorizontalOffset">0</parameter>
            <parameter name="SecondaryCategoryFraction">0.1</parameter>
            <parameter name="Fill factor for cells">0.5</parameter>
            <parameter name="MinFractionCategorizedCells">0.5</parameter>
            <parameter name="MinCells">3</parameter>
            <parameter name="SchoolCategorizationDistribution">CELL</parameter>
            <parameter name="EnableUncategorizedCategory">false</parameter>
            <parameter name="EnableUnknownCategory">false</parameter>
         </parameters>
      </module>
      <module name="TemporaryComputationsEndModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">8. MODEL BASED IDENTIFICATION ("species identification")</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="PlanktonInversionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="VerticalUnit">SAMPLES</parameter>
            <parameter name="PingsPerBin">7</parameter>
            <parameter name="DepthSamplesPerBin">30</parameter>
            <parameter name="DepthDurationPerBin">4</parameter>
            <parameter name="DepthDistancePerBin">6</parameter>
            <parameter name="Use global noise threshold">true</parameter>
            <parameter name="Noise threshold">-90</parameter>
            <parameter name="Use min depth">false</parameter>
            <parameter name="Min depth">20</parameter>
            <parameter name="Use max depth">false</parameter>
            <parameter name="Max depth">110</parameter>
            <parameter name="CategoriesUsedForInversion">
               <parameter name="unknown">true</parameter>
               <parameter name="bottom">false</parameter>
               <parameter name="blindzone">false</parameter>
               <parameter name="noise">false</parameter>
               <parameter name="mackerel">true</parameter>
               <parameter name="herring">false</parameter>
               <parameter name="resonant_18">true</parameter>
               <parameter name="horse_mack">false</parameter>
               <parameter name="capelin">false</parameter>
               <parameter name="squid">false</parameter>
               <parameter name="krill_north">false</parameter>
               <parameter name="krill_south">false</parameter>
               <parameter name="salp_south">false</parameter>
               <parameter name="sandeel">false</parameter>
               <parameter name="tobis1">false</parameter>
               <parameter name="tobis2">false</parameter>
               <parameter name="amphipod_south">false</parameter>
               <parameter name="mac_thyssanoessa">false</parameter>
               <parameter name="blue_whiting">false</parameter>
               <parameter name="gobies">false</parameter>
               <parameter name="reds">false</parameter>
               <parameter name="mags">false</parameter>
               <parameter name="amphipod_libel">false</parameter>
               <parameter name="krill_thyssanoessa">false</parameter>
               <parameter name="herring_nvg">false</parameter>
               <parameter name="cod">false</parameter>
               <parameter name="capelin_2">false</parameter>
               <parameter name="anchovy">false</parameter>
               <parameter name="sardine">false</parameter>
            </parameter>
            <parameter name="UseAllFrequencies">true</parameter>
            <parameter name="ActiveFrequencies">
               <parameter name="18">true</parameter>
               <parameter name="38">true</parameter>
               <parameter name="70">true</parameter>
               <parameter name="120">true</parameter>
               <parameter name="200">true</parameter>
               <parameter name="333">false</parameter>
               <parameter name="364">false</parameter>
               <parameter name="500">false</parameter>
            </parameter>
            <parameter name="MinResidualErrorThreshold">0.01</parameter>
            <parameter name="MaxResidualErrorThreshold">0.2</parameter>
            <parameter name="LevenbergMarquardtFactor">5E-4</parameter>
            <parameter name="MaxIter">4</parameter>
            <parameter name="HardShelled">true</parameter>
            <parameter name="HardShelledR">0.5</parameter>
            <parameter name="GaseousSphere">true</parameter>
            <parameter name="GasSphereG">0.0012</parameter>
            <parameter name="GasSphereH">0.22</parameter>
            <parameter name="FluidSpheriod">true</parameter>
            <parameter name="FluidSpheriodG">1.043</parameter>
            <parameter name="FluidSpheriodH">1.052</parameter>
            <parameter name="FluidSpheriodBeta">5</parameter>
            <parameter name="FluidBent">true</parameter>
            <parameter name="FluidBentR">0.058</parameter>
            <parameter name="FluidBentS">0.1</parameter>
            <parameter name="FluidBentBeta">11</parameter>
            <parameter name="FluidBent2">false</parameter>
            <parameter name="FluidBentR2">0.058</parameter>
            <parameter name="FluidBentS2">0.1</parameter>
            <parameter name="FluidBentBeta 2">11</parameter>
            <parameter name="SDWBA">false</parameter>
            <parameter name="SDWBA coeff">N(0, 30)</parameter>
            <parameter name="SDWBA2">false</parameter>
            <parameter name="SDWBA 2 coeff">N(0, 30)</parameter>
            <parameter name="SDWBA3">false</parameter>
            <parameter name="SDWBA 3 coeff">N(0, 30)</parameter>
            <parameter name="SDWBA4">false</parameter>
            <parameter name="SDWBA 4 coeff">N(0, 30)</parameter>
            <parameter name="SDWBA5">false</parameter>
            <parameter name="SDWBA 5 coeff">N(0, 30)</parameter>
            <parameter name="SDWBAS">false</parameter>
            <parameter name="SDWBA smooth coeff">N(4, 2)</parameter>
            <parameter name="PlotResolution">0.01</parameter>
            <parameter name="PlotMaxRange">5</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
