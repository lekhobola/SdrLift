library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity accum is
	generic(
		dwidth : natural := 22
	);
	port(
		clk   : in std_logic;
		rst   : in std_logic;
		en    : in std_logic;
		iin  : in std_logic_vector(dwidth-1 downto 0);
		qin  : in std_logic_vector(dwidth-1 downto 0);
		vld   : out std_logic;
		iout : out std_logic_vector(dwidth-1 downto 0);
		qout : out std_logic_vector(dwidth-1 downto 0)
	);	
end accum;

architecture Behavioral of accum is
	
begin
	
end Behavioral;

