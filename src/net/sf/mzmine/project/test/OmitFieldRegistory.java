package net.sf.mzmine.project.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OmitFieldRegistory {
	private HashMap <Class,List>omitFields;
	public OmitFieldRegistory(){
		this.omitFields=new HashMap<Class,List>();
	}
	
	public void register(Class cls,String fieldName){
		if (!this.omitFields.containsKey(cls)){
			this.omitFields.put(cls,new ArrayList(0));
		}
		String name;
		for (int i=0;i<this.omitFields.get(cls).size();i++){
			name=(String)this.omitFields.get(cls).get(i);
			if (name.equals(fieldName)){
				return;
			}
		}
		this.omitFields.get(cls).add(fieldName);
	}
	
	public boolean registered(Class cls,String fieldName){
		if (!this.omitFields.containsKey(cls)){
			return false;
		}
		List <String>fields=this.omitFields.get(cls);
		boolean ok;
		for (String field:fields){
			ok=field.equals(fieldName);
			if (ok==true){
				return true;
			}
		}
		return false;
	}
}
