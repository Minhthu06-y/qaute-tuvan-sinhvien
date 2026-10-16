package vn.iotstar.qaute.service;

/** Lỗi gắn với 1 field của form để controller đưa vào BindingResult. */
public class FieldException extends RuntimeException {
    private final String field;
    public FieldException(String field, String message) { super(message); this.field = field; }
    public String getField() { return field; }
}

