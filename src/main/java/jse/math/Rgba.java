package jse.math;

public record Rgba(int red, int green, int blue, int alpha) {
    public Rgba {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255 || alpha < 0 || alpha > 255)
            throw new IllegalArgumentException("Color channels must be between 0 and 255");
    }

    public static Rgba parse(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))
            throw new IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA color");
        return new Rgba(Integer.parseInt(value.substring(1, 3), 16), Integer.parseInt(value.substring(3, 5), 16),
                Integer.parseInt(value.substring(5, 7), 16),
                value.length() == 9 ? Integer.parseInt(value.substring(7, 9), 16) : 255);
    }
}
