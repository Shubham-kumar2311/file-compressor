import java.io.*;
import java.util.*;

class TreeNode implements Comparable<TreeNode> {
    Byte value;
    int freq;
    TreeNode left;
    TreeNode right;

    TreeNode(Byte value, int freq, TreeNode left, TreeNode right) {
        this.value = value;
        this.freq = freq;
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(TreeNode t) {
        return this.freq - t.freq;
    }
}

public class Zip {
    static HashMap<Byte, String> chart = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Choose an option:");
        System.out.println("1. Compress a file");
        System.out.println("2. Decompress a file");
        System.out.print("Enter your choice (1 or 2): ");
        
        String choice = scanner.nextLine();

        try {
            switch (choice) {
                case "1":
                    System.out.println("Enter source file path:");
                    String sourceFile = scanner.nextLine();
                    System.out.println("Enter compressed file path:");
                    String compressedFile = scanner.nextLine();
                    long[] sizes = compress(sourceFile, compressedFile);
                    System.out.println("Compression completed successfully.");
                    double percentReduction = ((double)(sizes[0] - sizes[1]) / sizes[0]) * 100;
                    System.out.printf("File size reduced by %.2f%%\n", percentReduction);
                    break;
                case "2":
                    System.out.println("Enter compressed file path:");
                    String compressedFileForDecomp = scanner.nextLine();
                    System.out.println("Enter decompressed file path:");
                    String decompressedFile = scanner.nextLine();
                    decompress(compressedFileForDecomp, decompressedFile);
                    System.out.println("Decompression completed successfully.");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 or 2.");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    public static long[] compress(String src, String des) throws IOException {
        File file = new File(src);
        if (!file.exists()) {
            throw new IOException("Source file does not exist");
        }

        long originalSize;
        long compressedSize;

        try (FileInputStream inputStream = new FileInputStream(src);
             FileOutputStream outputStream = new FileOutputStream(des);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            
            byte[] b = new byte[(int) file.length()];
            inputStream.read(b);
            originalSize = file.length();
            byte[] outputData = createZip(b);
            
            objectOutputStream.writeInt(b.length);
            objectOutputStream.writeObject(outputData);
            objectOutputStream.writeObject(chart);
            
            objectOutputStream.flush();
            compressedSize = new File(des).length();
        }

        return new long[]{originalSize, compressedSize};
    }

    public static byte[] createZip(byte[] b) {
        if (b.length == 0) return new byte[0];
        
        HashMap<Byte, Integer> hashFrequency = frequencyHash(b);
        TreeNode hashTree = makeTree(hashFrequency);
        chart = hashByTree(hashTree);
        return outputArray(b, chart);
    }

    public static void decompress(String src, String des) throws IOException, ClassNotFoundException {
        File file = new File(src);
        if (!file.exists()) {
            throw new IOException("Compressed file does not exist");
        }

        try (FileInputStream inputStream = new FileInputStream(src);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
             FileOutputStream outputStream = new FileOutputStream(des)) {
            
            int originalLength = objectInputStream.readInt();
            byte[] coded = (byte[]) objectInputStream.readObject();
            @SuppressWarnings("unchecked")
            Map<Byte, String> sheet = (Map<Byte, String>) objectInputStream.readObject();

            byte[] output = decomp(coded, sheet, originalLength);
            outputStream.write(output);
        }
    }

    static byte[] decomp(byte[] coded, Map<Byte, String> sheet, int originalLength) {
        if (coded.length == 0) return new byte[0];

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < coded.length; i++) {
            sb.append(convertByteToBit(coded[i]));
        }

        Map<String, Byte> map = new HashMap<>();
        for (Map.Entry<Byte, String> entry : sheet.entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }

        List<Byte> list = new ArrayList<>();
        int i = 0;
        while (i < sb.length() && list.size() < originalLength) {
            int ct = 1;
            Byte b = null;
            while (b == null && i + ct <= sb.length()) {
                String key = sb.substring(i, i + ct);
                b = map.get(key);
                if (b == null) ct++;
            }
            if (b != null) {
                list.add(b);
                i += ct;
            } else {
                break;
            }
        }

        byte[] result = new byte[list.size()];
        for (int j = 0; j < result.length; j++) {
            result[j] = list.get(j);
        }
        return result;
    }

    static String convertByteToBit(byte b) {
        int curByte = b & 0xFF;
        return String.format("%8s", Integer.toBinaryString(curByte)).replace(' ', '0');
    }

    static HashMap<Byte, Integer> frequencyHash(byte[] str) {
        HashMap<Byte, Integer> hash = new HashMap<>();
        for (byte c : str) {
            hash.put(c, hash.getOrDefault(c, 0) + 1);
        }
        return hash;
    }

    static TreeNode makeTree(HashMap<Byte, Integer> hash) {
        if (hash.isEmpty()) return null;

        PriorityQueue<TreeNode> pq = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> v : hash.entrySet()) {
            pq.add(new TreeNode(v.getKey(), v.getValue(), null, null));
        }

        while (pq.size() > 1) {
            TreeNode t1 = pq.poll();
            TreeNode t2 = pq.poll();
            TreeNode t3 = new TreeNode(null, t1.freq + t2.freq, t1, t2);
            pq.add(t3);
        }
        return pq.poll();
    }

    static HashMap<Byte, String> hashByTree(TreeNode t) {
        HashMap<Byte, String> hash = new HashMap<>();
        if (t != null) {
            hashByTreeHelper(t, new StringBuilder(), hash);
        }
        return hash;
    }

    static void hashByTreeHelper(TreeNode t, StringBuilder sb, HashMap<Byte, String> hash) {
        if (t == null) return;

        if (t.value != null) {
            hash.put(t.value, sb.toString());
            return;
        }

        sb.append('0');
        hashByTreeHelper(t.left, sb, hash);
        sb.deleteCharAt(sb.length() - 1);

        sb.append('1');
        hashByTreeHelper(t.right, sb, hash);
        sb.deleteCharAt(sb.length() - 1);
    }

    static byte[] outputArray(byte[] bytes, HashMap<Byte, String> hash) {
        StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String code = hash.get(b);
                if (code != null) {
                    sb.append(code);
                }
            }

            int length = (sb.length() + 7) / 8;
            byte[] output = new byte[length];
            
            for (int i = 0; i < length; i++) {
                int start = i * 8;
                int end = Math.min(start + 8, sb.length());
                String chunk = sb.substring(start, end);
                if (chunk.length() < 8) {
                    chunk = String.format("%-8s", chunk).replace(' ', '0');
                }
                output[i] = (byte) Integer.parseInt(chunk, 2);
            }
            return output;
    }
}
