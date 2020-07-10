# SdrLift

A domain-specific intermediate-level framework for prototyping software-defined radio (SDR) applications.

## Directory structure
~~~~
sdrlift
├── iplib....................... The IP core libries. 
├── lib_managed................. Managed Scala libraries.
├── lib_unmanaged............... Unmanaged Scala libraries.
├── out......................... A directory for generated project files.
│   │   ├── data................ Keeps results in a form of CSV files.
│   │   ├── dot................. Contains all .dot and .pdf files for synthesized kernels and applications.
│   │   └── vhdl................ Contains gateware (i.e. FPGA code) files for the project.
├── script...................... Scripts for analysis and drawing the plots.
├── src
│   ├── analysis................ Benchmark analysis 
│   ├── codegen................. VHDL and DOT code generation.
│   ├── dev
│   │   ├── apps................ SdrLift applications. New applications must be added to this directory.
│   │   └── kernels............. SdrLift kernels. New kernels must be added to this directory.
│   ├── exp..................... Compiler implementation, analyses and SdrLift Language definition.
│   ├── graph................... Directed flow graphs IR generation.     
│   ├── model................... Dataflow Model IR generation.
│   └── SdrLiftMain............. The Main Scala file.
└── build.sbt................... SBT build file.
~~~~

## Installation
~~~~
  # Cloning
  git clone https://github.com/lekhobola/sdrlift.git

  # SdrLift Home Directory
  export SDRLIFT_HOME=`pwd`/sdrlift

  # JVM Options
  export JAVA_OPTS="$JAVA_OPTS\ 
  -Xmx4G\
  -XX:+UseConcMarkSweepGC\
  -XX:+CMSClassUnloadingEnabled\
  -Xss4M\
  -Duser.timezone=GMT"

  # Set path to Xilinx ISE License and path to Xilinx application
  export XILINXD_LICENSE_FILE=`path to license`
  export LM_LICENSE_FILE=`path to license`

  # Set path to Xilinx ISE Library e.g. /opt/Xilinx/14.7/ISE_DS/ISE/bin/lin64/
  export SDRLIFT_ISE_LIN=`path to ISE library`

  # The output directory for VHDL and graphical files (e.g. flow diagrams and schematics)
  export SDRLIFT_OUTPUT=$SDRLIFT_HOME/out/

  # The path to IP core libraries
  export SDRLIFT_IPLIB=$SDRLIFT_HOME/iplib/

  # Compile
  cd sdrlift
  sbt compile
~~~~ 

## Shell Commands
SdrLift Shell commands usage for application analysis, code generation and hardware synthesis.
~~~~
  Usage: 
        [-h | --h | --help]                                    Show this help message.
         -a | --app | --application  application               SDR application to be generated, below are examples:
                                     + tx80211a : IEEE 802.11a OFDM Transmitter
                                     + rx80211a : IEEE 802.11a OFDM Receiver
                                     + tx80222  : IEEE 802.22  OFDM Transmiter
                                     + rx80222  : IEEE 802.22  OFDM Receiver
                                     + mimotx   : IEEE 802.22  MIMO OFDM Receiver
                                     + mimorx   : IEEE 802.22  MIMO OFDM Transmitter
                                     + gsmddc   : GSM Digital  Down Converter
                                     + fmddc    : FM Digital Down Converter.
        [-s | --schematic | --schematic-diagram  schematic]    Schematic Diagram of the design.
	    [-f | --flow | --flow-diagram]                         Flow Diagram of the design.
	    [-g | --hdl | --hdl-create]                            HDL code generation.
	    [-o | --optimization | --optimization-type  o]         Optimization type for HDL generation.
	    [-t | --throughput | --throughput-constraint  t]       Specifies the throughput constraint for the design.
	    [-c | --compile | --compile-app]                       Compile the application HDL code with Xilinx ISE.
	    [-b | --benchmark | --save-benchmark]                  Save the Xilinx ISE benckmark results to the .csv files.
	    [-r | --raw | --raw-benchmark]                         Save the non-optimized VHDL FSMs and Code Lines results to
															   the .csv files.
	    [-q | --quiet]                                         Suppress some verbose output.
	    [remaining]                                            All remaining arguments that aren't associated with flags.

~~~~

## Quick start
All the commands described below are to be executed from SdrLift home directory:
~~~~
  cd $SDRLIFT_HOME
  sbt
~~~~
* And example to build and run within [SBT](https://www.scala-sbt.org/) a particular application in SdrLift (e.g. IEEE 802.11a OFDM Transmitter (tx80211a)).
~~~~
  sbt:sdrlift> run -a tx80211a -b
~~~~

### Contact ###

* Lekhobola Tsoeunyane (tsnlek001@myuct.ac.za)
