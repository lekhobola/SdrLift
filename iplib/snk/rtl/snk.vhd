library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity snk is
   port(
		clk  : in   std_logic;
		rst  : in   std_logic;
		en   : in   std_logic;
		din  : in   std_logic_vector(7 downto 0);
		vld  : out  std_logic;
		dout : out  std_logic_vector(7 downto 0)
	);
end snk;

architecture Behavioral of snk is
begin
	process(clk,rst)
	begin
		if(rst = '1') then
		elsif(rising_edge(clk)) then
			vld <= '0';
			if(en = '1') then
				dout <= din;
				vld <= '1';
			end if;
		end if;
	end process;
end Behavioral;	