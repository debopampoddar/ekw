package ekw.core;

/** Marks code you still have to write. Every stub throws this, naming the chapter of the development guide. */
public final class Todo extends UnsupportedOperationException {
    private static final long serialVersionUID = 1L;

    private Todo(String message) { super(message); }

    /** Usage: {@code throw Todo.chapter(2, "Fingerprint.ofRange");} */
    public static Todo chapter(int chapter, String what) {
        return new Todo("TODO (development guide, chapter " + chapter + "): " + what);
    }
}
