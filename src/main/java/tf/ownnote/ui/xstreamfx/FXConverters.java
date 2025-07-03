package tf.helper.xstreamfx;

import com.thoughtworks.xstream.XStream;

/**
 * Utility to configure a xStream with JavaFX property converters.<br>
 * <br>
 * Created at 17/09/11 11:18.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 */
public class FXConverters {
    public static void configure(XStream xStream) {
        xStream.registerConverter(new StringPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new BooleanPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new ObjectPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new DoublePropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new LongPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new IntegerPropertyConverter(xStream.getMapper()));
        xStream.registerConverter(new ObservableListConverter(xStream.getMapper()));
    }
}