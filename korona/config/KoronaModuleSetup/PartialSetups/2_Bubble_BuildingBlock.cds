<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="GroupStart">true</parameter>
            <parameter name="GroupCollapsed">false</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">8</parameter>
            <parameter name="Label">Bubble blocking correction</parameter>
            <parameter name="Comment">The bubble filter is the inverse of the spike-filter: the spike-filter detects samples that has a vertical structure and is much stronger than the surrounding samples. The
bubble filter detects samples that has a vertical structure and is much weaker than the surrounding samples. The filter is called bubble-filter since it is caused by
bubbles swept down by the ship hull that blocks the backscatter. 
</parameter>
         </parameters>
      </module>
      <module name="BubblSpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">2500</parameter>
            <parameter name="TotalDelta">14</parameter>
            <parameter name="VerticalDelta">14</parameter>
            <parameter name="Debug">false</parameter>
            <parameter name="VerticalUnit">DURATION</parameter>
            <parameter name="VerticalMedianSearchHeight">50</parameter>
            <parameter name="WindowMedianSearchHeight">35</parameter>
            <parameter name="VerticalMedianSearchDuration">2</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.9</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="GroupEndModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
