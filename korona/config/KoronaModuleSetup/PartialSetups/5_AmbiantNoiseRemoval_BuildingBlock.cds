<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">8</parameter>
            <parameter name="Label">Ambiant noise removal</parameter>
            <parameter name="Comment">The data are smoothed slightly. There are two smooth modules to avoid smoothing through the bottom.
Much smoothing reduce variance and improves the maximum usable range of the data, but that also reduce the spatial resolution.
The acoustic categorization library was originally generated for data smoothed over 8 m horizontally and 0.5 m vertically.</parameter>
         </parameters>
      </module>
      <module name="TemporaryComputationsBeginModule">
         <parameters>
            <parameter name="Active">true</parameter>
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
            <parameter name="MaxPing">10</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">4</parameter>
            <parameter name="VerticalWidth">0.25</parameter>
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
            <parameter name="MaxPing">10</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">4</parameter>
            <parameter name="VerticalWidth">0.25</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="NoiseQuantificationModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Smooth">running averager</parameter>
            <parameter name="SmoothInterval">5</parameter>
            <parameter name="Mask">dynamic</parameter>
            <parameter name="MinimumQuality">95</parameter>
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
            <parameter name="UseFallbackNoiseQuantile">true</parameter>
            <parameter name="FallbackNoiseQuantile">5</parameter>
            <parameter name="FallbackNoiseChannels">f&lt;50</parameter>
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
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">The data at long range were needed to quantify noise, but those data are not needed anymore. Remove data at ranges that are too large to be reliable (other than
tio estimate noise). Data at long ranges will have reduced sampling volume (i.e. reduced usable equivalent beam compared to shorter range).</parameter>
         </parameters>
      </module>
      <module name="DepthDependentResamplingModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="MinFrequency">150</parameter>
            <parameter name="MaxFrequency"/>
            <parameter name="SampleDistanceAtRange100">0.1</parameter>
         </parameters>
      </module>
      <module name="DepthDependentResamplingModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="MinFrequency">250</parameter>
            <parameter name="MaxFrequency"/>
            <parameter name="SampleDistanceAtRange100">0.2</parameter>
         </parameters>
      </module>
      <module name="NoiseRemoverModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="RemoveNoiseFromStart">true</parameter>
            <parameter name="MaxBufferSize">1</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
