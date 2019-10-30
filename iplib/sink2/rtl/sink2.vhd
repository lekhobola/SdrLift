library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity sink2 is
	generic(
		dWidth : natural := 22
	);
   port(
		clk  : in   std_logic;
		rst  : in   std_logic;
		en   : in   std_logic;
		vld  : out  std_logic;
		din  : in   std_logic_vector(dWidth-1 downto 0);
		dout : out  std_logic_vector(dWidth-1 downto 0)
	);
end sink2;

architecture Behavioral of sink2 is
begin
end Behavioral;
