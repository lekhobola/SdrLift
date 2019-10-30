library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity adder is
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
end adder;

architecture Behavioral of adder is
	signal cnt : integer := 0;
	signal sum : std_logic_vector(outWidth-1 downto 0) := (others => '0');
begin
	process(clk,rst)
	begin
		if(rst = '1') then
			dout <= (others => '0');
		elsif(rising_edge(clk)) then
			vld <= '0';
			if(en = '1') then
				cnt <= cnt + 1;
				if(cnt < 3) then
					sum <= sum + din;
					dout <= (others => '0');
				else
					vld <= '1';
					sum <= (others => '0');
					cnt <= 0;
					dout <= sum;
				end if;
			end if;
		end if;
	end process;
end Behavioral;

