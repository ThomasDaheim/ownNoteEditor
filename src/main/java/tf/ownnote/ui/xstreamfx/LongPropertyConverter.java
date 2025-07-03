package tf.helper.xstreamfx;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.WritableValue;

/**
 * Created at 17/09/11 11:17.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 */
public class LongPropertyConverter extends AbstractPropertyConverter<Number> implements Converter {

    public LongPropertyConverter(Mapper mapper) {
        super(LongProperty.class, mapper);
    }

    @Override
    protected WritableValue<Number> createProperty() {
        return new SimpleLongProperty();
    }

    @Override
    protected Class<? extends Number> readType(HierarchicalStreamReader reader) {
        return Long.class;
    }
}