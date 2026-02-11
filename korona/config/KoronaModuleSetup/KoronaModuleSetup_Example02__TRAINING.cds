<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">KORONA SETUP: Example02_TRAINING. Last modification: 2023.02.01</parameter>
            <parameter name="Comment">This setup is intended to ensure the same setup and resolution fort all data that are to be included into the acoustic feature library</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">1. SPIKE NOISE REMOVAL:</parameter>
            <parameter name="Comment">The SpikeFilterModule reove spike-noise, e.g. due to unsynchronized instruments or sonars on other ships (that is of course not synchronized
with the echosounders on this ship). A spike has to have a vertical extent, and be much stronger than the neighbouring valies. The measured
acoustic values in the detected spike is replaced by the median acoustic value of the surrounding pixels. There are three modules in sequence
because the settings are different for different depths: at shallow depth, it is more likely to detect a spike, e.g due to very slow ping-rate and small
schools close to the surface that may be detected in one ping only. (Note that the ping-rate has to be low and the school small.) Thus, it is required
that the spike at shallow depths has to be strong. At larger ranges, it is more likely that even small schools are detected in more than one ping.
Therefore, the required strength of the spikes are reduced with increasing depths.</parameter>
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
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">100</parameter>
            <parameter name="TotalDelta">20</parameter>
            <parameter name="VerticalDelta">20</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">SAMPLES</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.1</parameter>
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
            <parameter name="StartDepth">90</parameter>
            <parameter name="EndDepth">250</parameter>
            <parameter name="TotalDelta">14</parameter>
            <parameter name="VerticalDelta">14</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">SAMPLES</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.1</parameter>
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
            <parameter name="StartDepth">240</parameter>
            <parameter name="EndDepth">2500</parameter>
            <parameter name="TotalDelta">10</parameter>
            <parameter name="VerticalDelta">10</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">SAMPLES</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.1</parameter>
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
            <parameter name="Label">2. SPOT NOISE REMOVAL:</parameter>
            <parameter name="Comment">The SpotNoiseModule removes a single sample that is much stronger that the surrounding samples. This does not occur often, but some instruments,
e.g. the Simrad EK15, may have occasionally have this problem.</parameter>
         </parameters>
      </module>
      <module name="SpotNoiseModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">true</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">2500</parameter>
            <parameter name="Delta">14</parameter>
            <parameter name="Debug">false</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">3. CORRECT FOR BUBBLES:</parameter>
            <parameter name="Comment">BubbleSpikeFilterModule works in a similar way as the SpikeFilterModule, but looks for measured values with a vertical extent that is much weaker
than the surrounding values.</parameter>
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
            <parameter name="VerticalUnit">SAMPLES</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.15</parameter>
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
            <parameter name="StartDepth">90</parameter>
            <parameter name="EndDepth">250</parameter>
            <parameter name="TotalDelta">10</parameter>
            <parameter name="VerticalDelta">10</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">SAMPLES</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.15</parameter>
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
            <parameter name="StartDepth">240</parameter>
            <parameter name="EndDepth">2000</parameter>
            <parameter name="TotalDelta">7</parameter>
            <parameter name="VerticalDelta">7</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">SAMPLES</parameter>
            <parameter name="VerticalMedianSearchHeight">30</parameter>
            <parameter name="WindowMedianSearchHeight">7</parameter>
            <parameter name="VerticalMedianSearchDuration">0.15</parameter>
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
            <parameter name="Label">4. CORRECT FOR PING MISSING AT ONE FREQUENCY:</parameter>
            <parameter name="Comment">FillMissingDataModule duplicates the previous ping for a frequency if there is a time where there exists data for at least one other frequency.</parameter>
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
            <parameter name="Label">5. CORRECT FOR TRANSDUCER POSITIONING:</parameter>
            <parameter name="Comment">The offset correction modules compensates for the placement of the transducers and for the system delay (due to filtering effects of the electronics and
the transducers).</parameter>
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
            <parameter name="Label">6. BOTTOM DETECTION:</parameter>
            <parameter name="Comment">All changes of data between TemporaryComputationsBeginModule and TemporaryComputationsEndModule are discarded, but new telegrams are kept.
In this case the data are smoothed heavily horizontally to get a stable bottom detection, but such heavily smoothed data are not desired after the bottom
is detected. The bottom telegram are kept after TemporaryComputationsEndModule.</parameter>
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
            <parameter name="MaskBottom">false</parameter>
            <parameter name="MaskSecondBottom">true</parameter>
            <parameter name="MaskNoise">false</parameter>
            <parameter name="MaskRegion">none</parameter>
            <parameter name="MaskTrack">none</parameter>
            <parameter name="MinPing">0</parameter>
            <parameter name="MaxPing">20</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">50</parameter>
            <parameter name="VerticalWidth">1E-15</parameter>
            <parameter name="LogarithmicValues">false</parameter>
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
            <parameter name="MinBottomDepth">10</parameter>
            <parameter name="MaxBottomDepth">1000</parameter>
            <parameter name="PreferredKHz"/>
            <parameter name="MinKHz">0</parameter>
            <parameter name="MaxKHz">120</parameter>
            <parameter name="DoNotUseKHz"/>
            <parameter name="CoordinatedBottomOffset">0</parameter>
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
            <parameter name="Label">7. NOISE REMOVE</parameter>
            <parameter name="Comment">The data are smoothed slightly. There are two smooth modules to avoid smoothing trough the bottom.
Much smoothing reduce variance and improves the maximum usable range of the data, but that also reduce the spatial resolution.
The acoustic categorization library was originally generated for data smoothed over 8 m horizontally and 0.5 m vertically.</parameter>
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
            <parameter name="MinimumQuality">0</parameter>
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
            <parameter name="FallbackNoiseQuantile">10</parameter>
            <parameter name="FallbackNoiseChannels">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">Remove noise calculated in NoiseQuantificationModule.</parameter>
         </parameters>
      </module>
      <module name="NoiseRemoverModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="RemoveNoiseFromStart">true</parameter>
            <parameter name="MaxBufferSize">10</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">38</parameter>
            <parameter name="Label"/>
            <parameter name="Comment"/>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
