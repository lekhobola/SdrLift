library IEEE;
use IEEE.STD_LOGIC_1164.all;
use IEEE.STD_LOGIC_ARITH.all;
use IEEE.STD_LOGIC_UNSIGNED.all;


entity qammod is
  port (
    clk   : in  std_logic;
    rst   : in  std_logic;
	 en    : in  std_logic;
    din   : in  std_logic_vector(3 downto 0);
	 vld   : out std_logic;
    Iout  : out std_logic_vector(15 downto 0);
    Qout  : out std_logic_vector(15 downto 0));
end qam;

architecture qammod of qammod is

begin

  process (clk, rst)
    constant p3  : std_logic_vector(15 downto 0) := "0010100001111010"; 
	 constant n3  : std_logic_vector(15 downto 0) := "1101011110000110"; 
    constant p9  : std_logic_vector(15 downto 0) := "0111100101101110"; 
	 constant n9  : std_logic_vector(15 downto 0) := "1000011010010010";
  begin
    if rst = '1' then
      Iout <= (others => '0');
      Qout <= (others => '0');
    elsif clk'event and clk = '1' then

		vld <= '0';
		Iout <= (others => '0');
		Qout <= (others => '0');
		if(en ='1') then
			vld <= '1';
			case din is
			  when x"0" =>
				 Iout <= n9;
				 Qout <= p9;
			  when x"1" =>
				 Iout <= n9;
				 Qout <= p3;
			  when x"2" =>
				 Iout <= n9;
				 Qout <= n3;
			  when x"3" =>
				 Iout <= n9;
				 Qout <= n9;
			  when x"4" =>
				 Iout <= n3;
				 Qout <= p9;
			  when x"5" =>
				 Iout <= n3;
				 Qout <= p3;
			  when x"6" =>
				 Iout <= n3;
				 Qout <= n3;
			  when x"7" =>
				 Iout <= n3;
				 Qout <= n9;
			  when x"8" =>
				 Iout <= p3;
				 Qout <= p9;
			  when x"9" =>
				 Iout <= p3;
				 Qout <= p3;
			  when x"A" =>
				 Iout <= p3;
				 Qout <= n3;
			  when x"B" =>
				 Iout <= p3;
				 Qout <= n9;
			  when x"C" =>
				 Iout <= p9;
				 Qout <= p9;
			  when x"D" =>
				 Iout <= p9;
				 Qout <= p3;
			  when x"E" =>
				 Iout <= p9;
				 Qout <= n3;
			  when x"F" =>
				 Iout <= p9;
				 Qout <= n9;
			  when others =>
				Iout <= (others => '0');
				Qout <= (others => '0');
		
			end case;
		end if;
    end if;
  end process;

end qam;
