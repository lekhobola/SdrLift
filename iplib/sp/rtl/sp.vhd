library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity sp is
    generic(
		dWidth  : positive := 8
	 );
    PORT(
         clk  : in  std_logic;
         rst  : in  std_logic;
         en   : in  std_logic;
         din  : in  std_logic_vector(dWidth-1 downto 0);
         vld  : out std_logic;
         dout1: out std_logic_vector(dWidth-1 downto 0);
		 dout2: out std_logic_vector(dWidth-1 downto 0);
		 dout3: out std_logic_vector(dWidth-1 downto 0);
		 dout4: out std_logic_vector(dWidth-1 downto 0)
     );
end sp;

architecture Behavioral of sp is
begin
	
end Behavioral;
