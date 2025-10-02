<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="1">
   <parameters>
      <parameter name="UseTempFile">true</parameter>
   </parameters>
   <modules>
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
   </modules>
</ModuleContainer>
