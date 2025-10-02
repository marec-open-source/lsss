<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="3">
   <modules>
      <module name="RegionModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="ProcessLast">false</parameter>
            <parameter name="Channel">2</parameter>
            <parameter name="BlindZoneDepth">15</parameter>
            <parameter name="MaxDepth">200</parameter>
            <parameter name="Thresholds">-62</parameter>
            <parameter name="Thresholds">-62,-50</parameter>
            <parameter name="Thresholds">-62,-50,-40</parameter>
            <parameter name="Density">
               <parameter name="min">-120</parameter>
               <parameter name="max">-20</parameter>
            </parameter>
            <parameter name="MaxSv">
               <parameter name="min">-50</parameter>
               <parameter name="max">-10</parameter>
            </parameter>
            <parameter name="Length">
               <parameter name="min">8</parameter>
               <parameter name="max"/>
            </parameter>
            <parameter name="Thickness">
               <parameter name="min">9</parameter>
               <parameter name="max">1000001</parameter>
            </parameter>
            <parameter name="Area">
               <parameter name="min">10</parameter>
               <parameter name="max">1000002</parameter>
            </parameter>
            <parameter name="Overlap">
               <parameter name="min">0</parameter>
               <parameter name="max">100</parameter>
            </parameter>
            <parameter name="Compactness">
               <parameter name="min">0</parameter>
               <parameter name="max">1</parameter>
            </parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
