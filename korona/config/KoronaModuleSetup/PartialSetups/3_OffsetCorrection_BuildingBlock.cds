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
            <parameter name="Label">Ship and instrument-dependent offset corrections</parameter>
            <parameter name="Comment">1. Correct for horizontal transducer positioning: transducer coordinates have to be known.
2. Correct for verical offset due to (1) vertical transducer positioning and (2) known system delayay (due to filtering effects of the electronics and transducers).

Be aware that the transduser positioning is ship-dependent, and vertical offset correction is both ship and instrument dependant. The coordinates are set in the specified reference-files. The use of wrong coordinates worsen the data.</parameter>
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
      <module name="GroupEndModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
