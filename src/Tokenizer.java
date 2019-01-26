import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Tokenizer {
	private ArrayList<String> tokens;
	
	public Tokenizer(String fname) {
		tokens = new ArrayList<String>();
		File file = new File(fname);
		tokenize(file);
	}

	public void tokenize(File file) {
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String str = scanner.nextLine();
				StringBuilder sb = new StringBuilder();
				for (int index = 0; index < str.length(); index++) {
					char c = str.charAt(index);
					if (c == '/') {
						break;
						
					} else {
						sb.append(c);
					}
				}
				if (!sb.toString().isEmpty()) {
					tokens.add(sb.toString().replaceAll("\\s",""));
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public ArrayList<String> getTokens() {
		return tokens;
	}
	
	public int getNumberTokens() {
		return tokens.size();
	}
	

}
