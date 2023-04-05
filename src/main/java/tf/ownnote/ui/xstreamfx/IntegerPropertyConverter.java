package tf.helper.xstreamfx;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.WritableValue;

/**
 * Created at 17/09/11 11:16.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 */
public class IntegerPropertyConverter extends AbstractPropertyConverter<Number> implements Converter {

    public IntegerPropertyConverter(Mapper mapper) {
        super(IntegerProperty.class, mapper);
    }

    @Override
    protected WritableValue<Number> createProperty() {
        return new SimpleIntegerProperty();
    }

    @Override
    protected Class<? extends Number> readType(HierarchicalStreamReader reader) {
        return Integer.class;
    }
}