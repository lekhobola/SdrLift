library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity preamble is
	generic(
		dwidth : natural := 22
	);
	port(
		clk   : in std_logic;
		rst   : in std_logic;
		en    : in std_logic;
		Iin  : in std_logic_vector(dwidth-1 downto 0);
		Qin  : in std_logic_vector(dwidth-1 downto 0);
		vld   : out std_logic;
		Iout : out std_logic_vector(dwidth-1 downto 0);
		Qout : out std_logic_vector(dwidth-1 downto 0)
	);	
end preamble;

architecture Behavioral of preamble is
	
begin
	
end Behavioral;

