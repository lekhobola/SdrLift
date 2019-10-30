library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity last_bf2i_bf2i is
    port (
        xpi : in std_logic_vector(19 downto 0);
        xfr : in std_logic_vector(20 downto 0);
        xfi : in std_logic_vector(20 downto 0);
        xpr : in std_logic_vector(19 downto 0);
        s : in std_logic;
        zfi : out std_logic_vector(20 downto 0);
        znr : out std_logic_vector(20 downto 0);
        zni : out std_logic_vector(20 downto 0);
        zfr : out std_logic_vector(20 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of last_bf2i_bf2i is
    signal sub_xfi_xpi : std_logic_vector(20 downto 0);
    component mux_2_to_1
        generic (
            WIDTH : POSITIVE := 21
        );
        port (
            sel : in std_logic;
            din1 : in std_logic_vector(WIDTH - 1 downto 0);
            din2 : in std_logic_vector(WIDTH - 1 downto 0);
            dout : out std_logic_vector(WIDTH - 1 downto 0)
        );
    end component;
    signal znr_mux_dout : std_logic_vector(20 downto 0);
    signal zni_mux_dout : std_logic_vector(20 downto 0);
    signal xpr_20_downto_1 : std_logic_vector(20 downto 0);
    signal zfr_mux_dout : std_logic_vector(20 downto 0);
    signal xpi_20_downto_1 : std_logic_vector(20 downto 0);
    signal zfi_mux_dout : std_logic_vector(20 downto 0);
    signal sub_xfr_xpr : std_logic_vector(20 downto 0);
    signal add_xfi_xpi : std_logic_vector(20 downto 0);
    signal add_xfr_xpr : std_logic_vector(20 downto 0);
begin
    sub_xfi_xpi <= xfi - xpi;
    znr_mux : mux_2_to_1
        generic map (
            WIDTH => 21
        )
        port map (
            din1 => xfr,
            din2 => add_xfr_xpr,
            sel => s,
            dout => znr_mux_dout
        );
    zni_mux : mux_2_to_1
        generic map (
            WIDTH => 21
        )
        port map (
            sel => s,
            din1 => xfi,
            din2 => add_xfi_xpi,
            dout => zni_mux_dout
        );
    xpr_20_downto_1 <= (20 downto 20 => xpr(19)) & xpr;
    zfr_mux : mux_2_to_1
        generic map (
            WIDTH => 21
        )
        port map (
            din1 => xpr_20_downto_1,
            din2 => sub_xfr_xpr,
            sel => s,
            dout => zfr_mux_dout
        );
    xpi_20_downto_1 <= (20 downto 20 => xpi(19)) & xpi;
    zfi_mux : mux_2_to_1
        generic map (
            WIDTH => 21
        )
        port map (
            din2 => sub_xfi_xpi,
            sel => s,
            din1 => xpi_20_downto_1,
            dout => zfi_mux_dout
        );
    sub_xfr_xpr <= xfr - xpr;
    add_xfi_xpi <= xfi + xpi;
    add_xfr_xpr <= xfr + xpr;
    zfi <= zfi_mux_dout;
    znr <= znr_mux_dout;
    zni <= zni_mux_dout;
    zfr <= zfr_mux_dout;
    vld <= '1';
end;
