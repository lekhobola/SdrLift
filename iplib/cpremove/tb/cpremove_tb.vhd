--------------------------------------------------------------------------------
-- Company: 
-- Engineer:
--
-- Create Date:   13:55:06 03/19/2017
-- Design Name:   
-- Module Name:   /home/lekhobola/Documents/dev/research/xilinx/ofdm/pcores/zeropad/tb/zeropad_tb.vhd
-- Project Name:  ofdm
-- Target Device:  
-- Tool versions:  
-- Description:   
-- 
-- VHDL Test Bench Created by ISE for module: zeropad
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
 
ENTITY cpremove_tb IS
END cpremove_tb;
 
ARCHITECTURE behavior OF cpremove_tb IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT cpremove
	 GENERIC(
		dWidth 	 : positive;
		oSamples  : positive;
		prefixLength : positive
	 );
    PORT(
         clk  : IN  std_logic;
         rst  : IN  std_logic;
         en   : IN  std_logic;
         Iin  : IN  std_logic_vector(dWidth-1 downto 0);
         Qin  : IN  std_logic_vector(dWidth-1 downto 0);
         vld  : OUT  std_logic;
			done : OUT  std_logic;
         Iout : OUT  std_logic_vector(dWidth-1 downto 0);
         Qout : OUT  std_logic_vector(dWidth-1 downto 0);
			state_dbg : out std_logic_vector(1 downto 0)
        );
    END COMPONENT;
    

   --Inputs
   signal clk : std_logic := '0';
   signal rst : std_logic := '0';
   signal en : std_logic := '0';
	constant dWidth : integer := 8;
   signal Iin : std_logic_vector(dWidth-1 downto 0) := (others => '0');
   signal Qin : std_logic_vector(dWidth-1 downto 0) := (others => '0');

 	--Outputs
   signal vld, done : std_logic;
   signal Iout : std_logic_vector(dWidth-1 downto 0);
   signal Qout : std_logic_vector(dWidth-1 downto 0);
	signal state : std_logic_vector(1 downto 0);

   -- Clock period definitions
   constant clk_period : time := 10 ns;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: cpremove 
		 GENERIC MAP(
			dWidth 	    => 8,
			oSamples     => 5,
			prefixLength => 3
		 )
	    PORT MAP (
          clk  => clk,
          rst  => rst,
          en   => en,
          Iin  => Iin,
          Qin  => Qin,
          vld  => vld,
			 done => done,
          Iout => Iout,
          Qout => Qout,
			 state_dbg => state
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
		Iin <= x"01";
		Qin <= x"01";
		wait until rising_edge(clk);
		Iin <= x"02";
		Qin <= x"02";
		wait until rising_edge(clk);
		Iin <= x"03";
		Qin <= x"03";
		wait until rising_edge(clk);
		Iin <= x"04";
		Qin <= x"04";
		wait until rising_edge(clk);
		Iin <= x"05";
		Qin <= x"05";	   
		wait until rising_edge(clk);
		Iin <= x"06";
		Qin <= x"06";	   
		wait until rising_edge(clk);
		Iin <= x"07";
		Qin <= x"07";	   
		wait until rising_edge(clk);
		Iin <= x"08";
		Qin <= x"08";	   
		wait until rising_edge(clk);
		
		-- symbol 2
		
		Iin <= x"00";
		Qin <= x"00";
		wait until done = '1';
		en <= '0';
		en <= '0';
	
		
		--symbol 2
--		wait until vld = '0';
--		Iin <= x"00";
--		Qin <= x"00";
--		wait until rising_edge(clk);
--		Iin <= x"01";
--		Qin <= x"01";
--		wait until rising_edge(clk);
--		Iin <= x"02";
--		Qin <= x"02";
--		wait until rising_edge(clk);
--		Iin <= x"03";
--		Qin <= x"03";
--		wait until rising_edge(clk);
--		Iin <= x"04";
--		Qin <= x"04";

--		wait until rising_edge(clk);
--		Iin <= x"00";
--		Qin <= x"00";
      wait;
   end process;

END;
