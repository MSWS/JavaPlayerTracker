package xyz.msws.tracker;

import java.io.*;

public class Main {

	public static void main(String[] args) {
		File tokenFile = new File(System.getProperty("user.dir") + File.separator + "token.txt");
		if (!tokenFile.exists()) {
			try {
				tokenFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		FileReader fr;
		try {
			fr = new FileReader(tokenFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		BufferedReader reader = new BufferedReader(fr);

		String token;
		try {
			token = reader.readLine();
			reader.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		new PlayerTracker(token).start();
	}
}
