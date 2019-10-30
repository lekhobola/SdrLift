library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity p2s is
    generic(
		dWidth  : positive := 8
	 );
    PORT(
         clk  : in  std_logic;
         rst  : in  std_logic;
         en   : in  std_logic;
		 din1 : in std_logic_vector(dWidth-1 downto 0);
		 din2 : in std_logic_vector(dWidth-1 downto 0);
		 din3 : in std_logic_vector(dWidth-1 downto 0);
		 din4 : in std_logic_vector(dWidth-1 downto 0);
         dout : out  std_logic_vector(dWidth-1 downto 0);
         vld  : out std_logic
     );
end p2s;

architecture Behavioral of p2s is
begin
	
end Behavioral;
