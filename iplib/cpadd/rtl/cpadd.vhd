library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity cpadd is
	GENERIC(
		dWidth 	 	 : positive;
		iSamples  	 : positive;
		prefixLength : positive
	 );
    PORT(
         clk  : in  std_logic;
         rst  : in  std_logic;
         en   : in  std_logic;
         Iin  : in  std_logic_vector(dWidth-1 downto 0);
         Qin  : in  std_logic_vector(dWidth-1 downto 0);
         vld  : out std_logic;
			done : out std_logic;
         Iout : out std_logic_vector(dWidth-1 downto 0);
         Qout : out std_logic_vector(dWidth-1 downto 0);
			state_dbg : out std_logic_vector(1 downto 0)
        );
end cpadd;

architecture Behavioral of cpadd is
	type ram_type is array (0 to iSamples - 1) of std_logic_vector(dWidth-1 downto 0);
	signal ram_i             : ram_type := (others => (others => '0'));
	signal ram_q             : ram_type := (others => (others => '0'));
	signal sample_cnt        : integer  := 0;
	signal prefix_cnt        : integer  := 0;
	signal state	          : std_logic_vector(1 downto 0) := "00";
	signal started           : std_logic := '0';
begin
	process(clk,rst)
	begin
		if(rst = '1') then
			
		elsif(rising_edge(clk)) then
			vld  <= '0';
			done <= '0';
			if(en = '1') then
				case state is
					when "00" =>
						state <= "00";
						done <= '0';
						
						ram_i(sample_cnt) <= Iin;
						ram_q(sample_cnt) <= Qin;
						sample_cnt <= sample_cnt + 1;
						
						if(sample_cnt >= iSamples-prefixLength) then
							Iout <= Iin; 
							Qout <= Qin;
							vld <= '1';
						end if;

						if(sample_cnt = iSamples-1) then
							sample_cnt <= 0;
							state <= "01";
						end if;
							
					when "01" =>
						state <= "01";
						vld <= '1';
						if(sample_cnt < iSamples) then
							Iout <= ram_i(sample_cnt);
							Qout <= ram_q(sample_cnt);
							sample_cnt <= sample_cnt + 1;
						elsif(sample_cnt = iSamples) then
							sample_cnt <= 0;
							state <= "00";
							Iout <= (others => '0');
							Qout <= (others => '0');
							done <= '1';
							vld <= '0';
						end if;					
					when others =>
						null;
				end case;
			end if;
		end if;
	end process;
	
	state_dbg <= state;
end Behavioral;

