Library IEEE;
Use IEEE.STD_LOGIC_1164.all;
Use IEEE.STD_LOGIC_SIGNED.all;

entity mixer is
	generic(
		DIN1_WIDTH : natural;
		DIN2_WIDTH : natural;
		DOUT_WIDTH : natural
	);
	port(
		clk  : in std_logic;
		rst  : in std_logic;
		en   : in std_logic;
		din1 : in std_logic_vector (DIN1_WIDTH  - 1 downto 0);
		din2 : in std_logic_vector (DIN2_WIDTH  - 1 downto 0);
		vld  : out std_logic;
		dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)
	);
end mixer;

architecture Behavioral of mixer is
	signal product : std_logic_vector(DIN1_WIDTH + DIN2_WIDTH - 1 downto 0) := (others => '0');
begin
	process(din1,din2) 
	begin
		product <= din1 * din2;
	end process;
	dout <= product(DIN1_WIDTH + DIN2_WIDTH - 2 downto DIN1_WIDTH + DIN2_WIDTH - DOUT_WIDTH - 1);
end Behavioral;

