# \# COMP3011 Assignment 3 – Automatic Video Subtitles

# 

# \## Project Overview

# 

# This project extends a JavaFX desktop video player with automatic subtitle generation using a Spring Boot web service.

# 

# The application extracts audio from a selected video, sends it to the backend for transcription, and displays timestamped subtitles synchronized with video playback.

# 

# \## Features

# 

# \- Automatic subtitle generation through a transcription API.

# \- Audio extraction from video files.

# \- Timestamped subtitles synchronized with playback.

# \- Closed-caption (CC) button to show or hide subtitles.

# \- Keyboard shortcut (`C`) to toggle subtitles.

# \- Subtitle caching to reduce repeated transcription requests.

# \- Concurrency protection to prevent duplicate subtitle-generation requests.

# \- Asynchronous subtitle processing to keep the user interface responsive.

# \- Backend error logging for transcription API failures.

# 

# \## System Architecture

# 

# The project consists of two components:

# 

# \*\*JavaFX Desktop Application\*\*

# 

# Responsible for video playback, audio extraction, subtitle requests, caching, and displaying subtitles.

# 

# \*\*Spring Boot Web Application\*\*

# 

# Provides the `/api/v1/subtitles` endpoint and communicates with the transcription service.

# 

# The processing workflow is:

# 

# 1\. The user opens a video in the JavaFX application.

# 2\. Audio is extracted from the video.

# 3\. The audio is submitted to the Spring Boot backend.

# 4\. The backend requests transcription through the configured API.

# 5\. The response is converted into timestamped subtitle cues.

# 6\. The JavaFX application displays the appropriate subtitle during playback.

# 

# \## Technologies

# 

# \- Java 25

# \- JavaFX

# \- Spring Boot

# \- Maven

# \- FFmpeg

# \- REST APIs

# \- JSON

# \- Asynchronous programming using Java CompletableFuture

# 

# \## Project Structure

# 

# `COMP3011\_Assignment3\_starter\_code/COMP3011\_Assignment3\_starter\_code/` — JavaFX desktop application.

# 

# `SpeechToTextWebPage/` — Spring Boot transcription backend.

# 

# \## Running the Application

# 

# 1\. Install the required Java development environment, Maven, and FFmpeg.

# 2\. Configure the Spring Boot application with the required transcription API credentials.

# 3\. Start the Spring Boot backend on port `8080`.

# 4\. Start the JavaFX video player using its Maven configuration.

# 5\. Open a supported video file and initiate subtitle generation.

# 6\. Wait for transcription to complete and view the subtitles during playback.

# 

# API credentials must be configured locally and must not be committed to GitHub.

# 

# \## Subtitle Controls

# 

# The CC button enables or disables subtitle visibility. The `C` keyboard shortcut provides the same functionality.

# 

# Generated subtitles remain associated with the video, allowing them to be displayed during subsequent playback without repeating unnecessary transcription requests.

# 

# \## Testing

# 

# The application has been manually tested for:

# 

# \- Successful communication between the JavaFX application and Spring Boot backend.

# \- Audio extraction and transcription.

# \- Subtitle parsing and synchronization.

# \- Subtitle visibility controls.

# \- Asynchronous subtitle generation.

# 

# For short videos, playback may finish before transcription completes. Restarting playback allows the generated subtitles to be viewed.

# 

# \## Known Limitations

# 

# \- Subtitle generation requires a running backend and access to the transcription API.

# \- Transcription time depends on video duration and API response time.

# \- Subtitle timing and accuracy depend on the transcription response.

# \- The application requires a correctly configured local development environment.

# 

# \## Course

# 

# COMP3011 – Cloud and Concurrent Programming

# 

# Assignment 3 – Automatic Subtitle Generation

