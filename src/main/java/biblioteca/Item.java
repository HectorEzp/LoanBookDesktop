package biblioteca;

/** Elemento de combo (id + texto visible). */
public class Item {
    public final int id;
    public final String text;

    public Item(int id, String text) {
        this.id = id;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
