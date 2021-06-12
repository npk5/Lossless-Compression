import java.io.*;
import java.util.*;

/**
 * A recursive lossless compression algorithm based on Huffman encoding.<br>
 * <br>
 * This algorithm can be used on any type of file but will be more effective on
 * files with a lot of repeating bytes. These bytes will then be stored using
 * fewer bits, thus saving space. The flipside of this, however, is that the
 * file may take up more space if the data is more random.<br>
 * <br>
 * Usage:<br>
 * <br>
 * Call this class with a file name as a parameter. The file will then be
 * encoded and stored as the file name with .enc.<br>
 * <br>
 * Use {@link Decode} for decoding.<br>
 * <br>
 * @see Decode
 * @author NPK
 */
public class Encode {

	// File extension for the encoded file
	static final String EXTENSION = ".enc";

	private final String fileName;
	private Map<Byte, Integer> freqMap;
	private Map<Byte, Tuple<Long, Byte>> map;
	private final LinkedList<byte[]> headers;
	private byte[] body;

	public Encode(String fileName) throws IOException {
		this.fileName = fileName;
		headers = new LinkedList<>();
		readFromFile();
	}

	private void readFromFile() throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName))) {
			body = in.readAllBytes();
		}
	}

	public void writeToFile() throws IOException {
		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName + EXTENSION))) {
			out.write(headers.size());
			for (byte[] header : headers)
				out.write(header);
			out.write(body);
		}
	}

	private void createMaps() {
		// For each byte, increase its frequency
		freqMap = new HashMap<>();
		for (byte b : body)
			freqMap.put(b, freqMap.getOrDefault(b, 0) + 1);

		// Put all mappings into a PriorityQueue sorted by frequency
		Queue<Node> pq = new PriorityQueue<>(Math.max(freqMap.size(), 1));
		freqMap.forEach((key, value) -> pq.add(new Node(key, value)));

		// Create Heap
		while (pq.size() > 1)
			pq.add(new Node(pq.poll(), pq.poll()));

		// Get Heap and create a mapping for each Byte
		Node heap = pq.poll();
		map = new HashMap<>();
		createByteMap(heap, 0L, (byte) 0);
	}

	/**
	 * Creates a map from a max heap.
	 * @param heap max heap to construct map from
	 * @param rep the value to represent the bits
	 * @param length the amount of bits to represent the byte
	 */
	private void createByteMap(Node heap, Long rep, Byte length) {
		if (heap == null) return;
		if (heap.isLeaf()) {
			map.put(heap.elem, new Tuple<>(rep, (byte) Math.max(length, 1)));
		} else {
			createByteMap(heap.left, rep << 1, (byte) (length + 1));
			createByteMap(heap.right, (rep << 1) | 1, (byte) (length + 1));
		}
	}

	public Encode encode() {
		Map<Byte, Integer> lastFreqMap = freqMap;
		Map<Byte, Tuple<Long, Byte>> lastMap = map;

		createMaps();

		// Calculate size of encoded data
		int headerSize = 2, bodySize = 0;
		for (Map.Entry<Byte, Tuple<Long, Byte>> entry : map.entrySet()) {
			headerSize += 2 + (int) Math.ceil(entry.getValue()._2() / 8.0);
			bodySize += freqMap.get(entry.getKey()) * entry.getValue()._2();
		}
		bodySize = bodySize / 8 + 2; // Convert to bytes

		if (headerSize + bodySize < body.length) {
			encode(headerSize, bodySize);
			return encode();
		}

		freqMap = lastFreqMap;
		map = lastMap;
		return this;
	}

	private void encode(int headerSize, int bodySize) {
		createHeader(headerSize);
		encodeBody(bodySize);
	}

	private void createHeader(int size) {
		byte[] header = new byte[size];
		int i = 0;

		long rep;
		byte bits;
		for (Map.Entry<Byte, Tuple<Long, Byte>> entry : map.entrySet()) {
			header[i++] = entry.getKey(); // Byte

			Tuple<Long, Byte> mapping = entry.getValue();
			bits = mapping._2();
			header[i++] = bits; // Amount of bits

			rep = mapping._1();
			for (int j = (int) Math.ceil(bits / 8.0) - 1; j >= 0; j--)
				header[i++] = (byte) (rep >>> (8 * j)); // Representation
		}

		headers.add(header);
	}

	private void encodeBody(int size) {
		byte[] body = new byte[size];
		int i = 0;

		long rep;
		byte bits;
		byte buf = 0, bufSize = 0;
		for (byte b : this.body) {
			Tuple<Long, Byte> mapping = map.get(b);

			rep = mapping._1();
			for (bits = mapping._2(); bits > 0;) {
				buf |= (((bits -= 8 - bufSize) >= 0)
						? (rep >>> bits) // If able to fill byte
						: (rep << -bits))
						& ((0xff) >>> bufSize); // Mask bits to left

				if ((bufSize = (byte) (8 + bits)) >= 8) {
					body[i++] = buf;
					buf = bufSize = 0; // Clear buffer
				}
			}
		}
		body[i++] = buf;
		body[i] = bufSize;
		this.body = body;
	}

	/**
	 * Entry point of the encoding program.
	 * @param args file names
	 */
	public static void main(String[] args) {
		Arrays.stream(args).parallel().forEach(fileName -> {
			try {
				new Encode(fileName).encode().writeToFile();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		});
	}

	/**
	 * Inner Node record class.<br>
	 * Implements Comparable interface, natural order based on frequency.
	 * @since 16
	 * @see java.lang.Record
	 */
	private static record Node(Byte elem, Integer freq, Node left, Node right) implements Comparable<Node> {

		/**
		 * Constructor for Node record with left and right children.<br>
		 * The frequency will be the sum of its children's frequencies.
		 * @param left left child
		 * @param right right child
		 */
		Node(Node left, Node right) {
			this(null, left.freq + right.freq, left, right);
		}

		/**
		 * Constructor for Node record with element and frequency.
		 * @param elem byte
		 * @param freq frequency
		 */
		Node(Byte elem, Integer freq) {
			this(elem, freq, null, null);
		}

		/**
		 * Returns whether the given Node is a leaf Node.
		 * @return true if node does not have any children, false otherwise
		 */
		boolean isLeaf() {
			return left == null && right == null;
		}

		/**
		 * Implementation of Comparable interface.<br>
		 * Order based on frequency.
		 * @param o other Node
		 * @return an integer representing the order of the Nodes
		 */
		@Override
		public int compareTo(Node o) {
			return freq - o.freq;
		}
	}
}
