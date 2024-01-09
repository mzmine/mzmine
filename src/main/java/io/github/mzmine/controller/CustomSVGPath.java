package io.github.mzmine.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.shape.SVGPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

public class CustomSVGPath extends SVGPath {
    private StringProperty svgPathFile = new SimpleStringProperty();

    public CustomSVGPath() {
        super();
        svgPathFile.addListener((obs, oldPath, newPath) -> loadSVG(newPath));
    }

    public final String getSvgPathFile() {
        return svgPathFile.get();
    }

    public final void setSvgPathFile(String value) {
        svgPathFile.set(value);
    }

    public StringProperty svgPathFileProperty() {
        return svgPathFile;
    }

    private void loadSVG(String resourcePath) {
        try {
            URL url = new URL(resourcePath);
            // 读取URL内容
            InputStream inputStream = url.openStream();
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            // 解析 SVG 文件并提取路径数据
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            NodeList pathNodes = document.getElementsByTagName("path");
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < pathNodes.getLength(); i++) {
                Element pathElement = (Element) pathNodes.item(i);
                String pathData = pathElement.getAttribute("d");
                buffer.append(pathData);
               // buffer.append(" ");
            }
            this.setContent(buffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
