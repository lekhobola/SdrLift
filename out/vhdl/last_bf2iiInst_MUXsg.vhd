library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity last_bf2iiInst_MUXsg is
    port (
        g1 : in std_logic_vector(21 downto 0);
        cc : in std_logic;
        g2 : in std_logic_vector(21 downto 0);
        zni : out std_logic_vector(21 downto 0);
        znr : out std_logic_vector(21 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of last_bf2iiInst_MUXsg is
    signal add_g1_g2 : std_logic_vector(21 downto 0);
    component last_bf2iiInst_MUXsg_MUXim
        port (
            bi : in std_logic_vector(21 downto 0);
            cc : in std_logic;
            br : in std_logic_vector(21 downto 0);
            zr : out std_logic_vector(21 downto 0);
            zi : out std_logic_vector(21 downto 0);
            vld : out std_logic
        );
    end component;
    signal last_bf2iiInst_MUXsg_MUXim_Inst_zi : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_MUXsg_MUXim_Inst_zr : std_logic_vector(21 downto 0);
    signal last_bf2iiInst_MUXsg_MUXim_Inst_vld : std_logic;
    signal sub_g1_g2 : std_logic_vector(21 downto 0);
begin
    add_g1_g2 <= g1 + g2;
    last_bf2iiInst_MUXsg_MUXim_Inst : last_bf2iiInst_MUXsg_MUXim
        port map (
            bi => sub_g1_g2,
            br => add_g1_g2,
            cc => cc,
            zi => last_bf2iiInst_MUXsg_MUXim_Inst_zi,
            zr => last_bf2iiInst_MUXsg_MUXim_Inst_zr,
            vld => last_bf2iiInst_MUXsg_MUXim_Inst_vld
        );
    sub_g1_g2 <= g1 - g2;
    zni <= last_bf2iiInst_MUXsg_MUXim_Inst_zi;
    znr <= last_bf2iiInst_MUXsg_MUXim_Inst_zr;
    vld <= last_bf2iiInst_MUXsg_MUXim_Inst_vld;
end;
