<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">8</parameter>
            <parameter name="Label">Bottom detection</parameter>
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
            <parameter name="MaskSecondBottom">false</parameter>
            <parameter name="MaskNoise">false</parameter>
            <parameter name="MaskRegion">none</parameter>
            <parameter name="MaskTrack">none</parameter>
            <parameter name="MinPing">0</parameter>
            <parameter name="MaxPing">1</parameter>
            <parameter name="HorizontalKernelType">gaussian</parameter>
            <parameter name="VerticalKernelType">gaussian</parameter>
            <parameter name="HorizontalWidth">15</parameter>
            <parameter name="VerticalWidth">1E-15</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="DepthModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Algorithm">EK500</parameter>
            <parameter name="MinDepthLimit">1</parameter>
            <parameter name="MinDepthValueFraction">0.001</parameter>
            <parameter name="SignalStrengthThreshold">-31</parameter>
            <parameter name="MinimumDepthThresholdFactor">0.99</parameter>
            <parameter name="MaxRangeFactor">1</parameter>
            <parameter name="AlwaysDetectBottom">true</parameter>
            <parameter name="KeepBottomDeeperThanData">true</parameter>
            <parameter name="MinBottomDepth">10</parameter>
            <parameter name="MaxBottomDepth">1000</parameter>
            <parameter name="PreferredKHz">38</parameter>
            <parameter name="MinKHz">0</parameter>
            <parameter name="MaxKHz">240</parameter>
            <parameter name="DoNotUseKHz"/>
            <parameter name="CoordinatedBottomOffset">0.5</parameter>
         </parameters>
      </module>
      <module name="TemporaryComputationsEndModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
