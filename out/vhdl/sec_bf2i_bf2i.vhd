library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity sec_bf2i_bf2i is
    port (
        xfi : in std_logic_vector(18 downto 0);
        xpr : in std_logic_vector(17 downto 0);
        xpi : in std_logic_vector(17 downto 0);
        xfr : in std_logic_vector(18 downto 0);
        s : in std_logic;
        zni : out std_logic_vector(18 downto 0);
        zfr : out std_logic_vector(18 downto 0);
        zfi : out std_logic_vector(18 downto 0);
        znr : out std_logic_vector(18 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of sec_bf2i_bf2i is
    signal add_xfi_xpi : std_logic_vector(18 downto 0);
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
    signal xpr_18_downto_1 : std_logic_vector(18 downto 0);
    signal zfr_mux_dout : std_logic_vector(18 downto 0);
    signal xpi_18_downto_1 : std_logic_vector(18 downto 0);
    signal zfi_mux_dout : std_logic_vector(18 downto 0);
    signal add_xfr_xpr : std_logic_vector(18 downto 0);
    signal zni_mux_dout : std_logic_vector(18 downto 0);
    signal znr_mux_dout : std_logic_vector(18 downto 0);
    signal sub_xfi_xpi : std_logic_vector(18 downto 0);
    signal sub_xfr_xpr : std_logic_vector(18 downto 0);
begin
    add_xfi_xpi <= xfi + xpi;
    xpr_18_downto_1 <= (18 downto 18 => xpr(17)) & xpr;
    zfr_mux : mux_2_to_1
        generic map (
            WIDTH => 19
        )
        port map (
            sel => s,
            din1 => xpr_18_downto_1,
            din2 => sub_xfr_xpr,
            dout => zfr_mux_dout
        );
    xpi_18_downto_1 <= (18 downto 18 => xpi(17)) & xpi;
    zfi_mux : mux_2_to_1
        generic map (
            WIDTH => 19
        )
        port map (
            din1 => xpi_18_downto_1,
            sel => s,
            din2 => sub_xfi_xpi,
            dout => zfi_mux_dout
        );
    add_xfr_xpr <= xfr + xpr;
    zni_mux : mux_2_to_1
        generic map (
            WIDTH => 19
        )
        port map (
            din2 => add_xfi_xpi,
            sel => s,
            din1 => xfi,
            dout => zni_mux_dout
        );
    znr_mux : mux_2_to_1
        generic map (
            WIDTH => 19
        )
        port map (
            din1 => xfr,
            din2 => add_xfr_xpr,
            sel => s,
            dout => znr_mux_dout
        );
    sub_xfi_xpi <= xfi - xpi;
    sub_xfr_xpr <= xfr - xpr;
    zni <= zni_mux_dout;
    zfr <= zfr_mux_dout;
    zfi <= zfi_mux_dout;
    znr <= znr_mux_dout;
    vld <= '1';
end;
