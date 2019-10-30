library IEEE;
use IEEE.STD_LOGIC_1164.ALL;


entity sampler is
	generic(
		dwidth : natural := 8
	);
   port(
		clk  : in   std_logic;
		rst  : in   std_logic;
		en   : in   std_logic;
		din  : in   std_logic_vector(dwidth-1 downto 0);
		vld  : out  std_logic;
		dout : out  std_logic_vector(dwidth-1 downto 0)
	);
end sampler;

architecture Behavioral of sampler is
	signal cnt : integer := 0;
begin
	
	process(clk,rst)
	begin
		if(rst = '1') then
			dout <= (others => '0');
		elsif(rising_edge(clk)) then
			if(en = '1') then
				cnt <= cnt + 1;
				vld <= '0';
				if(cnt < 2) then
					dout <= din;
					vld <= '1';
				else
					dout <= (others => '0');
					cnt <= 0;
				end if;
			end if;
		end if;
	end process;

end Behavioral;

