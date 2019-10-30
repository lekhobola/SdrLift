library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity sec_bf2ii_MUXsg is
    port (
        g2 : in std_logic_vector(19 downto 0);
        cc : in std_logic;
        g1 : in std_logic_vector(19 downto 0);
        znr : out std_logic_vector(19 downto 0);
        zni : out std_logic_vector(19 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of sec_bf2ii_MUXsg is
    signal add_g1_g2 : std_logic_vector(19 downto 0);
    component sec_bf2ii_MUXsg_MUXim
        port (
            bi : in std_logic_vector(19 downto 0);
            cc : in std_logic;
            br : in std_logic_vector(19 downto 0);
            zi : out std_logic_vector(19 downto 0);
            zr : out std_logic_vector(19 downto 0);
            vld : out std_logic
        );
    end component;
    signal sec_bf2ii_MUXsg_MUXim_Inst_zi : std_logic_vector(19 downto 0);
    signal sec_bf2ii_MUXsg_MUXim_Inst_zr : std_logic_vector(19 downto 0);
    signal sec_bf2ii_MUXsg_MUXim_Inst_vld : std_logic;
    signal sub_g1_g2 : std_logic_vector(19 downto 0);
begin
    add_g1_g2 <= g1 + g2;
    sec_bf2ii_MUXsg_MUXim_Inst : sec_bf2ii_MUXsg_MUXim
        port map (
            cc => cc,
            br => add_g1_g2,
            bi => sub_g1_g2,
            zi => sec_bf2ii_MUXsg_MUXim_Inst_zi,
            zr => sec_bf2ii_MUXsg_MUXim_Inst_zr,
            vld => sec_bf2ii_MUXsg_MUXim_Inst_vld
        );
    sub_g1_g2 <= g1 - g2;
    znr <= sec_bf2ii_MUXsg_MUXim_Inst_zr;
    zni <= sec_bf2ii_MUXsg_MUXim_Inst_zi;
    vld <= sec_bf2ii_MUXsg_MUXim_Inst_vld;
end;
