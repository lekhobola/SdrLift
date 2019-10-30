LIBRARY IEEE;
USE IEEE.STD_LOGIC_1164.ALL;
USE IEEE.MATH_REAL.ALL;

ENTITY r22sdf_fft_ifft_core IS
	GENERIC(
		N		      : NATURAL := 64;
		DIN_WIDTH   : NATURAL := 16;
		DOUT_WIDTH  : NATURAL := 22;
		TF_W        : NATURAL := 16;
		MODE        : STD_LOGIC := '1'
	);
	PORT(
		CLK,RST  : IN  STD_LOGIC;
		EN 	   : IN  STD_LOGIC;
		XSr,XSi  : IN  STD_LOGIC_VECTOR(DIN_WIDTH - 1 downto 0);
		VLD	   : OUT STD_LOGIC;
		DONE	   : OUT STD_LOGIC;
		XKr,XKi  : OUT STD_LOGIC_VECTOR(DOUT_WIDTH - 1 downto 0)
	);
END r22sdf_fft_ifft_core;

architecture Behavioral of r22sdf_fft_ifft_core is
	COMPONENT r22sdf_fft_core IS
	GENERIC(
		N		 : NATURAL;
		DIN_W  : NATURAL;
		TF_W   : NATURAL 
	);
	PORT(
		CLK	  : IN STD_LOGIC;
		RST	  : IN STD_LOGIC;
		EN 	  : IN  STD_LOGIC;
		XSr,XSi : IN STD_LOGIC_VECTOR(DIN_W - 1 downto 0);
		VLD	  : OUT STD_LOGIC;
		DONE	  : OUT STD_LOGIC;
		XKr,XKi : OUT STD_LOGIC_VECTOR(DIN_W + INTEGER(LOG2(real(N))) - 1 downto 0)
	);
	END COMPONENT r22sdf_fft_core;
	
	COMPONENT r22sdf_ifft_core IS
		GENERIC(
			N		 : NATURAL := 8;
			DIN_W  : NATURAL := 11;
			TF_W   : NATURAL := 16
		);
		PORT(
			CLK,RST : IN  STD_LOGIC;
			EN 	  : IN STD_LOGIC;
			XKr,XKi : IN  STD_LOGIC_VECTOR (DIN_W - 1 downto 0);
			VLD	  : OUT STD_LOGIC;
			DONE	  : OUT STD_LOGIC;
			XSr,XSi : OUT STD_LOGIC_VECTOR(DIN_W - INTEGER(LOG2(real(N))) + 1 downto 0)
		);
	END COMPONENT r22sdf_ifft_core;

	constant FFT_DOUT_WIDTH  : natural  := DIN_WIDTH + INTEGER(LOG2(real(N)));
	constant IFFT_DOUT_WIDTH : natural  := DIN_WIDTH - INTEGER(LOG2(real(N))) + 2;
	signal doutr,douti   : std_logic_vector(FFT_DOUT_WIDTH - 1  downto 0);
	signal doutr1,douti1 : std_logic_vector(IFFT_DOUT_WIDTH - 1 downto 0);
	signal fft_done, ifft_done : std_logic;
begin

   DONE <= fft_done or ifft_done;
	
	gen_fft : if mode = '0' GENERATE
	BEGIN
	r22sdf_fft_core_inst :r22sdf_fft_core
		GENERIC MAP(
			N		 => N,
			DIN_W  => DIN_WIDTH,
			TF_W   => TF_W
		)
		PORT MAP(
			CLK => CLK,
			RST => RST,
			EN  => EN,
			XSr => XSr,
			XSi => XSi,
			VLD => vld,
			DONE => fft_done,
			XKr => doutr,
			XKi => douti
		);
		XKr <= doutr(FFT_DOUT_WIDTH - 1 downto FFT_DOUT_WIDTH - DOUT_WIDTH);
		XKi <= douti(FFT_DOUT_WIDTH - 1 downto FFT_DOUT_WIDTH - DOUT_WIDTH);
	 end GENERATE;
	 
	gen_ifft : if mode = '1' GENERATE
	BEGIN
	  r22sdf_ifft_core_inst :r22sdf_ifft_core
		GENERIC MAP(
			N		 => N,
			DIN_W  => DIN_WIDTH,
			TF_W   => TF_W
		)
		PORT MAP(
			CLK => CLK,
			RST => RST,
			EN  => EN,		
			XKr => XSr,
			XKi => XSi,
			VLD => VLD,
			DONE=> ifft_done,
			XSr => doutr1,
			XSi => douti1
		);
		XKr <= doutr1(IFFT_DOUT_WIDTH - 1 downto IFFT_DOUT_WIDTH - DOUT_WIDTH);
		XKi <= douti1(IFFT_DOUT_WIDTH - 1 downto IFFT_DOUT_WIDTH - DOUT_WIDTH);
	 end GENERATE;	
end Behavioral;

