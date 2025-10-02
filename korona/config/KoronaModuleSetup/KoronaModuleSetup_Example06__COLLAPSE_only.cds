<?xml version="1.0" encoding="UTF-8"?>

<ModuleContainer version="4">
   <modules>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">false</parameter>
            <parameter name="Label">COLLAPSE ONLY. Last changed 2023.02.28</parameter>
            <parameter name="Comment">Collapses sequential pinging. Could have some unwanted effects if original data has been used before, since work-files would then expect files with more pings.</parameter>
         </parameters>
      </module>
      <module name="CommentModule">
         <parameters>
            <parameter name="Active">true</parameter>
            <parameter name="LineBreak">true</parameter>
            <parameter name="VerticalSpace">20</parameter>
            <parameter name="Label">REMOVE_EMPTY_PINGS</parameter>
            <parameter name="Comment"/>
         </parameters>
      </module>
      <module name="PingCollapsingModule">
         <parameters>
            <parameter name="Active">true</parameter>
         </parameters>
      </module>
   </modules>
</ModuleContainer>
