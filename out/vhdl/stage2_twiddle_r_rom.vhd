library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
entity stage2_twiddle_r_rom is
    generic (
        ADDR_WIDTH : POSITIVE := 4;
        DATA_WIDTH : POSITIVE := 16
    );
    port (
        addr : in std_logic_vector(ADDR_WIDTH - 1 downto 0);
        dout : out std_logic_vector(DATA_WIDTH - 1 downto 0)
    );
end;
architecture rtl of stage2_twiddle_r_rom is
    type mem_type is array (0 to (2 ** ADDR_WIDTH) - 1) of std_logic_vector(DATA_WIDTH - 1 downto 0);
    constant memory : mem_type := ("0100000000000000", "0010110101000001", "0000000000000000", "1101001010111111", "0100000000000000", "0011101100100001", "0010110101000001", "0001100001111110", "0100000000000000", "0001100001111110", "1101001010111111", "1100010011011111", "0100000000000000", "0100000000000000", "0100000000000000", "0100000000000000");
begin
    dout <= std_logic_vector(memory(CONV_INTEGER(addr)));
end;
