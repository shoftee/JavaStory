package javastory.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author shoftee
 */
public class PropertyUtil {

	private PropertyUtil() {
	}

	public static void loadInto(final String filename, final Properties properties) {
		checkNotNull(filename);
		checkNotNull(properties);

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(filename);
			properties.load(stream);
		} catch (final FileNotFoundException ex) {
			Logger.getLogger(PropertyUtil.class.getName()).log(Level.SEVERE, null, ex);
		} catch (final IOException ex) {
			Logger.getLogger(PropertyUtil.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (final IOException ex) {
					Logger.getLogger(PropertyUtil.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}
}
