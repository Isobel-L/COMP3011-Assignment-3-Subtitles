
package comp3011;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

public class AudioExtractor {

    public static Path extractAudio(Path videoPath)
            throws IOException, InterruptedException {

        if (!Files.isRegularFile(videoPath)) {
            throw new IOException(
                    "Video file does not exist: " + videoPath);
        }

        Path audioPath = Files.createTempFile(
                "comp3011-audio-", ".mp3");

        boolean success = false;

        try {
            try (FFmpegFrameGrabber grabber =
                         new FFmpegFrameGrabber(videoPath.toFile())) {

                grabber.start();

                int sampleRate = grabber.getSampleRate();
                int audioChannels = grabber.getAudioChannels();

                if (audioChannels <= 0 || sampleRate <= 0) {
                    throw new IOException(
                            "Video has no valid audio stream.");
                }

                try (FFmpegFrameRecorder recorder =
                             new FFmpegFrameRecorder(
                                     audioPath.toFile(),
                                     0,
                                     audioChannels)) {

                    recorder.setFormat("mp3");
                    recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
                    recorder.setSampleRate(sampleRate);
                    recorder.setAudioChannels(audioChannels);
                    recorder.setAudioBitrate(128000);

                    recorder.start();

                    int audioFrames = 0;
                    Frame frame;

                    while ((frame = grabber.grabSamples()) != null) {

                        if (frame.samples != null
                                && frame.samples.length > 0) {

                            recorder.recordSamples(
                                    sampleRate,
                                    audioChannels,
                                    frame.samples);

                            audioFrames++;
                        }
                    }

                    if (audioFrames == 0) {
                        throw new IOException(
                                "No audio samples were extracted.");
                    }
                }
            }

            if (Files.size(audioPath) == 0) {
                throw new IOException(
                        "Extracted audio file is empty.");
            }

            System.out.println(
                    "Extracted audio saved at: "
                            + audioPath.toAbsolutePath());

            success = true;
            return audioPath;

        } catch (Exception exception) {
            throw new IOException(
                    "Failed to extract audio from: " + videoPath,
                    exception);

        } finally {
            if (!success) {
                Files.deleteIfExists(audioPath);
            }
        }
    }
}
