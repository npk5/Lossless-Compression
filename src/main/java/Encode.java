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

	public static void main(String[] args) throws IOException {
		if (args.length == 0) throw new IllegalArgumentException();

		String fileName = args[0];

		// Read file
		byte[] bytes;
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName))) {
			bytes = in.readAllBytes();
		}

		// For each character, increase its frequency
		Map<Byte, Integer> freqMap = new HashMap<>();
		for (byte b : bytes)
			freqMap.put(b, freqMap.getOrDefault(b, 0) + 1);

		// Put all mappings into a PriorityQueue sorted by frequency
		Queue<Node> pq = new PriorityQueue<>(freqMap.size());
		freqMap.forEach((key, value) -> pq.add(new Node(key, value)));

		// Create Heap
		while (pq.size() > 1)
			pq.add(new Node(pq.poll(), pq.poll()));

		// Get Heap and create a mapping for each Character
		Node heap = pq.poll();
		Map<Byte, Tuple<Long, Byte>> map = new HashMap<>();
		recursiveAdd(heap, map, new Tuple<>(0L, (byte) 0));

		writeToFile(fileName, map, bytes);
	}

	private static void recursiveAdd(Node heap, Map<Byte, Tuple<Long, Byte>> map, Tuple<Long, Byte> tuple) {
		if (heap == null) return;
		if (heap.isLeaf()) {
			map.put(heap.elem, tuple.component2() == 0 ? new Tuple<>(0L, (byte) 1) : tuple);
		} else {
			recursiveAdd(heap.left, map, new Tuple<>(
					tuple.component1() << 1, (byte) (tuple.component2() + 1)));
			recursiveAdd(heap.right, map, new Tuple<>(
					(tuple.component1() << 1) | 1, (byte) (tuple.component2() + 1)));
		}
	}

	private static void writeToFile(String fileName, Map<Byte, Tuple<Long, Byte>> map,
	                                byte[] bytes) throws IOException {
		File file = new File(fileName + ".enc");
		file.createNewFile();

		// Write output to .enc file
		try (final BufferedOutputStream enc = new BufferedOutputStream(new FileOutputStream(file))) {
			// Header
			for (Map.Entry<Byte, Tuple<Long, Byte>> entry : map.entrySet()) {
				enc.write(entry.getKey());
				byte bitCount = entry.getValue().component2();
				enc.write(bitCount);
				long rep = entry.getValue().component1();
				for (int i = (int) Math.ceil(bitCount/8.0) - 1; i >= 0; i--)
					enc.write((byte) (rep >>> (8 * i)));
			}

			// Mark end of header
			enc.write(new byte[]{0, 0});

			// Data
			byte buf = 0, bits = 0, next;
			for (byte b : bytes) {
				// Get mapping for byte
				Tuple<Long, Byte> mapping = map.get(b);

				// Get representation
				long rep = mapping.component1();
				for (byte bitCount = mapping.component2(); bitCount > 0;) {
					bitCount -= 8 - bits;
					if (bitCount >= 0) // If byte can be filled
						next = (byte) (rep >>> bitCount);
					else
						next = (byte) (rep << -bitCount);

					if (bits > 0)
						next &= ~((byte) (0b1000_0000) >> (bits - 1)); // Mask bits
					buf |= next;
					if ((bits = (byte) (8 + bitCount)) >= 8) {
						enc.write(buf);
						buf = bits = 0; // Clear buffer
					}
				}
			}
			enc.write(buf);
			enc.write(bits);
		}
	}

	private static record Node(Byte elem, Integer freq, Node left, Node right) implements Comparable<Node> {
		Node(Node left, Node right) {
			this((byte) 0, left.freq + right.freq, left, right);
		}

		Node(Byte elem, Integer freq) {
			this(elem, freq, null, null);
		}

		boolean isLeaf() {
			return left == null && right == null;
		}

		@Override
		public int compareTo(Node o) {
			return freq - o.freq;
		}
	}
}
