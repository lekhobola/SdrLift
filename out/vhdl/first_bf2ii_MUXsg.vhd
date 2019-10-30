library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity first_bf2ii_MUXsg is
    port (
        cc : in std_logic;
        g2 : in std_logic_vector(17 downto 0);
        g1 : in std_logic_vector(17 downto 0);
        znr : out std_logic_vector(17 downto 0);
        zni : out std_logic_vector(17 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of first_bf2ii_MUXsg is
    component first_bf2ii_MUXsg_MUXim
        port (
            br : in std_logic_vector(17 downto 0);
            cc : in std_logic;
            bi : in std_logic_vector(17 downto 0);
            zi : out std_logic_vector(17 downto 0);
            zr : out std_logic_vector(17 downto 0);
            vld : out std_logic
        );
    end component;
    signal first_bf2ii_MUXsg_MUXim_Inst_zi : std_logic_vector(17 downto 0);
    signal first_bf2ii_MUXsg_MUXim_Inst_zr : std_logic_vector(17 downto 0);
    signal first_bf2ii_MUXsg_MUXim_Inst_vld : std_logic;
    signal add_g1_g2 : std_logic_vector(17 downto 0);
    signal sub_g1_g2 : std_logic_vector(17 downto 0);
begin
    first_bf2ii_MUXsg_MUXim_Inst : first_bf2ii_MUXsg_MUXim
        port map (
            bi => sub_g1_g2,
            br => add_g1_g2,
            cc => cc,
            zi => first_bf2ii_MUXsg_MUXim_Inst_zi,
            zr => first_bf2ii_MUXsg_MUXim_Inst_zr,
            vld => first_bf2ii_MUXsg_MUXim_Inst_vld
        );
    add_g1_g2 <= g1 + g2;
    znr <= first_bf2ii_MUXsg_MUXim_Inst_zr;
    sub_g1_g2 <= g1 - g2;
    zni <= first_bf2ii_MUXsg_MUXim_Inst_zi;
    vld <= first_bf2ii_MUXsg_MUXim_Inst_vld;
end;
