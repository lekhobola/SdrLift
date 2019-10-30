library ieee;
	use ieee.std_logic_1164.all;
	use ieee.std_logic_unsigned.all;
	use ieee.numeric_std.all;
	
library unisim;
	use unisim.vcomponents.all;	

entity gbe_tx is
	generic(
		DDR3_THRESH 	: natural;
		RX_BYTES_WIDTH	: natural
	);
	port (
		
		SYS_CLK_buf   	: in std_logic;
		
		clk_125mhz      : in  std_logic;
	    clk_100mhz      : in  std_logic;
		rst     	    : in  std_logic; 
		

		en			    : in std_logic;
		vld				: out std_logic;
		dac_clk			: in std_logic;
	    dac_data_i      : out std_logic_vector(15 downto 0);
		dac_data_q      : out std_logic_vector(15 downto 0);
		
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
		GIGE_TX_ER		: out std_logic;
		
		mcb5_dram_dq                            : inout  std_logic_vector(15 downto 0);
		mcb5_dram_a                             : out std_logic_vector(13 downto 0);
		mcb5_dram_ba                            : out std_logic_vector(2 downto 0);
		mcb5_dram_ras_n                         : out std_logic;
		mcb5_dram_cas_n                         : out std_logic;
		mcb5_dram_we_n                          : out std_logic;
		mcb5_dram_odt                           : out std_logic;
		mcb5_dram_reset_n                       : out std_logic;
		mcb5_dram_cke                           : out std_logic;
		mcb5_dram_dm                            : out std_logic;
		mcb5_dram_udqs                          : inout std_logic;
		mcb5_dram_udqs_n                        : inout std_logic;
		mcb5_rzq                                : inout std_logic;
		mcb5_dram_udm                           : out std_logic;
		mcb5_dram_dqs                           : inout std_logic;
		mcb5_dram_dqs_n                         : inout std_logic;
		mcb5_dram_ck                            : out std_logic;
		mcb5_dram_ck_n                          : out std_logic
	);
end gbe_tx;

architecture Behavioral of gbe_tx is
end Behavioral;
