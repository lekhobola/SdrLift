library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity sync is
	generic(
		dwidth : natural := 12
	);
	port ( 
	  rst 	   : in  STD_LOGIC;
	  en	   : in  STD_LOGIC;
	  clk      : in  std_logic;
	  iin      : in  STD_LOGIC_VECTOR(dwidth-1 downto 0);
	  qin      : in  STD_LOGIC_VECTOR(dwidth-1 downto 0);
	  vld      : out std_logic;
	  iout     : out  STD_LOGIC_VECTOR(dwidth-1 downto 0);
	  qout     : out  STD_LOGIC_VECTOR(dwidth-1 downto 0)
	);
end sync;

architecture Behavioral of sync is

begin
   
end Behavioral;

