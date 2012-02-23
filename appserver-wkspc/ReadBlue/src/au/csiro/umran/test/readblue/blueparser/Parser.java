package au.csiro.umran.test.readblue.blueparser;

import java.io.IOException;

public interface Parser {

	public abstract void quit();

	public abstract void process() throws IOException;
	
	public abstract long getLastestReadTime();

}