library IEEE;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use ieee.std_logic_signed.all;
entity sec_cmulti_rounder is
    generic (
        DIN_WIDTH : POSITIVE := 34;
        DOUT_WIDTH : POSITIVE := 20
    );
    port (
        din : in std_logic_vector(DIN_WIDTH - 1 downto 0);
        dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)
    );
end;
architecture rtl of sec_cmulti_rounder is
    constant rad_pos : INTEGER := DIN_WIDTH - DOUT_WIDTH;
    signal rounded_reg : std_logic_vector(DOUT_WIDTH - 1 downto 0) := (others => '0');
begin
    rounder_proc : process(din)
        variable rounded_temp : std_logic_vector(DOUT_WIDTH + 1 - 1 downto 0);
    begin
        if rad_pos > 0 then
            if CONV_INTEGER(din(DIN_WIDTH - 1 downto rad_pos)) = (2 ** DOUT_WIDTH) - 1 then
                rounded_temp := din(DIN_WIDTH - 1) & din(DIN_WIDTH - 1 downto rad_pos);
            else
                if rad_pos = 1 then
                    rounded_temp := din(DIN_WIDTH - 1) & din(DIN_WIDTH - 1 downto rad_pos) + ((DIN_WIDTH - 1 downto rad_pos => '0') & din(0));
                else
                    if CONV_INTEGER(din) < 0 and CONV_INTEGER(din(rad_pos - 1 downto 0)) = 0 then
                        rounded_temp := din(DIN_WIDTH - 1) & din(DIN_WIDTH - 1 downto rad_pos);
                    else
                        rounded_temp := din(DIN_WIDTH - 1) & din(DIN_WIDTH - 1 downto rad_pos) + ((DIN_WIDTH - 1 downto rad_pos => '0') & din(rad_pos - 1));
                    end if;
                end if;
            end if;
            rounded_reg <= rounded_temp(DOUT_WIDTH - 1 downto 0);
        else
            rounded_reg <= rounded_temp(DIN_WIDTH - 1 downto rad_pos);
        end if;
    end process;
    dout <= rounded_reg;
end;
