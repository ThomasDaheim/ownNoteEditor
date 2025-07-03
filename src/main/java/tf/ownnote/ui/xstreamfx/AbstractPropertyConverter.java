package tf.helper.xstreamfx;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;

/**
 * Base class for all property converters.<br>
 * <br>
 * Created at 17/09/11 10:58.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 */
public abstract class AbstractPropertyConverter<T> implements Converter {

    private final Class clazz;

    protected final Mapper mapper;

    public AbstractPropertyConverter(Class clazz, Mapper mapper) {
        this.clazz = clazz;
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        if (source != null) {
            T value = ((ObservableValue<T>) source).getValue();
            if (value != null) {
                writeValue(writer, context, value);
            }
        }
    }

    protected void writeValue(HierarchicalStreamWriter writer, MarshallingContext context, T value) {
        context.convertAnother(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        WritableValue<T> property = createProperty();
        final T value = (T) context.convertAnother(null, readType(reader));
        property.setValue(value);
        return property;
    }

    protected abstract WritableValue<T> createProperty();

    protected abstract Class<? extends T> readType(HierarchicalStreamReader reader);

    @Override
    @SuppressWarnings("unchecked")
    public boolean canConvert(Class type) {
        return clazz.isAssignableFrom(type);
    }
}
