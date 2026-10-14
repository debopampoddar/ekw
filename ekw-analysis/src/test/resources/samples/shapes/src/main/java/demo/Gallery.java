package demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds shapes and reports on them. Used to test call-site flags.
 */
public class Gallery {
    private final List<Shape> shapes = new ArrayList<>();

    public Gallery add(Shape shape) {
        shapes.add(shape);
        return this;
    }

    public double totalArea() {
        return shapes.stream().mapToDouble(Shape::area).sum();
    }

    public void print() {
        shapes.forEach(s -> System.out.println(describe(s)));
    }

    static String describe(Shape s) {
        if (s instanceof Circle c) {
            return "circle " + c.radius();
        }
        return "shape " + s.area();
    }

    public static void main(String[] args) {
        new Gallery().add(new Circle(1)).add(new Square(2)).print();
    }
}
