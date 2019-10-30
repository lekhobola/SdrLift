library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity assembler is
	generic(
		dwidth : natural := 16
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
end assembler;

architecture Behavioral of assembler is
	
begin
	
end Behavioral;

