library IEEE;
use IEEE.STD_LOGIC_1164.ALL;


entity x is
	Port ( 
		clk    : in  std_logic;
		rst    : in  std_logic;
		en		 : in  std_logic;           
		din    : in  std_logic_vector(7 downto 0);
		vld    : out std_logic;
		dout   : out std_logic_vector(7 downto 0)
	);
end x;

architecture Behavioral of x is
	signal cnt    : integer := 0;
	signal state  : std_logic_vector(1 downto 0);
	signal dtemp  : std_logic_vector(7 downto 0);
begin
	process(clk,rst)
	begin
		if(rst = '1') then
			dtemp <= (others => '0');
			vld  <= '0';
		elsif(rising_edge(clk)) then
			vld  <= '0';
			state <= "00";
			dtemp <= (others => '0');
			
			if(en = '1') then
				case state is
					when "00" =>
						state <= "01";
						vld   <= '1';						
						
					when "01" =>
						state <= "10";
						vld   <= '1';
						dtemp  <= x"01";
						
					when "10" =>
						state <= "11";					
						dtemp <= x"02";
						vld   <= '1';
					
					when "11" =>
						state <= "00";					
						dtemp <= (others => '0');
						vld   <= '0';
						
					when others => null;	
				end case;		
			end if;	
			
		end if;
	end process;
	dout <= dtemp;
end Behavioral;
