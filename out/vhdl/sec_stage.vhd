library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity sec_stage is
    port (
        tfr : in std_logic_vector(15 downto 0);
        s2 : in std_logic;
        tfi : in std_logic_vector(15 downto 0);
        dini : in std_logic_vector(17 downto 0);
        s1 : in std_logic;
        dinr : in std_logic_vector(17 downto 0);
        en : in std_logic;
        clk : in std_logic;
        rst : in std_logic;
        doutr : out std_logic_vector(19 downto 0);
        douti : out std_logic_vector(19 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of sec_stage is
    component sec_cmult
        port (
            ai : in std_logic_vector(19 downto 0);
            ar : in std_logic_vector(19 downto 0);
            br : in std_logic_vector(15 downto 0);
            bi : in std_logic_vector(15 downto 0);
            ci : out std_logic_vector(35 downto 0);
            cr : out std_logic_vector(35 downto 0);
            vld : out std_logic
        );
    end component;
    signal sec_bf2ii_zni : std_logic_vector(19 downto 0);
    signal sec_bf2ii_znr : std_logic_vector(19 downto 0);
    signal sec_cr : std_logic_vector(35 downto 0);
    signal sec_ci : std_logic_vector(35 downto 0);
    signal sec_vld : std_logic;
    component sec_bf2ii_bf2ii
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
    end component;
    signal sec_bf2i_znr : std_logic_vector(18 downto 0);
    signal sec_bf2ii_sreg_i_dout : std_logic_vector(19 downto 0);
    signal sec_bf2i_zni : std_logic_vector(18 downto 0);
    signal sec_bf2ii_sreg_r_dout : std_logic_vector(19 downto 0);
    signal sec_bf2ii_zfi : std_logic_vector(19 downto 0);
    signal sec_bf2ii_zfr : std_logic_vector(19 downto 0);
    signal sec_bf2ii_vld : std_logic;
    component sec_bf2i_bf2i
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
    end component;
    signal sec_bf2i_sreg_r_dout : std_logic_vector(18 downto 0);
    signal sec_bf2i_sreg_i_dout : std_logic_vector(18 downto 0);
    signal sec_bf2i_zfi : std_logic_vector(18 downto 0);
    signal sec_bf2i_zfr : std_logic_vector(18 downto 0);
    signal sec_bf2i_vld : std_logic;
    component delay
        generic (
            WIDTH : POSITIVE := 19;
            DEPTH : POSITIVE := 8
        );
        port (
            clk : in std_logic;
            rst : in std_logic;
            en : in std_logic;
            din : in std_logic_vector(WIDTH - 1 downto 0);
            vld : out std_logic;
            dout : out std_logic_vector(WIDTH - 1 downto 0)
        );
    end component;
    signal sec_bf2i_sreg_r_vld : std_logic;
    component sec_cmultr_rounder
        generic (
            DIN_WIDTH : POSITIVE := 34;
            DOUT_WIDTH : POSITIVE := 20
        );
        port (
            din : in std_logic_vector(DIN_WIDTH - 1 downto 0);
            dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)
        );
    end component;
    signal sec_cr_33_downto_0 : std_logic_vector(33 downto 0);
    signal sec_cmultr_dout : std_logic_vector(19 downto 0);
    signal sec_bf2ii_sreg_r_vld : std_logic;
    component sec_cmulti_rounder
        generic (
            DIN_WIDTH : POSITIVE := 34;
            DOUT_WIDTH : POSITIVE := 20
        );
        port (
            din : in std_logic_vector(DIN_WIDTH - 1 downto 0);
            dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)
        );
    end component;
    signal sec_ci_33_downto_0 : std_logic_vector(33 downto 0);
    signal sec_cmulti_dout : std_logic_vector(19 downto 0);
    signal sec_bf2ii_sreg_i_vld : std_logic;
    signal sec_bf2i_sreg_i_vld : std_logic;
begin
    sec : sec_cmult
        port map (
            ai => sec_bf2ii_zni,
            br => tfr,
            ar => sec_bf2ii_znr,
            bi => tfi,
            cr => sec_cr,
            ci => sec_ci,
            vld => sec_vld
        );
    sec_bf2ii : sec_bf2ii_bf2ii
        port map (
            s => s2,
            xpr => sec_bf2i_znr,
            xfi => sec_bf2ii_sreg_i_dout,
            xpi => sec_bf2i_zni,
            xfr => sec_bf2ii_sreg_r_dout,
            t => s1,
            zfi => sec_bf2ii_zfi,
            zni => sec_bf2ii_zni,
            znr => sec_bf2ii_znr,
            zfr => sec_bf2ii_zfr,
            vld => sec_bf2ii_vld
        );
    sec_bf2i : sec_bf2i_bf2i
        port map (
            s => s1,
            xfr => sec_bf2i_sreg_r_dout,
            xpi => dini,
            xfi => sec_bf2i_sreg_i_dout,
            xpr => dinr,
            zfi => sec_bf2i_zfi,
            znr => sec_bf2i_znr,
            zni => sec_bf2i_zni,
            zfr => sec_bf2i_zfr,
            vld => sec_bf2i_vld
        );
    sec_bf2i_sreg_r : delay
        generic map (
            WIDTH => 19,
            DEPTH => 8
        )
        port map (
            en => en,
            din => sec_bf2i_zfr,
            dout => sec_bf2i_sreg_r_dout,
            clk => clk,
            rst => rst,
            vld => sec_bf2i_sreg_r_vld
        );
    sec_cr_33_downto_0 <= sec_cr(33 downto 0);
    sec_cmultr : sec_cmultr_rounder
        generic map (
            DIN_WIDTH => 34,
            DOUT_WIDTH => 20
        )
        port map (
            din => sec_cr_33_downto_0,
            dout => sec_cmultr_dout
        );
    sec_bf2ii_sreg_r : delay
        generic map (
            WIDTH => 20,
            DEPTH => 4
        )
        port map (
            din => sec_bf2ii_zfr,
            en => en,
            dout => sec_bf2ii_sreg_r_dout,
            clk => clk,
            rst => rst,
            vld => sec_bf2ii_sreg_r_vld
        );
    sec_ci_33_downto_0 <= sec_ci(33 downto 0);
    sec_cmulti : sec_cmulti_rounder
        generic map (
            DIN_WIDTH => 34,
            DOUT_WIDTH => 20
        )
        port map (
            din => sec_ci_33_downto_0,
            dout => sec_cmulti_dout
        );
    sec_bf2ii_sreg_i : delay
        generic map (
            WIDTH => 20,
            DEPTH => 4
        )
        port map (
            din => sec_bf2ii_zfi,
            en => en,
            dout => sec_bf2ii_sreg_i_dout,
            clk => clk,
            rst => rst,
            vld => sec_bf2ii_sreg_i_vld
        );
    sec_bf2i_sreg_i : delay
        generic map (
            WIDTH => 19,
            DEPTH => 8
        )
        port map (
            din => sec_bf2i_zfi,
            en => en,
            dout => sec_bf2i_sreg_i_dout,
            clk => clk,
            rst => rst,
            vld => sec_bf2i_sreg_i_vld
        );
    doutr <= sec_cmultr_dout;
    douti <= sec_cmulti_dout;
    vld <= sec_vld;
end;
