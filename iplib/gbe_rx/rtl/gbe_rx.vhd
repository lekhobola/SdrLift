library ieee;
	use ieee.std_logic_1164.all;
	use ieee.std_logic_unsigned.all;
	use ieee.numeric_std.all;
	
library unisim;
	use unisim.vcomponents.all;	

entity gbe_rx is
	port (
		
		clk_125mhz      : in  std_logic;
	    clk_62_5mhz     : in  std_logic;
		rst     	    : in  std_logic; 
		
		en			    : in std_logic;
		adc_clk			: in std_logic;
	    adc_data_i      : in std_logic_vector(15 downto 0);
		adc_data_q      : in std_logic_vector(15 downto 0);
		
		enable			: out std_logic;
		txnrx			: out std_logic;
		resetb 	 		: out std_logic;
		
		spi_csn 		: out std_logic;
		spi_clk			: out std_logic;
		spi_mosi		: out  std_logic;
		spi_miso		: in std_logic;
			
		GIGE_COL		: in std_logic;
		GIGE_CRS		: in std_logic;
		GIGE_MDC		: out std_logic;
		GIGE_MDIO		: inout std_logic;
		GIGE_TX_CLK	    : in std_logic;
		GIGE_nRESET	    : out std_logic;
		GIGE_RXD		: in std_logic_vector( 7 downto 0 );
		GIGE_RX_CLK		: in std_logic;
		GIGE_RX_DV		: in std_logic;
		GIGE_RX_ER		: in std_logic;
		GIGE_TXD		: out std_logic_vector( 7 downto 0 );
		GIGE_GTX_CLK 	: out std_logic;
		GIGE_TX_EN		: out std_logic;
		GIGE_TX_ER		: out std_logic
	);
end gbe_rx;

architecture Behavioral of gbe_rx is
end Behavioral;
