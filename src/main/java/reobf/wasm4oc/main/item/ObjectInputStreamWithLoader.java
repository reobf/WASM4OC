package reobf.wasm4oc.main.item;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;

public class ObjectInputStreamWithLoader extends ObjectInputStream
{
    private ClassLoader loader;

    /**
     * Loader must be non-null;
     */

    public ObjectInputStreamWithLoader(InputStream in, ClassLoader loader)
            throws IOException, StreamCorruptedException {

        super(in);
        if (loader == null) {
            throw new IllegalArgumentException("Illegal null argument to ObjectInputStreamWithLoader");
        }
        this.loader = loader;
    }

    /**
     * Use the given ClassLoader rather than using the system class
     */
    @SuppressWarnings("rawtypes")
    protected Class resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {


        try {
            return Class.forName(desc.getName(), false, loader);
        } catch (ClassNotFoundException e) {
        
            return super.resolveClass(desc);
        }
    }
}