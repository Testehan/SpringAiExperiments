package com.testehan.springai.immobiliare.util;

import com.luciad.imageio.webp.WebPWriteParam;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ImageConverter {

    @Value("classpath:static/images/logo_watermark.jpeg") // Inject image from classpath
    private Resource watermarkResource;

    // Method to convert image to WebP and return an InputStream
    public InputStream convertToWebPInputStream(InputStream imageInputStream) throws IOException {

        var imageWithWatermark = addWatermark(imageInputStream);
        // Read image from input stream
        BufferedImage image = ImageIO.read(imageWithWatermark);
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

    public InputStream addWatermark(InputStream imageInputStream) throws IOException {

        // Read images from input streams
        BufferedImage image = ImageIO.read(imageInputStream);
//        BufferedImage watermark = ImageIO.read(watermarkInputStream);

        BufferedImage watermarkImage = ImageIO.read(watermarkResource.getInputStream());

        // Process image with watermark
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .size(image.getWidth(), image.getHeight()) // Maintain original size
                .watermark(Positions.CENTER, watermarkImage, 0.3f) // 50% opacity
                .outputQuality(0.9)
                .outputFormat("jpg")
                .toOutputStream(baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

}
