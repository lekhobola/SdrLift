library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
entity stage2_twiddle_i_rom is
    generic (
        ADDR_WIDTH : POSITIVE := 4;
        DATA_WIDTH : POSITIVE := 16
    );
    port (
        addr : in std_logic_vector(ADDR_WIDTH - 1 downto 0);
        dout : out std_logic_vector(DATA_WIDTH - 1 downto 0)
    );
end;
architecture rtl of stage2_twiddle_i_rom is
    type mem_type is array (0 to (2 ** ADDR_WIDTH) - 1) of std_logic_vector(DATA_WIDTH - 1 downto 0);
    constant memory : mem_type := ("0000000000000000", "1101001010111111", "1100000000000000", "1101001010111111", "0000000000000000", "1110011110000010", "1101001010111111", "1100010011011111", "0000000000000000", "1100010011011111", "1101001010111111", "0001100001111110", "0000000000000000", "0000000000000000", "0000000000000000", "0000000000000000");
begin
    dout <= std_logic_vector(memory(CONV_INTEGER(addr)));
end;
