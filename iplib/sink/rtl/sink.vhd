library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity sink is
	generic(
		dWidth : natural := 22
	);
   port(
		clk  : in   std_logic;
		rst  : in   std_logic;
		en   : in   std_logic;
		vld  : out  std_logic;
		qin  : in   std_logic_vector(dWidth-1 downto 0);
		iin  : in   std_logic_vector(dWidth-1 downto 0);
		qout : out  std_logic_vector(dWidth-1 downto 0);
		iout : out  std_logic_vector(dWidth-1 downto 0)		
	);
end sink;

architecture Behavioral of sink is
begin
end Behavioral;
