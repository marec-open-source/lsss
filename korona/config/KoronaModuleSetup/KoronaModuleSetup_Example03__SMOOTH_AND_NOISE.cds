<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">SMOOTH DATA AND CORRECT FOR NOISE. Last changed 2023.02.01</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">CORRECT FOR SPIKES</parameter>
            <parameter name="Comment">Spike filter active below 100 m to avoid undesired effects from slow ping-rate</parameter>
         </parameters>
      </module>
      <module name="SpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">true</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">100</parameter>
            <parameter name="EndDepth">250</parameter>
            <parameter name="TotalDelta">14</parameter>
            <parameter name="VerticalDelta">14</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">DURATION</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.9</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.3</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="SpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">true</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">250</parameter>
            <parameter name="EndDepth">2000</parameter>
            <parameter name="TotalDelta">10</parameter>
            <parameter name="VerticalDelta">10</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">DURATION</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.9</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.3</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">CORRECT FOR BUBBLES</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="BubblSpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">true</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="TotalDelta">14</parameter>
            <parameter name="VerticalDelta">14</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">DURATION</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">2</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.9</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="BubblSpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">true</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">100</parameter>
            <parameter name="EndDepth">250</parameter>
            <parameter name="TotalDelta">10</parameter>
            <parameter name="VerticalDelta">10</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">DURATION</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">2</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.9</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="BubblSpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">true</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">250</parameter>
            <parameter name="EndDepth">2000</parameter>
            <parameter name="TotalDelta">7</parameter>
            <parameter name="VerticalDelta">7</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">DURATION</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">2</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.9</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">CORRECT FOR TRANSDUCER POSITIONING</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="HorizontalOffsetCorrectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="VerticalOffsetCorrectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">,CORRECT FOR PING MISSING AT ONE FREQUENCY</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="FillMissingDataModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">DETECT BOTTOM</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="DepthModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Algorithm">EK500</parameter>
            <parameter name="MinDepthLimit">10</parameter>
            <parameter name="MinDepthValueFraction">0.001</parameter>
            <parameter name="SignalStrengthThreshold">-31</parameter>
            <parameter name="MinimumDepthThresholdFactor">0.99</parameter>
            <parameter name="MaxRangeFactor">1.5</parameter>
            <parameter name="AlwaysDetectBottom">true</parameter>
            <parameter name="KeepBottomDeeperThanData">false</parameter>
            <parameter name="MinBottomDepth">0</parameter>
            <parameter name="MaxBottomDepth">9999</parameter>
            <parameter name="PreferredKHz"/>
            <parameter name="MinKHz">20</parameter>
            <parameter name="MaxKHz">240</parameter>
            <parameter name="DoNotUseKHz"/>
            <parameter name="CoordinatedBottomOffset">0</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">CORRECT FOR NOISE</parameter>
            <parameter name="Comment"/>
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
            <parameter name="MaxPing">1</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">1E-15</parameter>
            <parameter name="VerticalWidth">0.5</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="SmootherModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="OnlyLastChannel">false</parameter>
            <parameter name="MaskPelagic">true</parameter>
            <parameter name="MaskBottom">false</parameter>
            <parameter name="MaskSecondBottom">true</parameter>
            <parameter name="MaskNoise">false</parameter>
            <parameter name="MaskRegion">none</parameter>
            <parameter name="MaskTrack">none</parameter>
            <parameter name="MinPing">0</parameter>
            <parameter name="MaxPing">1</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">1E-15</parameter>
            <parameter name="VerticalWidth">0.5</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="NoiseQuantificationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Smooth">running averager</parameter>
            <parameter name="SmoothInterval">5</parameter>
            <parameter name="Mask">dynamic</parameter>
            <parameter name="MinimumQuality">100</parameter>
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
            <parameter name="SdevMasking">false</parameter>
            <parameter name="Sdev">10</parameter>
            <parameter name="SNMasking">false</parameter>
            <parameter name="SNRatio">10</parameter>
            <parameter name="UseFallbackNoiseQuantile">false</parameter>
            <parameter name="FallbackNoiseQuantile">5</parameter>
            <parameter name="FallbackNoiseChannels">true</parameter>
         </parameters>
      </module>
      <module name="NoiseRemoverModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="RemoveNoiseFromStart">true</parameter>
            <parameter name="MaxBufferSize">10</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
