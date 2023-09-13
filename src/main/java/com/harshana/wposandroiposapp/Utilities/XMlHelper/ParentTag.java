package com.harshana.wposandroiposapp.Utilities.XMlHelper;

import java.util.LinkedList;
import java.util.Queue;

public class ParentTag extends Tag {

	Queue<Tag> children;
	
	public ParentTag(String _name)
	{
		tagName = _name;
		children = new LinkedList<>();
	}
	
	
	public void insertChild(Tag child)
	{
		children.add(child);
	}
	
	public String getXMLString()
	{

		//parse its child as a string 
		String xmlChild = "";
		
		for (Tag t : children)
			xmlChild += t.getXMLString() ;

		
		//return  "<" + tagName + ">\n"  + xmlChild + "</" + tagName + ">\n";
		return  "<" + tagName + ">"  + xmlChild + "</" + tagName + ">";
	}
}
