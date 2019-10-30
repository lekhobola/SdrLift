Library IEEE;
Use IEEE.STD_LOGIC_1164.ALL;
Use IEEE.MATH_REAL.ALL;

Use work.fir_pkg.all;

entity fir_par is
	generic(				  
		DIN_WIDTH 		  : natural := 16;  							
		DOUT_WIDTH       : natural := 32;
		COEFF_WIDTH 	  : natural := 16; 
		NUMBER_OF_TAPS   : natural := 95;  													
		LATENCY          : natural := 0;
		COEFFS			   : coeff_type
	);
	port(
		clk   : in  std_logic;
		rst   : in  std_logic;									  
		en    : in  std_logic;
		loadc : in  std_logic;
		vld   : out std_logic;
		coeff : in  std_logic_vector(COEFF_WIDTH - 1 downto 0);  
		din   : in  std_logic_vector(DIN_WIDTH   - 1 downto 0);  
		dout  : out std_logic_vector(DOUT_WIDTH  - 1 downto 0)	   
	);
end fir_par;

architecture Behavioral of fir_par is
	component fir_ntap_par
	generic(
		DIN_WIDTH  		 : natural; 
		DOUT_WIDTH		 : natural;
		COEFF_WIDTH		 : natural;
		NUMBER_OF_TAPS  : natural;  	
		COEFFS		    : coeff_type
	);
	port(
		clk  : in std_logic;
		rst  : in std_logic;									  
		en   : in std_logic;
		loadc: in std_logic;
		vld  : out std_logic;
		coeff: in  std_logic_vector(COEFF_WIDTH - 1 downto 0);
		din  : in  std_logic_vector(DIN_WIDTH - 1 downto 0);
		dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)	 
	);
	end component;

	component fir_ntap_esym_par
	generic(
		DIN_WIDTH	   : natural;
		DOUT_WIDTH	   : natural;
		COEFF_WIDTH 	: natural;
		NUMBER_OF_TAPS	: natural;
		COEFFS		   : coeff_type
	);
	port(
		clk : in std_logic;
		rst : in std_logic;									  
		en  : in std_logic;
		loadc: in std_logic;
		vld : out std_logic;
		coeff: in  std_logic_vector(COEFF_WIDTH - 1 downto 0);
		din 	 : in  std_logic_vector(DIN_WIDTH - 1 downto 0);
		dout	 : out std_logic_vector(DOUT_WIDTH - 1 downto 0)	 
	);	end component;

	component fir_ntap_osym_par
	generic(
		DIN_WIDTH  		: natural;  							 	
		DOUT_WIDTH		: natural;
		COEFF_WIDTH    : natural;
		NUMBER_OF_TAPS : natural;  							
		COEFFS		   : coeff_type
	);
	port(
		clk  : in  std_logic;
		rst  : in  std_logic;									  
		en   : in  std_logic;
		vld  : out std_logic;
		din  : IN  std_logic_vector(DIN_WIDTH  - 1 downto 0);
		dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)	 
	  );
	end component;
	 
	component fir_ntap_avg_par is
	generic(
		DIN_WIDTH	   : natural;
		DOUT_WIDTH     : natural;
		COEFF_WIDTH    : natural;
		NUMBER_OF_TAPS	: natural
	);
	port(
		clk : in std_logic;
		rst : in std_logic;									  
		en  : in std_logic;
		vld : out std_logic;
		din  : in  std_logic_vector(DIN_WIDTH  - 1 downto 0);
		dout : out std_logic_vector(DOUT_WIDTH - 1 downto 0)	 
	);
	end component fir_ntap_avg_par;
begin			 		 
				 
	GENERIC_FIR : if LATENCY = 0 GENERATE	
	fir_ntap_par_inst: fir_ntap_par 
	generic map(
		DIN_WIDTH      => DIN_WIDTH, 
		DOUT_WIDTH     => DOUT_WIDTH, 
		COEFF_WIDTH    => COEFF_WIDTH,
		NUMBER_OF_TAPS	=> NUMBER_OF_TAPS,
		COEFFS		   => COEFFS
	)
	port map (
		 clk   => clk,
		 rst   => rst,								  
		 en    => en,
		 loadc => loadc,
		 vld   => vld,
		 coeff => coeff,
		 din   => din,
		 dout  => dout
	  );
	end generate;

	EVEN_SYM_FIR : if LATENCY = 1 generate	
	fir_ntap_esym_par_inst: fir_ntap_esym_par 
	generic map(
		DIN_WIDTH      => DIN_WIDTH, 
		DOUT_WIDTH     => DOUT_WIDTH, 
		COEFF_WIDTH    => COEFF_WIDTH,
		NUMBER_OF_TAPS	=> NUMBER_OF_TAPS,
		COEFFS		   => COEFFS
	)
	port map (
		 clk   => clk,
		 rst   => rst,								  
		 en    => en,
		 loadc => loadc,
		 vld   => vld,
		 coeff => coeff,
		 din   => din,
		 dout  => dout
	  );
	end generate;

	MOVING_AVG_FIR : if LATENCY = 2 GENERATE	
	fir_ntap_osym_par_inst: fir_ntap_osym_par 
	generic map(
		DIN_WIDTH      => DIN_WIDTH,
		DOUT_WIDTH     => DOUT_WIDTH,		
		COEFF_WIDTH    => COEFF_WIDTH,
		NUMBER_OF_TAPS	=> NUMBER_OF_TAPS,
		COEFFS		   => COEFFS
	)
	port map (
		 clk  => clk,
		 rst  => rst,								  
		 en   => en,
		 vld  => vld,
		 din  => din,
		 dout => dout
	  );
	end generate;
	
	ODD_SYM_FIR : if LATENCY = 3 GENERATE	
	fir_ntap_osym_par_inst: fir_ntap_avg_par 
	generic map(
		DIN_WIDTH      => DIN_WIDTH, 
		DOUT_WIDTH     => DOUT_WIDTH,
		COEFF_WIDTH    => COEFF_WIDTH,
		NUMBER_OF_TAPS	=> NUMBER_OF_TAPS
	)
	port map (
		 clk  => clk,
		 rst  => rst,								  
		 en   => en,
		 vld  => vld,
		 din  => din,
		 dout => dout
	  );
	end generate;
end Behavioral;

