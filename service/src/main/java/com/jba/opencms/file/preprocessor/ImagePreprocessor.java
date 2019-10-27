package com.jba.opencms.file.preprocessor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.jba.opencms.type.file.File;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class ImagePreprocessor implements FilePreprocessor {

    private java.util.List<String> acceptedMimeTypes = Arrays.asList("image/jpg", "image/jpeg");

    @Override
    public void accept(File file) {
        if(acceptedMimeTypes.contains(file.getMime())){
            byte[] data = file.getData();
            file.setData(preprocess(data));
        }
        else log.info("Class {} does not apply to file {}", this.getClass().getSimpleName(), file.getName());
    }

    @Data
    private static final class ImageInformation {
        private final int orientation;
        private final int width;
        private final int height;
    }

    private byte[] preprocess(byte[] input){
        try {
            final ImageInformation imageInformation = readImageInformation(input);
            final BufferedImage image = byteArrayToImage(input);
            final BufferedImage result = transformOrientation(image, imageInformation);
            return bufferedImageToByteArray(result);
        }
        catch (IOException e){
            log.error("IO Error occured during read of image!", e);
        }
        catch (MetadataException e){
            log.error("Metadata error occured during reading image!", e);
        }
        catch (ImageProcessingException e){
            log.error("Error during image processing!",e);
        }
        log.error("Fallback to original image bytes...");
        return input;
    }

    private BufferedImage transformOrientation(BufferedImage image, ImageInformation imageInformation){
        AffineTransformOp transformOp = new AffineTransformOp(
                exifDataToTransformation(imageInformation), AffineTransformOp.TYPE_BICUBIC);

        BufferedImage result;

        if(imageInformation.orientation!=1) {
            result = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
        }
        else result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        transformOp.filter(image, result);

        return result;
    }

    private AffineTransform exifDataToTransformation(ImageInformation info){
        AffineTransform t = new AffineTransform();

        switch (info.orientation) {
            case 1:
                break;
            case 2: // Flip X
                t.scale(-1.0, 1.0);
                t.translate(-info.width, 0);
                break;
            case 3: // PI rotation
                t.translate(info.width, info.height);
                t.rotate(Math.PI);
                break;
            case 4: // Flip Y
                t.scale(1.0, -1.0);
                t.translate(0, -info.height);
                break;
            case 5: // - PI/2 and Flip X
                t.rotate(-Math.PI / 2);
                t.scale(-1.0, 1.0);
                break;
            case 6: // -PI/2 and -width
                t.translate(info.height, 0);
                t.rotate(Math.PI / 2);
                break;
            case 7: // PI/2 and Flip
                t.scale(-1.0, 1.0);
                t.translate(-info.height, 0);
                t.translate(0, info.width);
                t.rotate(  3 * Math.PI / 2);
                break;
            case 8: // PI / 2
                t.translate(0, info.width);
                t.rotate(  3 * Math.PI / 2);
                break;
        }

        return t;
    }

    private ImageInformation readImageInformation(byte[] input)  throws IOException, MetadataException, ImageProcessingException {
        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(input));
        Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);

        int orientation = 1;
        try {
            if(directory!=null) {
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
            else throw new MetadataException("Exif data of provided image is empty!");
        } catch (MetadataException me) {
            log.warn("Could not get orientation. "+me.getMessage());
        }
        int width = jpegDirectory.getImageWidth();
        int height = jpegDirectory.getImageHeight();

        return new ImageInformation(orientation, width, height);
    }

    private BufferedImage byteArrayToImage(byte[] input) throws IOException{
        return ImageIO.read(new ByteArrayInputStream(input));
    }

    private byte[] bufferedImageToByteArray(BufferedImage image) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        return baos.toByteArray();
    }
}