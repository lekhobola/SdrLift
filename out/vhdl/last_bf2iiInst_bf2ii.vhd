library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity last_bf2iiInst_bf2ii is
    port (
        xfi : in std_logic_vector(21 downto 0);
        xpi : in std_logic_vector(20 downto 0);
        xfr : in std_logic_vector(21 downto 0);
        xpr : in std_logic_vector(20 downto 0);
        s : in std_logic;
        t : in std_logic;
        zni : out std_logic_vector(21 downto 0);
        zfi : out std_logic_vector(21 downto 0);
        znr : out std_logic_vector(21 downto 0);
        zfr : out std_logic_vector(21 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of last_bf2iiInst_bf2ii is
    component mux_2_to_1
        generic (
            WIDTH : POSITIVE := 22
        );
        port (
            sel : in std_logic;
            din1 : in std_logic_vector(WIDTH - 1 downto 0);
            din2 : in std_logic_vector(WIDTH - 1 downto 0);
            dout : out std_logic_vector(WIDTH - 1 downto 0)
        );
    end component;
    signal last_bf2iiInst_MUXsg_Inst_znr : std_logic_vector(21 downto 0);
    signal zni_mux_dout : std_logic_vector(21 downto 0);
    component last_bf2iiInst_MUXim
        port (
            cc : in std_logic;
            bi : in std_logic_vector(20 downto 0);
            br : in std_logic_vector(20 downto 0);
            zr : out std_logic_vector(20 downto 0);
            zi : out std_logic_vector(20 downto 0);
            vld : out std_logic
        );
    end component;
    signal last_bf2iiInst_MUXim_Inst_zi : std_logic_vector(20 downto 0);
    signal last_bf2iiInst_MUXim_Inst_zr : std_logic_vector(20 downto 0);
    signal last_bf2iiInst_MUXim_Inst_vld : std_logic;
    signal add_hqxki3 : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_MUXim_Inst_zr_21_downto_1 : std_logic_vector(21 downto 0);
    signal zfr_mux_dout : std_logic_vector(21 downto 0);
    signal not_t : std_logic;
    component last_bf2iiInst_MUXsg
        port (
            g1 : in std_logic_vector(21 downto 0);
            cc : in std_logic;
            g2 : in std_logic_vector(21 downto 0);
            zni : out std_logic_vector(21 downto 0);
            znr : out std_logic_vector(21 downto 0);
            vld : out std_logic
        );
    end component;
    signal last_bf2iiInst_MUXim_Inst_zi_21_downto_1 : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_MUXsg_Inst_zni : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_MUXsg_Inst_vld : std_logic;
    signal sub_jkrgwc : std_logic_vector(21 downto 0);
    signal znr_mux_dout : std_logic_vector(21 downto 0);
    signal and_s_not_t : std_logic;
    signal zfi_mux_dout : std_logic_vector(21 downto 0);
begin
    zni_mux : mux_2_to_1
        generic map (
            WIDTH => 22
        )
        port map (
            sel => s,
            din1 => xfi,
            din2 => last_bf2iiInst_MUXsg_Inst_znr,
            dout => zni_mux_dout
        );
    last_bf2iiInst_MUXim_Inst : last_bf2iiInst_MUXim
        port map (
            bi => xpi,
            br => xpr,
            cc => and_s_not_t,
            zi => last_bf2iiInst_MUXim_Inst_zi,
            zr => last_bf2iiInst_MUXim_Inst_zr,
            vld => last_bf2iiInst_MUXim_Inst_vld
        );
    add_hqxki3 <= xfr + last_bf2iiInst_MUXim_Inst_zr;
    last_bf2iiInst_MUXim_Inst_zr_21_downto_1 <= (21 downto 21 => last_bf2iiInst_MUXim_Inst_zr(20)) & last_bf2iiInst_MUXim_Inst_zr;
    zfr_mux : mux_2_to_1
        generic map (
            WIDTH => 22
        )
        port map (
            din2 => sub_jkrgwc,
            sel => s,
            din1 => last_bf2iiInst_MUXim_Inst_zr_21_downto_1,
            dout => zfr_mux_dout
        );
    not_t <= not t;
    last_bf2iiInst_MUXim_Inst_zi_21_downto_1 <= (21 downto 21 => last_bf2iiInst_MUXim_Inst_zi(20)) & last_bf2iiInst_MUXim_Inst_zi;
    last_bf2iiInst_MUXsg_Inst : last_bf2iiInst_MUXsg
        port map (
            g1 => xfi,
            cc => and_s_not_t,
            g2 => last_bf2iiInst_MUXim_Inst_zi_21_downto_1,
            zni => last_bf2iiInst_MUXsg_Inst_zni,
            znr => last_bf2iiInst_MUXsg_Inst_znr,
            vld => last_bf2iiInst_MUXsg_Inst_vld
        );
    sub_jkrgwc <= xfr - last_bf2iiInst_MUXim_Inst_zr;
    znr_mux : mux_2_to_1
        generic map (
            WIDTH => 22
        )
        port map (
            sel => s,
            din2 => add_hqxki3,
            din1 => xfr,
            dout => znr_mux_dout
        );
    and_s_not_t <= s and not_t;
    zni <= zni_mux_dout;
    last_bf2iiInst_MUXim_Inst_zi_21_downto_1 <= (21 downto 21 => last_bf2iiInst_MUXim_Inst_zi(20)) & last_bf2iiInst_MUXim_Inst_zi;
    zfi_mux : mux_2_to_1
        generic map (
            WIDTH => 22
        )
        port map (
            sel => s,
            din2 => last_bf2iiInst_MUXsg_Inst_zni,
            din1 => last_bf2iiInst_MUXim_Inst_zi_21_downto_1,
            dout => zfi_mux_dout
        );
    zfi <= zfi_mux_dout;
    znr <= znr_mux_dout;
    zfr <= zfr_mux_dout;
    vld <= last_bf2iiInst_MUXim_Inst_vld;
end;
