
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
USE IEEE.MATH_REAL.ALL;

entity CIC is
	generic(
		DIN_WIDTH	  			 : natural;
		DOUT_WIDTH	  			 : natural;
		NUMBER_OF_STAGES	 : natural;
		DIFFERENTIAL_DELAY : natural;
		SAMPLE_RATE_CHANGE : natural;
		FILTER_TYPE        : std_logic
	);
	port(
	   CLK  : in  std_logic;
		RST  : in  std_logic;
		EN   : in std_logic;
		DIN  : in  std_logic_vector(DIN_WIDTH - 1 downto 0);
		VLD  : out std_logic;
		DOUT : out std_logic_vector(DOUT_WIDTH-1 downto 0) 
	);
end CIC;

architecture Behavioral of CIC is

	COMPONENT decimator is
		GENERIC(
			DIN_WIDTH	  			 : natural;
			NUMBER_OF_STAGES	 : natural; 
			DIFFERENTIAL_DELAY : natural; 
			SAMPLE_RATE_CHANGE : natural;  
			CLKIN_PERIOD_NS    : real
		);
		PORT(
			CLK  : in  std_logic;
			RST  : in  std_logic;
			EN   : in std_logic;
			DIN  : in  std_logic_vector(DIN_WIDTH - 1 downto 0);
			VLD  : out std_logic;
			DOUT : out std_logic_vector(DIN_WIDTH + (NUMBER_OF_STAGES * integer(ceil(log2(real(DIFFERENTIAL_DELAY * SAMPLE_RATE_CHANGE))))) - 1 downto 0) 
		);
	END COMPONENT;
	
	COMPONENT interpolator IS
		GENERIC(
			DIN_WIDTH	  		 : natural;
			NUMBER_OF_STAGES	 : natural; 
			DIFFERENTIAL_DELAY : natural; 
			SAMPLE_RATE_CHANGE : natural; 
			CLKIN_PERIOD_NS    : real
		);
		PORT(
			CLK  : in  std_logic;
			RST  : in  std_logic;
			DIN  : in  std_logic_vector(DIN_WIDTH - 1 downto 0);
			RDY  : out std_logic;
			VLD  : out std_logic;
			DOUT : out std_logic_vector(DIN_WIDTH + (NUMBER_OF_STAGES * integer(ceil(log2(real(DIFFERENTIAL_DELAY * SAMPLE_RATE_CHANGE))))) - 1 downto 0)
		);
	END COMPONENT;
	
begin
	DecimatorGen : if FILTER_TYPE = '0' generate
	decimator_inst : decimator
		GENERIC MAP(
			DIN_WIDTH	  		 => DIN_WIDTH,
			NUMBER_OF_STAGES	 => NUMBER_OF_STAGES,
			DIFFERENTIAL_DELAY => DIFFERENTIAL_DELAY,
			SAMPLE_RATE_CHANGE => SAMPLE_RATE_CHANGE,
			CLKIN_PERIOD_NS    => CLKIN_PERIOD_NS
		)
		PORT MAP(
			CLK  => CLK,
			RST  => RST,
			EN   => EN,
			DIN  => DIN,
		   VLD  => VLD,
			DOUT => DOUT
		);
	end generate;
	
	InterpolatorGen : if FILTER_TYPE = '1' generate
	interpolator_inst : interpolator
		GENERIC MAP(
			DIN_WIDTH	  			 => DIN_WIDTH,
			NUMBER_OF_STAGES	 => NUMBER_OF_STAGES,
			DIFFERENTIAL_DELAY => DIFFERENTIAL_DELAY,
			SAMPLE_RATE_CHANGE => SAMPLE_RATE_CHANGE,
			CLKIN_PERIOD_NS    => CLKIN_PERIOD_NS
		)
		PORT MAP(
			CLK  => CLK,
			RST  => RST,
			DIN  => DIN,
			RDY  => open,
			VLD  => VLD,
			DOUT => DOUT
		);
	end generate;
end Behavioral;

