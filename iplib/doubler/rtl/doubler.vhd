library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;


entity doubler is
	generic(
		inWidth : natural := 8;
		outWidth : natural := 8
	);
   port(
		clk  : in   std_logic;
		rst  : in   std_logic;
		en   : in   std_logic;
		din  : in   std_logic_vector(inWidth-1 downto 0);
		vld  : out  std_logic;
		dout : out  std_logic_vector(outWidth-1 downto 0)
	);
end doubler;

architecture Behavioral of doubler is
	signal prod : std_logic_vector(2 * inWidth-1 downto 0);
begin
	process(clk,rst) 
	begin
		if(rst = '1') then
		elsif(rising_edge(clk)) then
			vld <= '0';
			prod <= (others => '0');
			if(en = '1') then
				prod <= din * x"02";
				vld <= '1';
			end if;
		end if;
	end process;
	dout <= prod(outWidth-1 downto 0);
end Behavioral;

