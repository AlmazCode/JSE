package jse.input;

public interface InputSource {
    InputState snapshotAndConsumePressed();
    void clear();
}
