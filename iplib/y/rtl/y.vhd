library IEEE;
use IEEE.STD_LOGIC_1164.ALL;


entity y is
	Port ( 
		clk    : in  std_logic;
		rst    : in  std_logic;
		en		 : in  std_logic;           
		din    : in  std_logic_vector(7 downto 0);
		vld    : out std_logic;
		dout   : out std_logic_vector(7 downto 0)
	);
end y;

architecture Behavioral of y is
	signal cnt    : integer := 0;
	signal state  : std_logic_vector(2 downto 0);
	signal dtemp  : std_logic_vector(7 downto 0);
	signal vldTmp : std_logic;
begin
	process(clk,rst)
	begin
		if(rst = '1') then
			dtemp <= (others => '0');
			vldTmp   <= '0';
		elsif(rising_edge(clk)) then
			vldTmp  <= '0';
			dtemp <= (others=>'0');
			state <= "000";			
			case state is
				when "000" =>
					state <= "000";
					dtemp <= din;
					if(en = '1') then
						vldTmp   <= '1';
						dtemp  <= din;
						state <= "001";
					end if;
				when "001" =>
					state <= "010";
					vldTmp   <= '0';
					dtemp  <= (others => '0');
					
				when "010" =>
					state <= "011";
					vldTmp   <= '1';
					dtemp  <= din;
					
				when "011" =>
					state <= "100";
					vldTmp <= '0';
					dtemp  <= (others => '0');
					
				when "100" =>
					state <= "000";
					vldTmp   <= '1';
					dtemp  <= din;						
					
				when others => null;	
			end case;		
		end if;
	end process;
	vld <= vldTmp;
	dout <= dtemp; 
end Behavioral;




