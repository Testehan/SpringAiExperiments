package com.testehan.springai.immobiliare.util;

import com.luciad.imageio.webp.WebPWriteParam;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageConverter {

    // Method to convert image to WebP and return an InputStream
    public static InputStream convertToWebPInputStream(InputStream imageInputStream) throws IOException {

        // Read image from input stream
        BufferedImage image = ImageIO.read(imageInputStream);
        if (image == null) {
            throw new IOException("Failed to read image. Input stream may be empty or unsupported format.");
        }

        // Use ByteArrayOutputStream to store the converted image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream imgOutStrm = ImageIO.createImageOutputStream(baos)) {
            // Get the WebP writer
            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
            writer.setOutput(imgOutStrm);

            // Set WebP parameters
            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            writeParam.setCompressionMode(WebPWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(writeParam.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
            writeParam.setCompressionQuality(1.0f); // Adjust quality if needed

            // Write the image
            writer.write(null, new IIOImage(image, null, null), writeParam);
            imgOutStrm.flush();
            writer.dispose(); // Ensure writer is properly disposed

        } catch (Exception e) {
            throw new IOException("Error converting image to WebP", e);
        }

        // Convert the ByteArrayOutputStream to an InputStream
        byte[] imageBytes = baos.toByteArray();
        if (imageBytes.length == 0) {
            throw new IOException("WebP conversion failed. Output is empty.");
        }
        return new ByteArrayInputStream(imageBytes);
    }

}
