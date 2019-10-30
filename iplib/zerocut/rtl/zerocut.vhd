library IEEE;
use IEEE.STD_LOGIC_1164.ALL;


entity zerocut is
	generic(
		dWidth 	 : positive;
		oSamples  : positive;
		padLength : positive
	);
	port(
	 clk   : in  std_logic;
	 rst   : in  std_logic;
	 en    : in  std_logic;
	 Iin   : in  std_logic_vector(dWidth-1 downto 0);
	 Qin   : in  std_logic_vector(dWidth-1 downto 0);
	 vld   : out std_logic;
	 Iout  : out std_logic_vector(dWidth-1 downto 0);
	 Qout  : out std_logic_vector(dWidth-1 downto 0);
	 done  : out std_logic
	); 
end zerocut;

architecture Behavioral of zerocut is
	signal pad_cnt          : integer  := 0;
	signal sample_cnt          : integer  := 0;
	signal index    	      : integer  := 0;
	signal state            : std_logic_vector(1 downto 0) := "00";
begin

	process(clk,rst)
	begin
		if(rst = '1') then
			Iout  <= (others => '0');
			Qout  <= (others => '0');
			index <= 0;
		elsif(rising_edge(clk)) then
			vld <= '0';
			state <= "00";
			Iout <= (others => '0');
			Qout <= (others => '0');
			done <= '0';
			if(en = '1') then
				case state is
					when "00" =>	
						state <= "00";
					   vld  <= '1';
						done <= '0';
						Iout <= Iin;
						Qout <= Qin;
						sample_cnt <= sample_cnt + 1;	
						
						if(sample_cnt = oSamples-1) then		
                     sample_cnt <= 0;						
							state <= "01";
						end if;
					when "01" =>
						state <= "01";
						vld <= '0';
						Iout <= (others => '0');
						Qout <= (others => '0');
						sample_cnt <= sample_cnt + 1;	
							
						if(sample_cnt = padLength-1) then
							sample_cnt <= 0;
							state <= "00";
							done <= '1';
						end if;
					when others =>
						null;
				end case;
			end if;
		end if;
	end process;
	
end Behavioral;

