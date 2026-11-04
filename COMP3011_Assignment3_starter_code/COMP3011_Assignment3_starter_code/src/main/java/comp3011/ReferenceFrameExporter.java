/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. <student name and student number insert here upon modification>
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * Exports a processed reference frame without opening the JavaFX player window.
 *
 * <p>
 * This follows the same frame-processing order used by {@link VideoPlayerModel}:
 * decode an image frame, build its {@link FrameInfo}, run the configured
 * {@link FrameProcessor}s, then convert the processed JavaCV frame through
 * {@link JavaFXFrameConverter}.
 * </p>
 */
public class ReferenceFrameExporter {
    private ReferenceFrameExporter() {
    }

    public static File export(File videoFile, int targetFrameNumber, List<FrameProcessor> frameProcessors)
            throws Exception {
        File outputFile = new File("reference-frame-" + targetFrameNumber + ".png");

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile);
                JavaFXFrameConverter converter = new JavaFXFrameConverter()) {
            grabber.setImageMode(FrameGrabber.ImageMode.COLOR);
            grabber.setSampleMode(FrameGrabber.SampleMode.SHORT);
            grabber.start();

            Frame frame = findFrame(grabber, targetFrameNumber);
            if (frame == null) {
                throw new IllegalArgumentException("Frame " + targetFrameNumber + " not found");
            }

            initialiseFrameProcessors(videoFile, grabber, frame, frameProcessors);
            FrameInfo frameInfo = new FrameInfo(targetFrameNumber, grabber.getTimestamp());
            processFrame(frame, frameInfo, frameProcessors);

            Image image = converter.convert(frame);
            if (image == null) {
                throw new IllegalStateException("Could not convert frame " + targetFrameNumber + " to an image");
            }

            BufferedImage bufferedImage = toBufferedImage(image);
            if (!ImageIO.write(bufferedImage, "png", outputFile)) {
                throw new IllegalStateException("No PNG writer is available");
            }

            return outputFile;
        }
    }

    private static Frame findFrame(FFmpegFrameGrabber grabber, int targetFrameNumber) throws Exception {
        int seekFrameNumber = Math.max(0, targetFrameNumber - 1);
        grabber.setFrameNumber(seekFrameNumber);

        while (true) {
            Frame frame = grabFrame(grabber);
            if (frame == null) {
                return null;
            }

            if (frame.image == null) {
                continue;
            }

            int frameNumber = grabber.getFrameNumber();
            if (frameNumber == targetFrameNumber) {
                return frame;
            }
            if (frameNumber > targetFrameNumber) {
                return null;
            }
        }
    }

    private static Frame grabFrame(FFmpegFrameGrabber grabber) throws Exception {
        if (grabber.hasAudio()) {
            return grabber.grab();
        }
        return grabber.grabImage();
    }

    private static void initialiseFrameProcessors(
            File videoFile,
            FFmpegFrameGrabber grabber,
            Frame frame,
            List<FrameProcessor> frameProcessors) throws Exception {
        VideoInfo videoInfo = new VideoInfo(
                mediaName(videoFile),
                grabber.getLengthInVideoFrames(),
                frame.imageWidth,
                frame.imageHeight,
                frame.imageDepth,
                frame.imageChannels,
                frame.imageStride,
                grabber.getFrameRate(),
                (int) Math.round(grabber.getFrameRate()),
                frameDurationUs(grabber.getFrameRate()),
                grabber.getPixelFormat());

        for (FrameProcessor processor : frameProcessors) {
            processor.initialise(videoInfo);
        }
    }

    private static int frameDurationUs(double frameRate) {
        if (frameRate <= 0) {
            return 0;
        }
        return (int) Math.round(1_000_000.0 / frameRate);
    }

    private static void processFrame(Frame frame, FrameInfo frameInfo, List<FrameProcessor> frameProcessors)
            throws Exception {
        for (FrameProcessor processor : frameProcessors) {
            processor.process(frame, frameInfo);
        }
    }

    private static BufferedImage toBufferedImage(Image image) {
        int width = (int) Math.round(image.getWidth());
        int height = (int) Math.round(image.getHeight());
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }

        return bufferedImage;
    }

    private static String mediaName(File file) {
        String fileName = file.getName();
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart <= 0) {
            return fileName;
        }
        return fileName.substring(0, extensionStart);
    }
}
