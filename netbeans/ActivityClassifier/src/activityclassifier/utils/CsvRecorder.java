/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package activityclassifier.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 *
 * @author Xevia
 */
public class CsvRecorder
{

	private File outputFile;
	private PrintWriter outputWriter;

	public CsvRecorder( File outputFile )
			throws Exception {
		OutputStream outputStream = new BufferedOutputStream( new FileOutputStream( outputFile ),
															  10240 );
		outputWriter = new PrintWriter( outputStream );
	}

	private void record( StringBuilder builder, Object val ) {

		if ( val != null ) {
			if ( (val instanceof Long) ||
				 (val instanceof Integer) ||
				 (val instanceof Short) ||
				 (val instanceof Byte) ||
				 (val instanceof Double) ||
				 (val instanceof Float) ||
				 (val instanceof Character) ||
				 (val.getClass().isPrimitive()) ) {
				if ( builder.length() > 0 ) {
					builder.append( "," );
				}
				builder.append( val.toString() );
			} else if ( val.getClass().isArray() ) {
				int length = Array.getLength( val );
				for ( int i = 0; i < length; ++i ) {
					record( builder, Array.get( val, i ) );
				}
			} else {
				if ( builder.length() > 0 ) {
					builder.append( "," );
				}
				builder.append( "\"" ).append( (String) val ).append( "\"" );
			}
		} else {
			if ( builder.length() > 0 ) {
				builder.append( "," );
			}
			builder.append( "\"null\"" );
		}
	}

	public void record( Object... vals ) {
		StringBuilder builder = new StringBuilder();

		for ( Object val : vals ) {
			record( builder, val );
		}

		String line = builder.toString();
		outputWriter.println( line );
	}

	public void flush() {
		outputWriter.flush();
	}

	public void close() {
		outputWriter.flush();
		outputWriter.close();
	}
}
