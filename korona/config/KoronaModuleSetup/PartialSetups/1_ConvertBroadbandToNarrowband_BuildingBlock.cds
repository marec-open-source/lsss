<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="GroupStart">true</parameter>
            <parameter name="GroupCollapsed">false</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">Convert broadband to narrowband</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">Executed_for_broadband_data_only:</parameter>
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
            <parameter name="Label">Executeed-for_complex_data_only_(broadband_or_narrowband):</parameter>
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
      <module name="GroupEndModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
