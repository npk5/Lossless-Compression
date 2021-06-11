import java.io.*;
import java.util.*;

/**
 * A lossless compression algorithm based on Huffman encoding.<br>
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

	/**
	 * Entry point of the encoding program.
	 * @param args file name at index 0
	 * @throws IllegalArgumentException when no arguments are specified
	 * @throws IOException when any of the I/O operations fail
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0) throw new IllegalArgumentException();

		String fileName = args[0];

		// Read file
		byte[] bytes;
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName))) {
			bytes = in.readAllBytes();
		}

		// For each byte, increase its frequency
		Map<Byte, Integer> freqMap = new HashMap<>();
		for (byte b : bytes)
			freqMap.put(b, freqMap.getOrDefault(b, 0) + 1);

		// Put all mappings into a PriorityQueue sorted by frequency
		Queue<Node> pq = new PriorityQueue<>(Math.max(freqMap.size(), 1));
		freqMap.forEach((key, value) -> pq.add(new Node(key, value)));

		// Create Heap
		while (pq.size() > 1)
			pq.add(new Node(pq.poll(), pq.poll()));

		// Get Heap and create a mapping for each Byte
		Node heap = pq.poll();
		Map<Byte, Tuple<Long, Byte>> map = new HashMap<>();
		createMap(heap, map, new Tuple<>(0L, (byte) 0));

		writeToFile(fileName + EXTENSION, map, bytes);
	}

	/**
	 * Creates a map from a heap
	 * @param heap max heap to construct map from
	 * @param map reference to the map to be used
	 * @param tuple tuple to represent the byte
	 */
	private static void createMap(Node heap, Map<Byte, Tuple<Long, Byte>> map, Tuple<Long, Byte> tuple) {
		if (heap == null) return;
		if (heap.isLeaf()) {
			map.put(heap.elem, tuple.component2() == 0 ? new Tuple<>(0L, (byte) 1) : tuple);
		} else {
			createMap(heap.left, map, new Tuple<>(
					tuple.component1() << 1, (byte) (tuple.component2() + 1)));
			createMap(heap.right, map, new Tuple<>(
					(tuple.component1() << 1) | 1, (byte) (tuple.component2() + 1)));
		}
	}

	/**
	 * Converts bytes to given mapping and writes them to file.<br>
	 * Includes a header containing all the mappings.
	 * @param fileName name of the file to write to
	 * @param map byte-to-bits map
	 * @param data bytes to convert
	 * @throws IOException when any of the I/O operations fail
	 */
	private static void writeToFile(String fileName, Map<Byte, Tuple<Long, Byte>> map,
	                                byte[] data) throws IOException {
		File file = new File(fileName);
		file.createNewFile();

		// Write output to .enc file
		try (BufferedOutputStream enc = new BufferedOutputStream(new FileOutputStream(file))) {
			long rep;
			byte bits;

			// Header
			for (Map.Entry<Byte, Tuple<Long, Byte>> entry : map.entrySet()) {
				enc.write(entry.getKey()); // Byte

				Tuple<Long, Byte> mapping = entry.getValue();
				bits = mapping.component2();
				enc.write(bits); // Amount of bits

				rep = mapping.component1();
				for (int i = (int) Math.ceil(bits / 8.0) - 1; i >= 0; i--)
					enc.write((byte) (rep >>> (8 * i))); // Representation
			}

			// Mark end of header
			enc.write(new byte[]{0, 0});

			// Data
			byte buf = 0, bufSize = 0;
			for (byte b : data) {
				Tuple<Long, Byte> mapping = map.get(b);

				rep = mapping.component1();
				for (bits = mapping.component2(); bits > 0;) {
					buf |= (((bits -= 8 - bufSize) >= 0)
							? (rep >>> bits) // If able to fill byte
							: (rep << -bits))
							& ((0xff) >>> bufSize); // Mask bits to left

					if ((bufSize = (byte) (8 + bits)) >= 8) {
						enc.write(buf);
						buf = bufSize = 0; // Clear buffer
					}
				}
			}
			enc.write(buf);
			enc.write(bufSize);
		}
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
