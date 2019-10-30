library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity fftifft is
	generic (
		N           : NATURAL   := 64;
		DIN_WIDTH   : NATURAL   := 16;
		DOUT_WIDTH  : NATURAL   := 22;
		MODE        : std_logic := '1'
	);
	port (
		clk   : in std_logic; 
		rst   : in std_logic;
		en    : in std_logic;
		iin   : in std_logic_vector(DIN_WIDTH-1 downto 0);
		qin   : in std_logic_vector(DIN_WIDTH-1 downto 0);
		vld   : out std_logic;
		iout  : out std_logic_vector(DOUT_WIDTH-1 downto 0);
		qout : out std_logic_vector(DOUT_WIDTH-1 downto 0)
	);
end fftifft;

architecture Behavioral of fftifft is
	
begin
	
end Behavioral;

