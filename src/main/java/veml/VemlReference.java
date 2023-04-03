package veml;

class VemlReference implements VemlElement {
    VemlElement value;

    public VemlReference(VemlElement value) {
        while(value.getClass().equals(VemlReference.class)) {
            value = ((VemlReference) value).value;
        }
        this.value = value;
    }
}
