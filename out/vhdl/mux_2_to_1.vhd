library IEEE;
use ieee.std_logic_1164.all;
entity mux_2_to_1 is
    generic (
        WIDTH : POSITIVE := 21
    );
    port (
        sel : in std_logic;
        din1 : in std_logic_vector(WIDTH - 1 downto 0);
        din2 : in std_logic_vector(WIDTH - 1 downto 0);
        dout : out std_logic_vector(WIDTH - 1 downto 0)
    );
end;
architecture rtl of mux_2_to_1 is
begin
    dout <= din1 when sel = '0' else din2;
end;
