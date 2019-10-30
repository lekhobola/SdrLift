library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;

entity src is
   port(
		clk  : in   std_logic;
		rst  : in   std_logic;
		en   : in   std_logic;
		din  : in   std_logic_vector(7 downto 0);
		vld  : out  std_logic;
		dout : out  std_logic_vector(7 downto 0)
	);
end src;

architecture Behavioral of src is
	signal cnt  : integer := 0;
	signal data : std_logic_vector(7 downto 0) := x"01";
begin
	process(clk,rst)
	begin
		if(rst = '1') then
		elsif(rising_edge(clk)) then
			vld <= '0';
			if(en = '1') then
				cnt <= cnt + 1;
				if(cnt = 7) then
					cnt <= 0;
					data <= data + 1;
					dout <= data;
					vld <= '1';
				end if;
			end if;
		end if;
	end process;
end Behavioral;
