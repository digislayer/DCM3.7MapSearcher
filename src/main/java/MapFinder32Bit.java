import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/*
Instructions:
Give your ECU dump file's location as FILE_PATH, and assign your ECU's maximum physical address location to MAX_ADDRESS_LOCATION_HEX. Then, compile and execute this class.
Copy the printed text from console and paste it into a Spreadsheet as Comma Separated Values.
False positives need to be filtered out from the spreadsheet, Look below under "caveats" for examples.

CAVEATS:
Tested with only Delphi DCM 3.7 from 90hp Hyundai i20/Accent 1.4L D4FC.
Works with only Big-Endian/HiLo ECUs. Code needs to be refactored for Little-Endian/LoHi ECUs.
This code is crude and dumb, it will also give a lot of false positives. These false positives usually have unrealistic X & Y axis dimensions using which they can be filtered out.
Examples of valid and realistic dimensions: 20x20, 16x16, 17x16, 8x8, etc.
Examples of unrealistic dimensions of false positives: 17846437 x 34, 0x10, 1x34565767, etc.

Working Principle:
This code Sliding Window Technique and reads the binary dump stream in chunks of 32 bits.
The buffer is scanned after every "slide", looking for specific sequence/pattern of a map declaration.
If a match is found, it's details are printed to console as comma separated text.
 */
public class MapFinder32Bit {
    static final int SEQ_SIZE = 5; //Scanning buffer sequence size. Delphi DCM3.7 uses a maximum of five 32-bit blocks for a 3d map declaration.
    static final String FILE_PATH = "/home/docker/Downloads/Verna1.4 - Stage1"; //Absolute path of the binary dump location
    static final String MAX_ADDRESS_LOCATION_HEX = "0021FFE0"; //HEX of last physical 16-bit address of the ECU file.
    public static void main(String[] args) throws IOException {
        if (FILE_PATH.trim().length() == 0) {
            System.err.println("Please declare a valid FILE_PATH");
            return;
        }
        List<String> seq = new ArrayList<>();
        System.out.println("Loading dump file...");
        ByteBuffer buf = readFile(new File(FILE_PATH));
        byte[] arr = buf.array();

        System.out.println("Starting the scan...");
        int i = 0;
        while(i < arr.length) {
            String cell = javax.xml.bind.DatatypeConverter.printHexBinary(new byte[]{
                    arr[i++], arr[i++], arr[i++], arr[i++]
            });
            seq.add(cell);

            if (seq.size() >= SEQ_SIZE) {
                if(valid3D(seq)) {
                    print3dLocations(seq);
                    seq.remove(0);
                    seq.remove(0);
                    seq.remove(0);
                    seq.remove(0);
                    seq.remove(0);
                } else if(valid2D(seq)) {
                    print2dLocations(seq);
                    seq.remove(0);
                    seq.remove(0);
                    seq.remove(0);
                    seq.remove(0);
                } else if(valid1D(seq)) {
                    print1dLocations(seq);
                    seq.remove(0);
                    seq.remove(0);
                    seq.remove(0);
                } else {
                    seq.remove(0);
                }
            }
        }
        System.out.flush();
    }

    /*
    Returns true if the buffer contains values in this sequence:
    1. 32bit block which is not a physical ROM address
    2. 32bit block which is an actual physical ROM address of X-Axis start
    3. 32bit block which is an actual physical ROM address of Y-Axis start
    4. 32bit block which is an actual physical ROM address of Map Data start
    5. 32bit block which is not a physical ROM address
     */
    public static boolean valid3D(List<String> seq) {
        if (seq.size() == SEQ_SIZE) {
            return !isAddress(seq.get(0))
                    && isAddress(seq.get(1))
                    && isAddress(seq.get(2))
                    && isAddress(seq.get(3))
                    && !isAddress(seq.get(4))
                    && Integer.parseInt(seq.get(2), 16) > Integer.parseInt(seq.get(1), 16)
                    && Integer.parseInt(seq.get(3), 16) > Integer.parseInt(seq.get(2), 16);
        }
        return false;
    }

