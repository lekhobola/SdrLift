library IEEE;
use IEEE.STD_LOGIC_1164.all;
use IEEE.STD_LOGIC_ARITH.all;
use IEEE.STD_LOGIC_UNSIGNED.all;

entity qamdemod is
  generic(
		iWidth : positive;
		oWidth : positive
  );
  port (
		 clk   : in  std_logic;
		 rst   : in  std_logic;
		 en    : in  std_logic;
		 Iin  : in  std_logic_vector(iWidth-1 downto 0);
		 Qin  : in  std_logic_vector(iWidth-1 downto 0);
		 vld   : out std_logic;
		 dout  : out std_logic_vector(oWidth-1 downto 0)
	 );
  end qamdemod;

architecture rtl of qamdemod is

begin

  process (clk, rst)
  
  begin
    if rst = '1' then
      dout <= (others => '0');
    elsif clk'event and clk = '1' then
		vld <= '0';
		dout <= (others => '0');
		if(en ='1') then
			vld <= '1';			
			dout <= Qin & Iin;
		end if;
    end if;
  end process;

end rtl;
