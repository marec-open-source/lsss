<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"> SCHOOL FROM PHANTOM. Last changed 2023.02.28</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label"/>
            <parameter name="Comment"/>
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
            <parameter name="Threshold">-70</parameter>
            <parameter name="CutBelow">false</parameter>
            <parameter name="CutAbove">false</parameter>
            <parameter name="BelowVal">-120</parameter>
            <parameter name="AboveVal">-50</parameter>
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
            <parameter name="MaxPing">50</parameter>
            <parameter name="HorizontalKernelType">tophat</parameter>
            <parameter name="VerticalKernelType">tophat</parameter>
            <parameter name="HorizontalWidth">20</parameter>
            <parameter name="VerticalWidth">1</parameter>
            <parameter name="LogarithmicValues">false</parameter>
         </parameters>
      </module>
      <module name="SchoolDetectionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="ProcessLast">false</parameter>
            <parameter name="Channel">3</parameter>
            <parameter name="MinDepth">10</parameter>
            <parameter name="MaxDepth"/>
            <parameter name="Threshold">-60</parameter>
            <parameter name="Density">
               <parameter name="min">-120</parameter>
               <parameter name="max">-20</parameter>
            </parameter>
            <parameter name="MaxSv">
               <parameter name="min"/>
               <parameter name="max"/>
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
               <parameter name="min">5</parameter>
               <parameter name="max">1000000</parameter>
            </parameter>
            <parameter name="Compactness">
               <parameter name="min"/>
               <parameter name="max"/>
            </parameter>
            <parameter name="FillHoles">true</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
