--------------------------------------------------------------------------------
-- Company: 
-- Engineer:
--
-- Create Date:   11:49:22 03/15/2017
-- Design Name:   
-- Module Name:   /home/lekhobola/Documents/dev/research/xilinx/ofdm/pcores/qpsk/tb/qpsk_tb.vhd
-- Project Name:  ofdm
-- Target Device:  
-- Tool versions:  
-- Description:   
-- 
-- VHDL Test Bench Created by ISE for module: qpsk
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
 
ENTITY qam_tb IS
END qam_tb;
 
ARCHITECTURE behavior OF qam_tb IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT qam
    PORT(
         clk : IN  std_logic;
         rst : IN  std_logic;
			en  : in std_logic;
			vld : out std_logic;
         din : IN  std_logic_vector(3 downto 0);
         Iout : OUT  std_logic_vector(15 downto 0);
         Qout : OUT  std_logic_vector(15 downto 0)
        );
    END COMPONENT;
    

   --Inputs
   signal clk : std_logic := '0';
   signal rst : std_logic := '0';
	signal vld : std_logic := '0';
	signal en : std_logic := '0';
   signal din : std_logic_vector(3 downto 0) := (others => '0');

 	--Outputs
   signal Iout : std_logic_vector(15 downto 0);
   signal Qout : std_logic_vector(15 downto 0);

   -- Clock period definitions
   constant clk_period : time := 10 ns;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: qam PORT MAP (
          clk => clk,
			 en  => en,
          rst => rst,
			 vld => vld,
          din => din,
          Iout => Iout,
          Qout => Qout
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
		
		wait until rising_edge(clk);
		en<= '1';
		din <= x"0";
		wait until rising_edge(clk);
		din <= x"1";
		wait until rising_edge(clk);
		din <= x"2";
	   wait until rising_edge(clk);
		din <= x"3";
		
		wait until rising_edge(clk);
		en <= '0';
      wait;
   end process;

END;
