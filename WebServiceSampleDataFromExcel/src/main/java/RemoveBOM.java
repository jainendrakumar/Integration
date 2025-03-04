import java.io.*;

public class RemoveBOM {
    public static void main(String[] args) throws IOException {
        String inputFilePath = "E:\\work\\Intellij\\Java\\WebServiceSampleDataFromExcel\\src\\main\\java\\DRTLogAnalyzer.java";
        String outputFilePath = "E:\\work\\Intellij\\Java\\WebServiceSampleDataFromExcel\\src\\main\\java\\DRTLogAnalyzer_Cleaned.java";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            int firstChar = reader.read();
            if (firstChar != '\ufeff') { // If BOM is not present, write the character back
                writer.write(firstChar);
            }
            int ch;
            while ((ch = reader.read()) != -1) {
                writer.write(ch);
            }
        }

        System.out.println("BOM removed. Cleaned file saved as: " + outputFilePath);
    }
}
