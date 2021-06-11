import java.io.*;
import java.util.*;

/**
 * A decompression algorithm for use with {@link Encode}.<br>
 * <br>
 * Usage:<br>
 * <br>
 * Call this class with a file name as a parameter. The file will then be
 * decoded and stored as the file name without .enc.<br>
 * <br>
 * Use {@link Encode} for encoding.<br>
 * <br>
 * @see Encode
 * @author NPK
 */
public class Decode {

	/**
	 * Entry point of the decoding program.
	 * @param args file name at index 0
	 * @throws IllegalArgumentException when no arguments are specified or file name has incorrect extension
	 * @throws IOException when any of the I/O operations fail
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0 || !args[0].endsWith(Encode.EXTENSION)) throw new IllegalArgumentException();

		String fileName = args[0];

		// Read file
		byte[] bytes;
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName))) {
			bytes = in.readAllBytes();
		}

		// Build map from file header
		Map<Tuple<Long, Byte>, Byte> map = new HashMap<>();
		int i = 0;
		byte b, len;
		long rep;
		while (true) {
			b = bytes[i++];
			len = bytes[i++];
			if (len == 0) // Control bytes, end of header
				break;

			rep = 0;
			for (int j = (int) Math.ceil(len / 8.0) - 1; j >= 0; j--)
				rep |= ((long) bytes[i++] & 0xff) /* Revert sign extension on cast */ << (8 * j);

			map.put(new Tuple<>(rep, len), b);
		}

		writeToFile(fileName.substring(0, fileName.length() - Encode.EXTENSION.length()), map, bytes, i);
	}

	/**
	 * Converts bits to given mapping and writes them to file.
	 * @param fileName name of the file to write to
	 * @param map bits-to-byte map
	 * @param data bytes to convert
	 * @param offset start of data
	 * @throws IOException when any of the I/O operations fail
	 */
	private static void writeToFile(String fileName, Map<Tuple<Long, Byte>, Byte> map,
	                                byte[] data, int offset) throws IOException {
		int last = data.length - 1;
		// Convert bit groups to respective bytes and write to file
		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName))) {
			long rep = 0;
			byte len = 0;
			Byte b;
			for (byte buf; offset < last;) {
				buf = data[offset++]; // Get next byte
				for (int bit = 7, to = (offset == last) ? data[last] : 0; bit >= to; bit--) {
					rep = (rep << 1) | ((buf >>> bit) & 1); // Shift bits over by one and add next bit
					len++; // Increase bit count

					if ((b = map.get(new Tuple<>(rep, len))) != null) {
						out.write(b); // Write mapped byte
						rep = len = 0; // Reset representation and length to 0
					}
				}
			}
		}
	}
}
