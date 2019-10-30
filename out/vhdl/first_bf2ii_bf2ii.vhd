library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity first_bf2ii_bf2ii is
    port (
        xpi : in std_logic_vector(16 downto 0);
        xfr : in std_logic_vector(17 downto 0);
        xfi : in std_logic_vector(17 downto 0);
        xpr : in std_logic_vector(16 downto 0);
        s : in std_logic;
        t : in std_logic;
        zni : out std_logic_vector(17 downto 0);
        znr : out std_logic_vector(17 downto 0);
        zfi : out std_logic_vector(17 downto 0);
        zfr : out std_logic_vector(17 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of first_bf2ii_bf2ii is
    component first_bf2ii_MUXim
        port (
            br : in std_logic_vector(16 downto 0);
            bi : in std_logic_vector(16 downto 0);
            cc : in std_logic;
            zi : out std_logic_vector(16 downto 0);
            zr : out std_logic_vector(16 downto 0);
            vld : out std_logic
        );
    end component;
    signal first_bf2ii_MUXim_Inst_zi : std_logic_vector(16 downto 0);
    signal first_bf2ii_MUXim_Inst_zr : std_logic_vector(16 downto 0);
    signal first_bf2ii_MUXim_Inst_vld : std_logic;
    signal sub_t941xo : std_logic_vector(17 downto 0);
    component first_bf2ii_MUXsg
        port (
            cc : in std_logic;
            g2 : in std_logic_vector(17 downto 0);
            g1 : in std_logic_vector(17 downto 0);
            znr : out std_logic_vector(17 downto 0);
            zni : out std_logic_vector(17 downto 0);
            vld : out std_logic
        );
    end component;
    signal first_bf2ii_MUXim_Inst_zi_17_downto_1 : std_logic_vector(17 downto 0);
    signal first_bf2ii_MUXsg_Inst_znr : std_logic_vector(17 downto 0);
    signal first_bf2ii_MUXsg_Inst_zni : std_logic_vector(17 downto 0);
    signal first_bf2ii_MUXsg_Inst_vld : std_logic;
    signal and_s_not_t : std_logic;
    signal not_t : std_logic;
    component mux_2_to_1
        generic (
            WIDTH : POSITIVE := 18
        );
        port (
            sel : in std_logic;
            din1 : in std_logic_vector(WIDTH - 1 downto 0);
            din2 : in std_logic_vector(WIDTH - 1 downto 0);
            dout : out std_logic_vector(WIDTH - 1 downto 0)
        );
    end component;
    signal znr_mux_dout : std_logic_vector(17 downto 0);
    signal zni_mux_dout : std_logic_vector(17 downto 0);
    signal first_bf2ii_MUXim_Inst_zr_17_downto_1 : std_logic_vector(17 downto 0);
    signal zfr_mux_dout : std_logic_vector(17 downto 0);
    signal add_d08act : std_logic_vector(17 downto 0);
    signal zfi_mux_dout : std_logic_vector(17 downto 0);
begin
    first_bf2ii_MUXim_Inst : first_bf2ii_MUXim
        port map (
            cc => and_s_not_t,
            br => xpr,
            bi => xpi,
            zi => first_bf2ii_MUXim_Inst_zi,
            zr => first_bf2ii_MUXim_Inst_zr,
            vld => first_bf2ii_MUXim_Inst_vld
        );
    sub_t941xo <= xfr - first_bf2ii_MUXim_Inst_zr;
    first_bf2ii_MUXim_Inst_zi_17_downto_1 <= (17 downto 17 => first_bf2ii_MUXim_Inst_zi(16)) & first_bf2ii_MUXim_Inst_zi;
    first_bf2ii_MUXsg_Inst : first_bf2ii_MUXsg
        port map (
            g2 => first_bf2ii_MUXim_Inst_zi_17_downto_1,
            cc => and_s_not_t,
            g1 => xfi,
            znr => first_bf2ii_MUXsg_Inst_znr,
            zni => first_bf2ii_MUXsg_Inst_zni,
            vld => first_bf2ii_MUXsg_Inst_vld
        );
    and_s_not_t <= s and not_t;
    not_t <= not t;
    znr_mux : mux_2_to_1
        generic map (
            WIDTH => 18
        )
        port map (
            din2 => add_d08act,
            din1 => xfr,
            sel => s,
            dout => znr_mux_dout
        );
    zni_mux : mux_2_to_1
        generic map (
            WIDTH => 18
        )
        port map (
            din2 => first_bf2ii_MUXsg_Inst_znr,
            din1 => xfi,
            sel => s,
            dout => zni_mux_dout
        );
    first_bf2ii_MUXim_Inst_zr_17_downto_1 <= (17 downto 17 => first_bf2ii_MUXim_Inst_zr(16)) & first_bf2ii_MUXim_Inst_zr;
    zfr_mux : mux_2_to_1
        generic map (
            WIDTH => 18
        )
        port map (
            sel => s,
            din2 => sub_t941xo,
            din1 => first_bf2ii_MUXim_Inst_zr_17_downto_1,
            dout => zfr_mux_dout
        );
    add_d08act <= xfr + first_bf2ii_MUXim_Inst_zr;
    first_bf2ii_MUXim_Inst_zi_17_downto_1 <= (17 downto 17 => first_bf2ii_MUXim_Inst_zi(16)) & first_bf2ii_MUXim_Inst_zi;
    zfi_mux : mux_2_to_1
        generic map (
            WIDTH => 18
        )
        port map (
            din1 => first_bf2ii_MUXim_Inst_zi_17_downto_1,
            din2 => first_bf2ii_MUXsg_Inst_zni,
            sel => s,
            dout => zfi_mux_dout
        );
    zni <= zni_mux_dout;
    znr <= znr_mux_dout;
    zfi <= zfi_mux_dout;
    zfr <= zfr_mux_dout;
    vld <= first_bf2ii_MUXim_Inst_vld;
end;
