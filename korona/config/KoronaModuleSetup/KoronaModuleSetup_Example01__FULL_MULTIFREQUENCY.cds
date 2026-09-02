<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">KORONA SETUP: Example01_FULL_MULTIFREQUENCY. Last update: 2023.02.01</parameter>
            <parameter name="Comment">Last modifications: text in comment fields
</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">A. CONVERT EK80 TO EK60 DATA____________________________________________</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">In case of broadband data: split data into bands. The file BroadbandSplitterBands.xml defines the bands. If no value is set, that means the lower value of that band. Common bands of the Simrad EK80 WBT in FM mode are:
18 kHz: 15-21 kHz;  38 kHz: 35-41 kHz; 70 kHz: 55-95 kHz; 120 kHz: 160-260 kHz; 200 kHz: 160-260; 333 kHz: 260-450 kHz. Note that nonlinear interactions creates sound at 2x, 3x, 4x (and so on) the fundamental frequency,

At IMR, Norway, these are currently (2018) the uses: 18 and 38 kHz: CW mode; 70 and 120 kHz: CW or FM mode; 200 kHz: FM mode; 333 kHz: CW or FM mode</parameter>
         </parameters>
      </module>
      <module name="BroadbandSplitterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="AutoSplit">false</parameter>
            <parameter name="SplitCount">3</parameter>
            <parameter name="SplitBandwidth"/>
            <parameter name="StopBandDistance">1</parameter>
            <parameter name="MinBandwidth">4</parameter>
            <parameter name="Downsampling">FACTOR</parameter>
            <parameter name="DownsamplingFactor">1</parameter>
            <parameter name="DownsamplingSampleSize">0.01</parameter>
            <parameter name="ComputeAngles">false</parameter>
            <parameter name="ComputationalMethod">BANDPASS_FILTERING</parameter>
            <parameter name="FftWindowSize">2</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">Convert complex data to real data, i.e. convert from EK80 to EK60 format. CW-data is then reduced to 1/8 of the volume.</parameter>
         </parameters>
      </module>
      <module name="ComplexToRealModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="ComputeAngles">true</parameter>
            <parameter name="KeepBroadband">false</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">B. DATA  CLEANING________________________________________________________</parameter>
            <parameter name="Comment">
</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">1. SPIKE NOISE REMOVAL:</parameter>
            <parameter name="Comment">The SpikeFilterModule reove spike-noise, e.g. due to unsynchronized instruments or sonars on other ships (that is of course not synchronized
with the echosounders on this ship). A spike has to have a vertical extent, and be much stronger than the neighbouring valies. The measured
acoustic values in the detected spike is replaced by the median acoustic value of the surrounding pixels. There are three modules in sequence
because the settings are different for different depths: at shallow depth, it is more likely to detect a spike, e.g due to very slow ping-rate and small
schools close to the surface that may be detected in one ping only. (Note that the ping-rate has to be low and the school small.) Thus, it is required
that the spike at shallow depths has to be strong. At larger ranges, it is more likely that even small schools are detected in more than one ping.
Therefore, the required strength of the spikes are reduced with increasing depth.

A spike filter is usually not needed close to the surface since the spikes there are not amplified by the TVG. Further, a small and strong school may be taken as a spike. For 1 ping/s, 7 degree beams are overlapping at
approximately 50 m depth, for 0.5 ping/second at 100 m. Therefore the forst spike filter starts around 100 m.</parameter>
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
            <parameter name="StartDepth">240</parameter>
            <parameter name="EndDepth">2500</parameter>
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
            <parameter name="StartDepth">90</parameter>
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
            <parameter name="StartDepth">240</parameter>
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
            <parameter name="MaxPing">1</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">10</parameter>
            <parameter name="VerticalWidth">1E-15</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="DepthModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Algorithm">EK500</parameter>
            <parameter name="MinDepthValueFraction">0.001</parameter>
            <parameter name="SignalStrengthThreshold">-31</parameter>
            <parameter name="MaxRangeFactor">1.5</parameter>
            <parameter name="AlwaysDetectBottom">true</parameter>
            <parameter name="UseExistingBottom">false</parameter>
            <parameter name="KeepBottomDeeperThanData">false</parameter>
            <parameter name="MinBottomDepth">10</parameter>
            <parameter name="MaxBottomDepth">1000</parameter>
            <parameter name="PreferredKHz"/>
            <parameter name="MinKHz">30</parameter>
            <parameter name="MaxKHz">240</parameter>
            <parameter name="DoNotUseKHz"/>
            <parameter name="MinimumDepthThresholdFactor">0.99</parameter>
            <parameter name="MinimumDepthThresholdDistance">10</parameter>
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
            <parameter name="Label">7. AMBIENT NOISE REMOVAL:</parameter>
            <parameter name="Comment">The data are smoothed slightly. There are two smooth modules to avoid smoothing through the bottom.
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
            <parameter name="HistogramInitializationSampleCount">5000</parameter>
            <parameter name="HistogramMinimumSampleCount">25000</parameter>
            <parameter name="HistogramSmooth">true</parameter>
            <parameter name="HistogramSmoothFactor">100</parameter>
            <parameter name="SdevMasking">false</parameter>
            <parameter name="Sdev">10</parameter>
            <parameter name="SNMasking">false</parameter>
            <parameter name="SNRatio">10</parameter>
            <parameter name="UseFallbackNoiseQuantile">true</parameter>
            <parameter name="FallbackNoiseQuantile">5</parameter>
            <parameter name="FallbackNoiseChannels">true</parameter>
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
      <module name="DataReductionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="BlindZone">false</parameter>
            <parameter name="MinRange"/>
            <parameter name="MinDepth"/>
            <parameter name="TransducerRange">true</parameter>
            <parameter name="MaxRange"/>
            <parameter name="MaxDepth"/>
            <parameter name="MaxBelowBottom"/>
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
            <parameter name="MaxBufferSize">25</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">C. TO SPEED UP CATEGORIZATION AND IMPROVE "SPECIES IDENTIFICATION"____</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">8. SCHOOL DETECTION</parameter>
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
            <parameter name="Comment">The first line will be tried first. If not possible, then the second line, and so on. F38 means Frequency 38 kHz. C1 means the first channel. Commonly C1 is 18 kHz that usually has a wider beam than the others. C1 will always exist.</parameter>
            <parameter name="ChannelID"/>
            <parameter name="Frequency"/>
            <parameter name="Expression">
               <value>(F38+3*F200)/4</value>
               <value>(C2+C3+C4+C5)/4</value>
               <value>(C2+C3+C4)/3</value>
               <value>(C2+C3)/2</value>
               <value>F38</value>
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
            <parameter name="FillHoles">true</parameter>
            <parameter name="FillVerticalGaps"/>
            <parameter name="FillHorizontalGaps"/>
            <parameter name="BoundarySmoothingIterations"/>
            <parameter name="Density">
               <parameter name="min">-120</parameter>
               <parameter name="max">-20</parameter>
            </parameter>
            <parameter name="MaxSv">
               <parameter name="min"/>
               <parameter name="max"/>
            </parameter>
            <parameter name="Length">
               <parameter name="min">3</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Thickness">
               <parameter name="min">3</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Area">
               <parameter name="min">3</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Compactness">
               <parameter name="min"/>
               <parameter name="max"/>
            </parameter>
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
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">D. "SPECIES IDENTIFICATION"_________________________________________________</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">3</parameter>
            <parameter name="Label">8. ACOUSTIC FEATURE LIBRARY CATEGORISATION ("species identification")</parameter>
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
               <parameter name="mackerel">false</parameter>
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
