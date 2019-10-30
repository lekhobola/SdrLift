--------------------------------------------------------------------------------
-- Company: 
-- Engineer:
--
-- Create Date:   14:50:08 02/13/2017
-- Design Name:   
-- Module Name:   /home/lekhobola/Documents/dev/research/xilinx/Sdf/pcores/std_fifo/rtl/STD_FIFO_TB.vhd
-- Project Name:  Sdf
-- Target Device:  
-- Tool versions:  
-- Description:   
-- 
-- VHDL Test Bench Created by ISE for module: STD_FIFO
-- 
-- Dependencies:
-- 
-- Revision:
-- Revision 0.01 - File Created
-- Additional Comments:
--
-- Notes: 
-- This testbench has been automatically generated using types std_logic and
-- std_logic_vector for the ports of the unit under test.  Xilinx recommends
-- that these types always be used for the top-level I/O of a design in order
-- to guarantee that the testbench will bind correctly to the post-implementation 
-- simulation model.
--------------------------------------------------------------------------------
LIBRARY ieee;
USE ieee.std_logic_1164.ALL;
 
-- Uncomment the following library declaration if using
-- arithmetic functions with Signed or Unsigned values
--USE ieee.numeric_std.ALL;
 
ENTITY STD_FIFO_TB IS
END STD_FIFO_TB;
 
ARCHITECTURE behavior OF STD_FIFO_TB IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT STD_FIFO
	 GENERIC(
	 	constant DATA_WIDTH  : positive := 8;
		constant FIFO_DEPTH	: positive := 256
	 );
    PORT(
         CLK : IN  std_logic;
         RST : IN  std_logic;
         WriteEn : IN  std_logic;
         DataIn : IN  std_logic_vector(7 downto 0);
         ReadEn : IN  std_logic;
         DataOut : OUT  std_logic_vector(7 downto 0);
         Empty : OUT  std_logic;
         Full : OUT  std_logic
        );
    END COMPONENT;
    

   --Inputs
   signal CLK : std_logic := '0';
   signal RST : std_logic := '0';
   signal WriteEn : std_logic := '0';
   signal DataIn : std_logic_vector(7 downto 0) := (others => '0');
   signal ReadEn : std_logic := '0';

 	--Outputs
   signal DataOut : std_logic_vector(7 downto 0);
   signal Empty : std_logic;
   signal Full : std_logic;

   -- Clock period definitions
   constant CLK_period : time := 10 ns;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: STD_FIFO
	GENERIC MAP(
	DATA_WIDTH => 8,
   FIFO_DEPTH => 4
	)
	PORT MAP (
          CLK => CLK,
          RST => RST,
          WriteEn => WriteEn,
          DataIn => DataIn,
          ReadEn => ReadEn,
          DataOut => DataOut,
          Empty => Empty,
          Full => Full
        );

   -- Clock process definitions
   CLK_process :process
   begin
		CLK <= '0';
		wait for CLK_period/2;
		CLK <= '1';
		wait for CLK_period/2;
   end process;
 

   -- Stimulus process
   stim_proc: process
   begin		
      -- hold reset state for 100 ns.
      wait for 100 ns;	

      wait for CLK_period*10;

      -- insert stimulus here 
		 wait until rising_edge(clk);
		 WriteEn <= '1';
       DataIn  <= x"01";
       ReadEn  <= '0';
		 wait until rising_edge(clk);
		 WriteEn <= '1';
       DataIn  <= x"02";
       ReadEn  <= '0';
		 wait until rising_edge(clk);
		 WriteEn <= '1';
       DataIn  <= x"03";
       ReadEn  <= '0';
		 wait until rising_edge(clk);
		 WriteEn <= '1';
       DataIn  <= x"04";
       ReadEn  <= '0';
		 wait until rising_edge(clk);
		 WriteEn <= '0';
       DataIn  <= x"00";
       ReadEn  <= '1';
      wait;
   end process;

END;
