library IEEE;
USE IEEE.STD_LOGIC_1164.ALL;
USE IEEE.NUMERIC_STD.ALL;

entity fifo is
	Generic (
		constant DATA_WIDTH : positive := 8;
		constant FIFO_DEPTH	: positive := 8
	);
	Port ( 
		clk		: in  STD_LOGIC;
		rst		: in  STD_LOGIC;
		we	    : in  STD_LOGIC;
		din	    : in  STD_LOGIC_VECTOR (DATA_WIDTH - 1 downto 0);
		re	    : in  STD_LOGIC;
		dout	: out STD_LOGIC_VECTOR (DATA_WIDTH - 1 downto 0);
		em	    : out STD_LOGIC;
		fl	    : out STD_LOGIC;
		vld	    : out STD_LOGIC
	);
end fifo;

architecture Behavioral of fifo is

begin

	fifo_proc : process (clk)
		type FIFO_Memory is array (0 to FIFO_DEPTH - 1) of STD_LOGIC_VECTOR (DATA_WIDTH - 1 downto 0);
		variable Memory : FIFO_Memory := (others => (others => '0'));
		
		variable Head : natural range 0 to FIFO_DEPTH - 1;
		variable Tail : natural range 0 to FIFO_DEPTH - 1;
		
		variable Looped : boolean;
	begin
		if rising_edge(clk) then
			if rst = '1' then
				Head := 0;
				Tail := 0;
				
				Looped := false;
				
				fl  <= '0';
				em <= '1';
			else
				vld <= '0';
				dout <= (others => '0');
				if (re = '1') then
					if ((Looped = true) or (Head /= Tail)) then
					
						dout <= Memory(Tail);
						vld    <= '1';
						
						if (Tail = FIFO_DEPTH - 1) then
							Tail := 0;
							
							Looped := false;
						else
							Tail := Tail + 1;
						end if;
						
						
					end if;
				end if;
				
				if (we = '1') then
					if ((Looped = false) or (Head /= Tail)) then
						
						Memory(Head) := din;
						
						
						if (Head = FIFO_DEPTH - 1) then
							Head := 0;
							
							Looped := true;
						else
							Head := Head + 1;
						end if;
					end if;
				end if;
				
				
				if (Head = Tail) then
					if Looped then
						fl <= '1';
					else
						em <= '1';
					end if;
				else
					em	<= '0';
					fl	<= '0';
				end if;
			end if;
		end if;
	end process;
		
end Behavioral;
