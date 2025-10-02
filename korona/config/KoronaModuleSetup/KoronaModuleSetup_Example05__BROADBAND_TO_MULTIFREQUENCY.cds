<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">BROADBAND TO MULTIFREQUENCY. Last changed 2023.02.28</parameter>
            <parameter name="Comment">Convert complecx broadband data to EK60-style multifrequency data</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">Notch filter</parameter>
         </parameters>
      </module>
      <module name="BroadbandNotchFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label"/>
            <parameter name="Comment">In case of broadband data: split data into bands</parameter>
         </parameters>
      </module>
      <module name="BroadbandSplitterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="AutoSplit">false</parameter>
            <parameter name="SplitCount">3</parameter>
            <parameter name="SplitBandwidth"/>
            <parameter name="StopBandDistance">1</parameter>
            <parameter name="MinBandwidth">8</parameter>
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
   </modules>
</ModuleContainer>
