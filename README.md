# COMP3011 Assignment 3 - Automatic Video Subtitles

## Project Overview

This project extends a JavaFX desktop video player with automatic subtitle generation using a Spring Boot backend and an OpenAI transcription service.

The desktop application extracts audio from a selected video, sends it to the backend, and displays timestamped subtitles synchronized with video playback.

## Features

- Automatic subtitle generation from video audio
- Timestamped subtitle display synchronized with playback
- Closed-caption (CC) button and S keyboard shortcut
- Command-line options to enable subtitles at startup
- Subtitle caching to avoid unnecessary repeat requests
- Protection against duplicate concurrent subtitle-generation requests
- Asynchronous processing to keep the JavaFX interface responsive
- Backend usage tracking and error handling

## Technologies

- Java 25
- JavaFX
- Spring Boot 4.0.5
- Maven
- FFmpeg
- REST APIs and JSON
- OpenAI transcription API

## Project Structure

- `COMP3011_Assignment3_starter_code/COMP3011_Assignment3_starter_code/` - JavaFX desktop application
- `SpeechToTextWebPage/` - Spring Boot transcription backend

Both components contain their own Maven `pom.xml`.

## System Architecture

1. The user selects a video in the JavaFX application.
2. The application extracts the audio using FFmpeg.
3. The audio is submitted to the Spring Boot backend.
4. The backend sends the audio to the configured transcription API.
5. The transcription response is converted into timestamped subtitle cues.
6. The JavaFX application displays the appropriate subtitles during playback.

Subtitle generation runs asynchronously to avoid blocking the user interface.

## Requirements

- Java Development Kit 25
- Apache Maven
- FFmpeg available to the application
- An OpenAI API key with transcription API access
- Internet access for transcription requests

## Running the Backend

Configure the `OPENAI_API_KEY` environment variable before starting the backend.

In PowerShell:

```powershell
$env:OPENAI_API_KEY = Read-Host "Enter your OpenAI API key"
mvn -f .\SpeechToTextWebPage\pom.xml spring-boot:run
```

If Maven is not on PATH, use the full path to `mvn.cmd`.

The backend runs on `http://localhost:8080` by default.

To check that it is responding:

```powershell
Invoke-WebRequest http://localhost:8080/api/v1/global/usage -UseBasicParsing
```

A successful request returns HTTP 200 and a JSON usage count.

The backend can alternatively be launched through Spring Tools for Eclipse with `OPENAI_API_KEY` configured in its run environment.

Do not commit API keys or other credentials to version control.

## Running the JavaFX Application

Start the Spring Boot backend first.

From the repository root:

```powershell
mvn -f .\COMP3011_Assignment3_starter_code\COMP3011_Assignment3_starter_code\pom.xml javafx:run
```

If Maven is not on PATH, use the full path to `mvn.cmd`.

### Command-Line Subtitle Options

The following options enable subtitles at startup:

- `-S`
- `--subtitle-frames`

Example:

```powershell
mvn -f .\COMP3011_Assignment3_starter_code\COMP3011_Assignment3_starter_code\pom.xml javafx:run "-Djavafx.args=-S C:\Videos\example.mp4"
```

Replace the example path with the path to an existing video.

## Subtitle Controls

- **CC button:** Toggle subtitle visibility.
- **S key:** Toggle subtitles using the keyboard.
- **-S:** Enable subtitles at startup.
- **--subtitle-frames:** Enable subtitles at startup.

When subtitles are enabled, the application generates them if they are not already cached.

Generated subtitles are retained for subsequent playback of the same video during the application session.

For short videos, transcription may finish after playback ends. Restart playback to view the generated subtitles.

## Testing

The implementation has been tested for:

- JavaFX compilation and automated tests
- Spring Boot compilation and automated tests
- Backend HTTP availability
- Audio extraction and transcription
- Subtitle parsing and synchronization
- CC button and S keyboard shortcut
- Both command-line subtitle options
- Subtitle caching and asynchronous generation

The JavaFX test suite completed with 4 passing tests.

The Spring Boot test suite completed with 7 passing tests, including a concurrent request stress test.

## Known Limitations

- The backend must be running before transcription requests can succeed.
- Transcription requires valid API credentials and internet access.
- Subtitle generation time depends on video length and API response time.
- Subtitle accuracy and timing depend on the transcription result.
- The backend limits multipart uploads to 25 MB.
- The application requires a correctly configured Java and FFmpeg environment.

## Course Information

COMP3011 - Cloud and Concurrent Programming

Assignment 3 - Automatic Video Subtitles
