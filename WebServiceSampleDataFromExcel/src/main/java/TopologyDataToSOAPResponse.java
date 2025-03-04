import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Iterator;

public class TopologyDataToSOAPResponse {

    public static void main(String[] args) {
        try {
            // Load Excel file
            String filePath = "src/test/java/TopologyData.xlsx";
            FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            Workbook workbook = new XSSFWorkbook(fileInputStream);

            // Prepare XML Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create SOAP Envelope
            Element envelope = doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Envelope");
            envelope.setAttribute("xmlns:soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
            envelope.setAttribute("xmlns:net", "http://example.com/networktopologyresponse");
            envelope.setAttribute("xmlns:mes", "http://example.com/messageheader");
            envelope.setAttribute("xmlns:top", "http://example.com/topologyversions");
            envelope.setAttribute("xmlns:top1", "http://example.com/topologynode");
            envelope.setAttribute("xmlns:top2", "http://example.com/topologylink");
            envelope.setAttribute("xmlns:top3", "http://example.com/topologyallowedlink");
            envelope.setAttribute("xmlns:top4", "http://example.com/topologyincompatiblelink");
            doc.appendChild(envelope);

            // Create Header
            Element header = doc.createElement("soapenv:Header");
            envelope.appendChild(header);

            // Create Body
            Element body = doc.createElement("soapenv:Body");
            envelope.appendChild(body);

            // Create NetworkTopologyResponse
            Element response = doc.createElement("net:NetworkTopologyResponse");
            body.appendChild(response);

            // Add MessageHeader
            Element messageHeader = doc.createElement("net:MessageHeader");
            response.appendChild(messageHeader);

            addElement(doc, messageHeader, "mes:InterchangeID", "12345");
            addElement(doc, messageHeader, "mes:RequestID", "REQ001");
            addElement(doc, messageHeader, "mes:Sender", "SystemA");
            addElement(doc, messageHeader, "mes:Receiver", "SystemB");
            addElement(doc, messageHeader, "mes:MessageType", "TopologyResponse");
            addElement(doc, messageHeader, "mes:xsdVersion", "1.0");
            addElement(doc, messageHeader, "mes:TestIndicator", "false");
            addElement(doc, messageHeader, "mes:Environment", "Production");
            addElement(doc, messageHeader, "mes:MsgDateTime", "2024-12-10T12:00:00Z");

            // Add MessageBody
            Element messageBody = doc.createElement("net:MessageBody");
            response.appendChild(messageBody);

            // Add TopologyVersions
            Element topologyVersions = doc.createElement("net:TopologyVersions");
            messageBody.appendChild(topologyVersions);

            // Add TopologyVersion
            Element topologyVersion = doc.createElement("top:TopologyVersion");
            topologyVersions.appendChild(topologyVersion);

            addElement(doc, topologyVersion, "top:VersionID", "1.0");
            addElement(doc, topologyVersion, "top:VersionName", "Initial Version");
            addElement(doc, topologyVersion, "top:VersionDescription", "First topology version");
            addElement(doc, topologyVersion, "top:ValidFrom", "2024-01-01");
            addElement(doc, topologyVersion, "top:ValidTo", "2024-12-31");

            // Process TopologyNode sheet
            Element topologyNodes = doc.createElement("top:TopologyNodes");
            topologyVersion.appendChild(topologyNodes);
            processSheet(workbook, "TopologyNode", topologyNodes, doc, "top1:TopologyNode");

            // Process TopologyLink sheet
            Element topologyLinks = doc.createElement("top:TopologyLinks");
            topologyVersion.appendChild(topologyLinks);
            processSheet(workbook, "TopologyLink", topologyLinks, doc, "top2:TopologyLink");

            // Process TopologyAllowedLink sheet
            Element topologyAllowedLinks = doc.createElement("top:TopologyAllowedLinks");
            topologyVersion.appendChild(topologyAllowedLinks);
            processSheet(workbook, "TopologyAllowedLink", topologyAllowedLinks, doc, "top3:TopologyAllowedLink");

            // Process TopologyIncompatibleLink sheet
            Element topologyIncompatibleLinks = doc.createElement("top:TopologyIncompatibleLinks");
            topologyVersion.appendChild(topologyIncompatibleLinks);
            processSheet(workbook, "TopologyIncompatibleLink", topologyIncompatibleLinks, doc, "top4:TopologyIncompatibleLink");

            // Transform to XML and write to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            File outputFile = new File("src/test/java/NetworkTopologyResponse.xml");
            StreamResult result = new StreamResult(outputFile);
            transformer.transform(source, result);

            System.out.println("SOAP response written to file: " + outputFile.getAbsolutePath());

            // Close workbook
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.setTextContent(textContent);
        parent.appendChild(element);
    }

    private static void processSheet(Workbook workbook, String sheetName, Element parent, Document doc, String nodeTagName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) return;

        Iterator<Row> rows = sheet.iterator();
        rows.next(); // Skip header

        while (rows.hasNext()) {
            Row row = rows.next();
            Element node = doc.createElement(nodeTagName);

            for (Cell cell : row) {
                String columnName = sheet.getRow(0).getCell(cell.getColumnIndex()).getStringCellValue();
                String value = cell.toString();
                addElement(doc, node, columnName, value);
            }

            parent.appendChild(node);
        }
    }
}
