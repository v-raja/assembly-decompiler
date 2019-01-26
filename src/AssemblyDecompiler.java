import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class AssemblyDecompiler {
	
	private Tokenizer tokenizer;
	private ArrayList<String> machineCode;
	private HashMap<String, Integer> symbols;
	private int nextVariableAddress;
	
	public AssemblyDecompiler (String fname) {
		nextVariableAddress = 16; 						// Initialise where new variable will be stored in memory
		tokenizer = new Tokenizer(fname);
		symbols = new HashMap<String, Integer>();
		machineCode = new ArrayList<String>();
		convertToMachineCode(tokenizer.getTokens());
	}
	
	
	private void convertToMachineCode(ArrayList<String> instructions) {
		populatePreDefinedSymbols(); 
		addSymbols(instructions);
		convert(instructions);
	}
	
	/**
	 * Returns type of {instruction}
	 * @param instruction An instruction in Hack Assembly language
	 * @return Type of {instruction} according to Hack Assembly language specification
	 */
	private String getType(String instruction) {
		if (instruction.charAt(0) == '(') return "LOOP_START";
		if (instruction.charAt(0) == '@') {
			String variable = instruction.substring(1);
			if (variable.matches("\\d+")) return "ADDRESS";    						// check if "@d" where d is an integer
			if (variable.equals(variable.toUpperCase())) return "LOOP_VARIABLE";	// check if "@VAR" where VAR is an upper-case variable name
			return "VARIABLE";														// check if "@var" where var is an upper-case variable name
		}
		return "COMPUTATION";
	}
	
	private void addSymbols(ArrayList<String> instructions) {
		addSymbols(instructions, 0);
	}
	
	private void addSymbols(ArrayList<String> instructions, int line_number) {
		if (instructions.isEmpty()) return;
		String instruction = instructions.get(0);
		switch (getType(instruction)) {
		case "VARIABLE":
			addVariable(instruction);
			break;
		case "LOOP_START":
			addLoopStart(instruction, line_number);
			line_number--;
			break;
		default:
			break;
		}
		line_number++;
		addSymbols(new ArrayList<String>(instructions.subList(1, instructions.size())), line_number);
	}
	
	/**
	 * Adds variable {variable} to symbols
	 * @param instruction An instruction in assembly that represents a variable
	 */
	private void addVariable(String instruction) {
		String variable = instruction.substring(1);
		if (symbols.putIfAbsent(variable, nextVariableAddress) == null) nextVariableAddress++;
	}
	
	/**
	 * Adds variable {loop_variable} to symbols
	 * @param instruction An instruction in assembly that represents the start of a loop
	 * @param line_number The line number after the start of a loop is declared. When a loop variable is used, the program will jump to this line number.
	 */
	private void addLoopStart(String instruction, int line_number) {
		String loop_variable = instruction.substring(instruction.indexOf('(') + 1, instruction.lastIndexOf(')'));
		symbols.put(loop_variable, line_number);
	}
	
	private void convert(ArrayList<String> instructions) {
		if (instructions.isEmpty()) return;
		String instruction = instructions.get(0);
		switch (getType(instruction)) {
		case "ADDRESS":
		case "VARIABLE":
		case "LOOP_VARIABLE":
		case "COMPUTATION":
			machineCode.add(toMachineCode(instruction));
			break;
		default:
			break;
		}
		convert(new ArrayList<String>(instructions.subList(1, instructions.size())));
	}

	
	/**
	 * Converts {decimal} to 16-bit long binary number
	 * @param decimal Decimal to be converted
	 * @return 16-bit long binary representation of decimal
	 */
	private String to16BitBinary(int decimal) {
		return addZeros(toBinary(decimal));
	}
	
	/**
	 * Returns binary representation of {decimal} 
	 * @param decimal Decimal to be converted
	 * @return Binary representation of {decimal}
	 */
	private String toBinary(int decimal) {
		return Integer.toBinaryString(decimal);
	}
	
	/**
	 * Adds zeros to make binary number 16-bits long
	 * @param binary Binary representation of a decimal
	 * @return 16-bit long binary representation of a decimal
	 */
	private String addZeros(String binary) {
		if (binary.length() == 16) return binary;
		return addZeros("0" + binary);
	}
	
	/**
	 * Populates HashMap symbols with variables defined in the Hack Assembly language specification
	 */
	private void populatePreDefinedSymbols() {
		for (int i = 0; i <= 15; i++) {
			symbols.put("R" + i, i);
		}
		symbols.put("SCREEN", 16384);
		symbols.put("KBD", 24576);
		symbols.put("SP", 0);
		symbols.put("LCL", 1);
		symbols.put("ARG", 2);
		symbols.put("THIS", 3);
		symbols.put("THAT", 4);
	}
	
	/**
	 * Returns value of a variable or loop variable from HashMap {symbols}
	 * @param symbol A variable or a loop variable
	 * @return value of {symbol} in {symbols}
	 */
	private int getSymbolValue(String symbol) {
		if (symbols.get(symbol) == null) {
			System.err.println("Cannot find symbol: " + symbol);
		}
		return symbols.get(symbol);
	}
	
	/**
	 * Converts an assembly instruction to its binary representation as defined in the Hack Assembly language specification
	 * @param instruction Instruction in assembly to be converted
	 * @return binary representation of {instruction}
	 */
	private String toMachineCode(String instruction) {
		switch (getType(instruction)) {
		case "VARIABLE":
		case "LOOP_VARIABLE":
			return to16BitBinary(getSymbolValue(instruction.substring(1)));
		case "ADDRESS":
			return to16BitBinary(Integer.parseInt(instruction.substring(1)));
		case "COMPUTATION":
			boolean contains_dest = instruction.contains("=");
			boolean contains_jump = instruction.contains(";");
			int comp_end = contains_jump ? instruction.indexOf(';') : instruction.length();
			int comp_start = contains_dest? instruction.indexOf('=') + 1 : 0;
			String computation = instruction.substring(comp_start, comp_end);
			String destination = instruction.substring(0, comp_start);
			String jump;
			try {
				jump = instruction.substring(comp_end + 1); 
			} catch (StringIndexOutOfBoundsException e) {
				jump = "000";
			}
			return "111" + getABit(computation) + getCompBits(computation) + getDestBits(destination) + getJumpBits(jump);
		case "LOOP_START":
		default:
			System.err.println("Invalid instruction: " + instruction);
			return null;
		}
	}
	
	
	private char getABit(String computation) {
		return computation.contains("M") ? '1' : '0';
	}
	
	/**
	 * Returns the 6 c-bits according to computation (as defined in Hack Assembly language specification)
	 * @param comp Computation that occurs in an assembly instruction
	 * @return Binary of computation {comp}
	 */
	private String getCompBits(String comp) {
		switch (comp) {
        case "0":
            return "101010";
        case "1":
        	return "111111";
        case "-1":
        	return "111010";
        case "D":
        	return "001100";
        case "M":
        case "A":
        	return "110000";
        case "!D":
        	return "001101";
        case "!M":
        case "!A":
        	return "110001";
        case "-D":
        	return "001111";
        case "-M":
        case "-A":
        	return "110011";
        case "D+1":
        	return "011111";
        case "M+1":
        case "A+1":
        	return "110111";
        case "D-1":
        	return "001110";
        case "M-1":
        case "A-1":
        	return "110010";
        case "D+M":
        case "D+A":
        	return "000010";
        case "D-M":
        case "D-A":
        	return "010011";
        case "M-D":
        case "A-D":
        	return "000111";
        case "D&M":
        case "D&A":
        	return "000000";
        case "D|M":
        case "D|A":
        	return "010101";
        default:
            throw new IllegalArgumentException("Invalid computation: " + comp);
		}
	}
	
	/**
	 * Returns the 3 d-bits according to destination (as defined in Hack Assembly language specification)
	 * M stands for MRegister, A for ARegister, and D for DRegister
	 * @param dest Destination of computation
	 * @return binary representation of destination {dest}
	 */
	private String getDestBits(String dest) {
		String writeM = dest.contains("M") ? "1" : "0";
		String writeD = dest.contains("D") ? "1" : "0";
		String writeA = dest.contains("A") ? "1" : "0";
		return writeA + writeD + writeM;
	}
	
	/**
	 * Returns the 3 j-bits according to jump condition (as defined in Hack Assembly language specification)
	 * @param jump Condition of jump of an instruction
	 * @return binary representation of jump condition {jump}
	 */
	private String getJumpBits(String jump) {
		switch (jump) {
		case "000":
		return "000";
		case "JGT":
			return "001";
		case "JEQ":
			return "010";
		case "JGE":
			return "011";
		case "JLT":
			return "100";
		case "JNE":
			return "101";
		case "JLE":
			return "110";
		case "JMP":
			return "111";
		default:
            throw new IllegalArgumentException("Invalid jump: " + jump);
		}
	}
	
	public ArrayList<String> getMachineCode() {
		return machineCode;
	}
	

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: java AssemblyDecompiler <String: file name>");
			return;
		}
		// Convert to machine code
		String file = args[0];
		ArrayList<String> machineCode = new AssemblyDecompiler(file).getMachineCode();
		
		// Write to file
		String fname = file.substring(0, file.indexOf('.'));
		FileWriter fileWriter = new FileWriter(fname + ".hack");
		PrintWriter printWriter = new PrintWriter(fileWriter);
		for (String code : machineCode) {
			printWriter.println(code);
		}
		printWriter.close();
	}
	
	
}
