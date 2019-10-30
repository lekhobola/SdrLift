library IEEE;
use ieee.std_logic_1164.all;
use ieee.std_logic_signed.all;
entity first_cmult is
    port (
        ar : in std_logic_vector(17 downto 0);
        ai : in std_logic_vector(17 downto 0);
        br : in std_logic_vector(15 downto 0);
        bi : in std_logic_vector(15 downto 0);
        ci : out std_logic_vector(33 downto 0);
        cr : out std_logic_vector(33 downto 0);
        vld : out std_logic
    );
end;
architecture rtl of first_cmult is
    signal mul_ar_bi : std_logic_vector(33 downto 0);
    signal mul_ai_bi : std_logic_vector(33 downto 0);
    signal mul_ar_br : std_logic_vector(33 downto 0);
    signal mul_ai_br : std_logic_vector(33 downto 0);
    signal add_mul_ai_br_mul_ar_bi : std_logic_vector(33 downto 0);
    signal sub_mul_ar_br_mul_ai_bi : std_logic_vector(33 downto 0);
begin
    mul_ar_bi <= ar * bi;
    mul_ai_bi <= ai * bi;
    mul_ar_br <= ar * br;
    mul_ai_br <= ai * br;
    add_mul_ai_br_mul_ar_bi <= mul_ai_br + mul_ar_bi;
    sub_mul_ar_br_mul_ai_bi <= mul_ar_br - mul_ai_bi;
    ci <= add_mul_ai_br_mul_ar_bi;
    cr <= sub_mul_ar_br_mul_ai_bi;
    vld <= '1';
end;
