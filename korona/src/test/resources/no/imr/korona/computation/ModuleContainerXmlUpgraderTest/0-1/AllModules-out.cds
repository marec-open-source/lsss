<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="1">
   <parameters>
      <parameter name="UseTempFile">true</parameter>
   </parameters>
   <modules>
      <module name="AngleDeletionModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="BadDataModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="StartValue">10</parameter>
            <parameter name="EndValue">5</parameter>
            <parameter name="MaxBuffer">25</parameter>
            <parameter name="Debug">false</parameter>
         </parameters>
      </module>
      <module name="BufferingObserverModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CalibrationModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CategorizationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="CategoryCount">3</parameter>
            <parameter name="BlindZoneDepth">5</parameter>
            <parameter name="Categorizer">Gauss</parameter>
            <parameter name="Discriminant">Aposteriori</parameter>
            <parameter name="ContextualCorrection">ICM</parameter>
            <parameter name="ContextualIterations">3</parameter>
         </parameters>
      </module>
      <module name="CdsViewerModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CombinationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="FirstOperandKhz">0</parameter>
            <parameter name="SecondOperandKhz">0</parameter>
            <parameter name="FirstOperandChannel">1</parameter>
            <parameter name="SecondOperandChannel">2</parameter>
            <parameter name="LogarithmicOperands">false</parameter>
            <parameter name="Operation">mean</parameter>
         </parameters>
      </module>
      <module name="CombinationRemoverModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="DataReductionModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="DepthModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Algorithm">EK500</parameter>
            <parameter name="MinDepthLimit">10</parameter>
            <parameter name="MinDepthValueFraction">1E-5</parameter>
            <parameter name="SignalStrengthThreshold">-35</parameter>
            <parameter name="MinimumDepthThresholdFactor">0.99</parameter>
            <parameter name="MaxRangeFactor">2</parameter>
         </parameters>
      </module>
      <module name="DilateModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="VertResolution">1</parameter>
         </parameters>
      </module>
      <module name="DownSampleModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="TimeBinSize">60</parameter>
            <parameter name="DepthBinSize">10</parameter>
         </parameters>
      </module>
      <module name="ES60CorrectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="PerformScan">false</parameter>
            <parameter name="ContinueAcrossFiles">false</parameter>
         </parameters>
      </module>
      <module name="EdgeDetectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="ScaleFactor">1</parameter>
            <parameter name="VertResolution">1</parameter>
         </parameters>
      </module>
      <module name="ErodeModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="VertResolution">1</parameter>
         </parameters>
      </module>
      <module name="ExpressionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Expression">(C1 + C2) / 2</parameter>
         </parameters>
      </module>
      <module name="FillMissingDataModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="Filter3X3Module">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="FilterType">User defined</parameter>
            <parameter name="00">1</parameter>
            <parameter name="01">1</parameter>
            <parameter name="02">1</parameter>
            <parameter name="10">1</parameter>
            <parameter name="11">1</parameter>
            <parameter name="12">1</parameter>
            <parameter name="20">1</parameter>
            <parameter name="21">1</parameter>
            <parameter name="22">1</parameter>
         </parameters>
      </module>
      <module name="FiskViewDisplayModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="HorizontalOffsetCorrectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="InfoModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="MessageInterval">100</parameter>
         </parameters>
      </module>
      <module name="IsolationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Category"/>
         </parameters>
      </module>
      <module name="MedianModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
         </parameters>
      </module>
      <module name="NoiseQuantificationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Smooth">running averager</parameter>
            <parameter name="SmoothInterval">5</parameter>
            <parameter name="Mask">dynamic</parameter>
            <parameter name="MinimumQuality">60</parameter>
            <parameter name="ThresholdMasking">false</parameter>
            <parameter name="Threshold">-80</parameter>
            <parameter name="Histogram">geometric</parameter>
            <parameter name="UseTimeStepBuffer">true</parameter>
            <parameter name="TimeStepBufferMaxSize">50</parameter>
            <parameter name="HistogramInitializationCellCount">500</parameter>
            <parameter name="HistogramMaximumCellCount">2000</parameter>
            <parameter name="HistogramInitializationSampleCount">1000</parameter>
            <parameter name="HistogramMinimumSampleCount">25000</parameter>
            <parameter name="HistogramSmooth">true</parameter>
            <parameter name="HistogramSmoothFactor">100</parameter>
         </parameters>
      </module>
      <module name="NoiseRemoverModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="RemoveNoiseFromStart">false</parameter>
            <parameter name="MaxBufferSize">10</parameter>
         </parameters>
      </module>
      <module name="NoiseVisualizationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="UpdatePeriod">20</parameter>
            <parameter name="TimeSeriesLength">200</parameter>
            <parameter name="HistogramSpan">3</parameter>
            <parameter name="MedianSampleCount">20</parameter>
         </parameters>
      </module>
      <module name="PingVisualizerModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="PlanktonInversionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="PingsPerBin">10</parameter>
            <parameter name="DepthSamplesPerBin">20</parameter>
            <parameter name="Use global noise threshold">true</parameter>
            <parameter name="Noise threshold">-90</parameter>
            <parameter name="MinResidualErrorThreshold">0.0010</parameter>
            <parameter name="MaxResidualErrorThreshold">0.1</parameter>
            <parameter name="LevenbergMarquardtFactor">1E-5</parameter>
            <parameter name="MaxIter">5</parameter>
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
            <parameter name="FluidBentS">0.06</parameter>
            <parameter name="FluidBentBeta">12</parameter>
            <parameter name="SDWBA">true</parameter>
            <parameter name="SDWBA coeff">N(4, 2)</parameter>
            <parameter name="PlotResolution">0.01</parameter>
            <parameter name="PlotMaxRange">5</parameter>
         </parameters>
      </module>
      <module name="RegionFilteringModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Thresholds">0</parameter>
            <parameter name="Density">
               <parameter name="min">-120</parameter>
               <parameter name="max">-20</parameter>
            </parameter>
            <parameter name="Length">
               <parameter name="min">0</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Thickness">
               <parameter name="min">0</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Area">
               <parameter name="min">0</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Overlap">
               <parameter name="min">0</parameter>
               <parameter name="max">100</parameter>
            </parameter>
            <parameter name="PrintInfo">false</parameter>
            <parameter name="PrintHistogram">false</parameter>
         </parameters>
      </module>
      <module name="RegionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="ProcessLast">false</parameter>
            <parameter name="Channel">1</parameter>
            <parameter name="Thresholds">-66</parameter>
            <parameter name="Density">
               <parameter name="min">-120</parameter>
               <parameter name="max">-20</parameter>
            </parameter>
            <parameter name="Length">
               <parameter name="min">5</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Thickness">
               <parameter name="min">5</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Area">
               <parameter name="min">15</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Overlap">
               <parameter name="min">0</parameter>
               <parameter name="max">100</parameter>
            </parameter>
         </parameters>
      </module>
      <module name="RemoveBottomModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="UseNeighbours">false</parameter>
            <parameter name="Above">0</parameter>
            <parameter name="Below">100</parameter>
            <parameter name="PixelValue">-120</parameter>
         </parameters>
      </module>
      <module name="RemoveSingleChannelModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="ChannelNumber">0</parameter>
         </parameters>
      </module>
      <module name="RescaleModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="RescaleChannels">all</parameter>
            <parameter name="DesiredMinimum">-85</parameter>
            <parameter name="DesiredMaximum">-30</parameter>
            <parameter name="MinMaxFraction">0.05</parameter>
            <parameter name="InitializationDatagramCount">10</parameter>
            <parameter name="mInitializationMaxPingCount">100</parameter>
         </parameters>
      </module>
      <module name="SchoolCategorizationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Horizontal cell size">20</parameter>
            <parameter name="Vertical cell size">40</parameter>
            <parameter name="Upper sample offset">5</parameter>
            <parameter name="Lower sample offset">5</parameter>
            <parameter name="Fill factor for cells">0.5</parameter>
         </parameters>
      </module>
      <module name="SmootherModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="MaskPelagic">false</parameter>
            <parameter name="MaskBottom">true</parameter>
            <parameter name="MaskNoise">false</parameter>
            <parameter name="MaskRegion">none</parameter>
            <parameter name="MinPing">0</parameter>
            <parameter name="MaxPing">10</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">8</parameter>
            <parameter name="VerticalWidth">0.5</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="SpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="TotalDelta">7</parameter>
            <parameter name="VerticalDelta">7</parameter>
            <parameter name="Debug">false</parameter>
         </parameters>
      </module>
      <module name="ThresholdModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="Threshold">-70</parameter>
            <parameter name="BelowSame">false</parameter>
            <parameter name="AboveSame">false</parameter>
            <parameter name="BelowVal">-120</parameter>
            <parameter name="AboveVal">0</parameter>
         </parameters>
      </module>
      <module name="TimeIntervalModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="StartDateString"/>
            <parameter name="EndDateString"/>
         </parameters>
      </module>
      <module name="TraceBorderModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">true</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">true</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="Channel">1</parameter>
            <parameter name="Density">
               <parameter name="min">-120</parameter>
               <parameter name="max">-20</parameter>
            </parameter>
            <parameter name="Length">
               <parameter name="min">0</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Thickness">
               <parameter name="min">0</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Area">
               <parameter name="min">0</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Overlap">
               <parameter name="min">0</parameter>
               <parameter name="max">100</parameter>
            </parameter>
            <parameter name="Threshold">-70</parameter>
         </parameters>
      </module>
      <module name="VerticalOffsetCorrectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="WriterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="FileName">output.cmr</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
