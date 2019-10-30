library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity sec_bf2ii_bf2ii is
    port (
        xpi : in std_logic_vector(18 downto 0);
        xpr : in std_logic_vector(18 downto 0);
        xfi : in std_logic_vector(19 downto 0);
        xfr : in std_logic_vector(19 downto 0);
        s : in std_logic;
        t : in std_logic;
        zni : out std_logic_vector(19 downto 0);
        zfi : out std_logic_vector(19 downto 0);
        znr : out std_logic_vector(19 downto 0);
        zfr : out std_logic_vector(19 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of sec_bf2ii_bf2ii is
    component sec_bf2ii_MUXim
        port (
            br : in std_logic_vector(18 downto 0);
            bi : in std_logic_vector(18 downto 0);
            cc : in std_logic;
            zr : out std_logic_vector(18 downto 0);
            zi : out std_logic_vector(18 downto 0);
            vld : out std_logic
        );
    end component;
    signal sec_bf2ii_MUXim_Inst_zi : std_logic_vector(18 downto 0);
    signal sec_bf2ii_MUXim_Inst_zr : std_logic_vector(18 downto 0);
    signal sec_bf2ii_MUXim_Inst_vld : std_logic;
    component sec_bf2ii_MUXsg
        port (
            g2 : in std_logic_vector(19 downto 0);
            cc : in std_logic;
            g1 : in std_logic_vector(19 downto 0);
            znr : out std_logic_vector(19 downto 0);
            zni : out std_logic_vector(19 downto 0);
            vld : out std_logic
        );
    end component;
    signal sec_bf2ii_MUXim_Inst_zi_19_downto_1 : std_logic_vector(19 downto 0);
    signal sec_bf2ii_MUXsg_Inst_znr : std_logic_vector(19 downto 0);
    signal sec_bf2ii_MUXsg_Inst_zni : std_logic_vector(19 downto 0);
    signal sec_bf2ii_MUXsg_Inst_vld : std_logic;
    signal sub_ksny3x : std_logic_vector(19 downto 0);
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
    signal znr_mux_dout : std_logic_vector(19 downto 0);
    signal not_t : std_logic;
    signal zni_mux_dout : std_logic_vector(19 downto 0);
    signal and_s_not_t : std_logic;
    signal zfi_mux_dout : std_logic_vector(19 downto 0);
    signal add_754f9q : std_logic_vector(19 downto 0);
    signal sec_bf2ii_MUXim_Inst_zr_19_downto_1 : std_logic_vector(19 downto 0);
    signal zfr_mux_dout : std_logic_vector(19 downto 0);
begin
    sec_bf2ii_MUXim_Inst : sec_bf2ii_MUXim
        port map (
            br => xpr,
            cc => and_s_not_t,
            bi => xpi,
            zi => sec_bf2ii_MUXim_Inst_zi,
            zr => sec_bf2ii_MUXim_Inst_zr,
            vld => sec_bf2ii_MUXim_Inst_vld
        );
    sec_bf2ii_MUXim_Inst_zi_19_downto_1 <= (19 downto 19 => sec_bf2ii_MUXim_Inst_zi(18)) & sec_bf2ii_MUXim_Inst_zi;
    sec_bf2ii_MUXsg_Inst : sec_bf2ii_MUXsg
        port map (
            g1 => xfi,
            g2 => sec_bf2ii_MUXim_Inst_zi_19_downto_1,
            cc => and_s_not_t,
            znr => sec_bf2ii_MUXsg_Inst_znr,
            zni => sec_bf2ii_MUXsg_Inst_zni,
            vld => sec_bf2ii_MUXsg_Inst_vld
        );
    sub_ksny3x <= xfr - sec_bf2ii_MUXim_Inst_zr;
    znr_mux : mux_2_to_1
        generic map (
            WIDTH => 20
        )
        port map (
            din1 => xfr,
            sel => s,
            din2 => add_754f9q,
            dout => znr_mux_dout
        );
    not_t <= not t;
    zni_mux : mux_2_to_1
        generic map (
            WIDTH => 20
        )
        port map (
            sel => s,
            din2 => sec_bf2ii_MUXsg_Inst_znr,
            din1 => xfi,
            dout => zni_mux_dout
        );
    and_s_not_t <= s and not_t;
    sec_bf2ii_MUXim_Inst_zi_19_downto_1 <= (19 downto 19 => sec_bf2ii_MUXim_Inst_zi(18)) & sec_bf2ii_MUXim_Inst_zi;
    zfi_mux : mux_2_to_1
        generic map (
            WIDTH => 20
        )
        port map (
            din1 => sec_bf2ii_MUXim_Inst_zi_19_downto_1,
            din2 => sec_bf2ii_MUXsg_Inst_zni,
            sel => s,
            dout => zfi_mux_dout
        );
    add_754f9q <= xfr + sec_bf2ii_MUXim_Inst_zr;
    zni <= zni_mux_dout;
    sec_bf2ii_MUXim_Inst_zr_19_downto_1 <= (19 downto 19 => sec_bf2ii_MUXim_Inst_zr(18)) & sec_bf2ii_MUXim_Inst_zr;
    zfr_mux : mux_2_to_1
        generic map (
            WIDTH => 20
        )
        port map (
            din1 => sec_bf2ii_MUXim_Inst_zr_19_downto_1,
            din2 => sub_ksny3x,
            sel => s,
            dout => zfr_mux_dout
        );
    zfi <= zfi_mux_dout;
    znr <= znr_mux_dout;
    zfr <= zfr_mux_dout;
    vld <= sec_bf2ii_MUXim_Inst_vld;
end;
