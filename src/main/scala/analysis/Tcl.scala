package analysis

object TclTemplate {
  def template(name: String, files : List[String]) = {
    var tcl =
      "# " +
        "\n# " +
        "Project automation script for VLSI " +
        "\n# " +
        "\n# Created for ISE version 14.7" +
        "\n# " +
        "\n# This file contains several Tcl procedures (procs) that you can use to automate" +
        "\n# your project by running from xtclsh or the Project Navigator Tcl console." +
        "\n# If you load this file (using the Tcl command: source /home/lekhobola/Documents/dev/research/scala-ide/workspace/sdr-moc/prj/fmddc/VLSI.tcl), then you can" +
        "\n# run any of the procs included here." +
        "\n# " +
        "\n# This script is generated assuming your project has HDL sources." +
        "\n# Several of the defined procs won't apply to an EDIF or NGC based project." +
        "\n# If that is the case, simply remove them from this script." +
        "\n# " +
        "\n# You may also edit any of these procs to customize them. See comments in each" +
        "\n# proc for more instructions." +
        "\n# " +
        "\n# This file contains the following procedures:" +
        "\n# " +
        "\n# Top Level procs (meant to be called directly by the user):" +
        "\n#    run_process: you can use this top-level procedure to run any processes" +
        "\n#        that you choose to by adding and removing comments, or by" +
        "\n#        adding new entries." +
        "\n#    rebuild_project: you can alternatively use this top-level procedure" +
        "\n#        to recreate your entire project, and the run selected processes." +
        "\n# " +
        "\n# Lower Level (helper) procs (called under in various cases by the top level procs):" +
        "\n#    show_help: print some basic information describing how this script works" +
        "\n#    add_source_files: adds the listed source files to your project." +
        "\n#    set_project_props: sets the project properties that were in effect when this" +
        "\n#        script was generated." +
        "\n#    create_libraries: creates and adds file to VHDL libraries that were defined when" +
        "\n#        this script was generated." +
        "\n#    set_process_props: set the process properties as they were set for your project" +
        "\n#        when this script was generated." +
        "\n# " +
        "\n" +
        "\nset myProject \"/home/lekhobola/Documents/dev/research/scala-ide/workspace/sdr-moc/prj/fmddc/fmddc.prj\"" +
        "\nset myScript \"/home/lekhobola/Documents/dev/research/scala-ide/workspace/sdr-moc/prj/fmddc/fmddc.tcl\"" +
        "\n" +
        "\n# " +
        "\n# Main (top-level) routines" +
        "\n# " +
        "\n# run_process" +
        "\n# This procedure is used to run processes on an existing project. You may comment or" +
        "\n# uncomment lines to control which processes are run. This routine is set up to run" +
        "\n# the Implement Design and Generate Programming File processes by default. This proc" +
        "\n# also sets process properties as specified in the \"set_process_props\" proc. Only" +
        "\n# those properties which have values different from their current settings in the project" +
        "\n# file will be modified in the project." +
        "\n# " +
        "\nproc run_process {} {" +
        "\n" +
        "\n   global myScript" +
        "\n   global myProject" +
        "\n" +
        "\n   ## put out a 'heartbeat' - so we know something's happening." +
        "\n   puts \"\\" +
        "n$myScript: running ($myProject)..." +
        "\n\"" +
        "\n" +
        "\n   if { ! [ open_project ] } {" +
        "\n      return false" +
        "\n   }" +
        "\n" +
        "\n   set_process_props" +
        "\n   #" +
        "\n   # Remove the comment characters (#'s) to enable the following commands " +
        "\n   # process run \"Synthesize\"" +
        "\n   # process run \"Translate\"" +
        "\n   # process run \"Map\"" +
        "\n   # process run \"Place & Route\"" +
        "\n   #" +
        "\n   set task \"Implement Design\"" +
        "\n   if { ! [run_task $task] } {" +
        "\n      puts \"$myScript: $task run failed, check run output for details.\"" +
        "\n      project close" +
        "\n      return" +
        "\n   }" +
        "\n" +
        "\n   set task \"Generate Programming File\"" +
        "\n   if { ! [run_task $task] } {" +
        "\n      puts \"$myScript: $task run failed, check run output for details.\"" +
        "\n      project close" +
        "\n      return" +
        "\n   }" +
        "\n   " +
        "\n   set task \"Generate Text Power Report\"" +
        "\n   if { ! [run_task $task] } {" +
        "\n      puts \"$myScript: $task run failed, check run output for details.\"" +
        "\n      project close" +
        "\n      return" +
        "\n   }" +
        "\n   " +
        "\n   puts \"Run completed (successfully).\"" +
        "\n   project close" +
        "\n" +
        "\n}" +
        "\n" +
        "\n# " +
        "\n# rebuild_project" +
        "\n# " +
        "\n# This procedure renames the project file (if it exists) and recreates the project." +
        "\n# It then sets project properties and adds project sources as specified by the" +
        "\n# set_project_props and add_source_files support procs. It recreates VHDL Libraries" +
        "\n# as they existed at the time this script was generated." +
        "\n# " +
        "\n# It then calls run_process to set process properties and run selected processes." +
        "\n# " +
        "\nproc rebuild_project {} {" +
        "\n" +
        "\n   global myScript" +
        "\n   global myProject" +
        "\n" +
        "\n   project close" +
        "\n   ## put out a 'heartbeat' - so we know something's happening." +
        "\n   puts \"\\" +
        "\n$myScript: Rebuilding ($myProject)..." +
        "\n\"" +
        "\n" +
        "\n   set proj_exts [ list ise xise gise ]" +
        "\n   foreach ext $proj_exts {" +
        "\n      set proj_name \"${myProject}.$ext\"" +
        "\n      if { [ file exists $proj_name ] } { " +
        "\n         file delete $proj_name" +
        "\n      }" +
        "\n   }" +
        "\n" +
        "\n   project new $myProject" +
        "\n   set_project_props" +
        "\n   add_source_files" +
        "\n   create_libraries" +
        "\n   puts \"$myScript: project rebuild completed.\"" +
        "\n" +
        "\n   run_process" +
        "\n" +
        "\n}" +
        "\n" +
        "\n# " +
        "\n# Support Routines" +
        "\n# " +
        "\n" +
        "\n# " +
        "\nproc run_task { task } {" +
        "\n" +
        "\n   # helper proc for run_process" +
        "\n" +
        "\n   puts \"Running '$task'\"" +
        "\n   set result [ process run \"$task\" ]" +
        "\n   #" +
        "\n   # check process status (and result)" +
        "\n   set status [ process get $task status ]" +
        "\n   if { ( ( $status != \"up_to_date\" ) && \\" +
        "\n            ( $status != \"warnings\" ) ) || \\" +
        "\n         ! $result } {" +
        "\n      return false" +
        "\n   }" +
        "\n   return true" +
        "\n}" +
        "\n" +
        "\n# " +
        "\n# show_help: print information to help users understand the options available when" +
        "\n#            running this script." +
        "\n# \nproc show_help {} {" +
        "\n" +
        "\n   global myScript" +
        "\n" +
        "\n   puts \"\"" +
        "\n   puts \"usage: xtclsh $myScript <options>\"" +
        "\n   puts \"       or you can run xtclsh and then enter 'source $myScript'.\"" +
        "\n   puts \"\"" +
        "\n   puts \"options:\"" +
        "\n   puts \"   run_process       - set properties and run processes.\"" +
        "\n   puts \"   rebuild_project   - rebuild the project from scratch and run processes.\"" +
        "\n   puts \"   set_project_props - set project properties (device, speed, etc.)\"" +
        "\n   puts \"   add_source_files  - add source files\"" +
        "\n   puts \"   create_libraries  - create vhdl libraries\"" +
        "\n   puts \"   set_process_props - set process property values\"" +
        "\n   puts \"   show_help         - print this message\"" +
        "\n   puts \"\"" +
        "\n}" +
        "\n" +
        "\nproc open_project {} {" +
        "\n" +
        "\n   global myScript" +
        "\n   global myProject" +
        "\n" +
        "\n   if { ! [ file exists ${myProject}.xise ] } { " +
        "\n      ## project file isn't there, rebuild it." +
        "\n      puts \"Project $myProject not found. Use project_rebuild to recreate it.\"" +
        "\n      return false" +
        "\n   }" +
        "\n" +
        "\n   project open $myProject" +
        "\n" +
        "\n   return true" +
        "\n" +
        "\n}" +
        "\n# " +
        "\n# set_project_props" +
        "\n# " +
        "\n# This procedure sets the project properties as they were set in the project" +
        "\n# at the time this script was generated." +
        "\n# " +
        "\nproc set_project_props {} {" +
        "\n" +
        "\n   global myScript" +
        "\n" +
        "\n   if { ! [ open_project ] } {" +
        "\n      return false" +
        "\n   }" +
        "\n" +
        "\n   puts \"$myScript: Setting project properties...\"" +
        "\n" +
        "\n   project set family \"Spartan6\"" +
        "\n   project set device \"xc6slx150t\"" +
        "\n   project set package \"fgg676\"" +
        "\n   project set speed \"-3\"" +
        "\n   project set top_level_module_type \"HDL\"" +
        "\n   project set synthesis_tool \"XST (VHDL/Verilog)\"" +
        "\n   project set simulator \"ISim (VHDL/Verilog)\"" +
        "\n   project set \"Preferred Language\" \"Verilog\"" +
        "\n   project set \"Enable Message Filtering\" \"false\"" +
        "\n" +
        "\n}" +
        "\n" +
        "\n" +
        "\n# " +
        "\n# add_source_files" +
        "\n# " +
        "\n# This procedure add the source files that were known to the project at the" +
        "\n# time this script was generated." +
        "\n# " +
        "\nproc add_source_files {} {" +
        "\n" +
        "\n   global myScript" +
        "\n" +
        "\n   if { ! [ open_project ] } {" +
        "\n      return false" +
        "\n   }" +
        "\n" +
        "\n\tputs \"$myScript: Adding sources to project...\""

    /* "\n" +
        "\n    xfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/source/source.vhd\"" +
        "\n    xfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/sink/sink.vhd\"" +
        "\n    xfile add \"/home/lekhobola/Documents/dev/research/scala-ide/workspace/sdr-moc/prj/fmddc/fmddc.vhd\"" +
        "\n    xfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/primitives/dspcomponents.vhd\" -lib_vhdl DSP_PRIMITIVES_Lib" +
        "\n    xfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/NCO_Lib/rtl/wav_gen/rtl/wav_rom_pkg.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/NCO_Lib/rtl/lfsr/rtl/lfsr_pkg.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/FIR_Lib/fir_pkg/fir_pkg.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/CIC_Lib/rtl/integrator/rtl/integrator.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/CIC_Lib/rtl/comb/rtl/comb.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/NCO_Lib/rtl/wav_gen/rtl/wav_gen.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/NCO_Lib/rtl/phase_accum/rtl/phase_acc.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/NCO_Lib/rtl/lfsr/rtl/lfsr.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/FIR_Lib/fir_par/rtl/fir_ntap_par/rtl/fir_ntap_par.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/FIR_Lib/fir_par/rtl/fir_ntap_osym_par/rtl/fir_ntap_osym_par.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/FIR_Lib/fir_par/rtl/fir_ntap_esym_par/rtl/fir_ntap_esym_par.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/FIR_Lib/fir_par/rtl/fir_ntap_avg_par/rtl/fir_ntap_avg_par.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/CIC_Lib/rtl/interpolator/rtl/interpolator.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/CIC_Lib/rtl/decimator/rtl/decimator.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/source/source.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/sink/sink.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/primitives/mixer/rtl/mixer.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/NCO_Lib/rtl/NCO.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/FIR_Lib/fir_par/rtl/fir_par.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/fifo/rtl/fifo.vhd\"" +
        "\n\txfile add \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/CIC_Lib/rtl/CIC.vhd\"" + */

    tcl += files.mkString("\n")
    tcl +=
      "\n" +
        "\n" +
        "\n   # Set the Top Module as well..." +
        "\n   project set top \"rtl\" \"fmddc\"" +
        "\n" +
        "\n   puts \"$myScript: project sources reloaded.\"" +
        "\n" +
        "\n} ; # end add_source_files" +
        "\n" +
        "\n# " +
        "\n# create_libraries" +
        "\n# " +
        "\n# This procedure defines VHDL libraries and associates files with those libraries." +
        "\n# It is expected to be used when recreating the project. Any libraries defined" +
        "\n# when this script was generated are recreated by this procedure." +
        "\n# " +
        "\nproc create_libraries {} {" +
        "\n" +
        "\n   global myScript" +
        "\n." +
        "\n   if { ! [ open_project ] } {" +
        "\n      return false" +
        "\n   }" +
        "\n" +
        "\n   puts \"$myScript: Creating libraries...\"" +
        "\n" +
        "\n   lib_vhdl new \"DSP_PRIMITIVES_Lib\"" +
        "\n      lib_vhdl add_file \"DSP_PRIMITIVES_Lib\" \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/pcores/ip/primitives/dspcomponents.vhd\"" +
        "\n" +
        "\n   # must close the project or library definitions aren't saved." +
        "\n   project save" +
        "\n" +
        "\n} ; # end create_libraries" +
        "\n" +
        "\n# " +
        "\n# set_process_props" +
        "\n# " +
        "\n# This procedure sets properties as requested during script generation (either" +
        "\n# all of the properties, or only those modified from their defaults)." +
        "\n# " +
        "\nproc set_process_props {} {" +
        "\n" +
        "\n   global myScript" +
        "\n" +
        "\n   if { ! [ open_project ] } {" +
        "\n      return false" +
        "\n   }" +
        "\n" +
        "\n   puts \"$myScript: setting process properties...\"" +
        "\n" +
        "\n   project set \"Compiled Library Directory\" \"\\$XILINX/<language>/<simulator>\"" +
        "\n   project set \"Global Optimization\" \"Off\" -process \"Map\"" +
        "\n   project set \"Pack I/O Registers/Latches into IOBs\" \"Off\" -process \"Map\"" +
        "\n   project set \"Place And Route Mode\" \"Route Only\" -process \"Place & Route\"" +
        "\n   project set \"Regenerate Core\" \"Under Current Project Setting\" -process \"Regenerate Core\"" +
        "\n   project set \"Filter Files From Compile Order\" \"true\"" +
        "\n   project set \"Last Applied Goal\" \"Balanced\"" +
        "\n   project set \"Last Applied Strategy\" \"Xilinx Default (unlocked)\"" +
        "\n   project set \"Last Unlock Status\" \"false\"" +
        "\n   project set \"Manual Compile Order\" \"false\"" +
        "\n   project set \"Placer Effort Level\" \"High\" -process \"Map\"" +
        "\n   project set \"Extra Cost Tables\" \"0\" -process \"Map\"" +
        "\n   project set \"LUT Combining\" \"Off\" -process \"Map\"" +
        "\n   project set \"Combinatorial Logic Optimization\" \"false\" -process \"Map\"" +
        "\n   project set \"Starting Placer Cost Table (1-100)\" \"1\" -process \"Map\"" +
        "\n   project set \"Power Reduction\" \"Off\" -process \"Map\"" +
        "\n   project set \"Report Fastest Path(s) in Each Constraint\" \"true\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Generate Datasheet Section\" \"true\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Generate Timegroups Section\" \"false\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Report Fastest Path(s) in Each Constraint\" \"true\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Generate Datasheet Section\" \"true\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Generate Timegroups Section\" \"false\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Project Description\" \"\"" +
        "\n   project set \"Property Specification in Project File\" \"Store all values\"" +
        "\n   project set \"Reduce Control Sets\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Shift Register Minimum Size\" \"2\" -process \"Synthesize - XST\"" +
        "\n   project set \"Case Implementation Style\" \"None\" -process \"Synthesize - XST\"" +
        "\n   project set \"RAM Extraction\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"ROM Extraction\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"FSM Encoding Algorithm\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Optimization Goal\" \"Speed\" -process \"Synthesize - XST\"" +
        "\n   project set \"Optimization Effort\" \"Normal\" -process \"Synthesize - XST\"" +
        "\n   project set \"Resource Sharing\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"Shift Register Extraction\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"User Browsed Strategy Files\" \"/usr/local/bin/XilinxISE/14.7/ISE_DS/ISE/data/default.xds\"" +
        "\n   project set \"VHDL Source Analysis Standard\" \"VHDL-93\"" +
        "\n   project set \"Analysis Effort Level\" \"Standard\" -process \"Analyze Power Distribution (XPower Analyzer)\"" +
        "\n   project set \"Analysis Effort Level\" \"Standard\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Input TCL Command Script\" \"\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Load Physical Constraints File\" \"Default\" -process \"Analyze Power Distribution (XPower Analyzer)\"" +
        "\n   project set \"Load Physical Constraints File\" \"Default\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Load Simulation File\" \"Default\" -process \"Analyze Power Distribution (XPower Analyzer)\"" +
        "\n   project set \"Load Simulation File\" \"Default\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Load Setting File\" \"\" -process \"Analyze Power Distribution (XPower Analyzer)\"" +
        "\n   project set \"Load Setting File\" \"\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Setting Output File\" \"\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Produce Verbose Report\" \"false\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Other XPWR Command Line Options\" \"\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Essential Bits\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Other Bitgen Command Line Options\" \"\" -process \"Generate Programming File\"" +
        "\n   project set \"Maximum Signal Name Length\" \"20\" -process \"Generate IBIS Model\"" +
        "\n   project set \"Show All Models\" \"false\" -process \"Generate IBIS Model\"" +
        "\n   project set \"VCCAUX Voltage Level\" \"2.5V\" -process \"Generate IBIS Model\"" +
        "\n   project set \"Disable Detailed Package Model Insertion\" \"false\" -process \"Generate IBIS Model\"" +
        "\n   project set \"Launch SDK after Export\" \"true\" -process \"Export Hardware Design To SDK with Bitstream\"" +
        "\n   project set \"Launch SDK after Export\" \"true\" -process \"Export Hardware Design To SDK without Bitstream\"" +
        "\n   project set \"Target UCF File Name\" \"\" -process \"Back-annotate Pin Locations\"" +
        "\n   project set \"Ignore User Timing Constraints\" \"false\" -process \"Map\"" +
        "\n   project set \"Register Ordering\" \"4\" -process \"Map\"" +
        "\n   project set \"Use RLOC Constraints\" \"Yes\" -process \"Map\"" +
        "\n   project set \"Other Map Command Line Options\" \"\" -process \"Map\"" +
        "\n   project set \"Use LOC Constraints\" \"true\" -process \"Translate\"" +
        "\n   project set \"Other Ngdbuild Command Line Options\" \"\" -process \"Translate\"" +
        "\n   project set \"Use 64-bit PlanAhead on 64-bit Systems\" \"true\" -process \"Floorplan Area/IO/Logic (PlanAhead)\"" +
        "\n   project set \"Use 64-bit PlanAhead on 64-bit Systems\" \"true\" -process \"I/O Pin Planning (PlanAhead) - Pre-Synthesis\"" +
        "\n   project set \"Use 64-bit PlanAhead on 64-bit Systems\" \"true\" -process \"I/O Pin Planning (PlanAhead) - Post-Synthesis\"" +
        "\n   project set \"Ignore User Timing Constraints\" \"false\" -process \"Place & Route\"" +
        "\n   project set \"Other Place & Route Command Line Options\" \"\" -process \"Place & Route\"" +
        "\n   project set \"Use DSP Block\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"UserID Code (8 Digit Hexadecimal)\" \"0xFFFFFFFF\" -process \"Generate Programming File\"" +
        "\n   project set \"Configuration Pin Done\" \"Pull Up\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable External Master Clock\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Create ASCII Configuration File\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Create Bit File\" \"true\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable BitStream Compression\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Run Design Rules Checker (DRC)\" \"true\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Cyclic Redundancy Checking (CRC)\" \"true\" -process \"Generate Programming File\"" +
        "\n   project set \"Create IEEE 1532 Configuration File\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Create ReadBack Data Files\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Configuration Pin Program\" \"Pull Up\" -process \"Generate Programming File\"" +
        "\n   project set \"Place MultiBoot Settings into Bitstream\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Configuration Rate\" \"2\" -process \"Generate Programming File\"" +
        "\n   project set \"Set SPI Configuration Bus Width\" \"1\" -process \"Generate Programming File\"" +
        "\n   project set \"JTAG Pin TCK\" \"Pull Up\" -process \"Generate Programming File\"" +
        "\n   project set \"JTAG Pin TDI\" \"Pull Up\" -process \"Generate Programming File\"" +
        "\n   project set \"JTAG Pin TDO\" \"Pull Up\" -process \"Generate Programming File\"" +
        "\n   project set \"JTAG Pin TMS\" \"Pull Up\" -process \"Generate Programming File\"" +
        "\n   project set \"Unused IOB Pins\" \"Pull Down\" -process \"Generate Programming File\"" +
        "\n   project set \"Watchdog Timer Value\" \"0xFFFF\" -process \"Generate Programming File\"" +
        "\n   project set \"Security\" \"Enable Readback and Reconfiguration\" -process \"Generate Programming File\"" +
        "\n   project set \"FPGA Start-Up Clock\" \"CCLK\" -process \"Generate Programming File\"" +
        "\n   project set \"Done (Output Events)\" \"Default (4)\" -process \"Generate Programming File\"" +
        "\n   project set \"Drive Done Pin High\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Outputs (Output Events)\" \"Default (5)\" -process \"Generate Programming File\"" +
        "\n   project set \"Wait for DCM and PLL Lock (Output Events)\" \"Default (NoWait)\" -process \"Generate Programming File\"" +
        "\n   project set \"Release Write Enable (Output Events)\" \"Default (6)\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Internal Done Pipe\" \"true\" -process \"Generate Programming File\"" +
        "\n   project set \"Drive Awake Pin During Suspend/Wake Sequence\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Suspend/Wake Global Set/Reset\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Multi-Pin Wake-Up Suspend Mode\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"GTS Cycle During Suspend/Wakeup Sequence\" \"4\" -process \"Generate Programming File\"" +
        "\n   project set \"GWE Cycle During Suspend/Wakeup Sequence\" \"5\" -process \"Generate Programming File\"" +
        "\n   project set \"Wakeup Clock\" \"Startup Clock\" -process \"Generate Programming File\"" +
        "\n   project set \"Allow Logic Optimization Across Hierarchy\" \"false\" -process \"Map\"" +
        "\n   project set \"Maximum Compression\" \"false\" -process \"Map\"" +
        "\n   project set \"Generate Detailed MAP Report\" \"false\" -process \"Map\"" +
        "\n   project set \"Map Slice Logic into Unused Block RAMs\" \"false\" -process \"Map\"" +
        "\n   project set \"Perform Timing-Driven Packing and Placement\" \"false\"" +
        "\n   project set \"Trim Unconnected Signals\" \"true\" -process \"Map\"" +
        "\n   project set \"Create I/O Pads from Ports\" \"false\" -process \"Translate\"" +
        "\n   project set \"Macro Search Path\" \"\" -process \"Translate\"" +
        "\n   project set \"Netlist Translation Type\" \"Timestamp\" -process \"Translate\"" +
        "\n   project set \"User Rules File for Netlister Launcher\" \"\" -process \"Translate\"" +
        "\n   project set \"Allow Unexpanded Blocks\" \"false\" -process \"Translate\"" +
        "\n   project set \"Allow Unmatched LOC Constraints\" \"false\" -process \"Translate\"" +
        "\n   project set \"Allow Unmatched Timing Group Constraints\" \"false\" -process \"Translate\"" +
        "\n   project set \"Perform Advanced Analysis\" \"false\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Report Paths by Endpoint\" \"3\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Report Type\" \"Verbose Report\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Number of Paths in Error/Verbose Report\" \"3\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Stamp Timing Model Filename\" \"\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Report Unconstrained Paths\" \"\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Perform Advanced Analysis\" \"false\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Report Paths by Endpoint\" \"3\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Report Type\" \"Verbose Report\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Number of Paths in Error/Verbose Report\" \"3\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Report Unconstrained Paths\" \"\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Number of Clock Buffers\" \"16\" -process \"Synthesize - XST\"" +
        "\n   project set \"Add I/O Buffers\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"Global Optimization Goal\" \"AllClockNets\" -process \"Synthesize - XST\"" +
        "\n   project set \"Keep Hierarchy\" \"No\" -process \"Synthesize - XST\"" +
        "\n   project set \"Max Fanout\" \"100000\" -process \"Synthesize - XST\"" +
        "\n   project set \"Register Balancing\" \"No\" -process \"Synthesize - XST\"" +
        "\n   project set \"Register Duplication\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"Library for Verilog Sources\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"Export Results to XPower Estimator\" \"\" -process \"Generate Text Power Report\"" +
        "\n   project set \"Asynchronous To Synchronous\" \"false\" -process \"Synthesize - XST\"" +
        "\n   project set \"Automatic BRAM Packing\" \"false\" -process \"Synthesize - XST\"" +
        "\n   project set \"BRAM Utilization Ratio\" \"100\" -process \"Synthesize - XST\"" +
        "\n   project set \"Bus Delimiter\" \"<>\" -process \"Synthesize - XST\"" +
        "\n   project set \"Case\" \"Maintain\" -process \"Synthesize - XST\"" +
        "\n   project set \"Cores Search Directories\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"Cross Clock Analysis\" \"false\" -process \"Synthesize - XST\"" +
        "\n   project set \"DSP Utilization Ratio\" \"100\" -process \"Synthesize - XST\"" +
        "\n   project set \"Equivalent Register Removal\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"FSM Style\" \"LUT\" -process \"Synthesize - XST\"" +
        "\n   project set \"Generate RTL Schematic\" \"Yes\" -process \"Synthesize - XST\"" +
        "\n   project set \"Generics, Parameters\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"Hierarchy Separator\" \"/\" -process \"Synthesize - XST\"" +
        "\n   project set \"HDL INI File\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"LUT Combining\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Library Search Order\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"Netlist Hierarchy\" \"As Optimized\" -process \"Synthesize - XST\"" +
        "\n   project set \"Optimize Instantiated Primitives\" \"false\" -process \"Synthesize - XST\"" +
        "\n   project set \"Pack I/O Registers into IOBs\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Power Reduction\" \"false\" -process \"Synthesize - XST\"" +
        "\n   project set \"Read Cores\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"Use Clock Enable\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Use Synchronous Reset\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Use Synchronous Set\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Use Synthesis Constraints File\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"Verilog Include Directories\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"Verilog Macros\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"Work Directory\" \"/home/lekhobola/Documents/dev/research/xilinx/VLSI/xst\" -process \"Synthesize - XST\"" +
        "\n   project set \"Write Timing Constraints\" \"false\" -process \"Synthesize - XST\"" +
        "\n   project set \"Other XST Command Line Options\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"Timing Mode\" \"Performance Evaluation\" -process \"Map\"" +
        "\n   project set \"Generate Asynchronous Delay Report\" \"false\" -process \"Place & Route\"" +
        "\n   project set \"Generate Clock Region Report\" \"false\" -process \"Place & Route\"" +
        "\n   project set \"Generate Post-Place & Route Power Report\" \"false\" -process \"Place & Route\"" +
        "\n   project set \"Generate Post-Place & Route Simulation Model\" \"false\" -process \"Place & Route\"" +
        "\n   project set \"Power Reduction\" \"false\" -process \"Place & Route\"" +
        "\n   project set \"Place & Route Effort Level (Overall)\" \"High\" -process \"Place & Route\"" +
        "\n   project set \"Auto Implementation Compile Order\" \"true\"" +
        "\n   project set \"Equivalent Register Removal\" \"true\" -process \"Map\"" +
        "\n   project set \"Placer Extra Effort\" \"None\" -process \"Map\"" +
        "\n   project set \"Power Activity File\" \"\" -process \"Map\"" +
        "\n   project set \"Register Duplication\" \"Off\" -process \"Map\"" +
        "\n   project set \"Generate Constraints Interaction Report\" \"false\" -process \"Generate Post-Map Static Timing\"" +
        "\n   project set \"Synthesis Constraints File\" \"\" -process \"Synthesize - XST\"" +
        "\n   project set \"RAM Style\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Maximum Number of Lines in Report\" \"1000\" -process \"Generate Text Power Report\"" +
        "\n   project set \"MultiBoot: Insert IPROG CMD in the Bitfile\" \"Enable\" -process \"Generate Programming File\"" +
        "\n   project set \"Output File Name\" \"fmddc\" -process \"Generate IBIS Model\"" +
        "\n   project set \"Timing Mode\" \"Performance Evaluation\" -process \"Place & Route\"" +
        "\n   project set \"Create Binary Configuration File\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Debugging of Serial Mode BitStream\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Create Logic Allocation File\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Create Mask File\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Retry Configuration if CRC Error Occurs\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"MultiBoot: Starting Address for Next Configuration\" \"0x00000000\" -process \"Generate Programming File\"" +
        "\n   project set \"MultiBoot: Starting Address for Golden Configuration\" \"0x00000000\" -process \"Generate Programming File\"" +
        "\n   project set \"MultiBoot: Use New Mode for Next Configuration\" \"true\" -process \"Generate Programming File\"" +
        "\n   project set \"MultiBoot: User-Defined Register for Failsafe Scheme\" \"0x0000\" -process \"Generate Programming File\"" +
        "\n   project set \"Setup External Master Clock Division\" \"1\" -process \"Generate Programming File\"" +
        "\n   project set \"Allow SelectMAP Pins to Persist\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Mask Pins for Multi-Pin Wake-Up Suspend Mode\" \"0x00\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Multi-Threading\" \"Off\" -process \"Map\"" +
        "\n   project set \"Generate Constraints Interaction Report\" \"false\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Move First Flip-Flop Stage\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"Move Last Flip-Flop Stage\" \"true\" -process \"Synthesize - XST\"" +
        "\n   project set \"ROM Style\" \"Auto\" -process \"Synthesize - XST\"" +
        "\n   project set \"Safe Implementation\" \"No\" -process \"Synthesize - XST\"" +
        "\n   project set \"Power Activity File\" \"\" -process \"Place & Route\"" +
        "\n   project set \"Extra Effort (Highest PAR level only)\" \"None\" -process \"Place & Route\"" +
        "\n   project set \"MultiBoot: Next Configuration Mode\" \"001\" -process \"Generate Programming File\"" +
        "\n   project set \"Encrypt Bitstream\" \"false\" -process \"Generate Programming File\"" +
        "\n   project set \"Enable Multi-Threading\" \"Off\" -process \"Place & Route\"" +
        "\n   project set \"AES Initial Vector\" \"\" -process \"Generate Programming File\"" +
        "\n   project set \"Encrypt Key Select\" \"BBRAM\" -process \"Generate Programming File\"" +
        "\n   project set \"AES Key (Hex String)\" \"\" -process \"Generate Programming File\"" +
        "\n   project set \"Input Encryption Key File\" \"\" -process \"Generate Programming File\"" +
        "\n   project set \"Functional Model Target Language\" \"Verilog\" -process \"View HDL Source\"" +
        "\n   project set \"Change Device Speed To\" \"-3\" -process \"Generate Post-Place & Route Static Timing\"" +
        "\n   project set \"Change Device Speed To\" \"-3\" -process \"Generate Post-Map Static Timing\"" +
        "\n" +
        "\n   puts \"$myScript: project property values set.\"" +
        "\n" +
        "\n} ; # end set_process_props" +
        "\n" +
        "\nproc main {} {" +
        "\n" +
        "\n   if { [llength $::argv] == 0 } {" +
        "\n      show_help" +
        "\n      return true" +
        "\n   }" +
        "\n" +
        "\n   foreach option $::argv {" +
        "\n      switch $option {" +
        "\n         \"show_help\"           { show_help }" +
        "\n         \"run_process\"         { run_process }" +
        "\n         \"rebuild_project\"     { rebuild_project }" +
        "\n         \"set_project_props\"   { set_project_props }" +
        "\n         \"add_source_files\"    { add_source_files }" +
        "\n         \"create_libraries\"    { create_libraries }" +
        "\n         \"set_process_props\"   { set_process_props }" +
        "\n         default               { puts \"unrecognized option: $option\"; show_help }" +
        "\n      }" +
        "\n   }" +
        "\n}" +
        "\n" +
        "\nif { $tcl_interactive } {" +
        "\n   show_help" +
        "\n} else {" +
        "\n   if {[catch {main} result]} {" +
        "\n      puts \"$myScript failed: $result.\"" +
        "\n   }" +
        "\n}" +
        "\n"
  }
}
