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
            <parameter name="Label">Spike and spot noise removal</parameter>
            <parameter name="Comment">Spike noise are also called impulse-noise. The spike-filters are very mathematical: they look for data with a vertical structure that are much stronger than the surrondings.
Their origin may be instruments onboard the ship that are unsynchronized with (e.g.) the echosounder or sonar. Their origin may also be acoustically active equipment on 
nearby vessels, e.g. active sonars on fishing vessels that are seen by sonars on own vessel. By default, a sample that is deteced to be a part of a spike is replaced by the 
median value of the surrounding values. See manual of Spike filter module for details.

There are two spike filters to distinguish between how spikes usually appear at different depths and how they could potentially be confused with measurements of fish. 
Measurement of the spikes could be in duration or range, but should not be in samples as length covered by a single sample varies between echosounder models. For
Simrad EK60, typical sample-duration (as stored in files) is 20 cm, while it could be as little as 3 mm for Simrad EK80. Communication with the experient acoustic 
instrument engineer at IMR, Terje Haugland, have lead to the current default settings:
1) No spike filter at shorter range than 90 m. 
Explanation: The measured samples are TVG-amplified, i.e. amplified to compensated for the weakening by range. Spikes are usualy not a problem at short ranges since 
there is not much range-amplification at short ranges. Furthermore, at short ranges, very shallow, small and dense fish-schools may in extreme cases where the ping-
rate is low be detected by a single ping only e.g. in African waters. A ship cruising at the commonly speed of 10 knots (5 m/s) may see a small dense school at 10 m below 
the ship in only na single ping, and could then be removed by the spike filter. To prevent this, the spike-filter start-depth is set to 90 m. Thus, even a single fish should be 
detected by several pings provided a ship is cruising at 10 knots and pinging more frequently than 1 ping per second. In fact, it is sufficient that the ping-rate is more than 
0.5 ping per second.
2) First spike-filter goes from 90 m - 250 m, and searching for spikes 14 dB stronger than the median of the surrounding values.
3) Second spike-filter goes from 240 m - 2500 m, and searching for spikes 10 dB stronger than the median of the surrounding values. The requirement of the strength of
the spikes are lower than for the shallow filter since the overlap of samples at same depth at subsequent  pings are larger than for shallow samples.

Spot noise are aso called sample-noise. This filter was created due to frequent problems with this type of noise from some cheap Simrad echosounders like EK10 and 
EK15, but this kind of noise is seldom a problem anymore.That is sample-values that are much stronger than the surrounding values. By default, the deteced values are 
replaced by the median of the surrounding values.  See manual of Spot noise filter module for details.
</parameter>
         </parameters>
      </module>
      <module name="SpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Comment">First spike-filter goes from 90 m - 250 m, and searching for spikes 14 dB stronger than the median of the surrounding values.</parameter>
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
            <parameter name="VerticalMedianSearchHeight">7</parameter>
            <parameter name="WindowMedianSearchHeight">35</parameter>
            <parameter name="VerticalMedianSearchDuration">0.9</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.3</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="SpikeFilterModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="Comment">Second spike-filter deeper and weake than firs. Spike-strength allowed weaker than shallow filter since the overlap of samples at same depth at subsequent pings are larger</parameter>
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
            <parameter name="VerticalMedianSearchHeight">7</parameter>
            <parameter name="WindowMedianSearchHeight">35</parameter>
            <parameter name="VerticalMedianSearchDuration">0.9</parameter>
            <parameter name="WindowMedianSearchDuration">4.4</parameter>
            <parameter name="VerticalMedianSearchDistance">1.3</parameter>
            <parameter name="WindowMedianSearchDistance">6.6</parameter>
         </parameters>
      </module>
      <module name="SpotNoiseModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LogarithmicValues">true</parameter>
            <parameter name="ShowDetails">false</parameter>
            <parameter name="OnlyLast">false</parameter>
            <parameter name="ChannelsToProcess">-1</parameter>
            <parameter name="AutomaticDepthRange">false</parameter>
            <parameter name="StartDepth">10</parameter>
            <parameter name="EndDepth">2500</parameter>
            <parameter name="Delta">20</parameter>
            <parameter name="Debug">false</parameter>
         </parameters>
      </module>
      <module name="GroupEndModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
