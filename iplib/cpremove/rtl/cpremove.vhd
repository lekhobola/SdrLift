library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity cpremove is
	GENERIC(
		dWidth 	 	 : positive;
		oSamples     : positive;
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
         Qout : out std_logic_vector(dWidth-1 downto 0)
        );
end cpremove;

architecture Behavioral of cpremove is	
	signal sample_cnt        : integer  := 0;
	signal prefix_cnt        : integer  := 0;
	signal state	          : std_logic_vector(1 downto 0) := "00";
	signal IoutT  : std_logic_vector(dWidth-1 downto 0);
   signal QoutT  : std_logic_vector(dWidth-1 downto 0);
	signal vldT          : std_logic := '0';
	signal started           : std_logic := '0';
begin
	process(clk,rst)
	begin
		if(rst = '1') then
			
		elsif(rising_edge(clk)) then
			vldT  <= '0';
			done <= '0';
			if(en = '1') then
				case state is
					when "00" =>
						sample_cnt <= sample_cnt + 1;
						vldT <= '0';
					   IoutT <= (others => '0');
						QoutT <= (others => '0');
						if(sample_cnt = prefixLength-1) then
						   sample_cnt <= 0;
							state <= "01";
						end if;
							
					when "01" =>
						state <= "01";
						IoutT <= Iin;
						QoutT <= Qin;
						sample_cnt <= sample_cnt + 1;
						vldT <= '1';
						if(sample_cnt = oSamples) then							
							sample_cnt <= 0;
							state <= "00";
							IoutT <= (others => '0');
							QoutT <= (others => '0');
							done <= '1';
							vldT <= '0';
						end if;					
					when others =>
						null;
				end case;
			end if;
		end if;
	end process;
	Iout <= IoutT;
	Qout <= QoutT;
	vld <= vldT;
end Behavioral;

