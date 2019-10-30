library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity sec_bf2ii_MUXsg_MUXim is
    port (
        bi : in std_logic_vector(19 downto 0);
        cc : in std_logic;
        br : in std_logic_vector(19 downto 0);
        zi : out std_logic_vector(19 downto 0);
        zr : out std_logic_vector(19 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of sec_bf2ii_MUXsg_MUXim is
    component mux_2_to_1
        generic (
            WIDTH : POSITIVE := 20
        );
        port (
            sel : in std_logic;
            din1 : in std_logic_vector(WIDTH - 1 downto 0);
            din2 : in std_logic_vector(WIDTH - 1 downto 0);
            dout : out std_logic_vector(WIDTH - 1 downto 0)
        );
    end component;
    signal zi_mux_dout : std_logic_vector(19 downto 0);
    signal zr_mux_dout : std_logic_vector(19 downto 0);
begin
    zi_mux : mux_2_to_1
        generic map (
            WIDTH => 20
        )
        port map (
            sel => cc,
            din2 => br,
            din1 => bi,
            dout => zi_mux_dout
        );
    zr_mux : mux_2_to_1
        generic map (
            WIDTH => 20
        )
        port map (
            din2 => bi,
            sel => cc,
            din1 => br,
            dout => zr_mux_dout
        );
    zi <= zi_mux_dout;
    zr <= zr_mux_dout;
    vld <= '1';
end;
