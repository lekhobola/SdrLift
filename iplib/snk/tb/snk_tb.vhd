--------------------------------------------------------------------------------
-- Company: 
-- Engineer:
--
-- Create Date:   10:01:52 11/13/2017
-- Design Name:   
-- Module Name:   /home/lekhobola/Documents/dev/research/xilinx/Ad936xCtrl/pcores/snk/tb/snk_tb.vhd
-- Project Name:  Ad936xCtrl
-- Target Device:  
-- Tool versions:  
-- Description:   
-- 
-- VHDL Test Bench Created by ISE for module: snk
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
 
ENTITY snk_tb IS
END snk_tb;
 
ARCHITECTURE behavior OF snk_tb IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT snk
    PORT(
         clk : IN  std_logic;
         rst : IN  std_logic;
         en : IN  std_logic;
         din : IN  std_logic_vector(7 downto 0);
         vld : OUT  std_logic;
         dout : OUT  std_logic_vector(7 downto 0)
        );
    END COMPONENT;
    

   --Inputs
   signal clk : std_logic := '0';
   signal rst : std_logic := '0';
   signal en : std_logic := '0';
   signal din : std_logic_vector(7 downto 0) := (others => '0');

 	--Outputs
   signal vld : std_logic;
   signal dout : std_logic_vector(7 downto 0);

   -- Clock period definitions
   constant clk_period : time := 10 ns;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: snk PORT MAP (
          clk => clk,
          rst => rst,
          en => en,
          din => din,
          vld => vld,
          dout => dout
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
		en <= '1';
		din <= x"01";
		wait until rising_edge(clk);
		en <= '0';
		wait until rising_edge(clk);
		en <= '1';
		din <= x"02";
		wait until rising_edge(clk);
		en <= '0';
		wait until rising_edge(clk);
		en <= '1';
		din <= x"03";
		wait until rising_edge(clk);
		en <= '0';
		wait until rising_edge(clk);
		en <= '1';
		din <= x"04";
		wait until rising_edge(clk);
		en <= '0';
		wait until rising_edge(clk);
		en <= '1';
		din <= x"05";
		wait until rising_edge(clk);
		en <= '0';
		wait until rising_edge(clk);
		en <= '1';
		din <= x"06";
		wait until rising_edge(clk);
		en <= '0';
		
      wait;
   end process;

END;
