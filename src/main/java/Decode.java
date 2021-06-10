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

	public static void main(String[] args) throws IOException {
		if (args.length == 0) throw new IllegalArgumentException();

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
			if (len == 0) // Control byte
				break;
			rep = 0;
			for (int j = (int) Math.ceil(len / 8.0) - 1; j >= 0; j--)
				rep |= ((long) (bytes[i++] & 0b1111_1111)) /* Unsigned trick */ << (8 * j);
			map.put(new Tuple<>(rep, len), b);
		}

		writeToFile(fileName, map, bytes, i);
	}

	private static void writeToFile(String fileName, Map<Tuple<Long, Byte>, Byte> map,
	                                byte[] data, int offset) throws IOException {
		// Convert bits to characters and write to file
		try (BufferedOutputStream out = new BufferedOutputStream(
		     		new FileOutputStream(fileName.substring(0, fileName.length() - 4)))) {
			byte buf;
			long rep = 0;
			byte len = 0;
			Byte b;
			int last = data.length - 1, bit, to;
			while (offset < last) {
				buf = data[offset++];
				/* For each bit in buf, start with leftmost bit */
				for (bit = 7, to = (offset == last) ? data[last] : 0; bit >= to; bit--) {
					rep <<= 1; // Shift bits over by one
					rep |= (buf >>> bit) & 1; // Add next bit
					len++; // Increase bit count
					/* If mapping exists */
					Tuple<Long, Byte> t;
					if ((b = map.get(t = new Tuple<>(rep, len))) != null) {
						out.write(b); // Write mapped character
						rep = len = 0; // Reset representation and length to 0
					}
				}
			}
		}
	}
}
