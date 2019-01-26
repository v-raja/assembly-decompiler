import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class AssemblyDecompiler {
	
	private String[] instructions;
	private Tokenizer tokenizer;
	private String[] machineCode;
	private HashMap<String, Integer> symbols;
	private int nextVariableAddress;
	
	public AssemblyDecompiler (String fname) {
		nextVariableAddress = 16; 							// Initialise where new variable will be stored in memory
		tokenizer = new Tokenizer(fname);
		instructions = tokenizer.getTokens();
		symbols = new HashMap<String, Integer>();
		populateSymbols(); 
		machineCode = convertToMachineCode(instructions);
	}
	
	/**
	 * Performs two runs on each line on assembly instructions:
	 * First run:  Adds variables and location of loops in instructions to the HashMap symbols 
	 * Second run: Passes each instruction to toMachineCode and adds it to String[] machineCode
	 * @param instructions list of assembly instructions to convert
	 * @return String array of binary 
	 */
	private String[] convertToMachineCode(String[] instructions) {
		ArrayList<String> machineCode = new ArrayList<String>();
		int lineNumber = 0;
		for (String instruction : instructions) {
			addSymbol(instruction, lineNumber);
			if (instruction.charAt(0) != '(') lineNumber++; 
		}
		for (String instruction : instructions) {
			if (instruction.charAt(0) != '(') {
				machineCode.add(toMachineCode(instruction));
			}
		}
		return machineCode.toArray(new String[machineCode.size()]);
	}
	
	/**
	 * Converts decimal to 16-bit long binary number
	 * @param decimal decimal to be converted
	 * @return 16-bit binary of decimal
	 */
	private String to16BitBinary(int decimal) {
		String binary = Integer.toBinaryString(decimal);
		StringBuilder sb = new StringBuilder(binary);
		int num_zeros = 16 - sb.length();
		String zeros = String.join("", Collections.nCopies(num_zeros, "0"));
		sb.insert(0, zeros);
		return sb.toString();
	}
	
	/**
	 * Populates HashMap symbols with variables defined in the Hack language specifications
	 */
	private void populateSymbols() {
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
	 * Adds symbol in String assembly to HashMap symbols.
	 * If String assembly is a variable, put it in symbols with value int nextVariableAddress.
	 * If String assembly is a loop variable, put it symbols with value of int lineNumber.
	 * @param assembly variable in assembly to be stored in String[] symbols. Starts with '@' or '(',
	 * @param lineNumber the number of line in the instructions. Used only to store loop variables.
	 */
	private void addSymbol(String assembly, int lineNumber) {
		if (assembly.charAt(0) == '(') {
			symbols.put(assembly.substring(assembly.indexOf('(') + 1, assembly.lastIndexOf(')')), lineNumber);
		}
		String variable = assembly.substring(1);
		if (variable.equals(variable.toUpperCase())) {
			return;
		} else {
			symbols.putIfAbsent(variable, nextVariableAddress);
			nextVariableAddress++;
		}
	}
	
	/**
	 * Returns value of symbol from HashMap symbols
	 * If key is not found, symbol must be an address 
	 * @param symbol key to look up in HashMap symbols
	 * @return value of String symbol from HashMap symbols. If key is not found, return integer of symbol.
	 */
	private int getSymbolValue(String symbol) {
		if (symbols.get(symbol) == null) {
			return Integer.parseInt(symbol);
		}
		return symbols.get(symbol);
	}
	
	/**
	 * Converts String assembly to String binary by calling helper functions to decide binary of assembly.
	 * @param assembly instruction in assembly to be converted
	 * @return binary code of String assembly
	 */
	private String toMachineCode(String assembly) {
		if (assembly.charAt(0) == '@') {
			return to16BitBinary(getSymbolValue(assembly.substring(1)));
		} else {
			boolean contains_dest = assembly.contains("=");
			boolean contains_jump = assembly.contains(";");
			int comp_end = contains_jump ? assembly.indexOf(';') : assembly.length();
			int comp_start = contains_dest? assembly.indexOf('=') + 1 : 0;
			char a_bit = assembly.substring(comp_start, comp_end).contains("M") ? '1' : '0';
			String c_bits = getCompBits(assembly.substring(comp_start, comp_end));
			String d_bits = getDestBits(assembly.substring(0, comp_start));
			String j_bits = contains_jump ? getJumpBits(assembly.substring(comp_end + 1)) : "000";
			return "111" + a_bit + c_bits + d_bits + j_bits;
		}
	}
	
	/**
	 * Returns the 6 c-bits according to computation (as specified in Hack language specification)
	 * @param comp computation that occurs in an assembly instruction
	 * @return binary of computation comp
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
	 * Returns the 3 d-bits according to destination (as specified in Hack language specification)
	 * M stands for MRegister, A for ARegister, and D for DRegister
	 * @param dest destination of computation
	 * @return binary of String dest
	 */
	private String getDestBits(String dest) {
		String writeM = dest.contains("M") ? "1" : "0";
		String writeD = dest.contains("D") ? "1" : "0";
		String writeA = dest.contains("A") ? "1" : "0";
		return writeA + writeD + writeM;
	}
	
	/**
	 * Returns the 3 j-bits according to jump condition (as specified in Hack language specification)
	 * @param jump condition of jump of assembly instruction
	 * @return binary of String jump
	 */
	private String getJumpBits(String jump) {
		switch (jump) {
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
	
	public String[] getMachineCode() {
		return machineCode;
	}
	

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: ");
		}
		String file = args[0];
		
		String[] machineCode = new AssemblyDecompiler(file).getMachineCode();
		String fname = file.substring(0, file.indexOf('.'));
		FileWriter fileWriter = new FileWriter(fname + ".hack");
		PrintWriter printWriter = new PrintWriter(fileWriter);
		
		for (String code : machineCode) {
			printWriter.println(code);
		}
		printWriter.close();
	}
	
	
}
