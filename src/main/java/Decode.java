import java.io.*;
import java.util.*;

/**
 * The decompression algorithm for use with {@link Encode}.
 * @see Encode
 * @author NPK
 */
public class Decode {

	private final String fileName;
	private byte[] data;
	private int offset;
	private int depth;

	// Acts as ArrayList
	private byte[] decoded;
	private int size;

	public Decode(String fileName) throws IOException {
		if (!fileName.endsWith(Encode.EXTENSION))
			throw new IllegalArgumentException();

		this.fileName = fileName;
		readFromFile();
	}

	private void readFromFile() throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName))) {
			depth = in.read();
			data = in.readAllBytes();
		}
	}

	public void writeToFile() throws IOException {
		String fileName = this.fileName.substring(0, this.fileName.length() - Encode.EXTENSION.length());
		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName))) {
			out.write(data);
		}
	}

	private Map<Tuple<Long, Byte>, Byte> createByteMap() {
		Map<Tuple<Long, Byte>, Byte> map = new HashMap<>();
		byte b, len;
		long rep;
		while (true) {
			b = data[offset++];
			len = data[offset++];
			if (len == 0) // Control bytes, end of header
				break;

			rep = 0;
			for (int j = (int) Math.ceil(len / 8.0) - 1; j >= 0; j--)
				rep |= ((long) data[offset++] & 0xff) /* Revert sign extension on cast */ << (8 * j);

			map.put(new Tuple<>(rep, len), b);
		}

		return map;
	}

	public Decode decode() {
		if (depth <= 0) return this;
		depth--;

		Map<Tuple<Long, Byte>, Byte> map = createByteMap();
		decode();
		decode(map);

		return this;
	}

	private void decode(Map<Tuple<Long, Byte>, Byte> map) {
		clear(data.length);

		int last = data.length - 1;
		long rep = 0;
		byte length = 0;
		Byte b;
		for (byte buf; offset < last;) {
			buf = data[offset++]; // Get next byte
			for (int bit = 7, to = (offset == last) ? (8 - data[last]) : 0; bit >= to; bit--) {
				rep = (rep << 1) | ((buf >>> bit) & 1); // Shift bits over by one and add next bit
				length++; // Increase bit count

				if ((b = map.get(new Tuple<>(rep, length))) != null) {
					add(b); // Write mapped byte
					rep = length = 0; // Reset representation and length to 0
				}
			}
		}

		data = toArray();
		offset = 0;
	}

	// ArrayList methods

	private void clear(int newSize) {
		decoded = new byte[newSize];
		size = 0;
	}

	private byte[] toArray() {
		return Arrays.copyOfRange(decoded, 0, size);
	}

	private void add(byte b) {
		if (size == decoded.length) expand();
		decoded[size++] = b;
	}

	private void expand() {
		decoded = Arrays.copyOf(decoded, decoded.length * 2);
	}

	// End of ArrayList methods

	/**
	 * Entry point of the decoding program.
	 * @param args file names
	 */
	public static void main(String[] args) {
		Arrays.stream(args).parallel().forEach(fileName -> {
			try {
				new Decode(fileName).decode().writeToFile();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		});
	}
}
