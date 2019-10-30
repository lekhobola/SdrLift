library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

library UNISIM;
use UNISIM.VComponents.all;

entity ad9364_rx is
	port(
		rst     	  : in  std_logic; 
		en     	      : in  std_logic; 
		data_clk_p    : in  std_logic;
		data_clk_n    : in  std_logic;
		rx_frame_p    : in  std_logic;
		rx_frame_n    : in  std_logic;
		rx_data_p     : in  std_logic_vector(5 downto 0);
		rx_data_n     : in  std_logic_vector(5 downto 0);
		adc_vld		  : out std_logic;
		adc_data_i    : out std_logic_vector(11 downto 0);
		adc_data_q    : out std_logic_vector(11 downto 0);
		adc_clk		  : out std_logic
	);
end ad9364_rx;

architecture ad9364_rx_rtl of ad9364_rx is	
begin
end ad9364_rx_rtl;
