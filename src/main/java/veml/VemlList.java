package veml;

import reflection.ClassReflection;

import java.util.ArrayList;

class VemlList extends ArrayList<VemlElement> implements VemlElement {
    private Class<?> type;
    private boolean canChange = true;

    VemlList(String type) {
        if(type == null) {
            this.type = null;
        } else {
            this.type = ClassReflection.getClassByName(type);
            if(this.type == null) throw new IllegalArgumentException();
        }

        canChange = false;
    }

    VemlList() {

    }

    void setType(String type) {
        if(!canChange) throw new IllegalArgumentException();

        if(type == null) this.type = null;
        else this.type = ClassReflection.getClassByName(type);

        canChange = false;
    }

    Class<?> getType() {
        return type;
    }

    @Override
    public VemlElement get(int index) {
        VemlElement value = super.get(index);
        while(value.getClass().equals(VemlReference.class)) {
            value = ((VemlReference) value).value;
        }

        return value;
    }
}
