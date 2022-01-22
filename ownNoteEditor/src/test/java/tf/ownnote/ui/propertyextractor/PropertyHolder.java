/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.propertyextractor;

import java.util.List;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 *
 * @author thomas
 */
public class PropertyHolder {
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final ObservableList<PropertyHolder> children = 
            FXCollections.<PropertyHolder>observableArrayList(p -> new Observable[]{p.nameProperty(), p.iconNameProperty(), p.colorNameProperty(), p.parentProperty()});
    private final ObjectProperty<PropertyHolder> parentProperty = new SimpleObjectProperty<>(null);
    private final StringProperty iconNameProperty = new SimpleStringProperty();
    private final StringProperty colorNameProperty = new SimpleStringProperty("");

    private PropertyHolder() {
        this("");
    }

    protected PropertyHolder(final String na) {
        nameProperty.set(na);
        
        children.addListener((ListChangeListener.Change<? extends PropertyHolder> change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (PropertyHolder child: change.getAddedSubList()) {
//                        System.out.println("Setting parent: " + this.getName() + " for child: " + child.getName() + ", " + child);
                        child.setParent(this);
                    }
                }

                if (change.wasRemoved()) {
                    for (PropertyHolder child: change.getRemoved()) {
                        // might already have been added to other tag...
                        if (this.equals(child.getParent())) {
//                            System.out.println("Removing parent: " + this.getName() + " for child: " + child.getName() + ", " + child);
                            child.setParent(null);
                        }
                    }
                }
            }
        });
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PropertyHolder))
            return false;
        PropertyHolder other = (PropertyHolder)o;
        // we can't have two tags with same nameProperty...
        return this.nameProperty.get().equals(other.nameProperty.get());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.nameProperty.get());
        return hash;
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public String getName() {
        return nameProperty.get();
    }

    public void setName(final String na) {
        nameProperty.set(na);
    }

    public StringProperty iconNameProperty() {
        return iconNameProperty;
    }

    public String getIconName() {
        return iconNameProperty.get();
    }

    public void setIconName(final String na) {
        iconNameProperty.set(na);
    }

    public StringProperty colorNameProperty() {
        return colorNameProperty;
    }

    public String getColorName() {
        if (!colorNameProperty.get().isEmpty()) {
            return colorNameProperty.get();
        } else {
            return null;
        }
    }

    public void setColorName(final String col) {
        colorNameProperty.set(col);
    }

    public ObservableList<PropertyHolder> getChildren() {
        return children;
    }

    public void setChildren(final List<PropertyHolder> childs) {
        children.setAll(childs);
    }
    
    public ObjectProperty<PropertyHolder> parentProperty() {
        return parentProperty;
    }
    
    public PropertyHolder getParent() {
        return parentProperty.get();
    }

    public void setParent(final PropertyHolder parent) {
        // conveniance in case of e.g. test cases that trigger multiple setParent with the same values
        if (Objects.equals(this.getParent(), parent)) {
            // nothing to do here...
            return;
        }
        
//        if (parent != null) {
//            System.out.println("Setting parent to " + parent.getName() + " for tag " + getName());
//        } else {
//            System.out.println("Setting parent to 'null' for tag " + getName());
//        }
        parentProperty.set(parent);
    }
}
