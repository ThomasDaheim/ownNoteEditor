package tf.helper.xstreamfx;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;

/**
 * Created at 17/09/11 10:52.<br>
 *
 * @author antoine
 */
public class StringPropertyConverter extends AbstractPropertyConverter<String> implements Converter {

    public StringPropertyConverter(Mapper mapper) {
        super(StringProperty.class, mapper);
    }

    @Override
    protected WritableValue<String> createProperty() {
        return new SimpleStringProperty();
    }

    @Override
    protected Class<? extends String> readType(HierarchicalStreamReader reader) {
        return String.class;
    }
}