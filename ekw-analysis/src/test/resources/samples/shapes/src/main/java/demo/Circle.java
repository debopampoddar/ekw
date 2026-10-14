package demo;

/**
 * A circle given by its radius.
 */
public record Circle(double radius) implements Shape {
    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}
