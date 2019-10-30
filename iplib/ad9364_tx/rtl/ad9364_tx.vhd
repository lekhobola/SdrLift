library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

library UNISIM;
use UNISIM.VComponents.all;

entity ad9364_tx is
	port(
		rst     	: in  std_logic; 
		en     	    : in    std_logic; 
		data_clk_p	: in  std_logic;
		data_clk_n	: in  std_logic;
		data_clk    : out std_logic;
		fb_clk_p	: out std_logic;
		fb_clk_n	: out std_logic;
		tx_frame_p  : out std_logic;
		tx_frame_n  : out std_logic;
		tx_data_p   : out std_logic_vector(5 downto 0);
		tx_data_n   : out std_logic_vector(5 downto 0);
		dac_clk	    : out std_logic;
		dac_data_i  : in  std_logic_vector(11 downto 0);
		dac_data_q  : in  std_logic_vector(11 downto 0)
	);
end ad9364_tx;

architecture ad9364_tx_rtl of ad9364_tx is	
begin
end ad9364_tx_rtl;
