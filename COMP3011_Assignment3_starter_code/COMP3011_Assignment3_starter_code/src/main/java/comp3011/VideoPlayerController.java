
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

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Controller for the video player.
 *
 * Connects the view and model, handles playback controls,
 * and displays generated subtitles synchronized with playback.
 */
public class VideoPlayerController {

    private final VideoPlayerModel model;
    private final VideoPlayerView view;
    private final boolean subtitlesRequested;

    private final SubtitleGenerationService subtitleService =
            new SubtitleGenerationService();

    private List<SubtitleCue> subtitleCues = List.of();

    private final BiConsumer<Integer, Integer> videoSizeChangedHandler;

    public VideoPlayerController(
            List<FrameProcessor> frameProcessors,
            boolean audioEnabled,
            boolean subtitlesRequested,
            BiConsumer<Integer, Integer> videoSizeChangedHandler) {

        this.view = new VideoPlayerView();
        this.subtitlesRequested = subtitlesRequested;
        this.videoSizeChangedHandler = videoSizeChangedHandler;

        model = new VideoPlayerModel(
                frameProcessors,
                audioEnabled,
                this::onVideoSizeChanged,
                this::onFrameReady,
                this::onStatusChanged,
                this::onPlaybackStateChanged,
                this::onAudioOutputStateChanged);

        view.getStartOverButton().setOnAction(_ -> startOver());
        view.getBackFiveButton().setOnAction(_ -> backFiveSeconds());
        view.getPausePlayButton().setOnAction(_ -> togglePause());
        view.getForwardFiveButton().setOnAction(
                _ -> forwardFiveSeconds());
        view.getStopButton().setOnAction(_ -> stopAndExit());
        view.getAudioButton().setOnAction(_ -> toggleAudioOutput());

        view.showAudioEnabled(model.isAudioOutputEnabled());
    }

    public void installKeyboardShortcuts(Scene scene) {

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            if (event.getCode() == KeyCode.ESCAPE) {
                stopAndExit();
                event.consume();

            } else if (event.getCode() == KeyCode.SPACE) {
                togglePause();
                event.consume();

            } else if (event.getCode() == KeyCode.LEFT) {
                backFiveSeconds();
                event.consume();

            } else if (event.getCode() == KeyCode.RIGHT) {
                forwardFiveSeconds();
                event.consume();

            } else if (event.getCode() == KeyCode.HOME) {
                startOver();
                event.consume();

            } else if (event.getCode() == KeyCode.A) {
                toggleAudioOutput();
                event.consume();
            }
        });
    }

    public VideoPlayerView getView() {
        return view;
    }

    public void play(File file) {

        view.setPlaybackControlsDisabled(false);

        subtitleCues = List.of();
        view.setSubtitleText("");

        // Begin video playback immediately.
        model.play(file);

        // Only generate subtitles when -S is enabled.
        if (!subtitlesRequested) {
            return;
        }

        // Generate subtitles without blocking video playback.
        subtitleService.generate(file.toPath())
                .whenComplete((cues, error) -> {

                    Platform.runLater(() -> {

                        if (error != null) {
                            System.err.println(
                                    "Subtitle generation failed: "
                                    + error.getMessage());
                            return;
                        }

                        subtitleCues =
                                cues == null ? List.of() : List.copyOf(cues);

                        System.out.println(
                                "Generated " + subtitleCues.size()
                                + " subtitle cues.");

                        // Debug subtitle text and timing.
                        for (SubtitleCue cue : subtitleCues) {
                            System.out.println(
                                    "SUBTITLE DEBUG: text="
                                    + cue.text()
                                    + ", start=" + cue.start()
                                    + ", end=" + cue.end());
                        }

                        // Refresh subtitles immediately after generation.
                        updateSubtitle();
                    });
                });
    }

    public void shutdown() {
        model.shutdown();
    }

    private void startOver() {
        model.startOver();
        updateSubtitle();
    }

    private void backFiveSeconds() {
        model.backFiveSeconds();
        updateSubtitle();
    }

    private void togglePause() {
        model.togglePause();
        updateSubtitle();
    }

    private void forwardFiveSeconds() {
        model.forwardFiveSeconds();
        updateSubtitle();
    }

    private void toggleAudioOutput() {
        model.toggleAudioOutput();
    }

    private void stopAndExit() {
        view.setSubtitleText("");
        model.stopPlayback();
        Platform.exit();
    }

    private void onVideoSizeChanged(int width, int height) {
        videoSizeChangedHandler.accept(width, height);
    }

    private void onFrameReady(Image image) {
        view.setImage(image);
        updateSubtitle();
    }

    private void onStatusChanged(String status) {
        view.setStatus(status);
    }

    private void onPlaybackStateChanged(
            boolean playing, boolean paused) {

        if (playing && !paused) {
            view.showPauseAction();
        } else {
            view.showPlayAction();
        }

        updateSubtitle();
    }

    private void onAudioOutputStateChanged(
            boolean audioOutputEnabled) {

        view.showAudioEnabled(audioOutputEnabled);
    }

    /**
     * Displays the subtitle cue matching the current video time.
     * Clears the subtitle when no cue is active.
     */
    private void updateSubtitle() {

        if (!subtitlesRequested || subtitleCues.isEmpty()) {
            view.setSubtitleText("");
            return;
        }

        double currentTime =
                model.getCurrentPlaybackTimeSeconds();

        String activeText = "";

        for (SubtitleCue cue : subtitleCues) {

            if (cue.isActiveAt(currentTime)) {
                activeText = cue.text();
                break;
            }
        }

        view.setSubtitleText(activeText);
    }
}
