library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
entity delay is
    generic (
        WIDTH : POSITIVE := 21;
        DEPTH : POSITIVE := 2
    );
    port (
        clk : in std_logic;
        rst : in std_logic;
        en : in std_logic;
        din : in std_logic_vector(WIDTH - 1 downto 0);
        vld : out std_logic;
        dout : out std_logic_vector(WIDTH - 1 downto 0)
    );
end;
architecture rtl of delay is
    type dly_type is array (0 to DEPTH - 1) of std_logic_vector(WIDTH - 1 downto 0);
    signal dly : dly_type := (others => (others => '0'));
begin
    dout <= dly(DEPTH - 1);
    dly_proc : process(clk, rst)
    begin
        if rst = '1' then
            dly <= (others => (others => '0'));
        elsif clk'EVENT and clk = '1' then
            vld <= '0';
            dly <= (others => (others => '0'));
            if en = '1' then
                vld <= '1';
                dly(0) <= din;
                if DEPTH > 1 then
                    for i in DEPTH - 2 downto 0 loop
                        dly(i + 1) <= dly(i);
                    end loop;
                end if;
            end if;
        end if;
    end process;
end;
