library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity source is
	generic(
		dWidth : natural := 16
	);
   port(
		clk  : in   std_logic;
		rst  : in   std_logic;
		en   : in   std_logic;
		din  : in   std_logic_vector(dWidth-1 downto 0);
		vld  : out  std_logic;
		dout : out  std_logic_vector(dWidth-1 downto 0)
	);
end source;

architecture Behavioral of source is
begin
end Behavioral;
