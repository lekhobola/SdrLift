library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity first_stage is
    port (
        tfr : in std_logic_vector(15 downto 0);
        dini : in std_logic_vector(15 downto 0);
        s2 : in std_logic;
        tfi : in std_logic_vector(15 downto 0);
        dinr : in std_logic_vector(15 downto 0);
        s1 : in std_logic;
        en : in std_logic;
        clk : in std_logic;
        rst : in std_logic;
        douti : out std_logic_vector(17 downto 0);
        doutr : out std_logic_vector(17 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of first_stage is
    component first_cmult
        port (
            ar : in std_logic_vector(17 downto 0);
            ai : in std_logic_vector(17 downto 0);
            br : in std_logic_vector(15 downto 0);
            bi : in std_logic_vector(15 downto 0);
            ci : out std_logic_vector(33 downto 0);
            cr : out std_logic_vector(33 downto 0);
            vld : out std_logic
        );
    end component;
    signal first_bf2ii_znr : std_logic_vector(17 downto 0);
    signal first_bf2ii_zni : std_logic_vector(17 downto 0);
    signal first_ci : std_logic_vector(33 downto 0);
    signal first_cr : std_logic_vector(33 downto 0);
    signal first_vld : std_logic;
    component first_bf2i_bf2i
        port (
            xfi : in std_logic_vector(16 downto 0);
            xfr : in std_logic_vector(16 downto 0);
            xpr : in std_logic_vector(15 downto 0);
            xpi : in std_logic_vector(15 downto 0);
            s : in std_logic;
            zni : out std_logic_vector(16 downto 0);
            znr : out std_logic_vector(16 downto 0);
            zfr : out std_logic_vector(16 downto 0);
            zfi : out std_logic_vector(16 downto 0);
            vld : out std_logic
        );
    end component;
    signal first_bf2i_sreg_r_dout : std_logic_vector(16 downto 0);
    signal first_bf2i_sreg_i_dout : std_logic_vector(16 downto 0);
    signal first_bf2i_zfi : std_logic_vector(16 downto 0);
    signal first_bf2i_zni : std_logic_vector(16 downto 0);
    signal first_bf2i_znr : std_logic_vector(16 downto 0);
    signal first_bf2i_zfr : std_logic_vector(16 downto 0);
    signal first_bf2i_vld : std_logic;
    component first_bf2ii_bf2ii
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
    end component;
    signal first_bf2ii_sreg_i_dout : std_logic_vector(17 downto 0);
    signal first_bf2ii_sreg_r_dout : std_logic_vector(17 downto 0);
    signal first_bf2ii_zfi : std_logic_vector(17 downto 0);
    signal first_bf2ii_zfr : std_logic_vector(17 downto 0);
    signal first_bf2ii_vld : std_logic;
    component delay
        generic (
            WIDTH : POSITIVE := 18;
            DEPTH : POSITIVE := 16
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
    signal first_bf2ii_sreg_r_vld : std_logic;
    component first_cmulti_rounder
        generic (
            DIN_WIDTH : POSITIVE := 32;
            DOUT_WIDTH : POSITIVE := 18
        );
        port (
            din : in std_logic_vector(DIN_WIDTH - 1 downto 0);
            dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)
        );
    end component;
    signal first_ci_31_downto_0 : std_logic_vector(31 downto 0);
    signal first_cmulti_dout : std_logic_vector(17 downto 0);
    signal first_bf2i_sreg_i_vld : std_logic;
    component first_cmultr_rounder
        generic (
            DIN_WIDTH : POSITIVE := 32;
            DOUT_WIDTH : POSITIVE := 18
        );
        port (
            din : in std_logic_vector(DIN_WIDTH - 1 downto 0);
            dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)
        );
    end component;
    signal first_cr_31_downto_0 : std_logic_vector(31 downto 0);
    signal first_cmultr_dout : std_logic_vector(17 downto 0);
    signal first_bf2i_sreg_r_vld : std_logic;
    signal first_bf2ii_sreg_i_vld : std_logic;
begin
    first : first_cmult
        port map (
            ar => first_bf2ii_znr,
            ai => first_bf2ii_zni,
            br => tfr,
            bi => tfi,
            ci => first_ci,
            cr => first_cr,
            vld => first_vld
        );
    first_bf2i : first_bf2i_bf2i
        port map (
            xfr => first_bf2i_sreg_r_dout,
            xpr => dinr,
            xpi => dini,
            s => s1,
            xfi => first_bf2i_sreg_i_dout,
            zfi => first_bf2i_zfi,
            zni => first_bf2i_zni,
            znr => first_bf2i_znr,
            zfr => first_bf2i_zfr,
            vld => first_bf2i_vld
        );
    first_bf2ii : first_bf2ii_bf2ii
        port map (
            s => s2,
            xpi => first_bf2i_zni,
            t => s1,
            xpr => first_bf2i_znr,
            xfi => first_bf2ii_sreg_i_dout,
            xfr => first_bf2ii_sreg_r_dout,
            zfi => first_bf2ii_zfi,
            znr => first_bf2ii_znr,
            zni => first_bf2ii_zni,
            zfr => first_bf2ii_zfr,
            vld => first_bf2ii_vld
        );
    first_bf2ii_sreg_r : delay
        generic map (
            WIDTH => 18,
            DEPTH => 16
        )
        port map (
            din => first_bf2ii_zfr,
            en => en,
            dout => first_bf2ii_sreg_r_dout,
            clk => clk,
            rst => rst,
            vld => first_bf2ii_sreg_r_vld
        );
    first_ci_31_downto_0 <= first_ci(31 downto 0);
    first_cmulti : first_cmulti_rounder
        generic map (
            DIN_WIDTH => 32,
            DOUT_WIDTH => 18
        )
        port map (
            din => first_ci_31_downto_0,
            dout => first_cmulti_dout
        );
    first_bf2i_sreg_i : delay
        generic map (
            WIDTH => 17,
            DEPTH => 32
        )
        port map (
            din => first_bf2i_zfi,
            en => en,
            dout => first_bf2i_sreg_i_dout,
            clk => clk,
            rst => rst,
            vld => first_bf2i_sreg_i_vld
        );
    first_cr_31_downto_0 <= first_cr(31 downto 0);
    first_cmultr : first_cmultr_rounder
        generic map (
            DIN_WIDTH => 32,
            DOUT_WIDTH => 18
        )
        port map (
            din => first_cr_31_downto_0,
            dout => first_cmultr_dout
        );
    first_bf2i_sreg_r : delay
        generic map (
            WIDTH => 17,
            DEPTH => 32
        )
        port map (
            en => en,
            din => first_bf2i_zfr,
            dout => first_bf2i_sreg_r_dout,
            clk => clk,
            rst => rst,
            vld => first_bf2i_sreg_r_vld
        );
    first_bf2ii_sreg_i : delay
        generic map (
            WIDTH => 18,
            DEPTH => 16
        )
        port map (
            din => first_bf2ii_zfi,
            en => en,
            dout => first_bf2ii_sreg_i_dout,
            clk => clk,
            rst => rst,
            vld => first_bf2ii_sreg_i_vld
        );
    douti <= first_cmulti_dout;
    doutr <= first_cmultr_dout;
    vld <= first_vld;
end;
