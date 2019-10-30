library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
entity ctrl_counter is
    generic (
        WIDTH : POSITIVE := 6
    );
    port (
        clk : in std_logic;
        rst : in std_logic;
        en : in std_logic;
        dout : out std_logic_vector(WIDTH - 1 downto 0)
    );
end;
architecture rtl of ctrl_counter is
    signal count : std_logic_vector(WIDTH - 1 downto 0) := (others => '0');
begin
    dout <= count;
    count_proc : process(clk, rst)
    begin
        if rst = '1' then
            count <= (others => '0');
        elsif clk'EVENT and clk = '1' then
            count <= (others => '0');
            if en = '1' then
                count <= count + 1;
            end if;
        end if;
    end process;
end;
