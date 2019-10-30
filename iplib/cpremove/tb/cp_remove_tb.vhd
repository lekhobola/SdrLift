--------------------------------------------------------------------------------
-- Company: 
-- Engineer:
--
-- Create Date:   15:05:58 06/22/2017
-- Design Name:   
-- Module Name:   /home/lekhobola/Documents/dev/research/xilinx/FPTConference/fpt/pcores/ip/cpremove/tb/cp_remove_tb.vhd
-- Project Name:  fpt
-- Target Device:  
-- Tool versions:  
-- Description:   
-- 
-- VHDL Test Bench Created by ISE for module: cpremove
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
 
ENTITY cp_remove_tb IS
END cp_remove_tb;
 
ARCHITECTURE behavior OF cp_remove_tb IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT cpremove
    PORT(
         clk : IN  std_logic;
         rst : IN  std_logic;
         en : IN  std_logic;
         Iin : IN  std_logic_vector(-1 to 0);
         Qin : IN  std_logic_vector(-1 to 0);
         vld : OUT  std_logic;
         done : OUT  std_logic;
         Iout : OUT  std_logic_vector(-1 to 0);
         Qout : OUT  std_logic_vector(-1 to 0);
         state_dbg : OUT  std_logic_vector(1 downto 0)
        );
    END COMPONENT;
    

   --Inputs
   signal clk : std_logic := '0';
   signal rst : std_logic := '0';
   signal en : std_logic := '0';
   signal Iin : std_logic_vector(-1 to 0) := (others => '0');
   signal Qin : std_logic_vector(-1 to 0) := (others => '0');

 	--Outputs
   signal vld : std_logic;
   signal done : std_logic;
   signal Iout : std_logic_vector(-1 to 0);
   signal Qout : std_logic_vector(-1 to 0);
   signal state_dbg : std_logic_vector(1 downto 0);

   -- Clock period definitions
   constant clk_period : time := 10 ns;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: cpremove PORT MAP (
          clk => clk,
          rst => rst,
          en => en,
          Iin => Iin,
          Qin => Qin,
          vld => vld,
          done => done,
          Iout => Iout,
          Qout => Qout,
          state_dbg => state_dbg
        );

   -- Clock process definitions
   clk_process :process
   begin
		clk <= '0';
		wait for clk_period/2;
		clk <= '1';
		wait for clk_period/2;
   end process;
 

   -- Stimulus process
   stim_proc: process
   begin		
      -- hold reset state for 100 ns.
      wait for 100 ns;	

      wait for clk_period*10;

      -- insert stimulus here 

      wait;
   end process;

END;
