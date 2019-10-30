library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
USE IEEE.MATH_REAL.ALL;

entity gain is
	generic(
		dwidth : positive;
		divisor: positive
	);
	port(
		clk  : in  std_logic;
		rst  : in  std_logic;
		en   : in  std_logic;
		iin  : in  std_logic_vector(dwidth-1 downto 0);
		qin  : in  std_logic_vector(dwidth-1 downto 0);
		vld  : out std_logic;
		iout : out std_logic_vector(dwidth-1 downto 0);
		qout : out std_logic_vector(dwidth-1 downto 0));
end gain;

architecture Behavioral of gain is
	constant shiftCount : positive := INTEGER(LOG2(real(divisor)));
	signal vldT : std_logic := '0';
	signal quotientI : std_logic_vector(dwidth-1 downto 0);
	signal quotientQ : std_logic_vector(dwidth-1 downto 0);
begin
	process(clk,rst)
	begin
		if(rst = '1') then
			quotientI <= (others => '0');
			quotientQ <= (others => '0');
			vldT <= '0';
		elsif(rising_edge(clk)) then
			quotientI <= (others => '0');
			quotientQ <= (others => '0');
			vldT <= '0';
			if(en = '1') then
				vldT <= '1';
				quotientI <= (shiftCount-1 downto 0 => din(dwidth-1)) & iin(dwidth-1 downto shiftCount);
				quotientQ <= (shiftCount-1 downto 0 => din(dwidth-1)) & qin(dwidth-1 downto shiftCount);
			end if;
		end if;
	end process;
	vld <= vldT;
	iout <= quotientI;
	qout <= quotientQ;
end Behavioral;

