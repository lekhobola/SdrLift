library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity sec_bf2ii_MUXim is
    port (
        br : in std_logic_vector(18 downto 0);
        bi : in std_logic_vector(18 downto 0);
        cc : in std_logic;
        zr : out std_logic_vector(18 downto 0);
        zi : out std_logic_vector(18 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of sec_bf2ii_MUXim is
    component mux_2_to_1
        generic (
            WIDTH : POSITIVE := 19
        );
        port (
            sel : in std_logic;
            din1 : in std_logic_vector(WIDTH - 1 downto 0);
            din2 : in std_logic_vector(WIDTH - 1 downto 0);
            dout : out std_logic_vector(WIDTH - 1 downto 0)
        );
    end component;
    signal zr_mux_dout : std_logic_vector(18 downto 0);
    signal zi_mux_dout : std_logic_vector(18 downto 0);
begin
    zr_mux : mux_2_to_1
        generic map (
            WIDTH => 19
        )
        port map (
            din1 => br,
            din2 => bi,
            sel => cc,
            dout => zr_mux_dout
        );
    zi_mux : mux_2_to_1
        generic map (
            WIDTH => 19
        )
        port map (
            din2 => br,
            sel => cc,
            din1 => bi,
            dout => zi_mux_dout
        );
    zr <= zr_mux_dout;
    zi <= zi_mux_dout;
    vld <= '1';
end;
