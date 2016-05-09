/**
 * Created by Jeppe Vinberg on 09-05-2016.
 */
public class TextLengthEvent extends TextInsertEvent {

    private int length;

    public TextLengthEvent(int offset, int length){
        super("", -1, 0, offset, "");
        this.length = length;
    }
    @Override
    public int getLength() {
        return length;
    }
}
