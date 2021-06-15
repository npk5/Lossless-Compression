<h2>Lossless compression algorithm based on Huffman coding</h2>

This algorithm is not intended for regular file compression. It only performs well in specific cases.<br>
<br>
Recommended use cases:<br>
<ul>
	<li>Compression of plain text files</li>
	<li>Compression of PNG images with a lot of even colors</li>
</ul>
E.g., a file containing 8192 identical bytes can be compressed down to just 34 bytes.
<br>
Both classes can be called with a list of file names. If the files exists, it will then either encode or decode them. This will be done by recursively encoding the body of the file, until the space it would take up after encoding is greater than or equal to the size without encoding. This has the side effect that data that cannot be compressed by the algorithm will only take up one extra byte.<br>
<br>
If you want to learn more about Huffman coding, check out <a href="https://en.wikipedia.org/wiki/Huffman_coding">this Wikipedia article</a>.<br>
<br>
<h3>File format</h3>
The first byte is reserved for the recursion depth of the encoding. This is the exact amount of headers that will then follow. The headers contain the maps that tell the program how to decipher the data. Each header is terminated with two null values. The data is added last.
