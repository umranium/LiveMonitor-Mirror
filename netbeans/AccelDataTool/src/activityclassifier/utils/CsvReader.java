/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Xevia
 *
 * separator
 */
public class CsvReader {

	private File file;
	private BufferedReader reader;
	private Pattern cellPattern;

	public CsvReader(char cellSeparator, File file) throws FileNotFoundException {
		this.file = file;
		this.reader = new BufferedReader( new FileReader( file ));
		String regex = "\\s*(?:(?:\"([^\"]*)\")|([^"+cellSeparator+"]+))\\s*"+cellSeparator+"?";
		this.cellPattern = Pattern.compile(regex);
	}

	public void close() throws IOException {
		reader.close();
	}

	private void parseRow(String line, List<String> outputList) {
		Matcher m = cellPattern.matcher(line);
		int i = 0;
		while (m.find()) {
			++i;
			int groupCount = m.groupCount();
			for (int g=1; g<=groupCount; ++g) {
				String val = m.group(g);
				if (val!=null) {
					val = val.trim();
					outputList.add( val );
				}
			}
		}
	}

	public boolean readRow(List<String> outputList) throws IOException {
		outputList.clear();
		String line = this.reader.readLine();
		if (line==null)
			return false;
		parseRow( line, outputList );
		return true;
	}

}
