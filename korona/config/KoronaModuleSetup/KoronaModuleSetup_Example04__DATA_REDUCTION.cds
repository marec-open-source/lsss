<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">DATA REDUCTION. Last changed 2023.02.28</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">COMPLEX DATA:</parameter>
            <parameter name="Comment">Converting complex data to EK60-style data</parameter>
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
            <parameter name="ComputeAngles">false</parameter>
            <parameter name="KeepBroadband">false</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">8</parameter>
            <parameter name="Label">REAL DATA:</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="AngleDeletionModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
      <module name="DownsamplingModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Downsampling">SAMPLE_SIZE</parameter>
            <parameter name="DownsamplingFactor">1</parameter>
            <parameter name="DownsamplingSampleSize">0.38</parameter>
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
            <parameter name="MinRange">15</parameter>
            <parameter name="MinDepth">15</parameter>
            <parameter name="TransducerRange">true</parameter>
            <parameter name="MaxRange">300</parameter>
            <parameter name="MaxDepth">300</parameter>
            <parameter name="MaxBelowBottom"/>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
