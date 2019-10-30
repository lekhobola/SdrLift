library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity last_evenstage is
    port (
        s2 : in std_logic;
        dini : in std_logic_vector(19 downto 0);
        dinr : in std_logic_vector(19 downto 0);
        s1 : in std_logic;
        en : in std_logic;
        clk : in std_logic;
        rst : in std_logic;
        doutr : out std_logic_vector(21 downto 0);
        douti : out std_logic_vector(21 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of last_evenstage is
    component last_bf2iiInst_bf2ii
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
    end component;
    signal last_bf2i_znr : std_logic_vector(20 downto 0);
    signal last_bf2ii_sreg_r_dout : std_logic_vector(21 downto 0);
    signal last_bf2ii_sreg_i_dout : std_logic_vector(21 downto 0);
    signal last_bf2i_zni : std_logic_vector(20 downto 0);
    signal last_bf2iiInst_zfi : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_zfr : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_znr : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_zni : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_vld : std_logic;
    component last_bf2i_bf2i
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
    end component;
    signal last_bf2i_sreg_i_dout : std_logic_vector(20 downto 0);
    signal last_bf2i_sreg_r_dout : std_logic_vector(20 downto 0);
    signal last_bf2i_zfi : std_logic_vector(20 downto 0);
    signal last_bf2i_zfr : std_logic_vector(20 downto 0);
    signal last_bf2i_vld : std_logic;
    component delay
        generic (
            WIDTH : POSITIVE := 21;
            DEPTH : POSITIVE := 2
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
    signal last_bf2i_sreg_r_vld : std_logic;
    signal last_bf2i_sreg_i_vld : std_logic;
    signal last_bf2ii_sreg_i_vld : std_logic;
    signal last_bf2ii_sreg_r_vld : std_logic;
begin
    last_bf2iiInst : last_bf2iiInst_bf2ii
        port map (
            s => s2,
            xpr => last_bf2i_znr,
            xfr => last_bf2ii_sreg_r_dout,
            xfi => last_bf2ii_sreg_i_dout,
            t => s1,
            xpi => last_bf2i_zni,
            zfi => last_bf2iiInst_zfi,
            zfr => last_bf2iiInst_zfr,
            znr => last_bf2iiInst_znr,
            zni => last_bf2iiInst_zni,
            vld => last_bf2iiInst_vld
        );
    last_bf2i : last_bf2i_bf2i
        port map (
            xpi => dini,
            xpr => dinr,
            xfi => last_bf2i_sreg_i_dout,
            s => s1,
            xfr => last_bf2i_sreg_r_dout,
            zfi => last_bf2i_zfi,
            znr => last_bf2i_znr,
            zfr => last_bf2i_zfr,
            zni => last_bf2i_zni,
            vld => last_bf2i_vld
        );
    last_bf2i_sreg_r : delay
        generic map (
            WIDTH => 21,
            DEPTH => 2
        )
        port map (
            en => en,
            din => last_bf2i_zfr,
            dout => last_bf2i_sreg_r_dout,
            clk => clk,
            rst => rst,
            vld => last_bf2i_sreg_r_vld
        );
    doutr <= last_bf2iiInst_znr;
    last_bf2i_sreg_i : delay
        generic map (
            WIDTH => 21,
            DEPTH => 2
        )
        port map (
            din => last_bf2i_zfi,
            en => en,
            dout => last_bf2i_sreg_i_dout,
            clk => clk,
            rst => rst,
            vld => last_bf2i_sreg_i_vld
        );
    last_bf2ii_sreg_i : delay
        generic map (
            WIDTH => 22,
            DEPTH => 1
        )
        port map (
            en => en,
            din => last_bf2iiInst_zfi,
            dout => last_bf2ii_sreg_i_dout,
            clk => clk,
            rst => rst,
            vld => last_bf2ii_sreg_i_vld
        );
    last_bf2ii_sreg_r : delay
        generic map (
            WIDTH => 22,
            DEPTH => 1
        )
        port map (
            din => last_bf2iiInst_zfr,
            en => en,
            dout => last_bf2ii_sreg_r_dout,
            clk => clk,
            rst => rst,
            vld => last_bf2ii_sreg_r_vld
        );
    douti <= last_bf2iiInst_zni;
    vld <= last_bf2ii_sreg_i_vld;
end;
