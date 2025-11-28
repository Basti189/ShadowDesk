package de.wolfsline.shadowdesk.core;

public record MonitorInfo(int index, String description, int minBrightness, int currentBrightness, int maxBrightness,
                          boolean primary) {

    public double getBrightnessPercent() {
        if (maxBrightness <= minBrightness) return 0.0;
        return 100.0 * (currentBrightness - minBrightness) / (maxBrightness - minBrightness);
    }

    @Override
    public String toString() {
        return String.format(
                "[%d] %s %s | min=%d cur=%d max=%d (≈ %.1f %%)",
                index,
                description,
                primary ? "(PRIMARY)" : "",
                minBrightness,
                currentBrightness,
                maxBrightness,
                getBrightnessPercent()
        );
    }
}