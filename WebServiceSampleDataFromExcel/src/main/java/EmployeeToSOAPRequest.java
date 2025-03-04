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
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

public class EmployeeToSOAPRequest {

    public static void main(String[] args) {
        try {
            // Load Excel file
            String filePath = "src/test/java/Employee.xlsx"; // Replace with the actual path
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
            envelope.setAttribute("xmlns:quin", "http://www.quintiq.com/");
            doc.appendChild(envelope);

            // Create Header
            Element header = doc.createElement("soapenv:Header");
            envelope.appendChild(header);

            // Create Body
            Element body = doc.createElement("soapenv:Body");
            envelope.appendChild(body);

            // Create ImportEmployee
            Element importEmployee = doc.createElement("quin:ImportEmployee");
            body.appendChild(importEmployee);

            // Add Header inside ImportEmployee
            Element headerSection = doc.createElement("Header");
            importEmployee.appendChild(headerSection);

            addElement(doc, headerSection, "TimeStamp", "2024-12-16T12:00:00Z");
            addElement(doc, headerSection, "InterfaceID", "EMP001");
            addElement(doc, headerSection, "MessageID", "MSG12345");

            // Add Employees section
            Element employees = doc.createElement("Employees");
            importEmployee.appendChild(employees);

            // Process Employee data
            Sheet employeeSheet = workbook.getSheet("Employee"); // Replace with the exact sheet name
            Iterator<Row> rows = employeeSheet.iterator();
            rows.next(); // Skip the header row

            while (rows.hasNext()) {
                Row row = rows.next();
                Element employee = doc.createElement("Employee");
                employees.appendChild(employee);

                // Employee details
                addElement(doc, employee, "ID", getCellValue(row.getCell(0))); // ID
                addElement(doc, employee, "FirstName", getCellValue(row.getCell(1))); // FirstName
                addElement(doc, employee, "MiddleName", getCellValue(row.getCell(2))); // MiddleName
                addElement(doc, employee, "LastName", getCellValue(row.getCell(3))); // LastName
                addElement(doc, employee, "Initials", getCellValue(row.getCell(4))); // Initials
                addElement(doc, employee, "Gender", getCellValue(row.getCell(5))); // Gender
                addElement(doc, employee, "HireDate", getCellValue(row.getCell(6))); // HireDate
                addElement(doc, employee, "SeniorityDate", "0001-01-01");
                addElement(doc, employee, "ResignationDate", "9999-12-31");
                addElement(doc, employee, "Phone1", getCellValue(row.getCell(7))); // Phone1
                addElement(doc, employee, "Email1", getCellValue(row.getCell(8))); // Email1

                // Add EmployeeWorkingUnits
                Element employeeWorkingUnits = doc.createElement("EmployeeWorkingUnits");
                employee.appendChild(employeeWorkingUnits);
                processSheet(workbook, "EmployeeWorkingUnit", employeeWorkingUnits, doc, "WorkingUnitID", "EmployeeWorkingUnit");

                // Add EmployeeWorkingUnitRoles
                Element employeeWorkingUnitRoles = doc.createElement("EmployeeWorkingUnitRoles");
                employee.appendChild(employeeWorkingUnitRoles);
                processSheet(workbook, "EmployeeWorkingUnitRole", employeeWorkingUnitRoles, doc, "RoleID", "EmployeeWorkingUnitRole");
            }

            // Transform to XML and write to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            File outputFile = new File("src/test/java/EmployeeSOAPRequest.xml");
            StreamResult result = new StreamResult(outputFile);
            transformer.transform(source, result);

            System.out.println("SOAP request written to file: " + outputFile.getAbsolutePath());

            // Close workbook
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.setTextContent(textContent != null ? textContent : "");
        parent.appendChild(element);
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private static void processSheet(Workbook workbook, String sheetName, Element parent, Document doc, String matchingColumn, String nodeTagName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) return;

        Iterator<Row> rows = sheet.iterator();
        rows.next(); // Skip header

        while (rows.hasNext()) {
            Row row = rows.next();
            Element node = doc.createElement(nodeTagName);

            for (Cell cell : row) {
                String columnName = sheet.getRow(0).getCell(cell.getColumnIndex()).getStringCellValue();
                String value = getCellValue(cell);
                addElement(doc, node, columnName, value);
            }

            parent.appendChild(node);
        }
    }
}
