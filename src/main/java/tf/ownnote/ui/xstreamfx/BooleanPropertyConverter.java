package tf.helper.xstreamfx;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.WritableValue;

/**
 * Created at 17/09/11 11:09.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 */
public class BooleanPropertyConverter extends AbstractPropertyConverter<Boolean> implements Converter {

    public BooleanPropertyConverter(Mapper mapper) {
        super(BooleanProperty.class, mapper);
    }

    @Override
    protected WritableValue<Boolean> createProperty() {
        return new SimpleBooleanProperty();
    }

    @Override
    protected Class<? extends Boolean> readType(HierarchicalStreamReader reader) {
        return Boolean.class;
    }
}