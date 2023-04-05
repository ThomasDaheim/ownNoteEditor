package tf.helper.xstreamfx;

import com.sun.javafx.collections.ObservableSetWrapper;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * TODO write documentation<br>
 * <br>
 * Created at 21/09/11 09:32.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 * @since 2.2
 * 
 * using implementation from https://stackoverflow.com/a/33298946
 */
public class ObservableSetConverter extends CollectionConverter implements Converter {

    public ObservableSetConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return ObservableSet.class.isAssignableFrom(type);
    }

    @Override
    protected Object createCollection(Class type) {
        if (type == ObservableSetWrapper.class) {
            return FXCollections.observableSet();
        }
        if (type.getName().indexOf("$") > 0) {
            if (type.getName().equals("javafx.collections.FXCollections$SynchronizedObservableSet")) {
                return FXCollections.synchronizedObservableSet(FXCollections.observableSet());
            }
        }
        return new SimpleSetProperty<>(FXCollections.observableSet());
    }
}