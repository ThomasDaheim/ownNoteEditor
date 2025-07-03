package tf.helper.xstreamfx;

import com.sun.javafx.collections.ObservableListWrapper;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
public class ObservableListConverter extends CollectionConverter implements Converter {

    public ObservableListConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return ObservableList.class.isAssignableFrom(type);
    }

    @Override
    protected Object createCollection(Class type) {
        if (type == ObservableListWrapper.class) {
            return FXCollections.observableArrayList();
        }
        if (type.getName().indexOf("$") > 0) {
            if (type.getName().equals("javafx.collections.FXCollections$SynchronizedObservableList")) {
                return FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
            }
        }
        return new SimpleListProperty<>(FXCollections.observableArrayList());
    }
}