package io.github.mzmine.modules.io.sqlexport;

public class SQLRowObject {
    String Name;
    SQLExportDataType Type;
    String Value;

    SQLRowObject(String name , String value, SQLExportDataType type){
        this.Name=name;
        this.Value=value;
        this.Type=type;
    }
    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public SQLExportDataType getType() {
        return Type;
    }

    public void setType(SQLExportDataType type) {
        Type = type;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }

    @Override
    public String toString() {
        return "Name:"+Name+" Value:"+Value+" Type:"+Type;
    }
}
