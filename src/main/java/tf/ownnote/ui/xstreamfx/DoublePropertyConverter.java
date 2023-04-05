package tf.helper.xstreamfx;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;

/**
 * Created at 17/09/11 11:12.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 */
public class DoublePropertyConverter extends AbstractPropertyConverter<Number> implements Converter {

    public DoublePropertyConverter(Mapper mapper) {
        super(DoubleProperty.class, mapper);
    }

    @Override
    protected WritableValue<Number> createProperty() {
        return new SimpleDoubleProperty();
    }

    @Override
    protected Class<? extends Number> readType(HierarchicalStreamReader reader) {
        return Double.class;
    }
}