    /*
    Returns true if the buffer contains values in this sequence:
    1. 32bit block which is not a physical ROM address
    2. 32bit block which is an actual physical ROM address of X-Axis start
    3. 32bit block which is an actual physical ROM address of Map Data start
    4. 32bit block which is not a physical ROM address
     */
    public static boolean valid2D(List<String> seq) {
        if (seq.size() >= (SEQ_SIZE - 1)) {
            return !isAddress(seq.get(0))
                    && isAddress(seq.get(1))
                    && isAddress(seq.get(2))
                    && !isAddress(seq.get(3))
                    && Integer.parseInt(seq.get(2), 16) > Integer.parseInt(seq.get(1), 16);
        }
        return false;
    }

    /*
    UNTESTED
    Returns true if the buffer contains values in this sequence:
    1. 32bit block which is not a physical ROM address
    2. 32bit block which is an actual physical ROM address of Map Data start
    3. 32bit block which is not a physical ROM address
     */
    public static boolean valid1D(List<String> seq) {
        if (seq.size() >= (SEQ_SIZE - 2)) {
            return !isAddress(seq.get(0))
                    && isAddress(seq.get(1))
                    && !isAddress(seq.get(2));
        }
        return false;
    }

    /*
    Prints the following comma separated values:
    1. Map type
    2. X Axis start
    3. Y Axis start
    4. Map data start
    5. X - Axis dimension
    6. Y - Axis dimension
     */
    public static void print3dLocations(List<String> seq) {
        System.out.print("3d, ");
        System.out.print(seq.get(1) + ", ");
        System.out.print(seq.get(2) + ", ");
        System.out.print(seq.get(3) + ", ");
        System.out.print((Integer.parseInt(seq.get(2), 16) - Integer.parseInt(seq.get(1), 16))/2 + ", ");
        System.out.print((Integer.parseInt(seq.get(3), 16) - Integer.parseInt(seq.get(2), 16))/2);
        System.out.println();
    }

    /*
    Prints the following comma separated values:
    1. Map type
    2. X Axis start
    3. BLANK
    4. Map data start
    5. X - Axis dimension
    6. BLANK
     */
    public static void print2dLocations(List<String> seq) {
        System.out.print("2d, ");
        System.out.print(seq.get(1) + ", ");
        System.out.print(", ");
        System.out.print(seq.get(2) + ", ");
        System.out.print((Integer.parseInt(seq.get(2), 16) - Integer.parseInt(seq.get(1), 16))/2);
        System.out.print(", ");
        System.out.println();
    }

    /*
    Prints the following comma separated values:
    1. Map type
    2. BLANK
    3. BLANK
    4. Map data start
    5. BLANK
    6. BLANK
     */
    public static void print1dLocations(List<String> seq) {
        System.out.print("1d, ");
        System.out.print(", ");
        System.out.print(", ");
        System.out.print(seq.get(1) + ", ");
        System.out.print(", ");
        System.out.print(", ");
        System.out.println();
    }

    /*
    verifies if the value within a cell/block is a valid address location
     */
    public static boolean isAddress(String cell) {
        return (cell.compareTo(MAX_ADDRESS_LOCATION_HEX) < 0 //cell < MAX_ADDRESS_LOCATION_HEX
                && !"00000000".equalsIgnoreCase(cell));
    }

    public static ByteBuffer readFile(File file) throws IOException {
        DataInputStream dataInputStream = null;
        try {
            // FIXME: this is broken for files larger than 4GiB.
            int byteCount = (int) file.length();
            FileInputStream fileInputStream = new FileInputStream(file);
            dataInputStream = new DataInputStream(fileInputStream);
            final byte[] bytes = new byte[byteCount];
            dataInputStream.readFully(bytes);

            return ByteBuffer.wrap(bytes);
        } finally {
            dataInputStream.close();
        }
    }
}
